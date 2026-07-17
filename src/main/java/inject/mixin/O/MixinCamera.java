package inject.mixin.O;

import client.Shiori;
import client.modules.impl.render.ViewClip;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Camera.class})
public class MixinCamera {
   @Inject(
      at = {@At("HEAD")},
      method = {"getMaxZoom"},
      cancellable = true
   )
   private void getMaxZoom(double pStartingDistance, CallbackInfoReturnable<Double> cir) {
      if (Shiori.getInstance() != null && Shiori.getInstance().getModuleManager() != null) {
         ViewClip module = (ViewClip) Shiori.getInstance().getModuleManager().getModule(ViewClip.class);
         if (module.isEnabled()) {
            cir.setReturnValue(pStartingDistance * (double)module.scale.getCurrentValue() * (double)module.personViewAnimation.value / 100.0);
            cir.cancel();
         }
      }
   }
}
