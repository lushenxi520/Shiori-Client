package inject.mixin.O;

import client.ui.titlescreen.splash.SplashOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public abstract class MixinLoadingOverlay {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private long fadeOutStart;

    @Shadow
    @Final
    private boolean fadeIn;

    @Shadow
    private float currentProgress;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();

        long currentTime = System.currentTimeMillis();
        long fadeOutStart = this.fadeOutStart;
        boolean isReloading = this.fadeIn;

        float fadeOutProgress = fadeOutStart > -1L
                ? (float) (currentTime - fadeOutStart) / 1000.0F
                : -1.0F;

        float logoAlpha = 1.0F;
        if (fadeOutProgress >= 0.0F) {
            logoAlpha = Math.max(0.0F, 1.0F - fadeOutProgress);
        } else if (isReloading) {
            logoAlpha = Math.max(0.15F, this.currentProgress);
        }

        float loadProgress = this.currentProgress;

        SplashOverlayRenderer.render(loadProgress, logoAlpha);

        if (fadeOutProgress >= 2.0F) {
            this.minecraft.setOverlay(null);
            SplashOverlayRenderer.cleanup();
        }
    }
}