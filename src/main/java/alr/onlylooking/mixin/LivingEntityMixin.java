package alr.onlylooking.mixin;

import alr.onlylooking.LookUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(at = @At("HEAD"), method = "hasLineOfSight", cancellable = true)
    void hasLineOfSight(Entity target, CallbackInfoReturnable<Boolean> callbackInfo) {
        LivingEntity livingEntity = (LivingEntity) ((Object) this);
        if (target.level() == this.level() && !LookUtils.isLookingAtYou(livingEntity, target)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
