package inject.mixin.O;

import client.Shiori;
import client.events.impl.EventMouseClick;
import client.modules.Module;
import client.ui.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public class MixinMouseHandler {
   @Inject(
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/KeyMapping;set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V"
      )},
      method = {"onPress"}
   )
   private void onPress(long window, int button, int action, int mods, CallbackInfo ci) {
      double[] x = new double[1];
      double[] y = new double[1];
      GLFW.glfwGetCursorPos(window, x, y);
      EventMouseClick event = new EventMouseClick(button, action == 0, x[0], y[0]);
      Shiori.getInstance().getEventManager().call(event);

      Minecraft mc = Minecraft.getInstance();
      if (mc.screen == null && action == 1 && button == 0) {
         double guiScale = mc.getWindow().getGuiScale();
         int mouseX = (int)(x[0] / guiScale);
         int mouseY = (int)(y[0] / guiScale);
         for (Module module : Shiori.getInstance().getModuleManager().getModules()) {
            if (module instanceof HudElement hud && module.isEnabled()) {
               if (hud.mousePressed(mouseX, mouseY, button)) {
                  break;
               }
            }
         }
      }
   }
}