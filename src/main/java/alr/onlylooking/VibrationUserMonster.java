package alr.onlylooking;

import alr.onlylooking.api.IHearSound;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;

import javax.annotation.Nullable;

public class VibrationUserMonster implements VibrationSystem.User {
    private final PositionSource positionSource;
    private final PathfinderMob pathfinderMob;

    public VibrationUserMonster(PathfinderMob pathfinderMob) {
        this.positionSource = new EntityPositionSource(pathfinderMob, pathfinderMob.getEyeHeight());
        this.pathfinderMob = pathfinderMob;
    }

    @Override
    public int getListenerRadius() {
        return ModConfigs.COMMON.VIBRATION_RANGE.get();
    }

    @Override
    public PositionSource getPositionSource() {
        return this.positionSource;
    }

    @Override
    public TagKey<GameEvent> getListenableEvents() {
        return ModTags.GameEvents.MONSTER_CAN_LISTEN;
    }

    @Override
    public boolean canReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, GameEvent.Context context) {
        if (LookUtils.isVibrationAvaiable(this.pathfinderMob)) {
            if (!this.pathfinderMob.isNoAi() && this.pathfinderMob instanceof IHearSound iHearSound
                    && !(this.pathfinderMob instanceof Warden) && iHearSound.getSoundCooldown() <= 0
                    && level.getWorldBorder().isWithinBounds(pos)) {
                Entity entity = context.sourceEntity();
                if ((entity instanceof Enemy) || !(this.pathfinderMob instanceof Enemy)
                        || (entity instanceof LivingEntity monster) && !this.pathfinderMob.canAttack(monster)) {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void onReceiveVibration(
            ServerLevel level, BlockPos pos, Holder<GameEvent> event,
            @Nullable Entity sourceEntity, @Nullable Entity projectileOwner, float distance
    ) {
        if (!this.pathfinderMob.isDeadOrDying() && this.pathfinderMob instanceof IHearSound iHearSound
                && iHearSound.getSoundCooldown() <= 0) {
            iHearSound.setSoundCooldown(25);
            BlockPos blockpos = pos;
            if (projectileOwner != null && !this.pathfinderMob.hasLineOfSight(projectileOwner)) {
                if (this.pathfinderMob.closerThan(projectileOwner, 30.0)) {
                    if (projectileOwner instanceof LivingEntity living && this.pathfinderMob.canAttack(living)) {
                        blockpos = projectileOwner.blockPosition();
                    }
                    iHearSound.increaseAngerAt(projectileOwner);
                }
            }

            if (this.pathfinderMob.getTarget() == null) {
                this.pathfinderMob.getLookControl().setLookAt(blockpos.getCenter());
                var optional = iHearSound.getAngerManagement().getActiveEntity();
                if (projectileOwner != null && optional.isPresent() && projectileOwner == optional.get()) {
                    if (iHearSound.getAngerManagement().getActiveAnger(projectileOwner) > 60) {
                        this.pathfinderMob.getNavigation().moveTo(projectileOwner, 0.8F);
                    }
                }
                if (sourceEntity != null && optional.isPresent() && sourceEntity == optional.get()) {
                    if (iHearSound.getAngerManagement().getActiveAnger(sourceEntity) > 60) {
                        this.pathfinderMob.getNavigation().moveTo(sourceEntity, 0.8F);
                    }
                }
            }
        }
    }
}
