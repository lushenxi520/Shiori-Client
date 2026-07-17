package inject.mixin.O;

import client.Shiori;
import client.modules.impl.render.Custom;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {ForgeGui.class},
   remap = false,
   priority = 100
)
public class MixinForgeGui {

   @Inject(
      method = {"renderHealth"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   public void hookRenderHealth(int width, int height, GuiGraphics guiGraphics, CallbackInfo ci) {
      Custom custom = (Custom) Shiori.getInstance().getModuleManager().getModule(Custom.class);
      if (custom.isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderFood"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   public void hookRenderFood(int width, int height, GuiGraphics guiGraphics, CallbackInfo ci) {
      Custom custom = (Custom) Shiori.getInstance().getModuleManager().getModule(Custom.class);
      if (custom.isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderArmor"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   public void hookRenderArmor(GuiGraphics guiGraphics, int width, int height, CallbackInfo ci) {
      Custom custom = (Custom) Shiori.getInstance().getModuleManager().getModule(Custom.class);
      if (custom.isEnabled()) {
         ci.cancel();
      }
   }
}