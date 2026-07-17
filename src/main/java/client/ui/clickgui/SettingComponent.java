package client.ui.clickgui;

import com.mojang.blaze3d.vertex.PoseStack;
import client.values.Value;

public class SettingComponent {
    public Value setting;
    public ModuleButton parentButton;
    public int yOffset;

    public SettingComponent(Value setting, ModuleButton parentButton, int yOffset) {
        this.setting = setting;
        this.parentButton = parentButton;
        this.yOffset = yOffset;
    }

    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderWithAlpha(poseStack, mouseX, mouseY, partialTicks, 1.0f);
    }

    public void renderWithAlpha(PoseStack poseStack, int mouseX, int mouseY, float partialTicks, float alpha) {
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    public boolean isHovered(double mouseX, double mouseY) {
        int rowY = this.parentButton.panel.y + this.parentButton.yOffset + this.parentButton.panel.rowHeight + this.yOffset;
        int rowHeight = this.parentButton.panel.rowHeight;
        return mouseX >= (double) this.parentButton.panel.x
                && mouseX <= (double) (this.parentButton.panel.x + this.parentButton.panel.width)
                && mouseY >= (double) rowY
                && mouseY <= (double) (rowY + rowHeight);
    }
}