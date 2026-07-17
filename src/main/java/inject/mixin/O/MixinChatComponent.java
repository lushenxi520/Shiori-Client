package inject.mixin.O;

import client.Shiori;
import client.modules.impl.render.Custom;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ChatComponent.class})
public class MixinChatComponent {

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"
      )
   )
   private void onFill(GuiGraphics instance, int x1, int y1, int x2, int y2, int color) {
      Custom custom = (Custom) Shiori.getInstance().getModuleManager().getModule(Custom.class);
      if (custom.isEnabled() && custom.chatBackground.getCurrentValue()) {
         return;
      }
      instance.fill(x1, y1, x2, y2, color);
   }
}