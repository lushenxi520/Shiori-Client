package inject.mixin.O;

import client.Shiori;
import client.events.impl.EventKey;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardHandler.class})
public class MixinKeyboardHandler {
   @Inject(
      at = {@At("HEAD")},
      method = {"keyPress"}
   )
   private void onKeyPress(long pWindowPointer, int pKey, int pScanCode, int pAction, int pModifiers, CallbackInfo ci) {
      if (pKey != -1 && Shiori.getInstance() != null && Shiori.getInstance().getEventManager() != null) {
         Shiori.getInstance().getEventManager().call(new EventKey(pKey, pAction != 0));
      }
   }
}
