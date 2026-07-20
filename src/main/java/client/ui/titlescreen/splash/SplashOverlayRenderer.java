package client.ui.titlescreen.splash;

import client.ui.titlescreen.utils.GlStateUtils;
import client.ui.titlescreen.utils.UnitySpriteParser;
import client.ui.titlescreen.utils.UnitySpriteParser.SpriteAtlas;
import client.ui.titlescreen.utils.UnitySpriteParser.SpriteData;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL33C;

public class SplashOverlayRenderer {

    private static final float DESIGN_WIDTH = 1920f;
    private static final float DESIGN_HEIGHT = 1080f;

    private static DirectContext skiaContext;
    private static Surface surface;
    private static BackendRenderTarget renderTarget;

    private static Image brandLogo;
    private static Image companyLogo;
    private static boolean resourcesLoaded = false;

    public static void loadResources() {
        if (resourcesLoaded) return;

        try {
            var atlasStream = SplashOverlayRenderer.class.getResourceAsStream("/assets/SplashScreen.png");
            var jsonStream = SplashOverlayRenderer.class.getResourceAsStream("/assets/SplashScreen.json");
            if (atlasStream == null || jsonStream == null) return;

            byte[] atlasBytes = atlasStream.readAllBytes();
            atlasStream.close();
            String jsonString = new String(jsonStream.readAllBytes());
            jsonStream.close();

            SpriteAtlas atlasData = UnitySpriteParser.parseAtlas(jsonString);
            Image atlasImage = Image.makeFromEncoded(atlasBytes);

            SpriteData brandData = atlasData.sprites.get("BrandLogo_Acacia");
            if (brandData != null) {
                brandLogo = UnitySpriteParser.cropSprite(atlasImage, brandData);
            }

            SpriteData companyData = atlasData.sprites.get("CompanyLogo_ReAER");
            if (companyData != null) {
                companyLogo = UnitySpriteParser.cropSprite(atlasImage, companyData);
            }

            resourcesLoaded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void closeSkiaResources() {
        if (surface != null) { surface.close(); surface = null; }
        if (renderTarget != null) { renderTarget.close(); renderTarget = null; }
        if (skiaContext != null) { skiaContext.close(); skiaContext = null; }
    }

    private static void buildSkiaSurface() {
        Minecraft mc = Minecraft.getInstance();
        int frameWidth = mc.getMainRenderTarget().width;
        int frameHeight = mc.getMainRenderTarget().height;

        if (surface != null && surface.getWidth() == frameWidth && surface.getHeight() == frameHeight) {
            return;
        }

        closeSkiaResources();

        skiaContext = DirectContext.makeGL();
        renderTarget = BackendRenderTarget.makeGL(
                frameWidth, frameHeight, 0, 8,
                mc.getMainRenderTarget().frameBufferId, 0x8058
        );
        surface = Surface.makeFromBackendRenderTarget(
                skiaContext, renderTarget, SurfaceOrigin.BOTTOM_LEFT,
                SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB()
        );
    }

    private static void resetPixelStore() {
        GL33C.glBindBuffer(GL33C.GL_PIXEL_UNPACK_BUFFER, 0);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SWAP_BYTES, GL33C.GL_FALSE);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_LSB_FIRST, GL33C.GL_FALSE);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_ROW_LENGTH, 0);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_ROWS, 0);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_PIXELS, 0);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_ALIGNMENT, 4);
    }

    public static boolean render(float loadProgress, float alpha) {
        loadResources();
        buildSkiaSurface();

        Minecraft mc = Minecraft.getInstance();
        float fbWidth = mc.getMainRenderTarget().width;
        float fbHeight = mc.getMainRenderTarget().height;
        float scaleX = fbWidth / DESIGN_WIDTH;
        float scaleY = fbHeight / DESIGN_HEIGHT;
        float renderScale = Math.max(scaleX, scaleY);
        float offsetX = (fbWidth - DESIGN_WIDTH * renderScale) / 2f;
        float offsetY = (fbHeight - DESIGN_HEIGHT * renderScale) / 2f;

        GlStateUtils.save();
        resetPixelStore();
        if (skiaContext != null) skiaContext.resetGLAll();

        RenderSystem.enableBlend();

        if (surface != null) {
            Canvas canvas = surface.getCanvas();
            canvas.save();
            canvas.translate(offsetX, offsetY);
            canvas.scale(renderScale, renderScale);

            try (Paint bgPaint = new Paint()) {
                bgPaint.setColor(0xFF000000);
                canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(0, 0, DESIGN_WIDTH, DESIGN_HEIGHT), bgPaint);
            }

            try (Paint alphaPaint = new Paint()) {
                alphaPaint.setAlpha((int) (alpha * 255));

                float logoScale = 0.5f;
                float currentY = DESIGN_HEIGHT / 2f;

                if (brandLogo != null && companyLogo != null) {
                    float brandW = brandLogo.getWidth() * logoScale;
                    float brandH = brandLogo.getHeight() * logoScale;
                    float companyW = companyLogo.getWidth() * logoScale;
                    float companyH = companyLogo.getHeight() * logoScale;
                    float gap = 64f;
                    float totalWidth = brandW + gap + companyW;
                    float startX = (DESIGN_WIDTH - totalWidth) / 2f;

                    canvas.drawImageRect(brandLogo,
                            io.github.humbleui.types.Rect.makeXYWH(0, 0, brandLogo.getWidth(), brandLogo.getHeight()),
                            io.github.humbleui.types.Rect.makeXYWH(startX, currentY - brandH / 2f, brandW, brandH),
                            alphaPaint
                    );
                    canvas.drawImageRect(companyLogo,
                            io.github.humbleui.types.Rect.makeXYWH(0, 0, companyLogo.getWidth(), companyLogo.getHeight()),
                            io.github.humbleui.types.Rect.makeXYWH(startX + brandW + gap, currentY - companyH / 2f + 32f, companyW, companyH),
                            alphaPaint
                    );
                } else if (brandLogo != null) {
                    float brandW = brandLogo.getWidth() * logoScale;
                    float brandH = brandLogo.getHeight() * logoScale;
                    canvas.drawImageRect(brandLogo,
                            io.github.humbleui.types.Rect.makeXYWH(0, 0, brandLogo.getWidth(), brandLogo.getHeight()),
                            io.github.humbleui.types.Rect.makeXYWH((DESIGN_WIDTH - brandW) / 2f, currentY - brandH / 2f, brandW, brandH),
                            alphaPaint
                    );
                } else if (companyLogo != null) {
                    float companyW = companyLogo.getWidth() * logoScale;
                    float companyH = companyLogo.getHeight() * logoScale;
                    canvas.drawImageRect(companyLogo,
                            io.github.humbleui.types.Rect.makeXYWH(0, 0, companyLogo.getWidth(), companyLogo.getHeight()),
                            io.github.humbleui.types.Rect.makeXYWH((DESIGN_WIDTH - companyW) / 2f, currentY - companyH / 2f, companyW, companyH),
                            alphaPaint
                    );
                }
            }
            canvas.restore();
            surface.flushAndSubmit();
        }

        GlStateUtils.restore();
        RenderSystem.disableBlend();

        return true;
    }

    public static void cleanup() {
        closeSkiaResources();
        resourcesLoaded = false;
        brandLogo = null;
        companyLogo = null;
    }
}