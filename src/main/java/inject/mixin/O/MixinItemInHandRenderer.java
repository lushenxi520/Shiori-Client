package inject.mixin.O;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.PickaxeItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import client.Shiori;
import client.events.impl.EventUpdateHeldItem;
import client.modules.impl.move.Scaffold;
import client.modules.impl.render.OldHitting;
import net.minecraft.world.item.ItemDisplayContext;

@Mixin({ItemInHandRenderer.class})
public class MixinItemInHandRenderer {
   private static final Minecraft mc = Minecraft.getInstance();

   @Redirect(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"
      )
   )
   public ItemStack hookMainHand(LocalPlayer player) {
      EventUpdateHeldItem event = new EventUpdateHeldItem(InteractionHand.MAIN_HAND, player.getMainHandItem());
      if (player == Minecraft.getInstance().player) {
         Shiori.getInstance().getEventManager().call(event);
      }

      return event.getItem();
   }

   @Redirect(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"
      )
   )
   public ItemStack hookOffHand(LocalPlayer player) {
      EventUpdateHeldItem event = new EventUpdateHeldItem(InteractionHand.OFF_HAND, player.getOffhandItem());
      if (player == Minecraft.getInstance().player) {
         Shiori.getInstance().getEventManager().call(event);
      }

      return event.getItem();
   }
   @Inject(
      method = "renderArmWithItem",
      at = @At("HEAD"),
      cancellable = true
   )
   private void onRenderArmWithItem(
         AbstractClientPlayer player,
         float partialTicks,
         float pitch,
         InteractionHand hand,
         float swingProgress,
         ItemStack itemStack,
         float equipProgress,
         PoseStack poseStack,
         MultiBufferSource buffer,
         int light,
         CallbackInfo ci) {

      OldHitting oldHitting = (OldHitting) Shiori.getInstance().getModuleManager().getModule(OldHitting.class);
      if (oldHitting != null && oldHitting.isEnabled()) {
         if (hand != InteractionHand.MAIN_HAND) {
            return;
         }
         if (oldHitting.swordOnly.getCurrentValue()) {
            if (!(itemStack.getItem() instanceof SwordItem)) {
               return;
            }
         } else {
            if (!(itemStack.getItem() instanceof SwordItem) &&
                !(itemStack.getItem() instanceof AxeItem) &&
                !(itemStack.getItem() instanceof PickaxeItem)) {
               return;
            }
         }
         boolean useKeyHeld = mc.options.keyUse.isDown() && mc.player.getOffhandItem().isEmpty();
         boolean killAuraAttacking = oldHitting.isKillAuraAttacking();
         if (useKeyHeld || killAuraAttacking) {
            ci.cancel();
            oldHitting.applyHitAnimation(poseStack, swingProgress, player.getMainArm(), equipProgress);
            boolean rightHand = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
            ((ItemInHandRenderer)(Object)this).renderItem(
               player, itemStack,
               rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
               !rightHand, poseStack, buffer, light);
            return;
         }
      }

      Scaffold scaffold = (Scaffold) Shiori.getInstance().getModuleManager().getModule(Scaffold.class);
      if (scaffold != null && scaffold.isEnabled()) {
         return;
      }
   }
}