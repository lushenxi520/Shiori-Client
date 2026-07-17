package client.ui;

import client.Shiori;
import client.modules.Category;
import client.modules.Module;
import client.utils.renderer.SkijaRenderer;
import client.values.Value;
import client.values.ValueType;
import client.values.impl.BooleanValue;
import client.values.impl.FloatValue;
import client.values.impl.ModeValue;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClickGUI extends Screen {
    public static final Map<String, int[]> panelPositions = new HashMap<>();

    private static final int PANEL_W = 620;
    private static final int PANEL_H = 360;
    private static final int SIDEBAR_W = 42;
    private static final int MODULE_LIST_W = 175;
    private static final int PADDING = 7;
    private static final int RADIUS = 12;

    private static final int ROW_H = 20;
    private static final int SEARCH_H = 22;
    private static final int CARD_GAP = 2;

    private static final int BG_BASE = 0xFF0D0D12;
    private static final int BG_SURFACE = 0xFF14141A;
    private static final int BG_ELEVATED = 0xFF1A1A24;
    private static final int BG_HOVER = 0xFF242438;
    private static final int ACCENT = 0xFF7C5CFC;
    private static final int ACCENT_SECONDARY = 0xFF5BC0EB;
    private static final int TEXT_PRIMARY = 0xFFEEEEF0;
    private static final int TEXT_SECONDARY = 0xFF8888A0;
    private static final int TEXT_MUTED = 0xFF55556A;
    private static final int BORDER = 0xFF1E1E2E;

    private static final int[] CATEGORY_COLORS = {
            0xFFF43F5E, 0xFF22C55E, 0xFF7C5CFC, 0xFFF59E0B,
    };

    private Category selectedCategory = Category.COMBAT;
    private Module selectedModule = null;
    private String searchText = "";
    private boolean searchFocused = false;
    private long lastTime = System.currentTimeMillis();

    private float openProgress = 0.0f;
    private boolean closing = false;
    private boolean firstInit = true;

    private float moduleScrollOffset = 0.0f;
    private float moduleScrollTarget = 0.0f;
    private float moduleMaxScroll = 0.0f;

    private float settingsScrollOffset = 0.0f;
    private float settingsScrollTarget = 0.0f;
    private float settingsMaxScroll = 0.0f;

    private final Map<Category, Float> catHoverAnim = new HashMap<>();
    private final Map<Category, Float> catActiveAnim = new HashMap<>();
    private final Map<Category, Float> catClickAnim = new HashMap<>();
    private final Map<Module, Float> moduleHoverAnim = new HashMap<>();
    private final Map<Module, Float> moduleToggleAnim = new HashMap<>();
    private final Map<String, Float> boolToggleAnim = new HashMap<>();

    private float settingsFadeAnim = 0.0f;
    private float settingsFadeTarget = 0.0f;
    private Module lastSettingsModule = null;

    private boolean draggingSlider = false;
    private FloatValue draggingFloatSetting = null;

    private final List<Module> filteredModules = new ArrayList<>();
    private final List<Value> selectedModuleSettings = new ArrayList<>();
    private final Map<String, Boolean> dropdownStates = new HashMap<>();

    public ClickGUI() {
        super(Component.nullToEmpty("Click GUI"));
        for (Category cat : Category.values()) {
            catHoverAnim.put(cat, 0.0f);
            catActiveAnim.put(cat, cat == selectedCategory ? 1.0f : 0.0f);
            catClickAnim.put(cat, 0.0f);
        }
        rebuildModuleList();
    }

    @Override
    public void init() {
        super.init();
        if (firstInit) {
            openProgress = 0.0f;
            firstInit = false;
        }
    }

    private void rebuildModuleList() {
        filteredModules.clear();
        filteredModules.addAll(
                Shiori.getInstance().getModuleManager()
                        .getModulesByCategory(selectedCategory).stream()
                        .filter(m -> searchText.isEmpty() ||
                                m.getName().toLowerCase().contains(searchText.toLowerCase()))
                        .collect(Collectors.toList())
        );
        moduleMaxScroll = Math.max(0,
                filteredModules.size() * (ROW_H + CARD_GAP) - getModuleListContentHeight());
        moduleScrollOffset = 0;
        moduleScrollTarget = 0;
    }

    private void rebuildSettings() {
        selectedModuleSettings.clear();
        dropdownStates.clear();
        if (selectedModule == null) {
            settingsFadeTarget = 0.0f;
            lastSettingsModule = null;
            return;
        }
        if (lastSettingsModule != selectedModule) {
            settingsFadeTarget = 0.0f;
            lastSettingsModule = selectedModule;
        }
        selectedModuleSettings.addAll(
                Shiori.getInstance().getValueManager()
                        .getValuesByHasValue(selectedModule).stream()
                        .filter(Value::isVisible)
                        .collect(Collectors.toList())
        );
        settingsScrollOffset = 0;
        settingsScrollTarget = 0;
        settingsMaxScroll = Math.max(0,
                getSettingsContentHeight() - (getSettingsVisibleHeight() - 42));
        settingsFadeTarget = 1.0f;
    }

    private int getSettingsContentHeight() {
        int h = 0;
        for (Value v : selectedModuleSettings) {
            h += ROW_H + 8;
            if (v.getValueType() == ValueType.MODE &&
                    dropdownStates.getOrDefault(v.getName(), false)) {
                h += ((ModeValue) v).getValues().length * ROW_H;
            }
        }
        return h;
    }

    private int getModuleListContentHeight() {
        return PANEL_H - PADDING * 2 - SEARCH_H - 6;
    }

    private int getSettingsVisibleHeight() {
        return PANEL_H - PADDING * 2 - 32;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float dt = (float) (now - lastTime) / 1000.0f;
        lastTime = now;
        if (dt <= 0.0f || dt > 0.1f) dt = 0.016f;

        if (closing) {
            openProgress -= dt * 5.0f;
            if (openProgress <= 0.0f) {
                openProgress = 0.0f;
                closing = false;
                firstInit = true;
                this.minecraft.setScreen(null);
                return;
            }
        } else {
            openProgress += dt * 5.0f;
            if (openProgress > 1.0f) openProgress = 1.0f;
        }

        settingsFadeAnim += (settingsFadeTarget - settingsFadeAnim) * Math.min(dt * 12.0f, 1.0f);
        if (Math.abs(settingsFadeAnim - settingsFadeTarget) < 0.003f)
            settingsFadeAnim = settingsFadeTarget;

        for (Category cat : Category.values()) {
            float target = cat == selectedCategory ? 1.0f : 0.0f;
            float current = catActiveAnim.get(cat);
            current += (target - current) * Math.min(dt * 10.0f, 1.0f);
            catActiveAnim.put(cat, current);
        }

        // Decay click animations
        for (Category cat : Category.values()) {
            float anim = catClickAnim.get(cat);
            anim -= dt * 5.0f;
            if (anim < 0.0f) anim = 0.0f;
            catClickAnim.put(cat, anim);
        }

        SkijaRenderer.beginFrame();
        Canvas canvas = SkijaRenderer.getCanvas();
        if (canvas == null) return;

        SkijaRenderer.fillRect(canvas, 0, 0, this.width, this.height, 0x00000000);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int px = cx - PANEL_W / 2;
        int py = cy - PANEL_H / 2;

        float eased = easeOutCubic(openProgress);
        float scale = 0.94f + 0.06f * eased;
        int alpha = (int) (255.0f * eased);
        if (alpha <= 0) {
            SkijaRenderer.endFrame();
            return;
        }

        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);
        canvas.translate(-cx, -cy);

        float tmx = toLocalX(mouseX, cx, scale);
        float tmy = toLocalY(mouseY, cy, scale);

        drawPanelBackground(canvas, px, py, alpha);
        drawSidebar(canvas, px, py, tmx, tmy, alpha, dt);
        drawModuleListPanel(canvas, px, py, tmx, tmy, alpha, dt);
        drawSettingsPanel(canvas, px, py, tmx, tmy, alpha, dt);

        canvas.restore();
        SkijaRenderer.endFrame();
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawPanelBackground(Canvas canvas, int px, int py, int alpha) {
        SkijaRenderer.drawRoundedRect(canvas, px, py, PANEL_W, PANEL_H, RADIUS,
                withAlpha(BG_BASE, (int) (245.0f * alpha / 255.0f)));

        try (Paint paint = new Paint()) {
            paint.setColor(withAlpha(0x88FFFFFF, (int) (5.0f * alpha / 255.0f)));
            paint.setAntiAlias(true);
            paint.setMode(PaintMode.STROKE);
            paint.setStrokeWidth(1.0f);
            canvas.drawRRect(
                    RRect.makeLTRB(px, py, px + PANEL_W, py + PANEL_H, RADIUS), paint);
        }

        SkijaRenderer.drawGlowRoundedRect(canvas, px, py, PANEL_W, PANEL_H, RADIUS,
                withAlpha(ACCENT, (int) (6.0f * alpha / 255.0f)), 18.0f);
    }

    private void drawSidebar(Canvas canvas, int px, int py, float mx, float my,
                             int alpha, float dt) {
        int sx = px + PADDING;
        int sy = py + PADDING;
        int sh = PANEL_H - PADDING * 2;

        SkijaRenderer.drawRoundedRect(canvas, sx, sy, SIDEBAR_W, sh, RADIUS - 2,
                withAlpha(BG_SURFACE, alpha));

        Category[] cats = Category.values();
        float iconAreaH = 36.0f;
        float totalH = cats.length * iconAreaH;
        float startY = sy + (sh - totalH) / 2.0f;

        Font iconFont = SkijaRenderer.getIconFont(11.0f);
        Font labelFont = SkijaRenderer.getOpensansFont(6.0f);

        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            float areaTop = startY + i * iconAreaH;
            float areaCx = sx + SIDEBAR_W / 2.0f;
            float areaCy = areaTop + iconAreaH / 2.0f;

            boolean hovered = mx >= sx && mx <= sx + SIDEBAR_W
                    && my >= areaTop && my <= areaTop + iconAreaH;

            float curHover = catHoverAnim.get(cat);
            float targetHover = hovered ? 1.0f : 0.0f;
            curHover += (targetHover - curHover) * Math.min(dt * 10.0f, 1.0f);
            catHoverAnim.put(cat, curHover);

            float activeAnim = catActiveAnim.get(cat);
            int catColor = CATEGORY_COLORS[i % CATEGORY_COLORS.length];

            // Click animation scale
            float clickAnim = catClickAnim.get(cat);
            float iconScale = 1.0f - clickAnim * 0.15f;

            // Background highlight
            float bgAlpha = 0.0f;
            if (cat == selectedCategory) {
                bgAlpha = 0.12f;
            } else if (curHover > 0.01f) {
                bgAlpha = 0.06f * curHover;
            }
            if (bgAlpha > 0.001f) {
                int bg = (catColor & 0x00FFFFFF) | (((int)(bgAlpha * 255)) << 24);
                SkijaRenderer.drawRoundedRect(canvas,
                        sx + 3, areaTop + 2, SIDEBAR_W - 6, iconAreaH - 4, 7.0f, bg);
            }

            // Active indicator line
            float indicatorAlpha = activeAnim;
            if (indicatorAlpha > 0.01f) {
                SkijaRenderer.drawRoundedRect(canvas,
                        sx + 2, areaTop + iconAreaH * 0.25f,
                        2.0f, iconAreaH * 0.5f * indicatorAlpha, 1.0f,
                        withAlpha(catColor, alpha));
            }

            // Icon with click scale animation
            String icon = cat.getIcon();
            float iw = SkijaRenderer.measureTextWidth(icon, iconFont);

            int iconColor = cat == selectedCategory
                    ? catColor
                    : lerpColor(TEXT_MUTED, TEXT_SECONDARY, (int) (curHover * 255));

            float iconY = areaCy - 5;
            canvas.save();
            canvas.translate(areaCx, iconY);
            canvas.scale(iconScale, iconScale);
            canvas.translate(-areaCx, -iconY);
            SkijaRenderer.drawText(canvas, icon,
                    areaCx - iw / 2.0f, iconY,
                    iconFont, withAlpha(iconColor, alpha));
            canvas.restore();

            // Label
            String label = cat.getDisplayName().substring(0,
                    Math.min(3, cat.getDisplayName().length()));
            float lw = SkijaRenderer.measureTextWidth(label, labelFont);
            float labelFontH = SkijaRenderer.getFontHeight(labelFont);
            SkijaRenderer.drawText(canvas, label,
                    areaCx - lw / 2.0f, iconY + labelFontH + 2,
                    labelFont, withAlpha(iconColor, alpha));
        }
    }

    private void drawModuleListPanel(Canvas canvas, int px, int py, float mx, float my,
                                     int alpha, float dt) {
        int lx = px + PADDING + SIDEBAR_W + PADDING;
        int ly = py + PADDING;

        moduleScrollOffset += (moduleScrollTarget - moduleScrollOffset)
                * Math.min(dt * 14.0f, 1.0f);
        if (Math.abs(moduleScrollOffset - moduleScrollTarget) < 0.3f)
            moduleScrollOffset = moduleScrollTarget;

        drawSearchBar(canvas, lx, ly, mx, my, alpha);

        int contentY = ly + SEARCH_H + 6;
        int contentH = PANEL_H - PADDING * 2 - SEARCH_H - 6;

        canvas.save();
        canvas.clipRect(Rect.makeXYWH(lx, contentY, MODULE_LIST_W, contentH));

        Font nameFont = SkijaRenderer.getOpensansFont(8.0f);
        float fontH = SkijaRenderer.getFontHeight(nameFont);

        for (int i = 0; i < filteredModules.size(); i++) {
            Module m = filteredModules.get(i);
            float rowY = contentY + i * (ROW_H + CARD_GAP) - moduleScrollOffset;

            if (rowY + ROW_H < contentY || rowY > contentY + contentH) continue;

            boolean hovered = mx >= lx && mx <= lx + MODULE_LIST_W
                    && my >= rowY && my <= rowY + ROW_H;
            boolean isSelected = m == selectedModule;

            // Hover animation
            float curModHover = moduleHoverAnim.getOrDefault(m, 0.0f);
            float targetModHover = hovered ? 1.0f : 0.0f;
            curModHover += (targetModHover - curModHover) * Math.min(dt * 12.0f, 1.0f);
            moduleHoverAnim.put(m, curModHover);

            // Toggle animation
            float curToggle = moduleToggleAnim.getOrDefault(m, 0.0f);
            float targetToggle = m.isEnabled() ? 1.0f : 0.0f;
            curToggle += (targetToggle - curToggle) * Math.min(dt * 10.0f, 1.0f);
            if (Math.abs(curToggle - targetToggle) < 0.005f) curToggle = targetToggle;
            moduleToggleAnim.put(m, curToggle);

            // Hover scale + translate
            float hoverScale = 1.0f + curModHover * 0.02f;
            float hoverTransX = curModHover * 3.0f;

            canvas.save();
            float itemCx = lx + MODULE_LIST_W / 2.0f;
            float itemCy = rowY + ROW_H / 2.0f;
            canvas.translate(itemCx, itemCy);
            canvas.scale(hoverScale, hoverScale);
            canvas.translate(-itemCx, -itemCy);

            // Background
            int bgColor = 0;
            if (isSelected) {
                bgColor = BG_ELEVATED;
            } else if (curModHover > 0.01f) {
                bgColor = BG_HOVER;
            }

            if (bgColor != 0) {
                SkijaRenderer.drawRoundedRect(canvas, lx + 2, rowY, MODULE_LIST_W - 4, ROW_H, 4.0f,
                        withAlpha(bgColor, alpha));
            }

            if (isSelected) {
                SkijaRenderer.drawRoundedRect(canvas, lx + 2, rowY, 2.0f, ROW_H, 1.0f,
                        withAlpha(ACCENT, alpha));
            }

            // Text color lerp based on toggle animation
            int textColor = lerpColor(TEXT_PRIMARY, ACCENT, (int) (curToggle * 255));
            float textY = rowY + (ROW_H + fontH) / 2.0f - fontH;
            SkijaRenderer.drawText(canvas, m.getName(),
                    lx + (isSelected ? 12 : 10) + hoverTransX, textY,
                    nameFont, withAlpha(textColor, alpha));

            canvas.restore();

            drawMiniToggle(canvas,
                    lx + MODULE_LIST_W - 20, rowY + (ROW_H - 9) / 2.0f,
                    m.isEnabled(), alpha, curToggle);
        }

        canvas.restore();

        if (moduleMaxScroll > 0) {
            float thumbH = Math.max(16.0f,
                    contentH / (filteredModules.size() * (ROW_H + CARD_GAP)) * contentH);
            float thumbY = contentY + (moduleScrollOffset / moduleMaxScroll) * (contentH - thumbH);
            SkijaRenderer.drawRoundedRect(canvas, lx + MODULE_LIST_W - 3, thumbY, 2.0f, thumbH, 1.0f,
                    withAlpha(TEXT_MUTED, (int) (40.0f * alpha / 255.0f)));
        }
    }

    private void drawSearchBar(Canvas canvas, int x, int y, float mx, float my, int alpha) {
        SkijaRenderer.drawRoundedRect(canvas, x, y, MODULE_LIST_W, SEARCH_H, 6.0f,
                withAlpha(BG_ELEVATED, alpha));

        Font searchFont = SkijaRenderer.getOpensansFont(7.0f);
        String display = searchText.isEmpty() && !searchFocused ? "Search..." : searchText;
        int textColor = searchText.isEmpty() && !searchFocused
                ? withAlpha(TEXT_MUTED, (int) (alpha * 0.6f))
                : withAlpha(TEXT_PRIMARY, alpha);

        float searchFontH = SkijaRenderer.getFontHeight(searchFont);
        float textY = y + (SEARCH_H + searchFontH) / 2.0f - searchFontH;
        SkijaRenderer.drawText(canvas, display, x + 8,
                textY, searchFont, textColor);

        if (searchFocused) {
            float cursorX = x + 8 + SkijaRenderer.measureTextWidth(searchText, searchFont);
            long tick = System.currentTimeMillis() % 1000;
            if (tick < 500) {
                SkijaRenderer.fillRect(canvas, cursorX + 1, y + 4, 1.0f, SEARCH_H - 8,
                        withAlpha(ACCENT, alpha));
            }
        }
    }

    private void drawMiniToggle(Canvas canvas, float x, float y, boolean on, int alpha, float anim) {
        float tw = 16.0f;
        float th = 9.0f;

        // Track color lerp
        int trackColor = lerpColor(BG_HOVER, ACCENT, (int) (anim * 255));
        SkijaRenderer.drawRoundedRect(canvas, x, y, tw, th, 4.5f,
                withAlpha(trackColor, alpha));

        // Knob position lerp
        float kr = 3.5f;
        float offX = tw - kr * 2 - 1;
        float kx = x + 1 + offX * anim;
        float ky = y + (th - kr * 2) / 2.0f;
        SkijaRenderer.drawRoundedRect(canvas, kx, ky, kr * 2, kr * 2, kr,
                withAlpha(0xFFFFFFFF, alpha));
    }

    private void drawSettingsPanel(Canvas canvas, int px, int py, float mx, float my,
                                   int alpha, float dt) {
        int sx = px + PADDING + SIDEBAR_W + PADDING + MODULE_LIST_W + PADDING;
        int sy = py + PADDING;
        int sw = PANEL_W - PADDING - SIDEBAR_W - PADDING - MODULE_LIST_W - PADDING * 2;
        int sh = getSettingsVisibleHeight();

        SkijaRenderer.drawRoundedRect(canvas, sx, sy, sw, sh, RADIUS - 2,
                withAlpha(BG_SURFACE, alpha));

        if (selectedModule == null || settingsFadeAnim < 0.01f) {
            Font hintFont = SkijaRenderer.getOpensansFont(8.0f);
            String hint = "Right-click to edit";
            float hw = SkijaRenderer.measureTextWidth(hint, hintFont);
            float hintFontH = SkijaRenderer.getFontHeight(hintFont);
            int hintAlpha = (int) (120.0f * alpha / 255.0f);
            if (selectedModule == null) hintAlpha = (int) (hintAlpha * (1.0f - settingsFadeAnim));
            float hintY = sy + (sh + hintFontH) / 2.0f - hintFontH;
            SkijaRenderer.drawText(canvas, hint,
                    sx + (sw - hw) / 2.0f, hintY,
                    hintFont, withAlpha(TEXT_MUTED, Math.max(0, hintAlpha)));
            return;
        }

        float fade = settingsFadeAnim;
        float slide = (1.0f - fade) * 16.0f;

        settingsScrollOffset += (settingsScrollTarget - settingsScrollOffset)
                * Math.min(dt * 14.0f, 1.0f);
        if (Math.abs(settingsScrollOffset - settingsScrollTarget) < 0.3f)
            settingsScrollOffset = settingsScrollTarget;

        Font headerFont = SkijaRenderer.getOpensansFont(12.0f);
        float headerFontH = SkijaRenderer.getFontHeight(headerFont);
        int nameColor = selectedModule.isEnabled() ? ACCENT : TEXT_PRIMARY;

        SkijaRenderer.drawRoundedRect(canvas, sx + 6, sy + 8, sw - 12, 24, 4.0f,
                withAlpha(BG_ELEVATED, (int) (alpha * fade)));

        float headerTextY = sy + 8 + (24 + headerFontH) / 2.0f - headerFontH + slide;
        SkijaRenderer.drawText(canvas, selectedModule.getName(),
                sx + 14, headerTextY,
                headerFont, withAlpha(nameColor, (int) (alpha * fade)));

        Font settingFont = SkijaRenderer.getOpensansFont(8.0f);
        float fontH = SkijaRenderer.getFontHeight(settingFont);

        int contentY = sy + 42 + (int) slide;
        int visibleH = sh - 42;

        canvas.save();
        canvas.clipRect(Rect.makeXYWH(sx, sy + 32, sw, visibleH + 16));

        int accY = 0;
        for (int i = 0; i < selectedModuleSettings.size(); i++) {
            Value v = selectedModuleSettings.get(i);
            int compH = ROW_H + 8;
            if (v.getValueType() == ValueType.MODE &&
                    dropdownStates.getOrDefault(v.getName(), false)) {
                compH += ((ModeValue) v).getValues().length * ROW_H;
            }

            float rowY = contentY + accY - settingsScrollOffset;

            if (rowY + compH < contentY || rowY > contentY + visibleH) {
                accY += compH;
                continue;
            }

            boolean hovered = mx >= sx + 6 && mx <= sx + sw - 6
                    && my >= rowY && my <= rowY + ROW_H + 4;

            if (hovered) {
                SkijaRenderer.drawRoundedRect(canvas, sx + 6, rowY, sw - 12, ROW_H + 8, 4.0f,
                        withAlpha(BG_HOVER, (int) (alpha * fade)));
            }

            float settingTextY = rowY + (ROW_H + 8 + fontH) / 2.0f - fontH;
            SkijaRenderer.drawText(canvas, v.getName(),
                    sx + 12, settingTextY,
                    settingFont, withAlpha(TEXT_SECONDARY, (int) (alpha * fade)));

            float rightX = sx + sw - 12;

            if (v.getValueType() == ValueType.BOOLEAN) {
                drawToggle(canvas, rightX, rowY, (BooleanValue) v, alpha, fade, dt);
            } else if (v.getValueType() == ValueType.FLOAT) {
                drawSlider(canvas, sx, rightX, rowY, (FloatValue) v, mx, alpha, fade);
            } else if (v.getValueType() == ValueType.MODE) {
                drawDropdown(canvas, sx, rightX, rowY, (ModeValue) v, mx, my, alpha, fade);
            }

            accY += compH;
        }

        canvas.restore();

        if (settingsMaxScroll > 0) {
            float thumbH = Math.max(16.0f,
                    visibleH / (float) getSettingsContentHeight() * visibleH);
            float thumbY = sy + 32 + (settingsScrollOffset / settingsMaxScroll) * (visibleH - thumbH);
            SkijaRenderer.drawRoundedRect(canvas, sx + sw - 3, thumbY, 2.0f, thumbH, 1.0f,
                    withAlpha(TEXT_MUTED, (int) (40.0f * alpha * fade / 255.0f)));
        }
    }

    private void drawToggle(Canvas canvas, float rightX, float rowY,
                            BooleanValue bv, int alpha, float fade, float dt) {
        boolean on = bv.getCurrentValue();
        String key = bv.getName() + "@" + (selectedModule != null ? selectedModule.getName() : "");

        float curAnim = boolToggleAnim.getOrDefault(key, on ? 1.0f : 0.0f);
        float target = on ? 1.0f : 0.0f;
        curAnim += (target - curAnim) * Math.min(dt * 10.0f, 1.0f);
        if (Math.abs(curAnim - target) < 0.003f) curAnim = target;
        boolToggleAnim.put(key, curAnim);

        float tw = 28.0f;
        float th = 14.0f;
        float tx = rightX - tw;
        float ty = rowY + (ROW_H + 8 - th) / 2.0f;

        int trackColor = lerpColor(BG_HOVER, ACCENT, (int) (curAnim * 255));
        SkijaRenderer.drawRoundedRect(canvas, tx, ty, tw, th, 7.0f,
                withAlpha(trackColor, (int) (alpha * fade)));

        float kr = 5.0f;
        float offX = tw - kr * 2 - 2;
        float kx = tx + 2 + offX * curAnim;
        float ky = ty + (th - kr * 2) / 2.0f;
        SkijaRenderer.drawRoundedRect(canvas, kx, ky, kr * 2, kr * 2, kr,
                withAlpha(0xFFFFFFFF, (int) (alpha * fade)));
    }

    private void drawSlider(Canvas canvas, int sx, float rightX, float rowY,
                            FloatValue fv, float mx, int alpha, float fade) {
        float sliderW = (rightX - sx - 36) - 36;
        float sliderX = sx + 36;
        float sliderY = rowY + (ROW_H + 8) / 2.0f + 4;
        float sliderH = 3.0f;

        float min = fv.getMinValue();
        float max = fv.getMaxValue();
        float val = fv.getCurrentValue();
        float progress = (val - min) / (max - min);

        SkijaRenderer.drawRoundedRect(canvas, sliderX, sliderY, sliderW, sliderH, 1.5f,
                withAlpha(BG_HOVER, (int) (alpha * fade)));

        int[] gradColors = new int[]{ACCENT, ACCENT_SECONDARY};
        float[] gradPositions = new float[]{0.0f, 1.0f};
        try (Shader shader = Shader.makeLinearGradient(
                sliderX, sliderY, sliderX + sliderW * progress, sliderY,
                gradColors, gradPositions)) {
            try (Paint paint = new Paint()) {
                paint.setShader(shader);
                paint.setAntiAlias(true);
                canvas.drawRRect(RRect.makeLTRB(
                        sliderX, sliderY, sliderX + sliderW * progress,
                        sliderY + sliderH, 1.5f), paint);
            }
        }

        float knobX = sliderX + sliderW * progress - 4;
        float knobY = sliderY - 3;
        SkijaRenderer.drawRoundedRect(canvas, knobX, knobY, 8, 8, 4.0f,
                withAlpha(0xFFFFFFFF, (int) (alpha * fade)));

        Font valFont = SkijaRenderer.getOpensansFont(7.0f);
        String valText = fv.getStep() % 1.0f == 0.0f
                ? String.format("%d", (int) val)
                : String.format("%.2f", val);
        float tw = SkijaRenderer.measureTextWidth(valText, valFont);
        float valFontH = SkijaRenderer.getFontHeight(valFont);
        float valTextY = rowY + (ROW_H + 8 + valFontH) / 2.0f - valFontH;
        SkijaRenderer.drawText(canvas, valText, rightX - tw,
                valTextY, valFont, withAlpha(TEXT_PRIMARY, (int) (alpha * fade)));
    }

    private void drawDropdown(Canvas canvas, int sx, float rightX, float rowY,
                              ModeValue mv, float mx, float my, int alpha, float fade) {
        String sel = mv.getCurrentMode();
        Font font = SkijaRenderer.getOpensansFont(8.0f);
        float tw = SkijaRenderer.measureTextWidth(sel, font);

        float boxW = Math.max(72, tw + 22);
        float boxX = rightX - boxW;
        float boxH = ROW_H + 2;
        float boxY = rowY + 3;

        boolean open = dropdownStates.getOrDefault(mv.getName(), false);
        boolean hovered = mx >= boxX && mx <= boxX + boxW && my >= boxY && my <= boxY + boxH;

        int boxBg = hovered ? BG_ELEVATED : BG_SURFACE;
        SkijaRenderer.drawRoundedRect(canvas, boxX, boxY, boxW, boxH, 4.0f,
                withAlpha(boxBg, (int) (alpha * fade)));

        try (Paint paint = new Paint()) {
            paint.setColor(withAlpha(BORDER, (int) (80.0f * alpha * fade / 255.0f)));
            paint.setAntiAlias(true);
            paint.setMode(PaintMode.STROKE);
            paint.setStrokeWidth(1.0f);
            canvas.drawRRect(
                    RRect.makeLTRB(boxX, boxY, boxX + boxW, boxY + boxH, 4.0f), paint);
        }

        float fontH = SkijaRenderer.getFontHeight(font);
        float textY = boxY + (boxH + fontH) / 2.0f - fontH;
        SkijaRenderer.drawText(canvas, sel, boxX + 7,
                textY, font, withAlpha(ACCENT, (int) (alpha * fade)));

        Font arrowFont = SkijaRenderer.getIconFont(6.0f);
        String arrow = open ? "^" : "v";
        float aw = SkijaRenderer.measureTextWidth(arrow, arrowFont);
        float arrowFontH = SkijaRenderer.getFontHeight(arrowFont);
        float arrowY = boxY + (boxH + arrowFontH) / 2.0f - arrowFontH;
        SkijaRenderer.drawText(canvas, arrow, boxX + boxW - aw - 7,
                arrowY, arrowFont, withAlpha(TEXT_SECONDARY, (int) (alpha * fade)));

        if (open) {
            String[] modes = mv.getValues();
            float ddY = boxY + boxH + 2;
            float ddH = modes.length * ROW_H + 4;

            SkijaRenderer.drawRoundedRect(canvas, boxX, ddY, boxW, ddH, 4.0f,
                    withAlpha(BG_ELEVATED, (int) (alpha * fade)));

            try (Paint paint = new Paint()) {
                paint.setColor(withAlpha(BORDER, (int) (60.0f * alpha * fade / 255.0f)));
                paint.setAntiAlias(true);
                paint.setMode(PaintMode.STROKE);
                paint.setStrokeWidth(1.0f);
                canvas.drawRRect(
                        RRect.makeLTRB(boxX, ddY, boxX + boxW, ddY + ddH, 4.0f), paint);
            }

            for (int j = 0; j < modes.length; j++) {
                float itemY = ddY + 2 + j * ROW_H;
                boolean itemHovered = mx >= boxX && mx <= boxX + boxW
                        && my >= itemY && my <= itemY + ROW_H;
                if (itemHovered) {
                    SkijaRenderer.drawRoundedRect(canvas, boxX + 3, itemY, boxW - 6, ROW_H, 3.0f,
                            withAlpha(BG_HOVER, (int) (alpha * fade)));
                }
                int itemColor = modes[j].equals(sel) ? ACCENT : TEXT_PRIMARY;
                float itemTextY = itemY + (ROW_H + fontH) / 2.0f - fontH;
                SkijaRenderer.drawText(canvas, modes[j], boxX + 7,
                        itemTextY, font, withAlpha(itemColor, (int) (alpha * fade)));
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (openProgress < 1.0f) return true;

        int cx = this.width / 2;
        int cy = this.height / 2;
        float scale = 0.94f + 0.06f * easeOutCubic(openProgress);
        float tmx = toLocalX((float) mx, cx, scale);
        float tmy = toLocalY((float) my, cy, scale);

        int px = cx - PANEL_W / 2;
        int py = cy - PANEL_H / 2;

        if (handleSidebarClick(px, py, tmx, tmy, button)) return true;
        if (handleSearchClick(px, py, tmx, tmy, button)) return true;
        if (handleModuleListClick(px, py, tmx, tmy, button)) return true;
        if (handleSettingsClick(px, py, tmx, tmy, button)) return true;

        return super.mouseClicked(mx, my, button);
    }

    private boolean handleSidebarClick(int px, int py, float mx, float my, int button) {
        int sx = px + PADDING;
        int sy = py + PADDING;
        int sh = PANEL_H - PADDING * 2;

        if (mx < sx || mx > sx + SIDEBAR_W || my < sy || my > sy + sh) return false;

        Category[] cats = Category.values();
        float iconAreaH = 36.0f;
        float totalH = cats.length * iconAreaH;
        float startY = sy + (sh - totalH) / 2.0f;

        for (int i = 0; i < cats.length; i++) {
            float areaTop = startY + i * iconAreaH;
            if (my >= areaTop && my <= areaTop + iconAreaH) {
                if (button == 0) {
                    // Trigger click animation
                    catClickAnim.put(cats[i], 1.0f);
                    selectedCategory = cats[i];
                    selectedModule = null;
                    searchText = "";
                    searchFocused = false;
                    rebuildModuleList();
                    rebuildSettings();
                }
                return true;
            }
        }
        return true;
    }

    private boolean handleSearchClick(int px, int py, float mx, float my, int button) {
        int lx = px + PADDING + SIDEBAR_W + PADDING;
        int ly = py + PADDING;
        if (mx >= lx && mx <= lx + MODULE_LIST_W && my >= ly && my <= ly + SEARCH_H) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;
        return false;
    }

    private boolean handleModuleListClick(int px, int py, float mx, float my, int button) {
        int lx = px + PADDING + SIDEBAR_W + PADDING;
        int contentY = py + PADDING + SEARCH_H + 6;
        int contentH = PANEL_H - PADDING * 2 - SEARCH_H - 6;

        if (mx < lx || mx > lx + MODULE_LIST_W
                || my < contentY || my > contentY + contentH) return false;

        for (int i = 0; i < filteredModules.size(); i++) {
            Module m = filteredModules.get(i);
            float rowY = contentY + i * (ROW_H + CARD_GAP) - moduleScrollOffset;
            if (my >= rowY && my <= rowY + ROW_H) {
                if (button == 0) {
                    m.toggle();
                } else if (button == 1) {
                    selectedModule = m;
                    rebuildSettings();
                }
                return true;
            }
        }
        return true;
    }

    private boolean handleSettingsClick(int px, int py, float mx, float my, int button) {
        int sx = px + PADDING + SIDEBAR_W + PADDING + MODULE_LIST_W + PADDING;
        int sy = py + PADDING;
        int sw = PANEL_W - PADDING - SIDEBAR_W - PADDING - MODULE_LIST_W - PADDING * 2;
        int sh = getSettingsVisibleHeight();

        if (mx < sx || mx > sx + sw || my < sy || my > sy + sh) return false;
        if (selectedModule == null) return true;

        float fade = settingsFadeAnim;
        if (fade < 0.5f) return true;

        float slide = (1.0f - fade) * 16.0f;
        int contentY = sy + 42 + (int) slide;
        int visibleH = sh - 42;

        int accY = 0;
        for (Value v : selectedModuleSettings) {
            int compH = ROW_H + 8;
            if (v.getValueType() == ValueType.MODE &&
                    dropdownStates.getOrDefault(v.getName(), false)) {
                compH += ((ModeValue) v).getValues().length * ROW_H;
            }

            float rowY = contentY + accY - settingsScrollOffset;

            if (my >= rowY && my <= rowY + ROW_H + 4) {
                if (v.getValueType() == ValueType.BOOLEAN) {
                    if (button == 0) {
                        BooleanValue bv = (BooleanValue) v;
                        bv.setCurrentValue(!bv.getCurrentValue());
                    }
                    return true;
                } else if (v.getValueType() == ValueType.FLOAT) {
                    if (button == 0) {
                        draggingSlider = true;
                        draggingFloatSetting = (FloatValue) v;
                        updateSliderFromMouse(mx, sx, (FloatValue) v);
                    }
                    return true;
                } else if (v.getValueType() == ValueType.MODE) {
                    if (button == 0) {
                        String key = v.getName();
                        boolean wasOpen = dropdownStates.getOrDefault(key, false);
                        dropdownStates.put(key, !wasOpen);
                        settingsMaxScroll = Math.max(0,
                                getSettingsContentHeight() - (getSettingsVisibleHeight() - 42));
                    }
                    return true;
                }
            }

            if (v.getValueType() == ValueType.MODE &&
                    dropdownStates.getOrDefault(v.getName(), false)) {
                ModeValue mv = (ModeValue) v;
                float ddY = rowY + ROW_H + 8;
                String[] modes = mv.getValues();

                Font font = SkijaRenderer.getOpensansFont(8.0f);
                float tw = SkijaRenderer.measureTextWidth(mv.getCurrentMode(), font);
                float boxW = Math.max(72, tw + 22);
                float boxX = sx + sw - 12 - boxW;

                if (my >= ddY + 2 && my <= ddY + 2 + modes.length * ROW_H
                        && mx >= boxX && mx <= boxX + boxW) {
                    int idx = (int) ((my - ddY - 2) / ROW_H);
                    if (idx >= 0 && idx < modes.length && button == 0) {
                        mv.setCurrentValue(idx);
                        dropdownStates.put(v.getName(), false);
                        settingsMaxScroll = Math.max(0,
                                getSettingsContentHeight() - (getSettingsVisibleHeight() - 42));
                    }
                    return true;
                }
            }

            accY += compH;
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingSlider && draggingFloatSetting != null) {
            int cx = this.width / 2;
            float scale = 0.94f + 0.06f * easeOutCubic(openProgress);
            float tmx = toLocalX((float) mx, cx, scale);
            int px = cx - PANEL_W / 2;
            updateSliderFromMouse(tmx, px, draggingFloatSetting);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingSlider = false;
        draggingFloatSetting = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollDelta) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        float scale = 0.94f + 0.06f * easeOutCubic(openProgress);
        float tmx = toLocalX((float) mx, cx, scale);
        float tmy = toLocalY((float) my, cy, scale);

        int px = cx - PANEL_W / 2;
        int py = cy - PANEL_H / 2;

        int lx = px + PADDING + SIDEBAR_W + PADDING;
        int listContentY = py + PADDING + SEARCH_H + 6;
        int listContentH = PANEL_H - PADDING * 2 - SEARCH_H - 6;

        int sx = px + PADDING + SIDEBAR_W + PADDING + MODULE_LIST_W + PADDING;
        int sy = py + PADDING;
        int sw = PANEL_W - PADDING - SIDEBAR_W - PADDING - MODULE_LIST_W - PADDING * 2;
        int sh = getSettingsVisibleHeight();

        if (tmx >= lx && tmx <= lx + MODULE_LIST_W
                && tmy >= listContentY && tmy <= listContentY + listContentH) {
            moduleScrollTarget = (float) Math.max(0,
                    Math.min(moduleMaxScroll, moduleScrollTarget - scrollDelta * 15));
            return true;
        }

        if (tmx >= sx && tmx <= sx + sw && tmy >= sy && tmy <= sy + sh) {
            settingsScrollTarget = (float) Math.max(0,
                    Math.min(settingsMaxScroll, settingsScrollTarget - scrollDelta * 15));
            return true;
        }

        return super.mouseScrolled(mx, my, scrollDelta);
    }

    private void updateSliderFromMouse(float mx, int px, FloatValue fv) {
        int sx = px + PADDING + SIDEBAR_W + PADDING + MODULE_LIST_W + PADDING;
        int sw = PANEL_W - PADDING - SIDEBAR_W - PADDING - MODULE_LIST_W - PADDING * 2;
        float sliderX = sx + 36;
        float sliderW = (sx + sw - 12) - 36 - sliderX;
        float min = fv.getMinValue();
        float max = fv.getMaxValue();
        float range = max - min;
        double clampedX = Math.max(sliderX, Math.min(mx, sliderX + sliderW));
        float newValue = min + range * (float) ((clampedX - sliderX) / sliderW);
        fv.setCurrentValue(Math.round(newValue * 100.0f) / 100.0f);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == 256) {
                searchFocused = false;
                return true;
            }
            if (keyCode == 259 && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                rebuildModuleList();
                return true;
            }
            return true;
        }
        if (keyCode == 256) {
            closing = true;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused) {
            if (codePoint >= 32 && codePoint != 127) {
                searchText += codePoint;
                rebuildModuleList();
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        closing = true;
        super.onClose();
    }

    private float toLocalX(float mx, int cx, float scale) {
        return (mx - cx) / scale + cx;
    }

    private float toLocalY(float my, int cy, float scale) {
        return (my - cy) / scale + cy;
    }

    private float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 3.0f);
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private int lerpColor(int from, int to, int alpha) {
        int fr = (from >> 16) & 0xFF, fg = (from >> 8) & 0xFF, fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF, tg = (to >> 8) & 0xFF, tb = to & 0xFF;
        float t = alpha / 255.0f;
        int r = (int) (fr + (tr - fr) * t);
        int g = (int) (fg + (tg - fg) * t);
        int b = (int) (fb + (tb - fb) * t);
        return (from & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}