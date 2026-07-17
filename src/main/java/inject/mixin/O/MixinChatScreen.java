package inject.mixin.O;

import client.Shiori;
import client.modules.Module;
import client.ui.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({net.minecraft.client.gui.screens.ChatScreen.class})
public class MixinChatScreen {

   @Inject(
      at = {@At("HEAD")},
      method = {"mouseClicked(DDI)Z"}
   )
   private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      for (Module module : Shiori.getInstance().getModuleManager().getModules()) {
         if (module instanceof HudElement hud && module.isEnabled()) {
            if (hud.mousePressed((int)mouseX, (int)mouseY, button)) {
               break;
            }
         }
      }
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}
   )
   private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      long window = Minecraft.getInstance().getWindow().getWindow();
      boolean leftDown = GLFW.glfwGetMouseButton(window, 0) == 1;

      for (Module module : Shiori.getInstance().getModuleManager().getModules()) {
         if (module instanceof HudElement hud && module.isEnabled()) {
            if (hud.isHudDragging()) {
               if (leftDown) {
                  hud.mouseDragged(mouseX, mouseY);
               } else {
                  hud.stopDragging();
               }
            }
         }
      }}}
