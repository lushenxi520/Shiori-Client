package client.modules.impl.render;

import client.events.api.EventTarget;
import client.events.impl.EventRender2D;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.ui.hud.HudElement;
import client.utils.RenderUtils;
import client.utils.SmoothAnimationTimer;
import client.utils.renderer.Fonts;
import client.utils.renderer.SkijaRenderer;
import client.values.ValueBuilder;
import client.values.impl.BooleanValue;
import client.values.impl.FloatValue;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

@ModuleInfo(
   name = "Custom",
   description = "Custom render toggles",
   category = Category.RENDER
)
public class Custom extends Module implements HudElement {

   public BooleanValue heart = ValueBuilder.create(this, "Heart")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();

   public FloatValue heartX = ValueBuilder.create(this, "Heart X")
      .setVisibility(() -> false)
      .setMinFloatValue(-5000.0F)
      .setMaxFloatValue(5000.0F)
      .setDefaultFloatValue(0.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();

   public FloatValue heartY = ValueBuilder.create(this, "Heart Y")
      .setVisibility(() -> false)
      .setMinFloatValue(-5000.0F)
      .setMaxFloatValue(5000.0F)
      .setDefaultFloatValue(0.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();

   public BooleanValue chatBackground = ValueBuilder.create(this, "ChatBackground")
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();

   public BooleanValue logo = ValueBuilder.create(this, "Logo")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();

   public FloatValue logoX = ValueBuilder.create(this, "Logo X")
      .setVisibility(() -> false)
      .setMinFloatValue(-5000.0F)
      .setMaxFloatValue(5000.0F)
      .setDefaultFloatValue(0.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();

   public FloatValue logoY = ValueBuilder.create(this, "Logo Y")
      .setVisibility(() -> false)
      .setMinFloatValue(-5000.0F)
      .setMaxFloatValue(5000.0F)
      .setDefaultFloatValue(0.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();

   public FloatValue logoScale = ValueBuilder.create(this, "Logo Scale")
      .setVisibility(this.logo::getCurrentValue)
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(5.0F)
      .setDefaultFloatValue(1.0F)
      .setFloatStep(0.1F)
      .build()
      .getFloatValue();

   private static final Color BAR_BG = new Color(0, 0, 0, 100);
   private static final Color BAR_LAG = new Color(99, 99, 99, 120);
   private static final Color BAR_HEALTH = new Color(255, 210, 0);
   private static final Color BAR_ARMOR = new Color(100, 180, 255);

   private static final float PANEL_WIDTH = 160.0F;
   private static final float PANEL_HEIGHT = 40.0F;
   private static final float FACE_SIZE = 30.0F;
   private static final float PADDING = 5.0F;
   private static final float BAR_HEIGHT = 8.0F;
   private static final float BAR_GAP = 3.0F;
   private static final float TEXT_RESERVE = 16.0F;

   private final SmoothAnimationTimer healthAnim = new SmoothAnimationTimer();
   private final SmoothAnimationTimer healthLagAnim = new SmoothAnimationTimer();
   private final SmoothAnimationTimer armorAnim = new SmoothAnimationTimer();
   private final SmoothAnimationTimer armorLagAnim = new SmoothAnimationTimer();

   private float lastHealth;
   private int lastArmor;

   private boolean hudDragging;
   private float hudDragOffsetX;
   private float hudDragOffsetY;

   private boolean heartDragging;
   private float heartDragOffsetX;
   private float heartDragOffsetY;
   private float heartRenderedX;
   private float heartRenderedY;
   private float heartRenderedWidth;
   private float heartRenderedHeight;

   private boolean logoDragging;
   private float logoDragOffsetX;
   private float logoDragOffsetY;
   private float logoRenderedX;
   private float logoRenderedY;
   private float logoRenderedWidth;
   private float logoRenderedHeight;

   @Override
   public float getHudX() {
      if (heartDragging) return heartRenderedX;
      if (logoDragging) return logoRenderedX;
      if (this.heart.getCurrentValue()) return heartRenderedX;
      return logoRenderedX;
   }

   @Override
   public float getHudY() {
      if (heartDragging) return heartRenderedY;
      if (logoDragging) return logoRenderedY;
      if (this.heart.getCurrentValue()) return heartRenderedY;
      return logoRenderedY;
   }

   @Override
   public float getHudWidth() {
      if (heartDragging) return heartRenderedWidth;
      if (logoDragging) return logoRenderedWidth;
      if (this.heart.getCurrentValue()) return heartRenderedWidth;
      return logoRenderedWidth;
   }

   @Override
   public float getHudHeight() {
      if (heartDragging) return heartRenderedHeight;
      if (logoDragging) return logoRenderedHeight;
      if (this.heart.getCurrentValue()) return heartRenderedHeight;
      return logoRenderedHeight;
   }

   @Override
   public void setHudX(float x) {
      if (heartDragging) {
         this.heartX.setCurrentValue(x - 6.0f);
      } else if (logoDragging) {
         int screenWidth = mc.getWindow().getGuiScaledWidth();
         this.logoX.setCurrentValue(x - (screenWidth / 2.0F - logoRenderedWidth / 2.0F));
      }
   }

   @Override
   public void setHudY(float y) {
      if (heartDragging) {
         int screenHeight = mc.getWindow().getGuiScaledHeight();
         this.heartY.setCurrentValue(y - (screenHeight - PANEL_HEIGHT - 6.0f));
      } else if (logoDragging) {
         int screenHeight = mc.getWindow().getGuiScaledHeight();
         this.logoY.setCurrentValue(y - (screenHeight / 2.0F - logoRenderedHeight / 2.0F));
      }
   }

   @Override
   public boolean isHudDragging() {
      return hudDragging;
   }

   @Override
   public void setHudDragging(boolean dragging, float offsetX, float offsetY) {
      this.hudDragging = dragging;
      this.hudDragOffsetX = offsetX;
      this.hudDragOffsetY = offsetY;
   }

   @Override
   public float getHudDragOffsetX() {
      return hudDragOffsetX;
   }

   @Override
   public float getHudDragOffsetY() {
      return hudDragOffsetY;
   }

   @Override
   public boolean mousePressed(int mouseX, int mouseY, int button) {
      if (button != 0) return false;
      if (this.heart.getCurrentValue()) {
         if (mouseX >= heartRenderedX && mouseX <= heartRenderedX + heartRenderedWidth
               && mouseY >= heartRenderedY && mouseY <= heartRenderedY + heartRenderedHeight) {
            heartDragging = true;
            hudDragging = true;
            hudDragOffsetX = mouseX - heartRenderedX;
            hudDragOffsetY = mouseY - heartRenderedY;
            heartDragOffsetX = mouseX - heartRenderedX;
            heartDragOffsetY = mouseY - heartRenderedY;
            return true;
         }
      }
      if (this.logo.getCurrentValue()) {
         if (mouseX >= logoRenderedX && mouseX <= logoRenderedX + logoRenderedWidth
               && mouseY >= logoRenderedY && mouseY <= logoRenderedY + logoRenderedHeight) {
            logoDragging = true;
            hudDragging = true;
            hudDragOffsetX = mouseX - logoRenderedX;
            hudDragOffsetY = mouseY - logoRenderedY;
            logoDragOffsetX = mouseX - logoRenderedX;
            logoDragOffsetY = mouseY - logoRenderedY;
            return true;
         }
      }
      return false;
   }

   @Override
   public void mouseDragged(int mouseX, int mouseY) {
      if (heartDragging) {
         float newX = mouseX - heartDragOffsetX;
         float newY = mouseY - heartDragOffsetY;
         this.heartX.setCurrentValue(newX - 6.0f);
         int screenHeight = mc.getWindow().getGuiScaledHeight();
         this.heartY.setCurrentValue(newY - (screenHeight - PANEL_HEIGHT - 6.0f));
      } else if (logoDragging) {
         float newX = mouseX - logoDragOffsetX;
         float newY = mouseY - logoDragOffsetY;
         int screenWidth = mc.getWindow().getGuiScaledWidth();
         int screenHeight = mc.getWindow().getGuiScaledHeight();
         this.logoX.setCurrentValue(newX - (screenWidth / 2.0F - logoRenderedWidth / 2.0F));
         this.logoY.setCurrentValue(newY - (screenHeight / 2.0F - logoRenderedHeight / 2.0F));
      }
   }

   @Override
   public void stopDragging() {
      heartDragging = false;
      logoDragging = false;
      hudDragging = false;
      hudDragOffsetX = 0;
      hudDragOffsetY = 0;
   }

   private ResourceLocation logoLocation = null;
   private int logoImgWidth;
   private int logoImgHeight;

   @EventTarget
   public void onRender(EventRender2D e) {
      if (mc.player == null || mc.level == null) return;

      if (this.logo.getCurrentValue()) {
         renderLogo(e);
      }
      if (this.heart.getCurrentValue()) {
         renderHeart(e);
      }
   }

   private void renderHeart(EventRender2D e) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      float health = mc.player.getHealth();
      float maxHealth = mc.player.getMaxHealth();
      float healthRatio = Math.min(health / maxHealth, 1.0F);

      int armor = mc.player.getArmorValue();
      float armorRatio = Math.min(armor / 20.0F, 1.0F);

      if (health != this.lastHealth) {
         this.lastHealth = health;
      }
      this.healthAnim.animate(healthRatio, 0.5F);
      this.healthLagAnim.animate(healthRatio, 1.5F);

      if (armor != this.lastArmor) {
         this.lastArmor = armor;
      }
      this.armorAnim.animate(armorRatio, 0.5F);
      this.armorLagAnim.animate(armorRatio, 1.5F);

      this.healthAnim.tick();
      this.healthLagAnim.tick();
      this.armorAnim.tick();
      this.armorLagAnim.tick();

      int screenHeight = mc.getWindow().getGuiScaledHeight();

      float x = 6.0F + this.heartX.getCurrentValue();
      float y = screenHeight - PANEL_HEIGHT - 6.0F + this.heartY.getCurrentValue();

      drawPlayerFace(e.getGuiGraphics(), e.getStack(), mc.player, x + PADDING, y + (PANEL_HEIGHT - FACE_SIZE) / 2.0F, FACE_SIZE);

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      e.getGuiGraphics().bufferSource().endBatch();

      float faceX = x + PADDING;
      float barX = faceX + FACE_SIZE + PADDING;
      float barWidth = x + PANEL_WIDTH - barX - PADDING - TEXT_RESERVE;
      float textX = barX + barWidth + 2.0F;
      float healthY = y + PADDING + 2.0F;
      float armorY = healthY + BAR_HEIGHT + BAR_GAP;

      SkijaRenderer.beginFrame();
      Canvas canvas = SkijaRenderer.getCanvas();
      if (canvas != null) {
         SkijaRenderer.fillRect(canvas, barX, healthY, barWidth, BAR_HEIGHT, BAR_BG.getRGB());
         float lagW = this.healthLagAnim.getValueF() * barWidth;
         SkijaRenderer.fillRect(canvas, barX, healthY, lagW, BAR_HEIGHT, BAR_LAG.getRGB());
         float barW = this.healthAnim.getValueF() * barWidth;
         SkijaRenderer.fillRect(canvas, barX, healthY, barW, BAR_HEIGHT, BAR_HEALTH.getRGB());

         SkijaRenderer.fillRect(canvas, barX, armorY, barWidth, BAR_HEIGHT, BAR_BG.getRGB());
         float aLagW = this.armorLagAnim.getValueF() * barWidth;
         SkijaRenderer.fillRect(canvas, barX, armorY, aLagW, BAR_HEIGHT, BAR_LAG.getRGB());
         float aBarW = this.armorAnim.getValueF() * barWidth;
         SkijaRenderer.fillRect(canvas, barX, armorY, aBarW, BAR_HEIGHT, BAR_ARMOR.getRGB());

         String hpText = (int) Math.ceil(health) + "HP";
         String armorText = armor + "AR";

         Font hpFont = SkijaRenderer.getHarmonyFont(10.0f);
         Font armorFont = SkijaRenderer.getOpensansFont(9.0f);
         float hpTextY = healthY + (BAR_HEIGHT - SkijaRenderer.getFontHeight(hpFont)) / 2.0f;
         float armorTextY = armorY + (BAR_HEIGHT - SkijaRenderer.getFontHeight(armorFont)) / 2.0f;

         SkijaRenderer.drawText(canvas, hpText, textX, hpTextY, hpFont, Color.WHITE.getRGB());
         SkijaRenderer.drawText(canvas, armorText, textX, armorTextY, armorFont, Color.GRAY.getRGB());

         SkijaRenderer.endFrame();
      } else {
         SkijaRenderer.endFrame();
      }

      this.heartRenderedX = x;
      this.heartRenderedY = y;
      this.heartRenderedWidth = PANEL_WIDTH;
      this.heartRenderedHeight = PANEL_HEIGHT;
   }

   private void renderLogo(EventRender2D e) {
      ResourceLocation tex = getLogoTexture();
      if (tex == null) return;

      float scale = this.logoScale.getCurrentValue();
      int w = (int)((float)this.logoImgWidth * scale);
      int h = (int)((float)this.logoImgHeight * scale);

      int screenWidth = mc.getWindow().getGuiScaledWidth();
      int screenHeight = mc.getWindow().getGuiScaledHeight();

      int x = (int)((float)screenWidth / 2.0F - (float)w / 2.0F + this.logoX.getCurrentValue());
      int y = (int)((float)screenHeight / 2.0F - (float)h / 2.0F + this.logoY.getCurrentValue());

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      e.getGuiGraphics().blit(tex, x, y, w, h, 0, 0, this.logoImgWidth, this.logoImgHeight, this.logoImgWidth, this.logoImgHeight);
      e.getGuiGraphics().bufferSource().endBatch();

      this.logoRenderedX = x;
      this.logoRenderedY = y;
      this.logoRenderedWidth = w;
      this.logoRenderedHeight = h;
   }

   private ResourceLocation getLogoTexture() {
      if (this.logoLocation != null) {
         return this.logoLocation;
      }

      loadLogoTexture();
      return this.logoLocation;
   }

   private void loadLogoTexture() {
      try {
         File logoFile = new File(mc.gameDirectory, "Shiori/logo.png");
         if (logoFile.exists()) {
            BufferedImage img = ImageIO.read(logoFile);
            if (img != null) {
               NativeImage nativeImage = bufferedImageToNativeImage(img);
               this.logoImgWidth = nativeImage.getWidth();
               this.logoImgHeight = nativeImage.getHeight();
               DynamicTexture tex = new DynamicTexture(nativeImage);
               this.logoLocation = mc.getTextureManager().register("shiori_logo", tex);
               return;
            }
         }

         ResourceLocation builtinLocation = new ResourceLocation("shiori", "logo.png");
         Resource resource = mc.getResourceManager().getResource(builtinLocation).orElse(null);
         if (resource != null) {
            NativeImage nativeImage = NativeImage.read(resource.open());
            this.logoImgWidth = nativeImage.getWidth();
            this.logoImgHeight = nativeImage.getHeight();
            DynamicTexture tex = new DynamicTexture(nativeImage);
            this.logoLocation = mc.getTextureManager().register("shiori_logo", tex);
            return;
         }

         InputStream stream = Custom.class.getClassLoader().getResourceAsStream("assets/shiori/logo.png");
         if (stream != null) {
            BufferedImage img = ImageIO.read(stream);
            stream.close();
            if (img != null) {
               NativeImage nativeImage = bufferedImageToNativeImage(img);
               this.logoImgWidth = nativeImage.getWidth();
               this.logoImgHeight = nativeImage.getHeight();
               DynamicTexture tex = new DynamicTexture(nativeImage);
               this.logoLocation = mc.getTextureManager().register("shiori_logo", tex);
               return;
            }
         }
      } catch (Exception ex) {
         System.err.println("[Shiori] Failed to load logo: " + ex.getMessage());
      }
   }

   private NativeImage bufferedImageToNativeImage(BufferedImage img) {
      int w = img.getWidth();
      int h = img.getHeight();
      NativeImage nativeImage = new NativeImage(w, h, false);

      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            int argb = img.getRGB(x, y);
            int a = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            nativeImage.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
         }
      }

      return nativeImage;
   }

   @Override
   public void onDisable() {
      if (this.logoLocation != null) {
         mc.getTextureManager().release(this.logoLocation);
         this.logoLocation = null;
      }
   }

   private void drawPlayerFace(GuiGraphics graphics, PoseStack poseStack, AbstractClientPlayer player, float x, float y, float size) {
      ResourceLocation skinTexture = player.getSkinTextureLocation();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      try {
         graphics.blit(
            skinTexture,
            (int) x,
            (int) y,
            (int) size,
            (int) size,
            8, 8, 8, 8,
            64, 64
         );
         graphics.blit(
            skinTexture,
            (int) x,
            (int) y,
            (int) size,
            (int) size,
            40, 8, 8, 8,
            64, 64
         );
      } catch (Exception ex) {
         RenderUtils.drawRoundedRect(poseStack, x, y, size, size, size * 0.17F, new Color(150, 150, 150, 255).getRGB());
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
   }
}