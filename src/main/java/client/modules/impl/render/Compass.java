package client.modules.impl.render;

import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventMotion;
import client.events.impl.EventRender;
import client.events.impl.EventRender2D;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.utils.BlinkingPlayer;
import client.utils.InventoryUtils;
import client.utils.RenderUtils;
import client.values.ValueBuilder;
import client.values.impl.BooleanValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

@ModuleInfo(
   name = "Compass",
   description = "Shows a compass",
   category = Category.RENDER
)
public class Compass extends Module {
   public BooleanValue compassOnly = ValueBuilder.create(this, "Compass Only").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue noPlayerOnly = ValueBuilder.create(this, "No Player Only").setDefaultBooleanValue(true).build().getBooleanValue();
   private boolean hasCompass = false;
   private BlockPos spawnPosition;
   private float renderYaw;
   private double renderX;
   private double renderZ;

   private BlockPos getSpawnPosition(ClientLevel p_117922_) {
      return p_117922_.dimensionType().natural() ? p_117922_.getSharedSpawnPos() : null;
   }

   private boolean hasPlayer() {
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity != mc.player && !(entity instanceof BlinkingPlayer) && entity instanceof Player) {
            return true;
         }
      }

      return false;
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         this.hasCompass = InventoryUtils.hasItem(Items.COMPASS);
         this.spawnPosition = this.getSpawnPosition(mc.level);
      }
   }

   @EventTarget
   public void onRender(EventRender e) {
      this.renderX = Mth.lerp((double)e.getRenderPartialTicks(), mc.player.xOld, mc.player.getX());
      this.renderZ = Mth.lerp((double)e.getRenderPartialTicks(), mc.player.zOld, mc.player.getZ());
      this.renderYaw = Mth.lerp(e.getRenderPartialTicks(), mc.player.yRotO, mc.player.getYRot());
   }

   @EventTarget(4)
   public void onRender2D(EventRender2D e) {
      this.draw(e.getStack());
   }

   private void draw(PoseStack stack) {
      if (this.hasCompass || !this.compassOnly.getCurrentValue()) {
         if (!this.hasPlayer() || !this.noPlayerOnly.getCurrentValue()) {
            if (this.spawnPosition != null) {
               float yaw = (float)(
                  Math.toDegrees(Math.atan2((double)this.spawnPosition.getZ() - this.renderZ, (double)this.spawnPosition.getX() - this.renderX))
                     - 90.0
                     - (double)this.renderYaw
               );
               float x = (float)mc.getWindow().getGuiScaledWidth() / 2.0F;
               float y = (float)mc.getWindow().getGuiScaledHeight() / 2.0F;
               stack.pushPose();
               stack.translate(x, y, 0.0F);
               stack.mulPose(Axis.ZP.rotationDegrees(yaw));
               stack.translate(-x, -y, 0.0F);
               RenderUtils.drawTracer(stack, x, y - 45.0F, 10.0F, 2.0F, 1.0F, -1);
               stack.translate(x, y, 0.0F);
               stack.mulPose(Axis.ZP.rotationDegrees(-yaw));
               stack.translate(-x, -y, 0.0F);
               stack.popPose();
            }
         }
      }
   }
}
