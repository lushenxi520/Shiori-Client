package client.ui.clickgui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import client.Shiori;
import client.modules.Module;
import client.utils.Colors;
import client.utils.RenderUtils;
import client.utils.SmoothAnimationTimer;
import client.utils.renderer.Fonts;
import client.values.Value;
import client.values.ValueType;
import client.values.impl.BooleanValue;
import client.values.impl.FloatValue;
import client.values.impl.ModeValue;

public class ModuleButton {
    public Module module;
    public CategoryPanel panel;
    public int yOffset;
    public List<SettingComponent> settingComponents;
    public boolean expanded;
    public final SmoothAnimationTimer expandAnim;
    private float hoverProgress = 0.0f;
    private final float hoverSpeed = 4.0f;
    private long lastTime = System.currentTimeMillis();

    public ModuleButton(Module module, CategoryPanel panel, int yOffset) {
        this.module = module;
        this.panel = panel;
        this.yOffset = yOffset;
        this.expanded = false;
        this.settingComponents = new ArrayList<>();

        List<Value> settings = Shiori.getInstance().getValueManager().getValuesByHasValue(module);
        for (Value setting : settings) {
            if (setting.getValueType() == ValueType.BOOLEAN) {
                this.settingComponents.add(new BooleanComponent(setting, this, 0));
            } else if (setting.getValueType() == ValueType.FLOAT) {
                this.settingComponents.add(new NumberComponent(setting, this, 0));
            } else if (setting.getValueType() == ValueType.MODE) {
                this.settingComponents.add(new ModeComponent(setting, this, 0));
            }
        }

        this.expandAnim = new SmoothAnimationTimer(0.0f, 0.0f);
    }

    public int getTotalHeight() {
        if (!this.expanded) {
            return this.panel.rowHeight;
        }

        int total = this.panel.rowHeight;
        List<SettingComponent> visibleComponents = this.settingComponents.stream()
                .filter(sc -> sc.setting.isVisible())
                .collect(Collectors.toList());

        for (SettingComponent sc : visibleComponents) {
            total += this.panel.rowHeight;
            if (sc instanceof ModeComponent mode && mode.isDropdownOpen()) {
                total += mode.getDropdownHeight();
            }
        }

        return total;
    }

    public int getExpandedHeight() {
        int total = 0;
        List<SettingComponent> visibleComponents = this.settingComponents.stream()
                .filter(sc -> sc.setting.isVisible())
                .collect(Collectors.toList());

        for (SettingComponent sc : visibleComponents) {
            total += this.panel.rowHeight;
            if (sc instanceof ModeComponent mode && mode.isDropdownOpen()) {
                total += mode.getDropdownHeight();
            }
        }

        return total;
    }

    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.expandAnim.update(true);
        long now = System.currentTimeMillis();
        float deltaSeconds = (float) (now - this.lastTime) / 1000.0f;
        this.lastTime = now;

        if (!this.panel.expanded) {
            return;
        }

        if (this.isHovered(mouseX, mouseY)) {
            if (this.hoverProgress < 1.0f) {
                this.hoverProgress += deltaSeconds * this.hoverSpeed;
            }
            if (this.hoverProgress > 1.0f) {
                this.hoverProgress = 1.0f;
            }
        } else {
            if (this.hoverProgress > 0.0f) {
                this.hoverProgress -= deltaSeconds * this.hoverSpeed;
            }
            if (this.hoverProgress < 0.0f) {
                this.hoverProgress = 0.0f;
            }
        }

        int bgAlpha = (int) (160.0f + 40.0f * this.hoverProgress);
        int rowY = this.panel.y + this.yOffset;
        int rowHeight = this.panel.rowHeight;

        if (rowHeight > 0) {
            RenderUtils.drawRoundedRect(poseStack, this.panel.x, rowY, this.panel.width, rowHeight, 6.0f,
                    new Color(21, 21, 21, bgAlpha).getRGB());
        }

        String name = this.module.getName();
        float nameWidth = Fonts.opensans.getWidth(name, 0.4);
        float nameX = (float) this.panel.x + (float) this.panel.width / 2.0f - nameWidth / 2.0f;
        float nameY = (float) (this.panel.y + this.yOffset)
                + (float) this.panel.rowHeight / 2.0f - (float) Fonts.opensans.getHeight(true, 0.4) / 2.0f;

        int textColor = this.module.isEnabled()
                ? Colors.getColor(138, 180, 248, 255)
                : Colors.getColor(255, 255, 255, 255);
        Fonts.opensans.render(poseStack, name, nameX, nameY, new Color(textColor, true), false, 0.4);

        if (!this.settingComponents.isEmpty()) {
            float arrowX = (float) (this.panel.x + this.panel.width - 15);
            float arrowY = (float) (this.panel.y + this.yOffset) + (float) this.panel.rowHeight / 2.0f;
            float arrowAngle = 180.0f * this.expandAnim.value;
            String arrowChar = "v";

            poseStack.pushPose();
            poseStack.translate(arrowX, arrowY, 0.0f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(arrowAngle));
            poseStack.translate(-arrowX, -arrowY, 0.0f);
            Fonts.opensans.render(poseStack, arrowChar,
                    arrowX - Fonts.opensans.getWidth(arrowChar, 0.4) / 2.0f,
                    arrowY - (float) Fonts.opensans.getHeight(true, 0.4) / 2.0f,
                    Color.WHITE, false, 0.4);
            poseStack.popPose();
        }

        float expandedHeight2 = (float) this.getExpandedHeight() * this.expandAnim.value;
        if (expandedHeight2 > 1.0f) {
            int contentY = this.panel.y + this.yOffset + this.panel.rowHeight;
            int contentHeight = (int) expandedHeight2;

            if (contentHeight > 0) {
                RenderUtils.drawRoundedRect(poseStack, this.panel.x, contentY, this.panel.width, contentHeight, 6.0f,
                        new Color(11, 11, 11, 120).getRGB());
            }

            List<SettingComponent> visibleComponents = this.settingComponents.stream()
                    .filter(sc -> sc.setting.isVisible())
                    .collect(Collectors.toList());

            int componentOffsetY = 0;
            for (SettingComponent sc : visibleComponents) {
                sc.yOffset = componentOffsetY;
                sc.renderWithAlpha(poseStack, mouseX, mouseY, partialTicks, this.expandAnim.value);
                componentOffsetY += this.panel.rowHeight;
                if (sc instanceof ModeComponent mode && mode.isDropdownOpen()) {
                    componentOffsetY += mode.getDropdownHeight();
                }
            }
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.panel.expanded) {
            return;
        }

        if (this.isHovered(mouseX, mouseY)) {
            if (button == 0) {
                this.module.toggle();
            } else if (button == 1 && !this.settingComponents.isEmpty()) {
                this.expanded = !this.expanded;
                this.expandAnim.target = this.expanded ? 1.0f : 0.0f;
                this.panel.recalcLayout();
            }
        }

        if (this.expanded) {
            List<SettingComponent> visibleComponents = this.settingComponents.stream()
                    .filter(sc -> sc.setting.isVisible())
                    .collect(Collectors.toList());

            int componentOffsetY = 0;
            for (SettingComponent sc : visibleComponents) {
                sc.yOffset = componentOffsetY;
                if (sc.isHovered(mouseX, mouseY)) {
                    sc.mouseClicked(mouseX, mouseY, button);
                }
                componentOffsetY += this.panel.rowHeight;
                if (sc instanceof ModeComponent mode && mode.isDropdownOpen()) {
                    componentOffsetY += mode.getDropdownHeight();
                }
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (this.expanded) {
            for (SettingComponent sc : this.settingComponents) {
                sc.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public void mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
    }

    public void reset() {
        this.expanded = false;
        this.expandAnim.target = 0.0f;
        this.expandAnim.value = 0.0f;
        this.hoverProgress = 0.0f;
        for (SettingComponent sc : this.settingComponents) {
            if (sc instanceof ModeComponent mode) {
                mode.closeDropdown();
            }
        }
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= (double) this.panel.x && mouseX <= (double) (this.panel.x + this.panel.width)
                && mouseY >= (double) (this.panel.y + this.yOffset)
                && mouseY <= (double) (this.panel.y + this.yOffset + this.panel.rowHeight);
    }

    public boolean isAnimating() {
        return Math.abs(this.expandAnim.value - this.expandAnim.target) > 0.01f;
    }
}