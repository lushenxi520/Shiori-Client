package client.ui.clickgui;

import client.utils.SmoothAnimationTimer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import client.Shiori;
import client.modules.Category;
import client.modules.Module;
import client.utils.Colors;
import client.utils.RenderUtils;
import client.utils.renderer.Fonts;
import net.minecraft.client.gui.GuiGraphics;

public class CategoryPanel {
    public int x;
    public int y;
    public int dragOffsetX;
    public int dragOffsetY;
    public int width;
    public int rowHeight;
    public Category category;
    public boolean dragging;
    public boolean expanded;
    public List<ModuleButton> moduleButtons;
    private float[] targetOffsets;
    private float[] currentOffsets;
    private long lastTime = System.currentTimeMillis();
    private boolean needsLayout = false;
    private SmoothAnimationTimer expandAnim;

    public CategoryPanel(int x, int y, int width, int rowHeight, Category category) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.rowHeight = rowHeight;
        this.category = category;
        this.dragging = false;
        this.expanded = false;
        this.moduleButtons = new ArrayList<>();
        this.expandAnim = new SmoothAnimationTimer(0.0f, 0.0f);

        int buttonY = rowHeight;
        for (Module module : Shiori.getInstance().getModuleManager().getModulesByCategory(category)) {
            this.moduleButtons.add(new ModuleButton(module, this, buttonY));
            buttonY += rowHeight;
        }

        this.initOffsets();
    }

    private void initOffsets() {
        this.targetOffsets = new float[this.moduleButtons.size()];
        this.currentOffsets = new float[this.moduleButtons.size()];
        for (int i = 0; i < this.moduleButtons.size(); i++) {
            this.targetOffsets[i] = this.rowHeight + i * this.rowHeight;
            this.currentOffsets[i] = this.rowHeight + i * this.rowHeight;
        }
    }

    public void tick() {
        boolean anyButtonAnimating = false;
        for (ModuleButton mb : this.moduleButtons) {
            if (mb.isAnimating()) {
                anyButtonAnimating = true;
                break;
            }
        }

        if (!this.needsLayout && !anyButtonAnimating) {
            return;
        }

        boolean stillAnimating = false;
        long now = System.currentTimeMillis();
        float deltaSeconds = (float) (now - this.lastTime) / 1000.0f;
        this.lastTime = now;

        if (deltaSeconds <= 0.0f || deltaSeconds > 0.1f) {
            deltaSeconds = 0.016f;
        }

        for (int i = 0; i < this.moduleButtons.size(); i++) {
            float diff = this.targetOffsets[i] - this.currentOffsets[i];
            if (Math.abs(diff) > 0.5f) {
                this.currentOffsets[i] += diff * 0.2f * (deltaSeconds * 60.0f);
                stillAnimating = true;
            } else {
                this.currentOffsets[i] = this.targetOffsets[i];
            }
            this.moduleButtons.get(i).yOffset = (int) this.currentOffsets[i];
        }

        this.needsLayout = stillAnimating || anyButtonAnimating;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        PoseStack poseStack = guiGraphics.pose();
        this.tick();
        this.expandAnim.update(true);

        RenderUtils.drawRoundedRect(poseStack, this.x, this.y, this.width, this.rowHeight, 6.0f,
                new Color(16, 16, 20, 200).getRGB());

        String iconChar = this.category.getIcon();
        float iconY = (float) this.y + ((float) this.rowHeight / 2.0f - (float) Fonts.icons.getHeight(false, 0.4) / 2.0f) + 2.0f;
        Fonts.icons.render(poseStack, iconChar, this.x + 4, iconY, Color.WHITE, false, 0.4);

        float labelY = (float) this.y + ((float) this.rowHeight / 2.0f - (float) Fonts.opensans.getHeight(true, 0.4) / 2.0f) - 0.5f;
        Fonts.opensans.render(poseStack, this.category.getDisplayName(), this.x + this.rowHeight + 4, labelY, Color.WHITE, true, 0.4);

        if (this.expanded || this.expandAnim.value > 0.01f) {
            float panelExpandedHeight = this.getPanelContentHeight() * this.expandAnim.value;
            if (panelExpandedHeight > 1.0f) {
                RenderUtils.drawRoundedRect(poseStack, this.x, this.y + this.rowHeight, this.width, (int) panelExpandedHeight, 6.0f,
                        new Color(11, 11, 11, 160).getRGB());
            }

            for (ModuleButton mb : this.moduleButtons) {
                mb.render(poseStack, mouseX, mouseY, partialTicks);
            }
        }
    }

    private int getPanelContentHeight() {
        if (!this.expanded) {
            return 0;
        }
        int total = 0;
        for (ModuleButton mb : this.moduleButtons) {
            total += mb.getTotalHeight();
        }
        return total;
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isHovered(mouseX, mouseY)) {
            if (button == 0) {
                this.dragging = true;
                this.dragOffsetX = (int) (mouseX - (double) this.x);
                this.dragOffsetY = (int) (mouseY - (double) this.y);
            } else if (button == 1) {
                this.expanded = !this.expanded;
                this.expandAnim.target = this.expanded ? 1.0f : 0.0f;
                if (!this.expanded) {
                    for (ModuleButton mb : this.moduleButtons) {
                        if (mb.expanded) {
                            mb.expanded = false;
                            mb.expandAnim.target = 0.0f;
                        }
                    }
                }
                this.recalcLayout();
            }
        }

        if (this.expanded) {
            for (ModuleButton mb : this.moduleButtons) {
                mb.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (ModuleButton mb : this.moduleButtons) {
            mb.mouseReleased(mouseX, mouseY, button);
        }
        if (button == 0 && this.dragging) {
            this.dragging = false;
        }
    }

    public void mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        for (ModuleButton mb : this.moduleButtons) {
            mb.mouseScrolled(mouseX, mouseY, scrollDelta);
        }
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX > (double) this.x && mouseX < (double) (this.x + this.width)
                && mouseY > (double) this.y && mouseY < (double) (this.y + this.rowHeight);
    }

    public void mouseDragged(double mouseX, double mouseY) {
        if (this.dragging) {
            this.x = (int) (mouseX - (double) this.dragOffsetX);
            this.y = (int) (mouseY - (double) this.dragOffsetY);
        }
    }

    public void recalcLayout() {
        int currentY = this.rowHeight;
        for (int i = 0; i < this.moduleButtons.size(); i++) {
            ModuleButton mb = this.moduleButtons.get(i);
            this.targetOffsets[i] = currentY;
            currentY += mb.getTotalHeight();
        }
        this.needsLayout = true;
        this.lastTime = System.currentTimeMillis();
    }
}