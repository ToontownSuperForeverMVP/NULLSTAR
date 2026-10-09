package com.nullstar.server;

import com.nullstar.Nullstar;
import com.nullstar.core.WeaponCatalog;
import com.nullstar.core.ScaleTier;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;

public final class NullstarCommands {
    private NullstarCommands() {}
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher,registries,environment)-> {
            var root=Commands.literal("nullstar");
            root.then(Commands.literal("give").requires(NullstarCommands::operator).executes(ctx->{
                ctx.getSource().getPlayerOrException().getInventory().add(new ItemStack(Nullstar.CONTROLLER));return 1;
            }));
            root.then(Commands.literal("target").executes(ctx->{Nullstar.engine().acquire(ctx.getSource().getPlayerOrException());return 1;}));
            root.then(strike("fire",false));root.then(strike("preview",true));
            var scale=Commands.literal("scale").requires(NullstarCommands::operator);
            for(var tier:ScaleTier.values())scale.then(Commands.literal(tier.name().toLowerCase(java.util.Locale.ROOT)).executes(ctx->{
                Nullstar.engine().scale(ctx.getSource().getPlayerOrException(),tier);return 1;
            }));
            root.then(scale);
            root.then(Commands.literal("confirm").requires(NullstarCommands::operator).executes(ctx->Nullstar.engine().confirm(ctx.getSource().getPlayerOrException())?1:0));
            root.then(Commands.literal("abort").requires(NullstarCommands::operator).then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                try{return Nullstar.engine().abort(java.util.UUID.fromString(StringArgumentType.getString(ctx,"id")),ctx.getSource().getServer())?1:0;}
                catch(IllegalArgumentException exception){ctx.getSource().sendFailure(Component.literal("Invalid strike UUID."));return 0;}
            })));
            root.then(Commands.literal("emergency_abort").requires(NullstarCommands::operator).executes(ctx->{
                int count=Nullstar.engine().emergencyAbort(ctx.getSource().getServer());ctx.getSource().sendSuccess(()->Component.literal("Aborted "+count+" events/jobs."),true);return count;
            }));
            root.then(Commands.literal("diagnostics").requires(NullstarCommands::operator).executes(ctx->{
                var data=Nullstar.engine().destruction().diagnostics();
                data.forEach(d->{ctx.getSource().sendSuccess(()->Component.literal(d.toString()),false);Nullstar.LOGGER.info("NULLSTAR diagnostics: {}",d);});
                return data.size();
            }));
            var bounds=Commands.literal("bounds").requires(NullstarCommands::operator);
            for(String weapon:WeaponCatalog.IDS) {
                var weaponNode=Commands.literal(weapon);
                for(var tier:ScaleTier.values())weaponNode.then(Commands.literal(tier.name().toLowerCase(java.util.Locale.ROOT))
                        .executes(ctx->bounds(ctx.getSource(),weapon,tier,null))
                        .then(Commands.argument("position",BlockPosArgument.blockPos()).executes(ctx->bounds(ctx.getSource(),weapon,tier,BlockPosArgument.getBlockPos(ctx,"position")))));
                bounds.then(weaponNode);
            }
            root.then(bounds);
            root.then(Commands.literal("cancel").executes(ctx->{
                int count=Nullstar.engine().cancelOwner(ctx.getSource().getPlayerOrException().getUUID(),ctx.getSource().getServer());
                ctx.getSource().sendSuccess(()->Component.literal(count==0?"No active NULLSTAR strike to cancel.":"Cancelled "+count+" NULLSTAR strike(s)."),false);
                return count;
            }));
            root.then(Commands.literal("status").executes(ctx->{
                var engine=Nullstar.engine();ctx.getSource().sendSuccess(()->Component.literal("NULLSTAR: "+engine.active().size()+" active strikes; "+
                        engine.destruction().queued()+" jobs; last tick "+engine.destruction().lastTickOperations()+" candidates."),false);
                engine.destruction().diagnostics().stream().filter(d->d.active()>0).forEach(d->ctx.getSource().sendSuccess(()->Component.literal(
                        d.strike()+" "+String.format(java.util.Locale.ROOT,"%.2f%%",100*d.scanned()/(double)Math.max(1,d.total()))
                        +"; "+d.changed()+" changes; "+d.active()+" jobs; "+d.milliseconds()+" ms"),false));return 1;
            }));
            root.then(Commands.literal("reload").requires(NullstarCommands::operator).executes(ctx->{
                if(!Nullstar.engine().active().isEmpty()||Nullstar.engine().destruction().queued()>0){ctx.getSource().sendFailure(Component.literal("Cancel active strikes/jobs before reloading."));return 0;}
                try{Nullstar.reload();ctx.getSource().sendSuccess(()->Component.literal("NULLSTAR configuration reloaded."),false);return 1;}
                catch(RuntimeException exception){ctx.getSource().sendFailure(Component.literal(exception.getMessage()));Nullstar.LOGGER.error("Reload failed; prior configuration retained",exception);return 0;}
            }));
            dispatcher.register(root);
        });
    }
    private static boolean operator(CommandSourceStack source){return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);}
    private static int bounds(CommandSourceStack source,String weapon,ScaleTier tier,BlockPos explicit) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player=source.getPlayerOrException();var base=Nullstar.catalog().get(weapon);
        var target=explicit==null?TargetingService.acquire(player,base.targeting().range(),base.targeting().groundRequired()):explicit;
        if(target==null||!TargetingService.valid(player,target,base.targeting().range(),base.targeting().groundRequired())) {
            source.sendFailure(Component.literal("Aim at or specify a loaded surface within range."));return 0;
        }
        var profile=Nullstar.scales().get(weapon,tier,base);var cfg=Nullstar.config();int radius=Math.min(profile.radius(),cfg.radiusCap(tier));
        int depth=Math.min(profile.depth(),cfg.depthCap(tier));var definition=profile.apply(base);
        if(definition.impact().shape().equals("sphere"))radius=Math.min(radius,depth);
        final int r=radius;
        source.sendSuccess(()->Component.literal("Inert footprint "+weapon+" "+tier+": main diameter "+r*2+", depth "+depth
                +"; "+profile.subimpacts()+" subimpacts; "+profile.rings()+". Public caps: "+cfg.publicServerMode+". Heights are relative to each resolved surface."),false);
        for(var cue:definition.events())if(cue.effect().equals("impact")) {
            var geometry=com.nullstar.core.CraterGeometry.forCue(definition.impact(),radius,depth,cfg.radiusCap(tier),0,cue);
            if(geometry==null){source.sendSuccess(()->Component.literal("Wave "+cue.wave()+" clipped empty"),false);continue;}
            var offset=tier==ScaleTier.STANDARD?new com.nullstar.core.ImpactFootprint.Offset((int)Math.round(cue.x()),(int)Math.round(cue.z()))
                    :com.nullstar.core.ImpactFootprint.offset(cue.x(),cue.z(),profile.radius(),radius,geometry.radius(),cfg.radiusCap(tier));
            BlockPos center=target.offset(offset.x(),0,offset.z());
            source.sendSuccess(()->Component.literal("Wave "+cue.wave()+" center "+center.toShortString()+" outer "+geometry.radius()+" inner "+geometry.innerRadius()+" depth "+geometry.floor()
                    +" bounds "+center.offset(-geometry.radius(),-geometry.floor()-1,-geometry.radius()).toShortString()+".."
                    +center.offset(geometry.radius(),geometry.ceiling(),geometry.radius()).toShortString()
                    +"; "+new com.nullstar.core.DestructionJob(geometry).total()+" candidates"),false);
        }
        return 1;
    }
    private static LiteralArgumentBuilder<CommandSourceStack> strike(String name,boolean preview) {
        var root=Commands.literal(name);
        if(!preview)root.requires(NullstarCommands::operator);
        var weapon=Commands.argument("weapon",StringArgumentType.word()).suggests((context,builder)-> {
            WeaponCatalog.IDS.forEach(builder::suggest);return builder.buildFuture();
        }).executes(ctx->{
            var player=ctx.getSource().getPlayerOrException();var id=StringArgumentType.getString(ctx,"weapon");
            if(!WeaponCatalog.IDS.contains(id)){ctx.getSource().sendFailure(Component.literal("Unknown weapon."));return 0;}
            var def=Nullstar.catalog().get(id);BlockPos target=TargetingService.acquire(player,def.targeting().range(),def.targeting().groundRequired());
            if(target==null){ctx.getSource().sendFailure(Component.literal("Aim at a loaded surface."));return 0;}
            return Nullstar.engine().activate(player,id,target,preview)?1:0;
        }).then(Commands.argument("position",BlockPosArgument.blockPos()).executes(ctx->Nullstar.engine().activate(ctx.getSource().getPlayerOrException(),
                StringArgumentType.getString(ctx,"weapon"),BlockPosArgument.getBlockPos(ctx,"position"),preview)?1:0));
        for(var tier:ScaleTier.values()) {
            var tierNode=Commands.literal(tier.name().toLowerCase(java.util.Locale.ROOT)).executes(ctx->{
                var player=ctx.getSource().getPlayerOrException();var id=StringArgumentType.getString(ctx,"weapon");
                if(!WeaponCatalog.IDS.contains(id))return 0;
                var def=Nullstar.catalog().get(id);var target=TargetingService.acquire(player,def.targeting().range(),def.targeting().groundRequired());
                if(target==null){ctx.getSource().sendFailure(Component.literal("Aim at a loaded surface."));return 0;}
                return Nullstar.engine().activate(player,id,target,preview,tier)?1:0;
            }).then(Commands.argument("position",BlockPosArgument.blockPos()).executes(ctx->Nullstar.engine().activate(ctx.getSource().getPlayerOrException(),
                    StringArgumentType.getString(ctx,"weapon"),BlockPosArgument.getBlockPos(ctx,"position"),preview,tier)?1:0));
            weapon.then(tierNode);
        }
        root.then(weapon);
        return root;
    }
}
