package alr.onlylooking.mixin;

import alr.onlylooking.LookUtils;
import alr.onlylooking.VibrationUserMonster;
import alr.onlylooking.api.IHearSound;
import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.function.BiConsumer;

@Mixin(PathfinderMob.class)
public abstract class PathfinderMobMixin extends Mob implements VibrationSystem, IHearSound {
    private static final Logger LOGGER = LogUtils.getLogger();
    private DynamicGameEventListener<VibrationSystem.Listener> dynamicVibrationListener;
    private VibrationSystem.User vibrationUser;
    private VibrationSystem.Data vibrationData;

    private AngerManagement angerManagement = new AngerManagement((entity) ->
            entity instanceof LivingEntity living && this.canAttack(living), Collections.emptyList());

    private int soundCooldown;

    public PathfinderMobMixin(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    public int getSoundCooldown() {
        return soundCooldown;
    }

    @Override
    public void setSoundCooldown(int soundCooldown) {
        this.soundCooldown = soundCooldown;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void onConstructor(EntityType<? extends PathfinderMob> type, Level level, CallbackInfo info) {
        PathfinderMob pathfinderMob = (PathfinderMob) (Object) this;
        this.dynamicVibrationListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
        this.vibrationUser = new VibrationUserMonster(pathfinderMob);
        this.vibrationData = new VibrationSystem.Data();
    }

    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SoundCooldown", this.soundCooldown);
        AngerManagement.codec(entity -> entity instanceof LivingEntity living && this.canAttack(living))
                .encodeStart(NbtOps.INSTANCE, this.angerManagement)
                .resultOrPartial(LOGGER::error)
                .ifPresent(encoded -> tag.put("monster_anger", encoded));
        VibrationSystem.Data.CODEC
                .encodeStart(NbtOps.INSTANCE, this.vibrationData)
                .resultOrPartial(LOGGER::error)
                .ifPresent(encoded -> tag.put("monster_listener", encoded));
    }

    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.soundCooldown = tag.getInt("SoundCooldown");
        if (tag.contains("monster_anger")) {
            AngerManagement.codec(entity -> entity instanceof LivingEntity living && this.canAttack(living))
                    .parse(new Dynamic<>(NbtOps.INSTANCE, tag.get("monster_anger")))
                    .resultOrPartial(LOGGER::error)
                    .ifPresent(parsed -> this.angerManagement = parsed);
        }
        if (tag.contains("monster_listener", 10)) {
            VibrationSystem.Data.CODEC
                    .parse(new Dynamic<>(NbtOps.INSTANCE, tag.getCompound("monster_listener")))
                    .resultOrPartial(LOGGER::error)
                    .ifPresent(parsed -> this.vibrationData = parsed);
        }
    }

    @Override
    public VibrationSystem.Data getVibrationData() {
        return this.vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return this.vibrationUser;
    }

    @Override
    public AngerManagement getAngerManagement() {
        return angerManagement;
    }

    public void clearAnger(Entity entity) {
        this.angerManagement.clearAnger(entity);
    }

    @Override
    public void increaseAngerAt(@Nullable Entity entity) {
        this.increaseAngerAt(entity, 35, true);
    }

    @VisibleForTesting
    @Override
    public void increaseAngerAt(@Nullable Entity entity, int ticks, boolean playSound) {
        if (!this.isNoAi() && entity instanceof LivingEntity living && this.canAttack(living)) {
            this.angerManagement.increaseAnger(entity, ticks);
            if (playSound) {
                this.playListeningSound();
            }
        }
    }

    private void playListeningSound() {
        if (this.getAmbientSound() != null) {
            this.playSound(this.getAmbientSound(), this.getSoundVolume(), 0.75F);
        }
    }

    public void tick() {
        super.tick();
        Level level = this.level();
        if (level instanceof ServerLevel serverLevel) {
            VibrationSystem.Ticker.tick(serverLevel, this.vibrationData, this.vibrationUser);
            if (this.tickCount % 20 == 0) {
                this.angerManagement.tick(serverLevel, entity ->
                        entity instanceof LivingEntity living && this.canAttack(living));
            }
        }
        if (this.soundCooldown > 0) {
            --this.soundCooldown;
        }
    }

    @Override
    public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> consumer) {
        Level level = this.level();
        if (LookUtils.isVibrationAvaiable(this)) {
            if (level instanceof ServerLevel serverLevel) {
                consumer.accept(this.dynamicVibrationListener, serverLevel);
            }
        }
    }
}
