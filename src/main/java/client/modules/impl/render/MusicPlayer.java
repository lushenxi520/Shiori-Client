package client.modules.impl.render;

import client.events.api.EventTarget;
import client.events.impl.EventRender2D;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.ui.hud.HudElement;
import client.utils.renderer.SkijaRenderer;
import client.utils.smtc.SmtcUtils;
import client.utils.smtc.SmtcUtils.SmtcInfo;
import client.values.ValueBuilder;
import client.values.impl.FloatValue;
import client.values.impl.ModeValue;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Image;
import java.awt.Color;
import java.util.Base64;

@ModuleInfo(
   name = "MusicPlayer",
   description = "Displays the currently playing music",
   category = Category.RENDER
)
public class MusicPlayer extends Module implements HudElement {

   public ModeValue position = ValueBuilder.create(this, "Position")
         .setVisibility(() -> false)
         .setDefaultModeIndex(3)
         .setModes("Top Left", "Top Right", "Bottom Left", "Bottom Right")
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

   private volatile SmtcInfo cachedSmtc = null;
   private volatile String cachedDisplayText = "\u266B No Music";
   private volatile boolean cachedHasCover = false;
   private static final float FONT_SIZE = 11.0f;
   private static final int COVER_SIZE = 40;
   private static final int COVER_GAP = 6;
   private static final int REFRESH_INTERVAL_MS = 3000;

   private String lastCoverBase64 = null;
   private Image cachedCoverImage = null;

   private boolean hudDragging;
   private float hudDragOffsetX;
   private float hudDragOffsetY;
   private float hudRenderedX;
   private float hudRenderedY;
   private float hudRenderedWidth;
   private float hudRenderedHeight;

   private Thread refreshThread = null;
   private volatile boolean refreshRunning = false;

   @Override
   public float getHudX() { return hudRenderedX; }

   @Override
   public float getHudY() { return hudRenderedY; }

   @Override
   public float getHudWidth() { return hudRenderedWidth; }

   @Override
   public float getHudHeight() { return hudRenderedHeight; }

   @Override
   public void setHudX(float x) {
      this.xOffset.setCurrentValue(x - 4.0f);
      this.position.setCurrentValue(0);
   }

   @Override
   public void setHudY(float y) {
      this.yOffset.setCurrentValue(y - 4.0f);
      this.position.setCurrentValue(0);
   }

   @Override
   public boolean isHudDragging() { return hudDragging; }

   @Override
   public void setHudDragging(boolean dragging, float offsetX, float offsetY) {
      this.hudDragging = dragging;
      this.hudDragOffsetX = offsetX;
      this.hudDragOffsetY = offsetY;
   }

   @Override
   public float getHudDragOffsetX() { return hudDragOffsetX; }

   @Override
   public float getHudDragOffsetY() { return hudDragOffsetY; }

   private void startRefreshThread() {
      if (refreshRunning) return;
      refreshRunning = true;
      refreshThread = new Thread(() -> {
         while (refreshRunning) {
            try {
               SmtcInfo info = SmtcUtils.getCurrentInfo();
               cachedSmtc = info;
               if (info != null && info.isPlaying()) {
                  if (info.duration > 0) {
                     cachedDisplayText = "\u266B " + info.title + " | " + info.getFormattedPosition() + " / " + info.getFormattedDuration();
                  } else {
                     cachedDisplayText = "\u266B " + info.title;
                  }
               } else {
                  cachedDisplayText = "\u266B No Music";
               }
               cachedHasCover = info != null && info.base64Cover != null && !info.base64Cover.isEmpty();
            } catch (Exception ignored) {}
            try {
               Thread.sleep(REFRESH_INTERVAL_MS);
            } catch (InterruptedException ignored) {
               break;
            }
         }
      }, "MusicPlayer-SMTC-Refresh");
      refreshThread.setDaemon(true);
      refreshThread.start();
   }

   @Override
   public void onEnable() {
      startRefreshThread();
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      SmtcInfo smtc = cachedSmtc;
      String displayText = cachedDisplayText;
      boolean hasCover = cachedHasCover;

      Image cover = hasCover ? getCoverImage(smtc.base64Cover) : null;
      boolean showCover = hasCover && cover != null;

      Font font = SkijaRenderer.getHarmonyFont(FONT_SIZE);
      float textWidth = SkijaRenderer.measureTextWidth(displayText, font);
      float textHeight = SkijaRenderer.getFontHeight(font);

      float coverAreaWidth = showCover ? COVER_SIZE + COVER_GAP : 0;
      float fullWidth = coverAreaWidth + textWidth + 16.0f;
      float fullHeight = showCover ? Math.max(COVER_SIZE, textHeight) + 12.0f : textHeight + 12.0f;

      float[] pos = getPosition(fullWidth, fullHeight);
      float x = pos[0];
      float y = pos[1];

      SkijaRenderer.beginFrame();
      Canvas canvas = SkijaRenderer.getCanvas();
      if (canvas == null) {
         SkijaRenderer.endFrame();
         return;
      }

      SkijaRenderer.drawRoundedRect(canvas, x, y, fullWidth, fullHeight, 5.0f,
            new Color(0, 0, 0, 80).getRGB());

      if (showCover) {
         float coverX = x + 4.0f;
         float coverY = y + (fullHeight - COVER_SIZE) / 2.0f;
         canvas.drawImageRect(cover,
               io.github.humbleui.types.Rect.makeXYWH(coverX, coverY, COVER_SIZE, COVER_SIZE));
      }

      float textX = x + 8.0f + coverAreaWidth;
      float textY = y + fullHeight / 2.0f - textHeight / 2.0f;
      SkijaRenderer.drawText(canvas, displayText, textX, textY, font, new Color(255, 255, 255, 255).getRGB());

      SkijaRenderer.endFrame();

      this.hudRenderedX = x;
      this.hudRenderedY = y;
      this.hudRenderedWidth = fullWidth;
      this.hudRenderedHeight = fullHeight;
   }

   private Image getCoverImage(String base64) {
      if (base64 == null || base64.isEmpty()) {
         return null;
      }
      if (base64.equals(lastCoverBase64) && cachedCoverImage != null) {
         return cachedCoverImage;
      }

      releaseCoverImage();

      try {
         byte[] bytes = Base64.getDecoder().decode(base64);
         Image img = Image.makeFromEncoded(bytes);

         if (img != null) {
            cachedCoverImage = img;
            lastCoverBase64 = base64;
         }
         return img;
      } catch (Exception ex) {
         System.err.println("[Shiori] Failed to load cover image: " + ex.getMessage());
         lastCoverBase64 = null;
         return null;
      }
   }

   private void releaseCoverImage() {
      if (cachedCoverImage != null) {
         cachedCoverImage.close();
         cachedCoverImage = null;
         lastCoverBase64 = null;
      }
   }

   private float[] getPosition(float width, float height) {
      int screenWidth = mc.getWindow().getGuiScaledWidth();
      int screenHeight = mc.getWindow().getGuiScaledHeight();
      float margin = 4.0f;
      float xo = xOffset.getCurrentValue();
      float yo = yOffset.getCurrentValue();

      String pos = position.getCurrentMode();
      float x, y;

      switch (pos) {
         case "Top Left":
            x = margin + xo;
            y = margin + yo;
            break;
         case "Top Right":
            x = screenWidth - width - margin + xo;
            y = margin + yo;
            break;
         case "Bottom Left":
            x = margin + xo;
            y = screenHeight - height - margin + yo;
            break;
         case "Bottom Right":
         default:
            x = screenWidth - width - margin + xo;
            y = screenHeight - height - margin + yo;
            break;
      }

      return new float[] { x, y };
   }

   @Override
   public void onDisable() {
      stopRefreshThread();
      releaseCoverImage();
   }

   private void stopRefreshThread() {
      refreshRunning = false;
      if (refreshThread != null) {
         refreshThread.interrupt();
         refreshThread = null;
      }
   }
}