package com.nullstar.server;

import com.nullstar.Nullstar;
import com.nullstar.core.WeaponCatalog;
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
            root.then(Commands.literal("cancel").executes(ctx->{
                int count=Nullstar.engine().cancelOwner(ctx.getSource().getPlayerOrException().getUUID(),ctx.getSource().getServer());
                ctx.getSource().sendSuccess(()->Component.literal(count==0?"No active NULLSTAR strike to cancel.":"Cancelled "+count+" NULLSTAR strike(s)."),false);
                return count;
            }));
            root.then(Commands.literal("status").executes(ctx->{
                var engine=Nullstar.engine();ctx.getSource().sendSuccess(()->Component.literal("NULLSTAR: "+engine.active().size()+" active strikes; "+
                        engine.destruction().queued()+" jobs; last tick "+engine.destruction().lastTickOperations()+" candidates."),false);return 1;
            }));
            root.then(Commands.literal("reload").requires(NullstarCommands::operator).executes(ctx->{
                if(!Nullstar.engine().active().isEmpty()){ctx.getSource().sendFailure(Component.literal("Cancel active strikes before reloading."));return 0;}
                try{Nullstar.reload();ctx.getSource().sendSuccess(()->Component.literal("NULLSTAR configuration reloaded."),false);return 1;}
                catch(RuntimeException exception){ctx.getSource().sendFailure(Component.literal(exception.getMessage()));Nullstar.LOGGER.error("Reload failed; prior configuration retained",exception);return 0;}
            }));
            dispatcher.register(root);
        });
    }
    private static boolean operator(CommandSourceStack source){return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);}
    private static LiteralArgumentBuilder<CommandSourceStack> strike(String name,boolean preview) {
        var root=Commands.literal(name);
        if(!preview)root.requires(NullstarCommands::operator);
        root.then(Commands.argument("weapon",StringArgumentType.word()).suggests((context,builder)-> {
            WeaponCatalog.IDS.forEach(builder::suggest);return builder.buildFuture();
        }).executes(ctx->{
            var player=ctx.getSource().getPlayerOrException();var id=StringArgumentType.getString(ctx,"weapon");
            if(!WeaponCatalog.IDS.contains(id)){ctx.getSource().sendFailure(Component.literal("Unknown weapon."));return 0;}
            var def=Nullstar.catalog().get(id);BlockPos target=TargetingService.acquire(player,def.targeting().range(),def.targeting().groundRequired());
            if(target==null){ctx.getSource().sendFailure(Component.literal("Aim at a loaded surface."));return 0;}
            return Nullstar.engine().activate(player,id,target,preview)?1:0;
        }).then(Commands.argument("position",BlockPosArgument.blockPos()).executes(ctx->Nullstar.engine().activate(ctx.getSource().getPlayerOrException(),
                StringArgumentType.getString(ctx,"weapon"),BlockPosArgument.getBlockPos(ctx,"position"),preview)?1:0)));
        return root;
    }
}
