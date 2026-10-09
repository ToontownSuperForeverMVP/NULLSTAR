package com.nullstar.server;

import com.nullstar.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public final class EntityEffects {
    private EntityEffects() {}
    public static void impact(ServerLevel level,UUID owner,BlockPos pos,int radius,float damage,double knockback,ServerConfig config) {
        impactRadius(level,owner,pos,radius*1.5,damage,knockback,config);
    }
    public static void impactRadius(ServerLevel level,UUID owner,BlockPos pos,double radius,float damage,double knockback,ServerConfig config) {
        Vec3 center=Vec3.atCenterOf(pos);int processed=0;
        var actor=level.getServer().getPlayerList().getPlayer(owner);
        var source=level.damageSources().explosion(actor,actor);
        for(var entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(pos).inflate(radius),e->eligible(e))) {
            if(++processed>config.entityBudget)break;
            if(!ProtectionHooks.allows(level,owner,entity.blockPosition()))continue;
            if(!allowsPlayerEffect(level,actor,entity))continue;
            Vec3 delta=entity.position().subtract(center);double distance=delta.length();double falloff=Math.max(0,1-distance/radius);
            if(falloff==0)continue;
            entity.hurtServer(level,source,(float)(damage*falloff));
            Vec3 direction=distance<.01?new Vec3(0,1,0):delta.scale(1/distance);
            entity.push(direction.x*knockback*falloff,Math.max(.1,direction.y*knockback*falloff),direction.z*knockback*falloff);
            entity.syncVelocity=true;
        }
    }
    public static void attract(ServerLevel level,UUID owner,BlockPos pos,int radius,ServerConfig config) {
        attractRadius(level,owner,pos,radius*2,config);
    }
    public static void attractRadius(ServerLevel level,UUID owner,BlockPos pos,double radius,ServerConfig config) {
        Vec3 center=Vec3.atCenterOf(pos).add(0,3,0);int processed=0;
        var actor=level.getServer().getPlayerList().getPlayer(owner);
        for(var entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(pos).inflate(radius),e->eligible(e))) {
            if(++processed>config.entityBudget)break;
            if(!ProtectionHooks.allows(level,owner,entity.blockPosition()))continue;
            if(!allowsPlayerEffect(level,actor,entity))continue;
            Vec3 delta=center.subtract(entity.position());double distance=delta.length();
            if(distance<.5 || distance>radius)continue;
            Vec3 impulse=delta.scale(Math.min(.24,.045+1/(distance+2))/distance);
            Vec3 velocity=entity.getDeltaMovement().add(impulse);
            if(velocity.lengthSqr()>1)velocity=velocity.normalize();
            entity.setDeltaMovement(velocity);entity.syncVelocity=true;
        }
    }
    private static boolean allowsPlayerEffect(ServerLevel level,Player actor,LivingEntity entity) {
        if(!(entity instanceof Player player))return true;
        return actor!=null && (actor.getUUID().equals(player.getUUID()) || level.isPvpAllowed() && actor.canHarmPlayer(player));
    }
    private static boolean eligible(LivingEntity entity) {
        return entity.isAlive() && !entity.isRemoved() && !entity.isSpectator() && !(entity instanceof Player player && player.isCreative());
    }
}
