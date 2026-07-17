package client.ui.clickgui;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import client.utils.Colors;
import client.utils.RenderUtils;
import client.utils.renderer.Fonts;
import client.values.Value;
import client.values.impl.ModeValue;

public class ModeComponent extends SettingComponent {
    private final ModeValue modeSetting;
    private boolean dropdownOpen = false;

    public ModeComponent(Value setting, ModuleButton parentButton, int yOffset) {
        super(setting, parentButton, yOffset);
        this.modeSetting = (ModeValue) setting;
    }

    public boolean isDropdownOpen() {
        return this.dropdownOpen;
    }

    public void closeDropdown() {
        this.dropdownOpen = false;
    }

    public void setDropdownOpen(boolean open) {
        this.dropdownOpen = open;
    }

    public ModeValue getModeSetting() {
        return this.modeSetting;
    }

    @Override
    public void renderWithAlpha(PoseStack poseStack, int mouseX, int mouseY, float partialTicks, float alpha) {
        int rowY = this.parentButton.panel.y + this.parentButton.yOffset + this.parentButton.panel.rowHeight + this.yOffset + 3;
        int panelX = this.parentButton.panel.x;
        int panelWidth = this.parentButton.panel.width;
        int rowHeight = this.parentButton.panel.rowHeight;
        int verticalPadding = 4;
        int contentHeight = rowHeight - verticalPadding * 2;
        int outerPadding = 8;
        int innerPadding = 4;
        int boxX = panelX + outerPadding + innerPadding;
        int boxWidth = panelWidth - (outerPadding + innerPadding) * 2;

        int textColor = Colors.getColor(255, 255, 255, (int) (255.0f * alpha));

        boolean hovered = mouseX >= boxX && mouseX <= boxX + boxWidth
                && mouseY >= rowY + verticalPadding && mouseY <= rowY + verticalPadding + contentHeight;

        Color boxColor = hovered && !this.dropdownOpen
                ? new Color(45, 45, 45, (int)(160.0f * alpha))
                : new Color(25, 25, 25, (int)(130.0f * alpha));
        RenderUtils.fill(poseStack, boxX, rowY + verticalPadding, boxX + boxWidth, rowY + verticalPadding + contentHeight,
                boxColor.getRGB());

        Color edgeColor = new Color(100, 100, 100, (int) (180.0f * alpha));
        RenderUtils.fill(poseStack, panelX + outerPadding + 2, rowY + verticalPadding,
                panelX + outerPadding + innerPadding, rowY + verticalPadding + contentHeight, edgeColor.getRGB());
        RenderUtils.fill(poseStack, panelX + panelWidth - outerPadding - innerPadding, rowY + verticalPadding,
                panelX + panelWidth - outerPadding - 2, rowY + verticalPadding + contentHeight, edgeColor.getRGB());

        String name = this.modeSetting.getName();
        float textY = (float) (rowY + verticalPadding)
                + ((float) contentHeight - (float) Fonts.opensans.getHeight(true, 0.4)) / 2.0f - 1.5f;
        Fonts.opensans.render(poseStack, name, boxX + innerPadding, textY, new Color(textColor, true), false, 0.4);

        String selectedValue = this.modeSetting.getCurrentMode();
        float valueX = (float) (boxX + boxWidth) - Fonts.opensans.getWidth(selectedValue, 0.4) - (float) innerPadding;
        Fonts.opensans.render(poseStack, selectedValue, valueX, textY, new Color(138, 180, 248), false, 0.4);

        if (this.dropdownOpen) {
            int dropdownY = rowY + verticalPadding + contentHeight;
            int dropdownHeight = this.getDropdownHeight();
            RenderUtils.fill(poseStack, boxX, dropdownY, boxX + boxWidth, dropdownY + dropdownHeight,
                    new Color(33, 33, 33, 130).getRGB());

            String[] modes = this.modeSetting.getValues();
            for (int i = 0; i < modes.length; i++) {
                String mode = modes[i];
                float itemY = dropdownY + i * contentHeight;
                boolean itemHovered = mouseX >= boxX && mouseX <= boxX + boxWidth
                        && (float) mouseY >= itemY && (float) mouseY < itemY + (float) contentHeight;

                if (itemHovered) {
                    RenderUtils.fill(poseStack, boxX, itemY, boxX + boxWidth, itemY + contentHeight,
                            new Color(0, 0, 0, 100).getRGB());
                }

                float modeTextWidth = Fonts.opensans.getWidth(mode, 0.4);
                float modeTextX = (float) boxX + ((float) boxWidth - modeTextWidth) / 2.0f;
                float modeTextY = itemY + ((float) contentHeight - (float) Fonts.opensans.getHeight(true, 0.4)) / 2.0f;

                boolean isSelected = mode.equals(this.modeSetting.getCurrentMode());
                Fonts.opensans.render(poseStack, mode, modeTextX, modeTextY,
                        isSelected ? new Color(138, 180, 248) : new Color(textColor, true), false, 0.4);
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        int rowY = this.parentButton.panel.y + this.parentButton.yOffset + this.parentButton.panel.rowHeight + this.yOffset + 3;
        int rowHeight = this.parentButton.panel.rowHeight;
        int verticalPadding = 4;
        int contentHeight = rowHeight - verticalPadding * 2;
        int panelX = this.parentButton.panel.x;
        int panelWidth = this.parentButton.panel.width;
        int outerPadding = 8;
        int innerPadding = 4;
        int boxX = panelX + outerPadding + innerPadding;
        int boxWidth = panelWidth - (outerPadding + innerPadding) * 2;

        String[] modes = this.modeSetting.getValues();

        if (this.dropdownOpen) {
            int dropdownStartY = rowY + verticalPadding + contentHeight;
            int dropdownHeight = this.getDropdownHeight();
            if (mouseX >= (double) boxX && mouseX <= (double) (boxX + boxWidth)
                    && mouseY >= (double) dropdownStartY && mouseY < (double) (dropdownStartY + dropdownHeight)) {
                int itemIndex = (int) ((mouseY - (double) dropdownStartY) / (double) contentHeight);
                if (itemIndex >= 0 && itemIndex < modes.length) {
                    this.modeSetting.setCurrentValue(itemIndex);
                    this.dropdownOpen = false;
                    this.parentButton.panel.recalcLayout();
                }
                return;
            }
        }

        boolean overHeader = mouseX >= (double) boxX && mouseX <= (double) (boxX + boxWidth)
                && mouseY >= (double) (rowY + verticalPadding)
                && mouseY <= (double) (rowY + verticalPadding + contentHeight);

        if (overHeader && (button == 0 || button == 1)) {
            this.dropdownOpen = !this.dropdownOpen;
            this.parentButton.panel.recalcLayout();
        }
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        int rowY = this.parentButton.panel.y + this.parentButton.yOffset + this.parentButton.panel.rowHeight + this.yOffset;
        int panelX = this.parentButton.panel.x;
        int panelWidth = this.parentButton.panel.width;
        int rowHeight = this.parentButton.panel.rowHeight;

        boolean overHeader = mouseX >= (double) panelX && mouseX <= (double) (panelX + panelWidth)
                && mouseY >= (double) rowY && mouseY <= (double) (rowY + rowHeight);

        if (overHeader) {
            return true;
        }

        if (this.dropdownOpen) {
            int dropdownY = rowY + rowHeight;
            int dropdownHeight = this.getDropdownHeight();
            return mouseX >= (double) panelX && mouseX <= (double) (panelX + panelWidth)
                    && mouseY >= (double) dropdownY && mouseY <= (double) (dropdownY + dropdownHeight);
        }

        return false;
    }

    public int getDropdownHeight() {
        int verticalPadding = 4;
        int contentHeight = this.parentButton.panel.rowHeight - verticalPadding * 2;
        return contentHeight * this.modeSetting.getValues().length;
    }
}