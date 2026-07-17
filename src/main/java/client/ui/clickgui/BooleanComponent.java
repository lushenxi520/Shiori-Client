package client.ui.clickgui;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import client.utils.Colors;
import client.utils.RenderUtils;
import client.utils.renderer.Fonts;
import client.values.Value;
import client.values.impl.BooleanValue;

public class BooleanComponent extends SettingComponent {
    private final BooleanValue booleanSetting;
    private float toggleAnim = 0.0f;

    public BooleanComponent(Value setting, ModuleButton parentButton, int yOffset) {
        super(setting, parentButton, yOffset);
        this.booleanSetting = (BooleanValue) setting;
        this.toggleAnim = this.booleanSetting.getCurrentValue() ? 1.0f : 0.0f;
    }

    @Override
    public void renderWithAlpha(PoseStack poseStack, int mouseX, int mouseY, float partialTicks, float alpha) {
        float target = this.booleanSetting.getCurrentValue() ? 1.0f : 0.0f;
        if (Math.abs(this.toggleAnim - target) > 0.01f) {
            this.toggleAnim += (target - this.toggleAnim) * 0.2f;
        } else {
            this.toggleAnim = target;
        }

        int rowY = this.parentButton.panel.y + this.parentButton.yOffset + this.parentButton.panel.rowHeight + this.yOffset;
        int verticalPadding = 4;
        int contentHeight = this.parentButton.panel.rowHeight - verticalPadding * 2;
        int panelX = this.parentButton.panel.x;
        int panelWidth = this.parentButton.panel.width;
        int textPadding = 8;

        String name = this.booleanSetting.getName();
        float textY = (float) (rowY + verticalPadding) + ((float) contentHeight - (float) Fonts.opensans.getHeight(true, 0.4)) / 2.0f;
        int textColor = Colors.getColor(255, 255, 255, (int) (255.0f * alpha));
        Fonts.opensans.render(poseStack, name, panelX + textPadding, textY, new Color(textColor, true), false, 0.4);

        int toggleWidth = 18;
        int toggleHeight = 8;
        int toggleX = panelX + panelWidth - textPadding - toggleWidth;
        int toggleY = rowY + verticalPadding + (contentHeight - toggleHeight) / 2 + 2;

        Color trackColor = this.booleanSetting.getCurrentValue()
                ? new Color(138, 180, 248, (int)(180.0f * alpha))
                : new Color(100, 100, 100, (int)(150.0f * alpha));
        RenderUtils.drawRoundedRect(poseStack, toggleX, toggleY, toggleWidth, toggleHeight, 4.5f, trackColor.getRGB());

        float knobWidth = 7.0f;
        float knobHeight = toggleHeight + 4;
        float knobX = (float) (toggleX + 1) + ((float) toggleWidth - knobWidth - 2.0f) * this.toggleAnim;
        float knobY = toggleY - 2;
        RenderUtils.drawRoundedRect(poseStack, knobX, knobY, knobWidth, knobHeight, 4.0f,
                new Color(160, 195, 255, (int)(255.0f * alpha)).getRGB());
    }

    public BooleanValue getBooleanSetting() {
        return this.booleanSetting;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isHovered(mouseX, mouseY) && button == 0) {
            this.booleanSetting.setCurrentValue(!this.booleanSetting.getCurrentValue());
            this.parentButton.panel.recalcLayout();
        }
        super.mouseClicked(mouseX, mouseY, button);
    }
}