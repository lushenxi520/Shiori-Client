package inject.mixin.O;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Unique
    private static final String[] WALLPAPER_FILES = {
        "background.png",
        "background2.png",
        "background3.jpg",
        "background4.png",
        "background5.png"
    };

    @Unique
    private static final ResourceLocation[] WALLPAPER_TEXTURES = new ResourceLocation[WALLPAPER_FILES.length];

    @Unique
    private static int currentWallpaperIndex = 0;

    @Unique
    private static int wallpaperCount = 0;

    @Unique
    private static final int TITLE_FADE_IN_DURATION = 3000;

    @Unique
    private static void registerAllTextures() {
        if (wallpaperCount > 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();

        for (int i = 0; i < WALLPAPER_FILES.length; i++) {
            String fileName = WALLPAPER_FILES[i];
            ResourceLocation texId = new ResourceLocation("shiori", "wallpaper_" + i);
            ResourceLocation rawLocation = new ResourceLocation("shiori", "textures/" + fileName);

            try {
                Resource resource = mc.getResourceManager().getResource(rawLocation).orElse(null);
                if (resource != null) {
                    DynamicTexture texture = new DynamicTexture(NativeImage.read(resource.open()));
                    mc.getTextureManager().register(texId, texture);
                    WALLPAPER_TEXTURES[i] = texId;
                    wallpaperCount++;
                }
            } catch (IOException e) {
                System.err.println("Failed to register wallpaper texture " + fileName + ": " + e.getMessage());
            }
        }
    }

    @Unique
    private static ResourceLocation getCurrentTexture() {
        if (wallpaperCount == 0) {
            return null;
        }
        return WALLPAPER_TEXTURES[currentWallpaperIndex];
    }

    @Inject(
        method = {"render"},
        at = {@At("HEAD")}
    )
    private void renderCustomBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        registerAllTextures();
        ResourceLocation currentTex = getCurrentTexture();
        if (currentTex == null) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();

            RenderSystem.enableBlend();
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(currentTex, 0, 0, 0.0F, 0.0F, width, height, width, height);
        } catch (Exception e) {
            System.err.println("Failed to render custom background: " + e.getMessage());
        }
    }

    @Inject(
        method = {"render"},
        at = {@At("RETURN")}
    )
    private void renderWallpaperSwitcher(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        renderFadeInOverlay(guiGraphics, mc);

        if (wallpaperCount <= 1) {
            return;
        }
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        int btnY = height - 22;
        int leftBtnX = width - 55;
        int rightBtnX = width - 20;
        int btnSize = 14;

        int leftColor = (mouseX >= leftBtnX && mouseX <= leftBtnX + btnSize && mouseY >= btnY && mouseY <= btnY + btnSize)
            ? 0xAAFFFFFF : 0x55FFFFFF;
        int rightColor = (mouseX >= rightBtnX && mouseX <= rightBtnX + btnSize && mouseY >= btnY && mouseY <= btnY + btnSize)
            ? 0xAAFFFFFF : 0x55FFFFFF;

        guiGraphics.fill(leftBtnX, btnY, leftBtnX + btnSize, btnY + btnSize, leftColor);
        guiGraphics.fill(rightBtnX, btnY, rightBtnX + btnSize, btnY + btnSize, rightColor);

        String indexText = (currentWallpaperIndex + 1) + "/" + wallpaperCount;
        guiGraphics.drawString(mc.font, indexText,
            leftBtnX + btnSize + 4, btnY + 3, 0xFFFFFFFF);

        guiGraphics.drawString(mc.font, "\u25C0", leftBtnX + 2, btnY + 1, 0xFF000000);
        guiGraphics.drawString(mc.font, "\u25B6", rightBtnX + 2, btnY + 1, 0xFF000000);
    }

    @Unique
    private static void renderFadeInOverlay(GuiGraphics guiGraphics, Minecraft mc) {
        if (client.gui.WelcomeScreen.titleFadeInStartTime < 0) {
            return;
        }
        long elapsed = System.currentTimeMillis() - client.gui.WelcomeScreen.titleFadeInStartTime;
        float progress = (float) elapsed / TITLE_FADE_IN_DURATION;
        if (progress >= 1.0F) {
            client.gui.WelcomeScreen.titleFadeInStartTime = -1;
            return;
        }
        float alpha = 1.0F - easeOutCubic(progress);
        int color = ((int)(alpha * 255.0F) << 24) | 0x000000;
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        guiGraphics.fill(0, 0, width, height, color);
    }

    @Unique
    private static float easeOutCubic(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
    }

    @Inject(
        method = {"mouseClicked"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void handleSwitcherClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (wallpaperCount <= 1 || button != 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int btnY = height - 22;
        int leftBtnX = width - 55;
        int rightBtnX = width - 20;
        int btnSize = 14;

        if (mouseX >= leftBtnX && mouseX <= leftBtnX + btnSize && mouseY >= btnY && mouseY <= btnY + btnSize) {
            currentWallpaperIndex = (currentWallpaperIndex - 1 + wallpaperCount) % wallpaperCount;
            cir.cancel();
            cir.setReturnValue(true);
        } else if (mouseX >= rightBtnX && mouseX <= rightBtnX + btnSize && mouseY >= btnY && mouseY <= btnY + btnSize) {
            currentWallpaperIndex = (currentWallpaperIndex + 1) % wallpaperCount;
            cir.cancel();
            cir.setReturnValue(true);
        }
    }

    @Redirect(
        method = {"render"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"
        )
    )
    private void skipPanorama(PanoramaRenderer instance, float bob, float time) {
    }
}