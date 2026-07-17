package client.utils.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL33;

public class SkijaRenderer {

    private static DirectContext context;
    private static BackendRenderTarget renderTarget;
    private static Surface surface;
    private static Canvas canvas;
    private static Typeface opensansTypeface;
    private static Typeface harmonyTypeface;
    private static Typeface iconTypeface;
    private static final Map<Float, Font> opensansFontCache = new HashMap<>();
    private static final Map<Float, Font> harmonyFontCache = new HashMap<>();
    private static final Map<Float, Font> iconFontCache = new HashMap<>();
    private static boolean initialized = false;
    private static int lastWidth = -1;
    private static int lastHeight = -1;
    private static int lastFbId = -1;
    private static int beginCount = 0;
    private static int savedUnpackAlignment;
    private static int savedUnpackRowLength;
    private static int savedUnpackSkipPixels;
    private static int savedUnpackSkipRows;
    private static int savedPixelUnpackBuffer;
    private static int savedActiveTexture;
    private static boolean savedDepthMask;

    public static void init() {
        if (initialized) return;
        try {
            context = DirectContext.makeGL();
            loadTypefaces();
            initialized = true;
        } catch (Exception e) {
            System.err.println("[SkiaRenderer] Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadTypefaces() {
        opensansTypeface = loadTypefaceFromResource("/assets/shiori/fonts/opensans.ttf");
        harmonyTypeface = loadTypefaceFromResource("/assets/shiori/fonts/harmony.ttf");
        iconTypeface = loadTypefaceFromResource("/assets/shiori/fonts/icon.ttf");
    }

    private static Typeface loadTypefaceFromResource(String path) {
        try (InputStream is = SkijaRenderer.class.getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("[SkiaRenderer] Font resource not found: " + path);
                return Typeface.makeDefault();
            }
            byte[] bytes = is.readAllBytes();
            Data data = Data.makeFromBytes(bytes);
            Typeface tf = Typeface.makeFromData(data);
            data.close();
            return tf;
        } catch (Exception e) {
            System.err.println("[SkiaRenderer] Failed to load font: " + path + " - " + e.getMessage());
            return Typeface.makeDefault();
        }
    }

    private static void createSurface() {
        if (surface != null) {
            surface.close();
            surface = null;
        }
        if (renderTarget != null) {
            renderTarget.close();
            renderTarget = null;
        }

        Minecraft mc = Minecraft.getInstance();
        int width = mc.getMainRenderTarget().width;
        int height = mc.getMainRenderTarget().height;
        int fbId = mc.getMainRenderTarget().frameBufferId;

        renderTarget = BackendRenderTarget.makeGL(width, height, 0, 8, fbId, 0x8058);
        surface = Surface.makeFromBackendRenderTarget(
            context, renderTarget, SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB()
        );
        if (surface != null) {
            canvas = surface.getCanvas();
        }

        lastWidth = width;
        lastHeight = height;
        lastFbId = fbId;
    }

    public static void checkAndUpdateSurface() {
        Minecraft mc = Minecraft.getInstance();
        if (lastWidth != mc.getMainRenderTarget().width
                || lastHeight != mc.getMainRenderTarget().height
                || lastFbId != mc.getMainRenderTarget().frameBufferId) {
            createSurface();
        }
    }

    public static void beginFrame() {
        if (!initialized) {
            init();
        }
        if (context == null) return;

        checkAndUpdateSurface();

        if (surface == null || canvas == null) return;

        RenderSystem.assertOnRenderThread();

        if (beginCount == 0) {
            GL.saveState();

            savedUnpackAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
            savedUnpackRowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
            savedUnpackSkipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
            savedUnpackSkipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
            savedPixelUnpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
            savedActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        }
        beginCount++;

        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glDisable(GL11.GL_CULL_FACE);

        RenderSystem.colorMask(false, false, false, true);
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.colorMask(true, true, true, true);

        if (context != null) {
            context.resetGLAll();
        }

        canvas.save();
        double scaleFactor = Minecraft.getInstance().getWindow().getGuiScale();
        canvas.scale((float) scaleFactor, (float) scaleFactor);
    }

    public static void endFrame() {
        if (canvas != null) {
            canvas.restore();
        }

        RenderSystem.assertOnRenderThread();

        if (surface != null) {
            surface.flushAndSubmit();
        }

        GL33.glBindSampler(0, 0);

        GL11.glEnable(GL11.GL_BLEND);
        RenderSystem.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        RenderSystem.blendEquation(GL14.GL_FUNC_ADD);

        GL11.glColorMask(true, true, true, true);
        RenderSystem.colorMask(true, true, true, true);

        GL11.glDepthMask(false);
        RenderSystem.depthMask(false);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        RenderSystem.disableScissor();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        RenderSystem.disableDepthTest();

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);

        GL11.glDisable(GL11.GL_CULL_FACE);
        RenderSystem.disableCull();

        beginCount--;
        if (beginCount == 0) {
            GL.restoreState();

            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, savedUnpackRowLength);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, savedUnpackSkipPixels);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, savedUnpackSkipRows);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, savedUnpackAlignment);

            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, savedPixelUnpackBuffer);

            GL13.glActiveTexture(savedActiveTexture);
            RenderSystem.activeTexture(savedActiveTexture);

            GL11.glDepthMask(savedDepthMask);
            RenderSystem.depthMask(savedDepthMask);
        }
    }

    public static Canvas getCanvas() {
        return canvas;
    }

    public static Font getOpensansFont(float size) {
        return opensansFontCache.computeIfAbsent(size, s -> new Font(opensansTypeface, s));
    }

    public static Font getHarmonyFont(float size) {
        return harmonyFontCache.computeIfAbsent(size, s -> new Font(harmonyTypeface, s));
    }

    public static Font getIconFont(float size) {
        return iconFontCache.computeIfAbsent(size, s -> new Font(iconTypeface, s));
    }

    public static void drawText(Canvas canvas, String text, float x, float y, Font font, int color) {
        drawText(canvas, text, x, y, font, color, false);
    }

    public static void drawText(Canvas canvas, String text, float x, float y, Font font, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        try (Paint paint = new Paint()) {
            paint.setColor(0xFF000000 | (r << 16) | (g << 8) | b);
            paint.setAlpha(a);
            paint.setAntiAlias(true);

            FontMetrics metrics = font.getMetrics();
            float baselineY = y - metrics.getAscent();
            canvas.drawString(text, x, baselineY, font, paint);
        }
    }

    public static void fillRect(Canvas canvas, float x, float y, float width, float height, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        try (Paint paint = new Paint()) {
            paint.setColor(0xFF000000 | (r << 16) | (g << 8) | b);
            paint.setAlpha(a);
            paint.setAntiAlias(false);
            canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(x, y, width, height), paint);
        }
    }

    public static void drawRoundedRect(Canvas canvas, float x, float y, float width, float height, float radius, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        if (radius <= 0.0f) {
            fillRect(canvas, x, y, width, height, color);
            return;
        }

        try (Paint paint = new Paint()) {
            paint.setColor(0xFF000000 | (r << 16) | (g << 8) | b);
            paint.setAlpha(a);
            paint.setAntiAlias(true);
            canvas.drawRRect(io.github.humbleui.types.RRect.makeLTRB(x, y, x + width, y + height, radius), paint);
        }
    }

    public static void drawGlowRect(Canvas canvas, float x, float y, float width, float height, int color, float glowSize) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        try (Paint paint = new Paint()) {
            paint.setColor(0xFF000000 | (r << 16) | (g << 8) | b);
            paint.setAlpha(a);
            paint.setAntiAlias(true);
            paint.setMaskFilter(MaskFilter.makeBlur(FilterBlurMode.OUTER, glowSize));
            canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(x, y, width, height), paint);
        }
    }

    public static void drawGlowRoundedRect(Canvas canvas, float x, float y, float width, float height, float radius, int color, float glowSize) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        try (Paint paint = new Paint()) {
            paint.setColor(0xFF000000 | (r << 16) | (g << 8) | b);
            paint.setAlpha(a);
            paint.setAntiAlias(true);
            paint.setMaskFilter(MaskFilter.makeBlur(FilterBlurMode.OUTER, glowSize));
            canvas.drawRRect(io.github.humbleui.types.RRect.makeLTRB(x, y, x + width, y + height, radius), paint);
        }
    }

    public static float measureTextWidth(String text, Font font) {
        if (text == null || text.isEmpty()) return 0;
        return font.measureTextWidth(text);
    }

    public static float getFontHeight(Font font) {
        FontMetrics metrics = font.getMetrics();
        return metrics.getDescent() - metrics.getAscent();
    }

    public static void drawHorizontalRainbow(Canvas canvas, float x, float y, float width, float height,
            float hueStart, float hueEnd, float saturation, float brightness, int alpha) {
        int steps = Math.max(2, (int) (width / 2));
        int[] colors = new int[steps];
        float[] positions = new float[steps];
        for (int i = 0; i < steps; i++) {
            float hue = hueStart + (hueEnd - hueStart) * i / (steps - 1);
            int rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            colors[i] = (alpha << 24) | (r << 16) | (g << 8) | b;
            positions[i] = (float) i / (steps - 1);
        }
        io.github.humbleui.skija.Shader shader = io.github.humbleui.skija.Shader.makeLinearGradient(x, y, x + width, y, colors, positions);
        try (Paint paint = new Paint()) {
            paint.setShader(shader);
            paint.setAntiAlias(false);
            canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(x, y, width, height), paint);
        }
        shader.close();
    }
}