package client.ui.titlescreen;

import client.ui.titlescreen.utils.GlStateUtils;
import client.ui.titlescreen.utils.UnitySpriteParser;
import client.ui.titlescreen.utils.UnitySpriteParser.SpriteAtlas;
import client.ui.titlescreen.utils.UnitySpriteParser.SpriteData;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.opengl.GL33C;

import java.util.HashMap;
import java.util.Map;

public class ShioriTitleScreen extends Screen {

    private static final float DESIGN_WIDTH = 1920f;
    private static final float DESIGN_HEIGHT = 1080f;
    private static final float BUTTON_SCALE = 0.375f;
    private static final float LOGO_SCALE = 0.5f;

    private DirectContext skiaContext;
    private Surface surface;
    private BackendRenderTarget renderTarget;

    private Image backgroundImage;
    private Image titleAtlasImage;
    private SpriteAtlas titleAtlasData;
    private Map<String, Image> buttonSprites = new HashMap<>();
    private Image titleLogo;
    private Image titleOverlay;
    private Typeface manosabaTypeface;

    private float backgroundAnimProgress = 0f;
    private float uiAlpha = 0f;
    private long animStartTime = -1;
    private boolean animPhase1Complete = false;

    private boolean showExitDialog = false;
    private float exitDialogAlpha = 0f;
    private boolean exitDialogClosing = false;
    private boolean exitDialogResult = false;

    private Image dialogAtlasImage;
    private SpriteAtlas dialogAtlasData;
    private Image commonAtlasImage;
    private SpriteAtlas commonAtlasData;
    private Image dialogBase;
    private Image topFrame;
    private Image bottomFrame;
    private Image buttonDefault;
    private Image buttonHighlighted;

    private int hoveredButton = -1;
    private int hoveredDialogButton = -1;

    private final String[] buttonNames = {
            "Button_NewGame", "Button_LoadGame", "Button_Options",
            "Button_Exit", "Button_Gallery", "Button_WitchBook"
    };

    public ShioriTitleScreen() {
        super(Component.literal("Shiori Title Screen"));
        loadResources();
    }

    private void loadResources() {
        loadAtlasResources();
        loadDialogResources();
        loadBackgroundImage();
        loadFont();
    }

    private void loadFont() {
        try (var is = getClass().getResourceAsStream("/assets/TsukushiMincho.otf")) {
            if (is != null) {
                Data data = Data.makeFromBytes(is.readAllBytes());
                manosabaTypeface = Typeface.makeFromData(data);
                data.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAtlasResources() {
        try {
            titleAtlasImage = UnitySpriteParser.loadAtlasImageFromResources("/assets/UI_Title.png");
            titleAtlasData = UnitySpriteParser.loadAtlasDataFromResources("/assets/UI_Title.json");
            if (titleAtlasImage == null || titleAtlasData == null) return;

            for (String name : buttonNames) {
                for (String suffix : new String[]{"_Normal", "_Highlighted"}) {
                    String spriteName = name + suffix;
                    SpriteData spriteData = titleAtlasData.sprites.get(spriteName);
                    if (spriteData != null) {
                        buttonSprites.put(spriteName, UnitySpriteParser.cropSprite(titleAtlasImage, spriteData));
                    }
                }
            }

            SpriteData logoData = titleAtlasData.sprites.get("TitleLogo@Ja");
            if (logoData != null) {
                titleLogo = UnitySpriteParser.cropSprite(titleAtlasImage, logoData);
            }

            SpriteData overlayData = titleAtlasData.sprites.get("TitleOverlay");
            if (overlayData != null) {
                titleOverlay = UnitySpriteParser.cropSprite(titleAtlasImage, overlayData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDialogResources() {
        try {
            dialogAtlasImage = UnitySpriteParser.loadAtlasImageFromResources("/assets/UI_Dialog.png");
            dialogAtlasData = UnitySpriteParser.loadAtlasDataFromResources("/assets/UI_Dialog.json");
            commonAtlasImage = UnitySpriteParser.loadAtlasImageFromResources("/assets/UI_Common.png");
            commonAtlasData = UnitySpriteParser.loadAtlasDataFromResources("/assets/UI_Common.json");

            if (dialogAtlasImage != null && dialogAtlasData != null) {
                SpriteData data = dialogAtlasData.sprites.get("DialogBase");
                if (data != null) dialogBase = UnitySpriteParser.cropSprite(dialogAtlasImage, data);
                data = dialogAtlasData.sprites.get("TopFrame");
                if (data != null) topFrame = UnitySpriteParser.cropSprite(dialogAtlasImage, data);
                data = dialogAtlasData.sprites.get("BottomFrame");
                if (data != null) bottomFrame = UnitySpriteParser.cropSprite(dialogAtlasImage, data);
            }

            if (commonAtlasImage != null && commonAtlasData != null) {
                SpriteData data = commonAtlasData.sprites.get("ButtonBase_Default");
                if (data != null) buttonDefault = UnitySpriteParser.cropSprite(commonAtlasImage, data);
                data = commonAtlasData.sprites.get("ButtonBase_Highlighted");
                if (data != null) buttonHighlighted = UnitySpriteParser.cropSprite(commonAtlasImage, data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBackgroundImage() {
        try (var is = getClass().getResourceAsStream("/assets/background_ema.png")) {
            if (is != null) {
                backgroundImage = Image.makeFromEncoded(is.readAllBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildSkiaSurface() {
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

    private void closeSkiaResources() {
        if (surface != null) { surface.close(); surface = null; }
        if (renderTarget != null) { renderTarget.close(); renderTarget = null; }
        if (skiaContext != null) { skiaContext.close(); skiaContext = null; }
    }

    private void resetPixelStore() {
        GL33C.glBindBuffer(GL33C.GL_PIXEL_UNPACK_BUFFER, 0);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SWAP_BYTES, GL33C.GL_FALSE);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_LSB_FIRST, GL33C.GL_FALSE);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_ROW_LENGTH, 0);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_ROWS, 0);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_PIXELS, 0);
        GL33C.glPixelStorei(GL33C.GL_UNPACK_ALIGNMENT, 4);
    }

    private float[] calculateScaleParams() {
        Minecraft mc = Minecraft.getInstance();
        float fbWidth = mc.getMainRenderTarget().width;
        float fbHeight = mc.getMainRenderTarget().height;

        float scaleX = fbWidth / DESIGN_WIDTH;
        float scaleY = fbHeight / DESIGN_HEIGHT;
        float renderScale = Math.max(scaleX, scaleY);
        float offsetX = (fbWidth - DESIGN_WIDTH * renderScale) / 2f;
        float offsetY = (fbHeight - DESIGN_HEIGHT * renderScale) / 2f;

        return new float[]{renderScale, offsetX, offsetY};
    }

    private float[] toDesignCoord(double windowX, double windowY) {
        Minecraft mc = Minecraft.getInstance();
        float scaleFactor = (float) mc.getWindow().getGuiScale();
        float fbX = (float) (windowX * scaleFactor);
        float fbY = (float) (windowY * scaleFactor);

        float[] params = calculateScaleParams();
        float renderScale = params[0];
        float offsetX = params[1];
        float offsetY = params[2];

        float designX = (fbX - offsetX) / renderScale;
        float designY = (fbY - offsetY) / renderScale;

        return new float[]{designX, designY};
    }

    private void updateAnimation() {
        if (animStartTime < 0) {
            animStartTime = System.currentTimeMillis();
        }

        long elapsed = System.currentTimeMillis() - animStartTime;

        if (!animPhase1Complete) {
            backgroundAnimProgress = Math.min(1f, elapsed / 2500f);
            backgroundAnimProgress = easeFastOutSlowIn(backgroundAnimProgress);
            if (elapsed >= 2500) {
                animPhase1Complete = true;
                animStartTime = System.currentTimeMillis();
            }
        } else {
            long phase2Elapsed = elapsed - 250L;
            if (phase2Elapsed > 0) {
                uiAlpha = Math.min(1f, phase2Elapsed / 500f);
                uiAlpha = easeFastOutSlowIn(uiAlpha);
            }
        }

        if (showExitDialog) {
            if (!exitDialogClosing) {
                exitDialogAlpha = Math.min(1f, exitDialogAlpha + 0.05f);
            } else {
                exitDialogAlpha = Math.max(0f, exitDialogAlpha - 0.05f);
                if (exitDialogAlpha <= 0f) {
                    if (exitDialogResult) {
                        Minecraft.getInstance().stop();
                    } else {
                        showExitDialog = false;
                        exitDialogClosing = false;
                    }
                }
            }
        }
    }

    private float easeFastOutSlowIn(float t) {
        return 1f - (float) Math.pow(1f - t, 3);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateAnimation();
        buildSkiaSurface();

        float[] params = calculateScaleParams();
        float renderScale = params[0];
        float offsetX = params[1];
        float offsetY = params[2];

        float[] designCoords = toDesignCoord(mouseX, mouseY);
        float designMouseX = designCoords[0];
        float designMouseY = designCoords[1];

        GlStateUtils.save();
        resetPixelStore();
        if (skiaContext != null) skiaContext.resetGLAll();

        RenderSystem.enableBlend();

        if (surface != null) {
            Canvas canvas = surface.getCanvas();
            canvas.save();
            canvas.translate(offsetX, offsetY);
            canvas.scale(renderScale, renderScale);

            renderBackground(canvas);
            renderOverlay(canvas);
            renderLogo(canvas);
            renderButtons(canvas, designMouseX, designMouseY);
            renderVersionText(canvas);

            if (showExitDialog) {
                renderExitDialog(canvas, designMouseX, designMouseY);
            }

            canvas.restore();
            surface.flushAndSubmit();
        }

        GlStateUtils.restore();
        RenderSystem.disableBlend();
    }

    private void renderBackground(Canvas canvas) {
        if (backgroundImage == null) return;

        float scale = 1.1f - (0.1f * backgroundAnimProgress);
        float blurAmount = 20f * (1f - backgroundAnimProgress);

        canvas.save();
        canvas.translate(DESIGN_WIDTH / 2f, DESIGN_HEIGHT / 2f);
        canvas.scale(scale, scale);
        canvas.translate(-DESIGN_WIDTH / 2f, -DESIGN_HEIGHT / 2f);

        io.github.humbleui.types.Rect srcRect = io.github.humbleui.types.Rect.makeXYWH(0, 0, backgroundImage.getWidth(), backgroundImage.getHeight());
        io.github.humbleui.types.Rect dstRect = io.github.humbleui.types.Rect.makeXYWH(0, 0, DESIGN_WIDTH, DESIGN_HEIGHT);

        if (blurAmount > 0.1f) {
            try (Paint paint = new Paint()) {
                paint.setMaskFilter(MaskFilter.makeBlur(FilterBlurMode.OUTER, blurAmount));
                canvas.drawImageRect(backgroundImage, srcRect, dstRect, paint);
            }
        } else {
            canvas.drawImageRect(backgroundImage, srcRect, dstRect);
        }

        canvas.restore();
    }

    private void renderOverlay(Canvas canvas) {
        if (titleOverlay == null) return;
        try (Paint paint = new Paint()) {
            paint.setAlpha((int) (uiAlpha * 255));
            canvas.drawImageRect(titleOverlay,
                    io.github.humbleui.types.Rect.makeXYWH(0, 0, titleOverlay.getWidth(), titleOverlay.getHeight()),
                    io.github.humbleui.types.Rect.makeXYWH(0, 0, DESIGN_WIDTH, DESIGN_HEIGHT),
                    paint
            );
        }
    }

    private void renderLogo(Canvas canvas) {
        if (titleLogo == null) return;

        float logoWidth = titleLogo.getWidth() * LOGO_SCALE;
        float logoHeight = titleLogo.getHeight() * LOGO_SCALE;
        float logoX = DESIGN_WIDTH - logoWidth - 24f;
        float logoY = 24f;

        try (Paint paint = new Paint()) {
            paint.setAlpha((int) (uiAlpha * 255));
            canvas.drawImageRect(titleLogo,
                    io.github.humbleui.types.Rect.makeXYWH(0, 0, titleLogo.getWidth(), titleLogo.getHeight()),
                    io.github.humbleui.types.Rect.makeXYWH(logoX, logoY, logoWidth, logoHeight),
                    paint
            );
        }
    }

    private void renderButtons(Canvas canvas, float mouseX, float mouseY) {
        if (buttonSprites.isEmpty()) return;

        Paint alphaPaint = new Paint();
        alphaPaint.setAlpha((int) (uiAlpha * 255));

        float startX = 24f;
        float startY = DESIGN_HEIGHT - 24f;

        ButtonConfig[] buttons = getButtonConfigs();
        hoveredButton = -1;

        float currentX = startX;
        for (int i = 0; i < buttons.length; i++) {
            ButtonConfig config = buttons[i];
            String normalKey = "Button_" + config.name + "_Normal";
            String highlightedKey = "Button_" + config.name + "_Highlighted";
            Image normalSprite = buttonSprites.get(normalKey);
            Image highlightedSprite = buttonSprites.get(highlightedKey);

            if (normalSprite == null) continue;

            float btnWidth = normalSprite.getWidth() * BUTTON_SCALE;
            float btnHeight = normalSprite.getHeight() * BUTTON_SCALE;
            float btnX = currentX;
            float btnY = startY - btnHeight + config.yOffset;

            boolean isHovered = mouseX >= btnX && mouseX <= btnX + btnWidth
                    && mouseY >= btnY && mouseY <= btnY + btnHeight;
            if (isHovered) hoveredButton = i;

            Image sprite = (isHovered && highlightedSprite != null) ? highlightedSprite : normalSprite;
            canvas.drawImageRect(sprite,
                    io.github.humbleui.types.Rect.makeXYWH(0, 0, sprite.getWidth(), sprite.getHeight()),
                    io.github.humbleui.types.Rect.makeXYWH(btnX, btnY, btnWidth, btnHeight),
                    alphaPaint
            );

            currentX += btnWidth + 8f;
        }

        alphaPaint.close();
    }

    private void renderVersionText(Canvas canvas) {
        String versionText = "Ver. " + Minecraft.getInstance().getLaunchedVersion();
        if (manosabaTypeface == null) return;

        try (Font font = new Font(manosabaTypeface, 20f);
             Paint paint = new Paint()) {
            paint.setColor(0xFFFFFFFF);
            paint.setAntiAlias(true);
            paint.setAlpha((int) (uiAlpha * 255));

            float textWidth = font.measureTextWidth(versionText);
            float x = DESIGN_WIDTH - textWidth - 48f;
            float y = DESIGN_HEIGHT - 24f - font.getMetrics().getDescent();

            try (Paint shadowPaint = new Paint()) {
                shadowPaint.setColor(0xB3000000);
                shadowPaint.setAntiAlias(true);
                shadowPaint.setAlpha((int) (uiAlpha * 255));
                canvas.drawString(versionText, x + 1, y + 1, font, shadowPaint);
            }
            canvas.drawString(versionText, x, y, font, paint);
        }
    }

    private void renderExitDialog(Canvas canvas, float mouseX, float mouseY) {
        Paint alphaPaint = new Paint();
        alphaPaint.setAlpha((int) (exitDialogAlpha * 255));

        try (Paint bgPaint = new Paint()) {
            bgPaint.setColor(0x80000000);
            bgPaint.setAlpha((int) (exitDialogAlpha * 255));
            canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(0, 0, DESIGN_WIDTH, DESIGN_HEIGHT), bgPaint);
        }

        float dialogWidth = DESIGN_WIDTH * 0.6f;
        float dialogHeight = DESIGN_HEIGHT * 0.5f;
        float dialogX = (DESIGN_WIDTH - dialogWidth) / 2f;
        float dialogY = (DESIGN_HEIGHT - dialogHeight) / 2f;

        if (dialogBase != null) {
            canvas.drawImageRect(dialogBase,
                    io.github.humbleui.types.Rect.makeXYWH(0, 0, dialogBase.getWidth(), dialogBase.getHeight()),
                    io.github.humbleui.types.Rect.makeXYWH(dialogX, dialogY, dialogWidth, dialogHeight),
                    alphaPaint
            );
        }

        if (topFrame != null) {
            float frameHeight = topFrame.getHeight() * (dialogWidth / topFrame.getWidth());
            canvas.drawImageRect(topFrame,
                    io.github.humbleui.types.Rect.makeXYWH(0, 0, topFrame.getWidth(), topFrame.getHeight()),
                    io.github.humbleui.types.Rect.makeXYWH(dialogX, dialogY, dialogWidth, frameHeight),
                    alphaPaint
            );
        }

        if (bottomFrame != null) {
            float frameHeight = bottomFrame.getHeight() * (dialogWidth / bottomFrame.getWidth());
            canvas.drawImageRect(bottomFrame,
                    io.github.humbleui.types.Rect.makeXYWH(0, 0, bottomFrame.getWidth(), bottomFrame.getHeight()),
                    io.github.humbleui.types.Rect.makeXYWH(dialogX, dialogY + dialogHeight - frameHeight, dialogWidth, frameHeight),
                    alphaPaint
            );
        }

        if (manosabaTypeface != null) {
            try (Font font = new Font(manosabaTypeface, 24f);
                 Paint paint = new Paint()) {
                paint.setColor(0xFF332B2B);
                paint.setAntiAlias(true);
                paint.setAlpha((int) (exitDialogAlpha * 255));
                String text = "\u5373\u5c06\u7ed3\u675f\u6e38\u620f\u3002";
                float textWidth = font.measureTextWidth(text);
                canvas.drawString(text, (DESIGN_WIDTH - textWidth) / 2f, dialogY + dialogHeight * 0.35f, font, paint);
            }
        }

        hoveredDialogButton = -1;
        if (buttonDefault != null) {
            float btnWidth = buttonDefault.getWidth() * 0.5f;
            float btnHeight = buttonDefault.getHeight() * 0.5f;
            float btnY = dialogY + dialogHeight * 0.55f;
            float gap = 40f;

            float cancelX = (DESIGN_WIDTH - btnWidth * 2 - gap) / 2f;
            float confirmX = cancelX + btnWidth + gap;

            boolean cancelHovered = mouseX >= cancelX && mouseX <= cancelX + btnWidth
                    && mouseY >= btnY && mouseY <= btnY + btnHeight;
            boolean confirmHovered = mouseX >= confirmX && mouseX <= confirmX + btnWidth
                    && mouseY >= btnY && mouseY <= btnY + btnHeight;

            if (cancelHovered) hoveredDialogButton = 0;
            if (confirmHovered) hoveredDialogButton = 1;

            Image cancelSprite = (cancelHovered && buttonHighlighted != null) ? buttonHighlighted : buttonDefault;
            Image confirmSprite = (confirmHovered && buttonHighlighted != null) ? buttonHighlighted : buttonDefault;

            canvas.drawImageRect(cancelSprite,
                    io.github.humbleui.types.Rect.makeXYWH(0, 0, cancelSprite.getWidth(), cancelSprite.getHeight()),
                    io.github.humbleui.types.Rect.makeXYWH(cancelX, btnY, btnWidth, btnHeight),
                    alphaPaint
            );
            canvas.drawImageRect(confirmSprite,
                    io.github.humbleui.types.Rect.makeXYWH(0, 0, confirmSprite.getWidth(), confirmSprite.getHeight()),
                    io.github.humbleui.types.Rect.makeXYWH(confirmX, btnY, btnWidth, btnHeight),
                    alphaPaint
            );

            if (manosabaTypeface != null) {
                try (Font btnFont = new Font(manosabaTypeface, 30f);
                     Paint cancelPaint = new Paint();
                     Paint confirmPaint1 = new Paint();
                     Paint confirmPaint2 = new Paint()) {

                    cancelPaint.setColor(0xFFD1BDB7);
                    cancelPaint.setAntiAlias(true);
                    cancelPaint.setAlpha((int) (exitDialogAlpha * 255));
                    String cancelText = "\u53d6\u6d88";
                    float cancelTextWidth = btnFont.measureTextWidth(cancelText);
                    canvas.drawString(cancelText, cancelX + (btnWidth - cancelTextWidth) / 2f,
                            btnY + btnHeight / 2f + btnFont.getMetrics().getCapHeight() / 2f, btnFont, cancelPaint);

                    confirmPaint1.setColor(0xFFF7879B);
                    confirmPaint1.setAntiAlias(true);
                    confirmPaint1.setAlpha((int) (exitDialogAlpha * 255));
                    confirmPaint2.setColor(0xFFFFFFFF);
                    confirmPaint2.setAntiAlias(true);
                    confirmPaint2.setAlpha((int) (exitDialogAlpha * 255));

                    String confirmChar1 = "\u7ed3";
                    String confirmChar2 = "\u675f";
                    float char1Width = btnFont.measureTextWidth(confirmChar1);
                    float char2Width = btnFont.measureTextWidth(confirmChar2);
                    float totalWidth = char1Width + char2Width;
                    float startX = confirmX + (btnWidth - totalWidth) / 2f;
                    float textY = btnY + btnHeight / 2f + btnFont.getMetrics().getCapHeight() / 2f;

                    canvas.drawString(confirmChar1, startX, textY, btnFont, confirmPaint1);
                    canvas.drawString(confirmChar2, startX + char1Width, textY, btnFont, confirmPaint2);
                }
            }
        }

        alphaPaint.close();
    }

    private ButtonConfig[] getButtonConfigs() {
        return new ButtonConfig[]{
                new ButtonConfig("LoadGame", 20, 0),
                new ButtonConfig("NewGame", -20, 1),
                new ButtonConfig("Gallery", 20, 2),
                new ButtonConfig("Options", -20, 3),
                new ButtonConfig("Exit", 20, 4)
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showExitDialog) {
            if (hoveredDialogButton == 0) {
                exitDialogClosing = true;
                exitDialogResult = false;
                return true;
            } else if (hoveredDialogButton == 1) {
                exitDialogClosing = true;
                exitDialogResult = true;
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (hoveredButton >= 0) {
            ButtonConfig[] buttons = getButtonConfigs();
            if (hoveredButton < buttons.length) {
                switch (buttons[hoveredButton].actionId) {
                    case 0 -> Minecraft.getInstance().setScreen(new SelectWorldScreen(this));
                    case 1 -> CreateWorldScreen.openFresh(Minecraft.getInstance(), this);
                    case 2 -> Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(this));
                    case 3 -> Minecraft.getInstance().setScreen(new OptionsScreen(this, Minecraft.getInstance().options));
                    case 4 -> {
                        showExitDialog = true;
                        exitDialogAlpha = 0f;
                        exitDialogClosing = false;
                    }
                }
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
    }

    @Override
    public void removed() {
        closeSkiaResources();
        super.removed();
    }

    @Override
    public void resize(Minecraft mc, int width, int height) {
        if (surface != null) { surface.close(); surface = null; }
        if (renderTarget != null) { renderTarget.close(); renderTarget = null; }
        super.resize(mc, width, height);
    }

    private static class ButtonConfig {
        final String name;
        final int yOffset;
        final int actionId;

        ButtonConfig(String name, int yOffset, int actionId) {
            this.name = name;
            this.yOffset = yOffset;
            this.actionId = actionId;
        }
    }
}