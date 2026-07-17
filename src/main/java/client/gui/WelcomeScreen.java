package client.gui;

import client.utils.renderer.SkijaRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.FilterBlurMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.MaskFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public class WelcomeScreen extends Screen {

    public static final int TITLE_FADE_IN_DURATION = 1200;
    public static long titleFadeInStartTime = -1;

    private static final float START_DELAY = 2.0f;
    private static final float INTRO_FADE = 0.5f;
    private static final float REVEAL_DURATION = 1.0f;
    private static final float GRADIENT_SHIFT = 1.5f;
    private static final float HOLD_DURATION = 1.5f;
    private static final float FADE_OUT_DURATION = 0.6f;
    private static final float TOTAL_DURATION = START_DELAY + INTRO_FADE + REVEAL_DURATION + GRADIENT_SHIFT + HOLD_DURATION + FADE_OUT_DURATION;

    private long startTime = -1;
    private boolean done = false;

    public WelcomeScreen() {
        super(Component.literal("操你妈"));
    }

    @Override
    protected void init() {
        this.startTime = System.currentTimeMillis();
        this.done = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (done) return;

        if (startTime < 0) {
            startTime = System.currentTimeMillis();
        }

        float elapsed = (System.currentTimeMillis() - startTime) / 1000.0f - START_DELAY;
        if (elapsed > TOTAL_DURATION - START_DELAY) {
            goToMainMenu();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (done) return;

        guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);

        if (startTime < 0) {
            startTime = System.currentTimeMillis();
        }

        float elapsed = (System.currentTimeMillis() - startTime) / 1000.0f - START_DELAY;
        if (elapsed > TOTAL_DURATION - START_DELAY) return;
        if (elapsed < 0) return;

        SkijaRenderer.beginFrame();
        Canvas canvas = SkijaRenderer.getCanvas();
        if (canvas == null) {
            SkijaRenderer.endFrame();
            return;
        }

        float alpha;
        float revealProgress;
        float gradientProgress;

        if (elapsed < INTRO_FADE) {
            float p = elapsed / INTRO_FADE;
            alpha = easeOutCubic(p);
            revealProgress = 0f;
            gradientProgress = 0f;
        } else if (elapsed < INTRO_FADE + REVEAL_DURATION) {
            float p = (elapsed - INTRO_FADE) / REVEAL_DURATION;
            alpha = 1.0f;
            revealProgress = easeOutCubic(p);
            gradientProgress = 0f;
        } else if (elapsed < INTRO_FADE + REVEAL_DURATION + GRADIENT_SHIFT) {
            float p = (elapsed - INTRO_FADE - REVEAL_DURATION) / GRADIENT_SHIFT;
            alpha = 1.0f;
            revealProgress = 1.0f;
            gradientProgress = easeInOutCubic(p);
        } else if (elapsed < INTRO_FADE + REVEAL_DURATION + GRADIENT_SHIFT + HOLD_DURATION) {
            alpha = 1.0f;
            revealProgress = 1.0f;
            gradientProgress = 1.0f;
        } else {
            float p = (elapsed - INTRO_FADE - REVEAL_DURATION - GRADIENT_SHIFT - HOLD_DURATION) / FADE_OUT_DURATION;
            alpha = 1.0f - easeInCubic(Math.min(1.0f, p));
            revealProgress = 1.0f;
            gradientProgress = 1.0f;
        }

        String text = "Hello";
        float fontSize = 88.0f;
        Font font = SkijaRenderer.getHarmonyFont(fontSize);
        float tw = SkijaRenderer.measureTextWidth(text, font);
        float tx = (this.width - tw) / 2.0f;
        float ty = this.height / 2.0f;

        if (alpha > 0.1f && revealProgress > 0.2f) {
            renderTextBloom(canvas, text, tx, ty, font, alpha, elapsed);
        }

        if (revealProgress > 0f) {
            drawAppleGradientText(canvas, text, tx, ty, font, tw, revealProgress, alpha, gradientProgress, elapsed);
        }

        if (revealProgress > 0.03f && revealProgress < 0.97f && alpha > 0.3f) {
            float revealX = tx + tw * revealProgress;
            renderCursorGlow(canvas, revealX, ty, alpha, elapsed);
        }

        SkijaRenderer.endFrame();
    }

    private void renderTextBloom(Canvas canvas, String text, float x, float y, Font font, float alpha, float elapsed) {
        float glowAlpha = alpha * 0.12f;
        float glowRadius = 18f + 6f * (float)Math.sin(elapsed * 2.5f);

        try (Paint glowPaint = new Paint()) {
            glowPaint.setAntiAlias(true);
            glowPaint.setColor(packRGBA(255, 255, 255, (int)(glowAlpha * 255)));
            glowPaint.setMaskFilter(MaskFilter.makeBlur(FilterBlurMode.OUTER, glowRadius));

            FontMetrics metrics = font.getMetrics();
            float baselineY = y - metrics.getAscent();
            canvas.drawString(text, x, baselineY, font, glowPaint);
        }
    }

    private void drawAppleGradientText(Canvas canvas, String text, float x, float y,
                                        Font font, float textWidth, float revealProgress,
                                        float alpha, float gradientProgress, float elapsed) {
        float revealX = x + textWidth * revealProgress;
        FontMetrics metrics = font.getMetrics();
        float baselineY = y - metrics.getAscent();

        canvas.save();
        try (Paint clipPaint = new Paint()) {
            clipPaint.setColor(packRGBA(255, 255, 255, 255));
            canvas.clipRect(Rect.makeXYWH(0, 0, revealX, this.height));
        }

        float[] charWidths = new float[text.length()];
        for (int i = 0; i < text.length(); i++) {
            charWidths[i] = font.measureTextWidth(String.valueOf(text.charAt(i)));
        }

        float charX = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));

            int color = getAppleCharColor(i, text.length(), gradientProgress, elapsed, alpha);
            try (Paint textPaint = new Paint()) {
                textPaint.setAntiAlias(true);
                textPaint.setColor(color);
                canvas.drawString(ch, charX, baselineY, font, textPaint);
            }

            charX += charWidths[i];
        }

        canvas.restore();
    }

    private int getAppleCharColor(int index, int total, float gradientProgress, float elapsed, float alpha) {
        float hueShift = elapsed * 0.12f;
        float hue = (float)index / total * 0.8f + hueShift + 0.55f;
        hue = hue - (float)Math.floor(hue);

        float saturation;
        float brightness;

        if (gradientProgress < 0.3f) {
            float p = gradientProgress / 0.3f;
            saturation = 0.05f + p * 0.65f;
            brightness = 0.95f;
        } else {
            saturation = 0.7f + 0.1f * (float)Math.sin(elapsed * 1.5f + index * 0.5f);
            brightness = 0.9f + 0.05f * (float)Math.sin(elapsed * 2f + index * 0.7f);
        }

        int rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        int a = (int)(alpha * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void renderCursorGlow(Canvas canvas, float x, float y, float alpha, float elapsed) {
        float pulse = 0.75f + 0.25f * (float)Math.sin(elapsed * 3.5f);

        try (Paint glow = new Paint()) {
            glow.setAntiAlias(true);
            glow.setColor(packRGBA(255, 255, 255, (int)(alpha * 50 * pulse)));
            glow.setMaskFilter(MaskFilter.makeBlur(FilterBlurMode.OUTER, 10f));
            canvas.drawRRect(RRect.makeXYWH(x - 4, y - 62, 8, 124, 4), glow);
        }

        try (Paint dot = new Paint()) {
            dot.setAntiAlias(true);
            dot.setColor(packRGBA(255, 255, 255, (int)(alpha * 180 * pulse)));
            canvas.drawRRect(RRect.makeXYWH(x - 1.5f, y - 55, 3, 110, 1.5f), dot);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!done) {
            goToMainMenu();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!done) {
            goToMainMenu();
        }
        return true;
    }

    private void goToMainMenu() {
        if (done) return;
        done = true;
        titleFadeInStartTime = System.currentTimeMillis();
        Minecraft.getInstance().setScreen(new TitleScreen());
    }

    private static int packRGBA(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float easeOutCubic(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }

    private static float easeInOutCubic(float t) {
        if (t < 0.5f) return 4f * t * t * t;
        return 1f - (float)Math.pow(-2f * t + 2f, 3) / 2f;
    }
}