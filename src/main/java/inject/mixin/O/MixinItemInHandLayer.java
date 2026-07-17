package inject.mixin.O;

import client.Shiori;
import client.events.impl.EventUpdateHeldItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ItemInHandLayer.class})
public class MixinItemInHandLayer {
   @Redirect(
      method = {"render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"
      )
   )
   private ItemStack hookMainHand(LivingEntity instance) {
      EventUpdateHeldItem event = new EventUpdateHeldItem(InteractionHand.MAIN_HAND, instance.getMainHandItem());
      if (instance == Minecraft.getInstance().player) {
         Shiori.getInstance().getEventManager().call(event);
      }

      return event.getItem();
   }

   @Redirect(
      method = {"render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"
      )
   )
   private ItemStack hookOffHand(LivingEntity instance) {
      EventUpdateHeldItem event = new EventUpdateHeldItem(InteractionHand.OFF_HAND, instance.getOffhandItem());
      if (instance == Minecraft.getInstance().player) {
         Shiori.getInstance().getEventManager().call(event);
      }

      return event.getItem();
   }
}
