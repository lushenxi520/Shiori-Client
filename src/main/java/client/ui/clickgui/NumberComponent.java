package client.ui.clickgui;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import client.utils.Colors;
import client.utils.MathUtils;
import client.utils.RenderUtils;
import client.utils.renderer.Fonts;
import client.values.Value;
import client.values.impl.FloatValue;

public class NumberComponent extends SettingComponent {
    private boolean dragging;
    private final FloatValue numberSetting;

    public NumberComponent(Value setting, ModuleButton parentButton, int yOffset) {
        super(setting, parentButton, yOffset);
        this.numberSetting = (FloatValue) setting;
        this.dragging = false;
    }

    @Override
    public void renderWithAlpha(PoseStack poseStack, int mouseX, int mouseY, float partialTicks, float alpha) {
        int rowY = this.parentButton.panel.y + this.parentButton.yOffset + this.parentButton.panel.rowHeight + this.yOffset;
        int panelX = this.parentButton.panel.x;
        int panelWidth = this.parentButton.panel.width;
        int rowHeight = this.parentButton.panel.rowHeight;
        int textColor = Colors.getColor(255, 255, 255, (int) (255.0f * alpha));
        int sliderColor = new Color(138, 180, 248, (int) (255.0f * alpha)).getRGB();
        int textPadding = 8;

        int sliderX = panelX + textPadding;
        int valueTextWidth = (int) Fonts.opensans.getWidth("00.00", 0.4);
        int sliderWidth = panelWidth - textPadding * 2 - valueTextWidth + 20;

        if (this.dragging) {
            this.updateSliderValue(mouseX, sliderX, sliderWidth);
        }

        String name = this.numberSetting.getName();
        String valueText = this.numberSetting.getStep() % 1.0f == 0.0f
                ? String.format("%d", (int) this.numberSetting.getCurrentValue())
                : String.format("%.2f", this.numberSetting.getCurrentValue());

        float textY = (float) rowY + ((float) rowHeight - (float) Fonts.opensans.getHeight(true, 0.4)) / 2.0f;
        Fonts.opensans.render(poseStack, name, sliderX, textY, new Color(textColor, true), false, 0.4);
        Fonts.opensans.render(poseStack, valueText,
                (float) (panelX + panelWidth - textPadding) - Fonts.opensans.getWidth(valueText, 0.4),
                textY, new Color(textColor, true), false, 0.4);

        float min = this.numberSetting.getMinValue();
        float max = this.numberSetting.getMaxValue();
        float current = this.numberSetting.getCurrentValue();
        float progress = (current - min) / (max - min);
        int sliderY = rowY + rowHeight / 2 + 10;
        int sliderHeight = 2;

        RenderUtils.drawRoundedRect(poseStack, sliderX, sliderY, sliderWidth, sliderHeight, 1.5f,
                new Color(10, 10, 10, 170).getRGB());
        RenderUtils.drawRoundedRect(poseStack, sliderX, sliderY, (int) ((float) sliderWidth * progress), sliderHeight, 1.5f,
                sliderColor);
    }

    private void updateSliderValue(double mouseX, int sliderX, int sliderWidth) {
        float min = this.numberSetting.getMinValue();
        float max = this.numberSetting.getMaxValue();
        float range = max - min;
        double clampedX = Math.max(sliderX, Math.min(mouseX, sliderX + sliderWidth));
        float newValue = min + range * (float) ((clampedX - (double) sliderX) / (double) sliderWidth);
        this.numberSetting.setCurrentValue((float) MathUtils.round(newValue, 2));
    }

    public FloatValue getNumberSetting() {
        return this.numberSetting;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        int panelX = this.parentButton.panel.x;
        int panelWidth = this.parentButton.panel.width;
        int rowY = this.parentButton.panel.y + this.parentButton.yOffset + this.parentButton.panel.rowHeight + this.yOffset;
        int rowHeight = this.parentButton.panel.rowHeight;
        int textPadding = 8;
        int sliderX = panelX + textPadding;
        int valueTextWidth = (int) Fonts.opensans.getWidth("00.00", 0.4);
        int sliderWidth = panelWidth - textPadding * 2 - valueTextWidth + 20;

        boolean overSlider = mouseX >= (double) sliderX && mouseX <= (double) (sliderX + sliderWidth)
                && mouseY >= (double) rowY && mouseY <= (double) (rowY + rowHeight);

        if (button == 0 && overSlider) {
            this.dragging = true;
            this.updateSliderValue(mouseX, sliderX, sliderWidth);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.dragging = false;
        }
    }
}