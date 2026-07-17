package client.ui;

import client.Shiori;
import client.modules.Category;
import client.modules.Module;
import client.utils.Colors;
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
import io.github.humbleui.types.RRect;
import java.awt.Color;
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

    private static final int PANEL_W = 740;
    private static final int PANEL_H = 470;
    private static final int TAB_H = 38;
    private static final int SEARCH_H = 32;
    private static final int MODULE_LIST_W = 200;
    private static final int SETTINGS_X = MODULE_LIST_W + 12;
    private static final int ROW_H = 22;
    private static final int PADDING = 12;
    private static final int RADIUS = 12;

    private static final int CHROME_BG = 0xFF202124;
    private static final int CHROME_SURFACE = 0xFF292A2D;
    private static final int CHROME_SURFACE_HIGH = 0xFF35363A;
    private static final int CHROME_HOVER = 0xFF3C4043;
    private static final int CHROME_ACCENT = 0xFF8AB4F8;
    private static final int CHROME_ACCENT_DIM = 0x668AB4F8;
    private static final int CHROME_TEXT = 0xFFE8EAED;
    private static final int CHROME_TEXT_SEC = 0xFF9AA0A6;
    private static final int CHROME_BORDER = 0xFF3C4043;
    private static final int CHROME_SWITCH_OFF = 0xFF5F6368;
    private static final int CHROME_RED = 0xFFEA4335;

    private Category selectedCategory = Category.COMBAT;
    private Module selectedModule = null;
    private String searchText = "";
    private boolean searchFocused = false;
    private long lastTime = System.currentTimeMillis();

    private float openProgress = 0.0f;
    private boolean closing = false;

    private float moduleScrollOffset = 0.0f;
    private float moduleScrollTarget = 0.0f;
    private float moduleMaxScroll = 0.0f;

    private float settingsScrollOffset = 0.0f;
    private float settingsScrollTarget = 0.0f;
    private float settingsMaxScroll = 0.0f;

    private final Map<Category, Float> tabHover = new HashMap<>();
    private final Map<Category, Float> tabActiveAnim = new HashMap<>();

    private float settingsFadeAnim = 0.0f;
    private float settingsFadeTarget = 0.0f;
    private Module lastSettingsModule = null;

    private float draggingSliderValue = 0.0f;
    private boolean draggingSlider = false;
    private FloatValue draggingFloatSetting = null;

    private List<Module> filteredModules = new ArrayList<>();
    private List<Value> selectedModuleSettings = new ArrayList<>();
    private Map<String, Boolean> dropdownStates = new HashMap<>();

    public ClickGUI() {
        super(Component.nullToEmpty("Click GUI"));
        for (Category cat : Category.values()) {
            tabHover.put(cat, 0.0f);
            tabActiveAnim.put(cat, cat == selectedCategory ? 1.0f : 0.0f);
        }
        rebuildModuleList();
    }

    @Override
    public void init() {
        super.init();
        if (!closing) openProgress = 0.0f;
    }

    private void rebuildModuleList() {
        filteredModules = Shiori.getInstance().getModuleManager()
                .getModulesByCategory(selectedCategory).stream()
                .filter(m -> searchText.isEmpty() ||
                        m.getName().toLowerCase().contains(searchText.toLowerCase()))
                .collect(Collectors.toList());
        moduleMaxScroll = Math.max(0, filteredModules.size() * ROW_H - getModuleListContentHeight());
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
        selectedModuleSettings = Shiori.getInstance().getValueManager()
                .getValuesByHasValue(selectedModule).stream()
                .filter(Value::isVisible)
                .collect(Collectors.toList());
        settingsScrollOffset = 0;
        settingsScrollTarget = 0;
        settingsMaxScroll = Math.max(0, getSettingsContentHeight() - getSettingsVisibleHeight());
        settingsFadeTarget = 1.0f;
    }

    private int getSettingsContentHeight() {
        int h = 0;
        for (Value v : selectedModuleSettings) {
            h += ROW_H + 6;
            if (v.getValueType() == ValueType.MODE &&
                    dropdownStates.getOrDefault(v.getName(), false)) {
                h += ((ModeValue) v).getValues().length * ROW_H;
            }
        }
        return h;
    }

    private int getModuleListContentHeight() {
        return PANEL_H - TAB_H - SEARCH_H - PADDING * 3;
    }

    private int getSettingsVisibleHeight() {
        return PANEL_H - TAB_H - PADDING * 2;
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
                this.minecraft.setScreen(null);
                return;
            }
        } else {
            openProgress += dt * 5.0f;
            if (openProgress > 1.0f) openProgress = 1.0f;
        }

        settingsFadeAnim += (settingsFadeTarget - settingsFadeAnim) * Math.min(dt * 10.0f, 1.0f);
        if (Math.abs(settingsFadeAnim - settingsFadeTarget) < 0.005f)
            settingsFadeAnim = settingsFadeTarget;

        for (Category cat : Category.values()) {
            float target = cat == selectedCategory ? 1.0f : 0.0f;
            float current = tabActiveAnim.get(cat);
            current += (target - current) * Math.min(dt * 10.0f, 1.0f);
            tabActiveAnim.put(cat, current);
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
        float scale = 0.95f + 0.05f * eased;
        int alpha = (int) (255.0f * eased);
        if (alpha <= 0) {
            SkijaRenderer.endFrame();
            return;
        }

        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);
        canvas.translate(-cx, -cy);

        drawPanelBackground(canvas, px, py, alpha);

        float tmx = toLocalX(mouseX, cx, scale);
        float tmy = toLocalY(mouseY, cy, scale);

        drawTabBar(canvas, px, py, tmx, tmy, alpha, dt);
        drawModuleList(canvas, px, py, tmx, tmy, alpha, dt);
        drawSettingsPanel(canvas, px, py, tmx, tmy, alpha, dt);

        canvas.restore();
        SkijaRenderer.endFrame();
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawPanelBackground(Canvas canvas, int px, int py, int alpha) {
        SkijaRenderer.drawRoundedRect(canvas, px, py, PANEL_W, PANEL_H, RADIUS,
                withAlpha(CHROME_BG, (int) (240.0f * alpha / 255.0f)));

        try (Paint paint = new Paint()) {
            paint.setColor(withAlpha(0xFFFFFFFF, (int) (8.0f * alpha / 255.0f)));
            paint.setAntiAlias(true);
            paint.setMode(PaintMode.STROKE);
            paint.setStrokeWidth(1.0f);
            canvas.drawRRect(RRect.makeLTRB(px, py, px + PANEL_W, py + PANEL_H, RADIUS), paint);
        }

        SkijaRenderer.drawGlowRoundedRect(canvas, px, py, PANEL_W, PANEL_H, RADIUS,
                withAlpha(CHROME_ACCENT, (int) (10.0f * alpha / 255.0f)), 20.0f);
    }

    private void drawTabBar(Canvas canvas, int px, int py, float mx, float my, int alpha, float dt) {
        Font titleFont = SkijaRenderer.getOpensansFont(14.0f);
        float titleFontH = SkijaRenderer.getFontHeight(titleFont);
        SkijaRenderer.drawText(canvas, Shiori.CLIENT_NAME, px + PADDING + 4,
                py + (TAB_H - titleFontH) / 2.0f,
                titleFont, withAlpha(CHROME_TEXT, alpha));

        Category[] cats = Category.values();
        int tabW = 80;
        int tabH = 28;
        int startX = px + PANEL_W - (cats.length * tabW) - PADDING;
        int tabY = py + (TAB_H - tabH) / 2;

        Font tabFont = SkijaRenderer.getOpensansFont(10.0f);

        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            int tx = startX + i * tabW;
            boolean hovered = mx >= tx && mx <= tx + tabW && my >= tabY && my <= tabY + tabH;
            float curHover = tabHover.get(cat);
            float targetHover = hovered ? 1.0f : 0.0f;
            curHover += (targetHover - curHover) * Math.min(dt * 12.0f, 1.0f);
            tabHover.put(cat, curHover);

            float anim = tabActiveAnim.get(cat);
            int bgColor;
            if (cat == selectedCategory) {
                bgColor = lerpColor(CHROME_HOVER, CHROME_SURFACE_HIGH, (int) (anim * 255));
            } else {
                bgColor = lerpColor(0x00000000, CHROME_HOVER, (int) (curHover * 255));
            }
            if (bgColor != 0) {
                SkijaRenderer.drawRoundedRect(canvas, tx + 2, tabY, tabW - 4, tabH, 6.0f,
                        withAlpha(bgColor, alpha));
            }

            int textColor = cat == selectedCategory
                    ? CHROME_ACCENT
                    : lerpColor(CHROME_TEXT_SEC, CHROME_TEXT, (int) (curHover * 255));
            String label = cat.getDisplayName();
            float tw = SkijaRenderer.measureTextWidth(label, tabFont);
            float tabFontH = SkijaRenderer.getFontHeight(tabFont);
            SkijaRenderer.drawText(canvas, label,
                    tx + (tabW - tw) / 2.0f,
                    tabY + (tabH - tabFontH) / 2.0f,
                    tabFont, withAlpha(textColor, alpha));

            if (cat == selectedCategory && anim > 0.01f) {
                SkijaRenderer.drawRoundedRect(canvas,
                        tx + tabW / 4.0f, tabY + tabH - 2,
                        tabW / 2.0f * anim, 2.0f, 1.0f,
                        withAlpha(CHROME_ACCENT, (int) (alpha * anim)));
            }
        }
    }

    private void drawModuleList(Canvas canvas, int px, int py, float mx, float my, int alpha, float dt) {
        int listX = px + PADDING;
        int listY = py + TAB_H + PADDING;

        moduleScrollOffset += (moduleScrollTarget - moduleScrollOffset) * Math.min(dt * 12.0f, 1.0f);
        if (Math.abs(moduleScrollOffset - moduleScrollTarget) < 0.5f)
            moduleScrollOffset = moduleScrollTarget;

        drawSearchBar(canvas, listX, listY, mx, my, alpha, dt);

        int contentY = listY + SEARCH_H + 6;
        int contentH = getModuleListContentHeight();

        SkijaRenderer.drawRoundedRect(canvas, listX, contentY, MODULE_LIST_W, contentH, 8.0f,
                withAlpha(CHROME_SURFACE, alpha));

        canvas.save();
        canvas.clipRect(io.github.humbleui.types.Rect.makeXYWH(listX, contentY, MODULE_LIST_W, contentH));

        Font nameFont = SkijaRenderer.getOpensansFont(10.0f);
        float fontH = SkijaRenderer.getFontHeight(nameFont);

        for (int i = 0; i < filteredModules.size(); i++) {
            Module m = filteredModules.get(i);
            float rowY = contentY + i * ROW_H - moduleScrollOffset;

            if (rowY + ROW_H < contentY || rowY > contentY + contentH) continue;

            boolean hovered = mx >= listX && mx <= listX + MODULE_LIST_W
                    && my >= rowY && my <= rowY + ROW_H;
            boolean isSelected = m == selectedModule;

            int bgAlpha = 0;
            if (isSelected) bgAlpha = 80;
            else if (hovered) bgAlpha = 50;

            if (bgAlpha > 0) {
                SkijaRenderer.drawRoundedRect(canvas, listX + 2, rowY, MODULE_LIST_W - 4, ROW_H, 4.0f,
                        withAlpha(0xFFFFFFFF, (int) (bgAlpha * alpha / 255.0f)));
            }

            int textColor = m.isEnabled() ? CHROME_ACCENT : CHROME_TEXT;
            SkijaRenderer.drawText(canvas, m.getName(),
                    listX + 10, rowY + (ROW_H - fontH) / 2.0f,
                    nameFont, withAlpha(textColor, alpha));
        }

        canvas.restore();

        if (moduleMaxScroll > 0) {
            float thumbH = Math.max(24.0f, contentH / (filteredModules.size() * ROW_H) * contentH);
            float thumbY = contentY + (moduleScrollOffset / moduleMaxScroll) * (contentH - thumbH);
            SkijaRenderer.drawRoundedRect(canvas, listX + MODULE_LIST_W - 4, thumbY, 3, thumbH, 2.0f,
                    withAlpha(CHROME_TEXT_SEC, (int) (60.0f * alpha / 255.0f)));
        }
    }

    private void drawSearchBar(Canvas canvas, int x, int y, float mx, float my, int alpha, float dt) {
        SkijaRenderer.drawRoundedRect(canvas, x, y, MODULE_LIST_W, SEARCH_H, 8.0f,
                withAlpha(CHROME_SURFACE, alpha));

        Font searchFont = SkijaRenderer.getOpensansFont(10.0f);
        String display = searchText.isEmpty() && !searchFocused ? "Search modules..." : searchText;
        int textColor = searchText.isEmpty() && !searchFocused
                ? withAlpha(CHROME_TEXT_SEC, (int) (alpha * 0.6f))
                : withAlpha(CHROME_TEXT, alpha);
        float searchFontH = SkijaRenderer.getFontHeight(searchFont);

        SkijaRenderer.drawText(canvas, display, x + 10,
                y + (SEARCH_H - searchFontH) / 2.0f,
                searchFont, textColor);

        if (searchFocused) {
            float cursorX = x + 10 + SkijaRenderer.measureTextWidth(searchText, searchFont);
            long tick = System.currentTimeMillis() % 1000;
            if (tick < 500) {
                SkijaRenderer.fillRect(canvas, cursorX + 1, y + 7, 1.5f, SEARCH_H - 14,
                        withAlpha(CHROME_ACCENT, alpha));
            }
        }
    }

    private void drawSettingsPanel(Canvas canvas, int px, int py, float mx, float my, int alpha, float dt) {
        int sx = px + SETTINGS_X;
        int sy = py + TAB_H + PADDING;
        int sw = PANEL_W - SETTINGS_X - PADDING;
        int sh = getSettingsVisibleHeight();

        SkijaRenderer.drawRoundedRect(canvas, sx, sy, sw, sh, 8.0f,
                withAlpha(CHROME_SURFACE, alpha));

        if (selectedModule == null || settingsFadeAnim < 0.01f) {
            Font hintFont = SkijaRenderer.getOpensansFont(10.0f);
            String hint = "Right-click a module to edit settings";
            float hw = SkijaRenderer.measureTextWidth(hint, hintFont);
            float hintFontH = SkijaRenderer.getFontHeight(hintFont);
            int hintAlpha = (int) (150.0f * alpha / 255.0f);
            if (selectedModule == null) hintAlpha = (int) (hintAlpha * (1.0f - settingsFadeAnim));
            SkijaRenderer.drawText(canvas, hint,
                    sx + (sw - hw) / 2.0f, sy + (sh - hintFontH) / 2.0f,
                    hintFont, withAlpha(CHROME_TEXT_SEC, Math.max(0, hintAlpha)));
            return;
        }

        float fade = settingsFadeAnim;
        float slide = (1.0f - fade) * 15.0f;

        settingsScrollOffset += (settingsScrollTarget - settingsScrollOffset) * Math.min(dt * 12.0f, 1.0f);
        if (Math.abs(settingsScrollOffset - settingsScrollTarget) < 0.5f)
            settingsScrollOffset = settingsScrollTarget;

        Font headerFont = SkijaRenderer.getOpensansFont(13.0f);
        float headerFontH = SkijaRenderer.getFontHeight(headerFont);
        int nameColor = selectedModule.isEnabled() ? CHROME_ACCENT : CHROME_TEXT;
        SkijaRenderer.drawText(canvas, selectedModule.getName(),
                sx + 10, sy + 18 + slide,
                headerFont, withAlpha(nameColor, (int) (alpha * fade)));

        Font settingFont = SkijaRenderer.getOpensansFont(10.0f);
        float fontH = SkijaRenderer.getFontHeight(settingFont);

        int contentY = sy + 38 + (int) slide;
        int visibleH = sh - 38;

        int accY = 0;
        for (int i = 0; i < selectedModuleSettings.size(); i++) {
            Value v = selectedModuleSettings.get(i);
            int compH = ROW_H + 6;
            if (v.getValueType() == ValueType.MODE &&
                    dropdownStates.getOrDefault(v.getName(), false)) {
                compH += ((ModeValue) v).getValues().length * ROW_H;
            }

            float rowY = contentY + accY - settingsScrollOffset;

            if (rowY + compH < contentY || rowY > contentY + visibleH) {
                accY += compH;
                continue;
            }

            boolean hovered = mx >= sx && mx <= sx + sw
                    && my >= rowY && my <= rowY + ROW_H + 6;

            if (hovered) {
                SkijaRenderer.drawRoundedRect(canvas, sx + 4, rowY, sw - 8, ROW_H + 6, 4.0f,
                        withAlpha(CHROME_HOVER, (int) (alpha * fade)));
            }

            SkijaRenderer.drawText(canvas, v.getName(),
                    sx + 12, rowY + (ROW_H + 6 - fontH) / 2.0f,
                    settingFont, withAlpha(CHROME_TEXT, (int) (alpha * fade)));

            float rightX = sx + sw - 16;

            if (v.getValueType() == ValueType.BOOLEAN) {
                drawToggle(canvas, rightX, rowY, (BooleanValue) v, alpha, fade);
            } else if (v.getValueType() == ValueType.FLOAT) {
                drawSlider(canvas, sx, rightX, rowY, (FloatValue) v, mx, alpha, fade);
            } else if (v.getValueType() == ValueType.MODE) {
                drawDropdown(canvas, sx, rightX, rowY, (ModeValue) v, mx, my, alpha, fade);
            }

            accY += compH;
        }
    }

    private void drawToggle(Canvas canvas, float rightX, float rowY,
                            BooleanValue bv, int alpha, float fade) {
        boolean on = bv.getCurrentValue();
        float tw = 28.0f;
        float th = 14.0f;
        float tx = rightX - tw;
        float ty = rowY + (ROW_H + 6 - th) / 2.0f;

        int trackColor = on ? CHROME_ACCENT : CHROME_SWITCH_OFF;
        SkijaRenderer.drawRoundedRect(canvas, tx, ty, tw, th, 7.0f,
                withAlpha(trackColor, (int) (alpha * fade)));

        float knobR = 6.0f;
        float knobX = tx + (on ? tw - knobR * 2 - 2 : 2);
        float knobY = ty + (th - knobR * 2) / 2.0f;
        SkijaRenderer.drawRoundedRect(canvas, knobX, knobY, knobR * 2, knobR * 2, knobR,
                withAlpha(0xFFFFFFFF, (int) (alpha * fade)));
    }

    private void drawSlider(Canvas canvas, int sx, float rightX, float rowY,
                            FloatValue fv, float mx, int alpha, float fade) {
        float sliderW = (rightX - sx - 40) - 40;
        float sliderX = sx + 40;
        float sliderY = rowY + (ROW_H + 6) / 2.0f + 6;
        float sliderH = 3.0f;

        float min = fv.getMinValue();
        float max = fv.getMaxValue();
        float val = fv.getCurrentValue();
        float progress = (val - min) / (max - min);

        SkijaRenderer.drawRoundedRect(canvas, sliderX, sliderY, sliderW, sliderH, 1.5f,
                withAlpha(CHROME_HOVER, (int) (alpha * fade)));
        SkijaRenderer.drawRoundedRect(canvas, sliderX, sliderY, sliderW * progress, sliderH, 1.5f,
                withAlpha(CHROME_ACCENT, (int) (alpha * fade)));

        float knobX = sliderX + sliderW * progress - 4;
        float knobY = sliderY - 4;
        SkijaRenderer.drawRoundedRect(canvas, knobX, knobY, 8, 8, 4.0f,
                withAlpha(0xFFFFFFFF, (int) (alpha * fade)));

        Font valFont = SkijaRenderer.getOpensansFont(9.0f);
        String valText = fv.getStep() % 1.0f == 0.0f
                ? String.format("%d", (int) val)
                : String.format("%.2f", val);
        float tw = SkijaRenderer.measureTextWidth(valText, valFont);
        float valFontH = SkijaRenderer.getFontHeight(valFont);
        SkijaRenderer.drawText(canvas, valText, rightX - tw,
                rowY + (ROW_H + 6 - valFontH) / 2.0f,
                valFont, withAlpha(CHROME_TEXT, (int) (alpha * fade)));
    }

    private void drawDropdown(Canvas canvas, int sx, float rightX, float rowY,
                              ModeValue mv, float mx, float my, int alpha, float fade) {
        String sel = mv.getCurrentMode();
        Font font = SkijaRenderer.getOpensansFont(10.0f);
        float tw = SkijaRenderer.measureTextWidth(sel, font);

        float boxW = Math.max(80, tw + 24);
        float boxX = rightX - boxW;
        float boxH = ROW_H + 2;
        float boxY = rowY + 2;

        boolean open = dropdownStates.getOrDefault(mv.getName(), false);
        boolean hovered = mx >= boxX && mx <= boxX + boxW && my >= boxY && my <= boxY + boxH;

        int boxBg = hovered ? CHROME_SURFACE_HIGH : CHROME_SURFACE;
        SkijaRenderer.drawRoundedRect(canvas, boxX, boxY, boxW, boxH, 4.0f,
                withAlpha(boxBg, (int) (alpha * fade)));

        try (Paint paint = new Paint()) {
            paint.setColor(withAlpha(CHROME_BORDER, (int) (60.0f * alpha * fade / 255.0f)));
            paint.setAntiAlias(true);
            paint.setMode(PaintMode.STROKE);
            paint.setStrokeWidth(1.0f);
            canvas.drawRRect(RRect.makeLTRB(boxX, boxY, boxX + boxW, boxY + boxH, 4.0f), paint);
        }

        SkijaRenderer.drawText(canvas, sel, boxX + 8,
                boxY + (boxH - SkijaRenderer.getFontHeight(font)) / 2.0f,
                font, withAlpha(CHROME_ACCENT, (int) (alpha * fade)));

        Font arrowFont = SkijaRenderer.getOpensansFont(8.0f);
        String arrow = open ? "▲" : "▼";
        float aw = SkijaRenderer.measureTextWidth(arrow, arrowFont);
        float arrowFontH = SkijaRenderer.getFontHeight(arrowFont);
        SkijaRenderer.drawText(canvas, arrow, boxX + boxW - aw - 8,
                boxY + (boxH - arrowFontH) / 2.0f,
                arrowFont, withAlpha(CHROME_TEXT_SEC, (int) (alpha * fade)));

        if (open) {
            String[] modes = mv.getValues();
            float ddY = boxY + boxH;
            float ddH = modes.length * ROW_H;

            SkijaRenderer.drawRoundedRect(canvas, boxX, ddY, boxW, ddH, 4.0f,
                    withAlpha(CHROME_SURFACE_HIGH, (int) (alpha * fade)));

            for (int j = 0; j < modes.length; j++) {
                float itemY = ddY + j * ROW_H;
                boolean itemHovered = mx >= boxX && mx <= boxX + boxW
                        && my >= itemY && my <= itemY + ROW_H;
                if (itemHovered) {
                    SkijaRenderer.drawRoundedRect(canvas, boxX + 2, itemY, boxW - 4, ROW_H, 3.0f,
                            withAlpha(CHROME_HOVER, (int) (alpha * fade)));
                }
                int itemColor = modes[j].equals(sel) ? CHROME_ACCENT : CHROME_TEXT;
                SkijaRenderer.drawText(canvas, modes[j], boxX + 8,
                        itemY + (ROW_H - SkijaRenderer.getFontHeight(font)) / 2.0f,
                        font, withAlpha(itemColor, (int) (alpha * fade)));
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (openProgress < 1.0f) return true;

        int cx = this.width / 2;
        int cy = this.height / 2;
        float scale = 0.95f + 0.05f * easeOutCubic(openProgress);
        float tmx = toLocalX((float) mx, cx, scale);
        float tmy = toLocalY((float) my, cy, scale);

        int px = cx - PANEL_W / 2;
        int py = cy - PANEL_H / 2;

        if (handleTabClick(px, py, tmx, tmy, button)) return true;
        if (handleSearchClick(px, py, tmx, tmy, button)) return true;
        if (handleModuleListClick(px, py, tmx, tmy, button)) return true;
        if (handleSettingsClick(px, py, tmx, tmy, button)) return true;

        return super.mouseClicked(mx, my, button);
    }

    private boolean handleTabClick(int px, int py, float mx, float my, int button) {
        Category[] cats = Category.values();
        int tabW = 80;
        int tabH = 28;
        int startX = px + PANEL_W - (cats.length * tabW) - PADDING;
        int tabY = py + (TAB_H - tabH) / 2;

        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            int tx = startX + i * tabW;
            if (mx >= tx && mx <= tx + tabW && my >= tabY && my <= tabY + tabH) {
                if (button == 0) {
                    selectedCategory = cat;
                    selectedModule = null;
                    searchText = "";
                    searchFocused = false;
                    rebuildModuleList();
                    rebuildSettings();
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleSearchClick(int px, int py, float mx, float my, int button) {
        int listX = px + PADDING;
        int listY = py + TAB_H + PADDING;
        if (mx >= listX && mx <= listX + MODULE_LIST_W && my >= listY && my <= listY + SEARCH_H) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;
        return false;
    }

    private boolean handleModuleListClick(int px, int py, float mx, float my, int button) {
        int listX = px + PADDING;
        int contentY = py + TAB_H + PADDING + SEARCH_H + 6;
        int contentH = getModuleListContentHeight();

        if (mx < listX || mx > listX + MODULE_LIST_W
                || my < contentY || my > contentY + contentH) return false;

        for (int i = 0; i < filteredModules.size(); i++) {
            Module m = filteredModules.get(i);
            float rowY = contentY + i * ROW_H - moduleScrollOffset;
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
        int sx = px + SETTINGS_X;
        int sy = py + TAB_H + PADDING;
        int sw = PANEL_W - SETTINGS_X - PADDING;
        int sh = getSettingsVisibleHeight();

        if (mx < sx || mx > sx + sw || my < sy || my > sy + sh) return false;
        if (selectedModule == null) return true;

        float fade = settingsFadeAnim;
        if (fade < 0.5f) return true;

        float slide = (1.0f - fade) * 15.0f;
        int contentY = sy + 38 + (int) slide;
        int visibleH = sh - 38;

        int accY = 0;
        for (Value v : selectedModuleSettings) {
            int compH = ROW_H + 6;
            if (v.getValueType() == ValueType.MODE &&
                    dropdownStates.getOrDefault(v.getName(), false)) {
                compH += ((ModeValue) v).getValues().length * ROW_H;
            }

            float rowY = contentY + accY - settingsScrollOffset;

            if (my >= rowY && my <= rowY + ROW_H + 6) {
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
                                getSettingsContentHeight() - getSettingsVisibleHeight());
                    }
                    return true;
                }
            }

            if (v.getValueType() == ValueType.MODE &&
                    dropdownStates.getOrDefault(v.getName(), false)) {
                ModeValue mv = (ModeValue) v;
                float ddY = rowY + ROW_H + 6;
                String[] modes = mv.getValues();

                Font font = SkijaRenderer.getOpensansFont(10.0f);
                float tw = SkijaRenderer.measureTextWidth(mv.getCurrentMode(), font);
                float boxW = Math.max(80, tw + 24);
                float boxX = sx + sw - 16 - boxW;

                if (my >= ddY && my <= ddY + modes.length * ROW_H
                        && mx >= boxX && mx <= boxX + boxW) {
                    int idx = (int) ((my - ddY) / ROW_H);
                    if (idx >= 0 && idx < modes.length && button == 0) {
                        mv.setCurrentValue(idx);
                        dropdownStates.put(v.getName(), false);
                        settingsMaxScroll = Math.max(0,
                                getSettingsContentHeight() - getSettingsVisibleHeight());
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
            int cy = this.height / 2;
            float scale = 0.95f + 0.05f * easeOutCubic(openProgress);
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
        float scale = 0.95f + 0.05f * easeOutCubic(openProgress);
        float tmx = toLocalX((float) mx, cx, scale);
        float tmy = toLocalY((float) my, cy, scale);

        int px = cx - PANEL_W / 2;
        int py = cy - PANEL_H / 2;

        int listX = px + PADDING;
        int listContentY = py + TAB_H + PADDING + SEARCH_H + 6;
        int listContentH = getModuleListContentHeight();

        int sx = px + SETTINGS_X;
        int sy = py + TAB_H + PADDING;
        int sw = PANEL_W - SETTINGS_X - PADDING;
        int sh = getSettingsVisibleHeight();

        if (tmx >= listX && tmx <= listX + MODULE_LIST_W
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
        float sliderX = px + SETTINGS_X + 40;
        float sliderW = (px + PANEL_W - PADDING - 16) - 40 - sliderX;
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