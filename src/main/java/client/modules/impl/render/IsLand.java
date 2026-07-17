package client.modules.impl.render;

import client.events.api.EventTarget;
import client.events.impl.EventRender2D;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.ui.hud.HudElement;
import client.utils.SmoothAnimationTimer;
import client.utils.renderer.SkijaRenderer;
import client.values.ValueBuilder;
import client.values.impl.FloatValue;
import client.values.impl.ModeValue;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import java.awt.Color;
import net.minecraft.client.multiplayer.PlayerInfo;

@ModuleInfo(
   name = "IsLand",
   description = "Dynamic Island style HUD showing client title, FPS, and ping",
   category = Category.RENDER
)
public class IsLand extends Module implements HudElement {

   private static final float BASE_HEIGHT = 32.0F;
   private static final float ISLAND_MAX_WIDTH = 240.0F;
   private static final float ISLAND_MIN_WIDTH = 120.0F;
   private static final float CORNER_RADIUS = 18.0F;
   private static final float FONT_SIZE = 13.0F;
   private static final float ITEM_GAP = 12.0F;
   private static final float GLOW_SIZE = 8.0F;
   private static final float EDGE_GLOW_SIZE = 5.0F;

   private static final Color BG_COLOR = new Color(18, 18, 18, 220);
   private static final Color GLOW_COLOR = new Color(255, 255, 255, 25);
   private static final Color TEXT_COLOR = new Color(180, 210, 255, 255);
   private static final Color EDGE_COLOR = new Color(180, 210, 255, 80);

   private final SmoothAnimationTimer expandAnim = new SmoothAnimationTimer(100.0F, 0.0F, 0.3F);
   private final SmoothAnimationTimer glowAnim = new SmoothAnimationTimer(0.0F, 0.0F, 0.15F);

   private long lastFpsUpdateTime;
   private int cachedFps;
   private int frameCount;
   private int cachedPing;

   public ModeValue position = ValueBuilder.create(this, "Position")
      .setVisibility(() -> false)
      .setDefaultModeIndex(0)
      .setModes("Top Center")
      .build()
      .getModeValue();

   public FloatValue xOffset = ValueBuilder.create(this, "X Offset")
      .setVisibility(() -> false)
      .setMinFloatValue(-5000.0F)
      .setMaxFloatValue(5000.0F)
      .setDefaultFloatValue(0.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();

   public FloatValue yOffset = ValueBuilder.create(this, "Y Offset")
      .setVisibility(() -> false)
      .setMinFloatValue(-5000.0F)
      .setMaxFloatValue(5000.0F)
      .setDefaultFloatValue(0.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();

   public FloatValue scale = ValueBuilder.create(this, "Scale")
      .setDefaultFloatValue(1.0F)
      .setMinFloatValue(0.5F)
      .setMaxFloatValue(2.0F)
      .setFloatStep(0.05F)
      .build()
      .getFloatValue();

   private boolean hudDragging;
   private float hudDragOffsetX;
   private float hudDragOffsetY;
   private float hudRenderedX;
   private float hudRenderedY;
   private float hudRenderedWidth;
   private float hudRenderedHeight;

   public IsLand() {
      this.expandAnim.setCurrentValue(0.0F);
      this.glowAnim.setCurrentValue(0.0F);
   }

   @Override
   public void onEnable() {
      this.expandAnim.target = 100.0F;
      this.glowAnim.target = 1.0F;
   }

   @Override
   public void onDisable() {
      this.expandAnim.target = 0.0F;
      this.glowAnim.target = 0.0F;
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      updateAnimations();
      updateFps();
      updatePing();

      float expandProgress = expandAnim.value / 100.0F;
      if (expandProgress <= 0.01F) return;

      float s = scale.getCurrentValue();
      float scaledFontSize = FONT_SIZE * s;
      float scaledHeight = BASE_HEIGHT * s;
      float scaledCornerRadius = CORNER_RADIUS * s;
      float scaledPadding = 16.0F * s;
      float scaledItemGap = ITEM_GAP * s;
      float scaledGlowSize = GLOW_SIZE * s;
      float scaledEdgeGlow = EDGE_GLOW_SIZE * s;

      Font font = SkijaRenderer.getHarmonyFont(scaledFontSize);
      float fontHeight = SkijaRenderer.getFontHeight(font);

      String title = "Shiori";
      String fpsText = cachedFps + " FPS";
      String pingText = cachedPing + "ms";
      String spacer = "  ";

      float titleWidth = SkijaRenderer.measureTextWidth(title, font);
      float fpsWidth = SkijaRenderer.measureTextWidth(fpsText, font);
      float pingWidth = SkijaRenderer.measureTextWidth(pingText, font);
      float spacerWidth = SkijaRenderer.measureTextWidth(spacer, font);

      float totalTextWidth = titleWidth + spacerWidth + fpsWidth + spacerWidth + pingWidth;
      float contentWidth = totalTextWidth + scaledPadding * 2.0F;

      float fullWidth = ISLAND_MIN_WIDTH * s + (contentWidth - ISLAND_MIN_WIDTH * s) * expandProgress;
      fullWidth = Math.min(fullWidth, ISLAND_MAX_WIDTH * s);
      float islandWidth = fullWidth;

      int screenWidth = mc.getWindow().getGuiScaledWidth();
      float x = screenWidth / 2.0F - islandWidth / 2.0F + xOffset.getCurrentValue();
      float y = 8.0F + yOffset.getCurrentValue();

      float currentCornerRadius = scaledCornerRadius * expandProgress;

      SkijaRenderer.beginFrame();
      Canvas canvas = SkijaRenderer.getCanvas();
      if (canvas == null) {
         SkijaRenderer.endFrame();
         return;
      }

      long time = System.currentTimeMillis();

      if (glowAnim.value > 0.0F) {
         float glowAlpha = glowAnim.value * expandProgress * (0.6F + 0.4F * (float) Math.sin(time / 600.0));
         int glowAlphaInt = (int) (GLOW_COLOR.getAlpha() * glowAlpha);
         if (glowAlphaInt > 0) {
            SkijaRenderer.drawGlowRoundedRect(
               canvas, x, y, islandWidth, scaledHeight, currentCornerRadius,
               new Color(GLOW_COLOR.getRed(), GLOW_COLOR.getGreen(), GLOW_COLOR.getBlue(), glowAlphaInt).getRGB(),
               scaledGlowSize * expandProgress
            );
         }
      }

      int bgAlpha = (int) (BG_COLOR.getAlpha() * expandProgress);
      SkijaRenderer.drawRoundedRect(
         canvas, x, y, islandWidth, scaledHeight, currentCornerRadius,
         new Color(BG_COLOR.getRed(), BG_COLOR.getGreen(), BG_COLOR.getBlue(), bgAlpha).getRGB()
      );

      if (glowAnim.value > 0.0F && expandProgress > 0.5F) {
         float edgeAlpha = 0.6F + 0.4F * (float) Math.sin(time / 600.0);
         float edgeFinalAlpha = edgeAlpha * expandProgress * glowAnim.value;
         int edgeGlowColor = new Color(
            EDGE_COLOR.getRed() / 255.0F, EDGE_COLOR.getGreen() / 255.0F, EDGE_COLOR.getBlue() / 255.0F, edgeFinalAlpha
         ).getRGB();
         SkijaRenderer.drawGlowRoundedRect(
            canvas, x, y, islandWidth, scaledHeight, currentCornerRadius,
            edgeGlowColor, scaledEdgeGlow
         );
      }

      if (expandProgress > 0.3F) {
         float textAlpha = Math.min(1.0F, (expandProgress - 0.3F) / 0.7F);
         float textY = y + scaledHeight / 2.0F - fontHeight / 2.0F;

         float startX = x + islandWidth / 2.0F - totalTextWidth / 2.0F;
         float cursorX = startX;

         int color = new Color(
            TEXT_COLOR.getRed(), TEXT_COLOR.getGreen(), TEXT_COLOR.getBlue(),
            (int) (255 * textAlpha)
         ).getRGB();

         SkijaRenderer.drawText(canvas, title, cursorX, textY, font, color);
         cursorX += titleWidth + spacerWidth;

         SkijaRenderer.drawText(canvas, fpsText, cursorX, textY, font, color);
         cursorX += fpsWidth + spacerWidth;

         SkijaRenderer.drawText(canvas, pingText, cursorX, textY, font, color);
      }

      SkijaRenderer.endFrame();

      this.hudRenderedX = x;
      this.hudRenderedY = y;
      this.hudRenderedWidth = islandWidth;
      this.hudRenderedHeight = scaledHeight;
   }

   private void updateAnimations() {
      if (this.isEnabled()) {
         this.expandAnim.target = 100.0F;
         this.glowAnim.target = 1.0F;
      } else {
         this.expandAnim.target = 0.0F;
         this.glowAnim.target = 0.0F;
      }
      this.expandAnim.update(true);
      this.glowAnim.update(true);
   }

   private void updateFps() {
      long now = System.currentTimeMillis();
      frameCount++;
      if (now - lastFpsUpdateTime >= 1000L) {
         float elapsed = (now - lastFpsUpdateTime) / 1000.0F;
         cachedFps = Math.round(frameCount / elapsed);
         frameCount = 0;
         lastFpsUpdateTime = now;
      }
   }

   private void updatePing() {
      if (mc.player == null || mc.getConnection() == null) {
         cachedPing = 0;
         return;
      }
      PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(mc.player.getUUID());
      if (playerInfo != null) {
         cachedPing = playerInfo.getLatency();
      }
   }

   @Override
   public float getHudX() {
      return hudRenderedX;
   }

   @Override
   public float getHudY() {
      return hudRenderedY;
   }

   @Override
   public float getHudWidth() {
      return hudRenderedWidth;
   }

   @Override
   public float getHudHeight() {
      return hudRenderedHeight;
   }

   @Override
   public void setHudX(float x) {
      this.xOffset.setCurrentValue(x - (mc.getWindow().getGuiScaledWidth() / 2.0F - hudRenderedWidth / 2.0F));
   }

   @Override
   public void setHudY(float y) {
      this.yOffset.setCurrentValue(y - 8.0F);
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
}