package inject.mixin.O;

import client.Shiori;
import client.events.impl.EventAttackSlowdown;
import client.events.impl.EventAttackYaw;
import client.events.impl.EventStayingOnGroundSurface;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
public abstract class MixinPlayer extends LivingEntity {
   protected MixinPlayer(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   @Redirect(
      method = {"attack"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"
      )
   )
   private float hookFixRotation(Player instance) {
      EventAttackYaw event = new EventAttackYaw(instance.getYRot());
      Shiori.getInstance().getEventManager().call(event);
      return event.getYaw();
   }

   @Redirect(
      method = {"attack"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
      )
   )
   private void hookSetDeltaMovement(Player instance, Vec3 vec3) {
      EventAttackSlowdown event = new EventAttackSlowdown();
      Shiori.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         instance.setDeltaMovement(vec3);
      }
   }

   @Inject(
      method = {"isStayingOnGroundSurface"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void isStayingOnGroundSurface(CallbackInfoReturnable<Boolean> info) {
      EventStayingOnGroundSurface event = new EventStayingOnGroundSurface((Boolean)info.getReturnValue());
      Shiori.getInstance().getEventManager().call(event);
      info.setReturnValue(event.isStay());
   }
}