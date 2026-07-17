package client.ui.notification;

import client.utils.RenderUtils;
import client.utils.SmoothAnimationTimer;
import client.utils.renderer.Fonts;
import client.utils.renderer.text.CustomTextRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.awt.Color;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public class Notification {
   public static byte[] authTokens;
   private NotificationLevel level;
   private String message;
   private long maxAge;
   private long createTime = System.currentTimeMillis();
   private SmoothAnimationTimer widthTimer = new SmoothAnimationTimer(0.0F);
   private SmoothAnimationTimer heightTimer = new SmoothAnimationTimer(0.0F);

   public Notification(NotificationLevel level, String message, long age) {
      this.level = level;
      this.message = message;
      this.maxAge = age;
   }

   public void render(PoseStack stack, float x, float y) {
      float width = getWidth();
      float height = getHeight();

      RenderUtils.fill(stack, x, y, x + width, y + height, new Color(36, 36, 36, 200).getRGB());

      float elapsed = (float)(System.currentTimeMillis() - this.createTime);
      float progress = Math.min(elapsed / (float)this.maxAge, 1.0F);
      RenderUtils.fill(stack, x, y + height - 1.5F, x + width * progress, y + height,
            new Color(255, 255, 255, 180).getRGB());

      CustomTextRenderer font = Fonts.opensans;
      String text = this.message;
      Color white = Color.WHITE;
      Color gray = new Color(170, 170, 170);
      double scale = 0.45;
      double textY = y + height / 2.0F - font.getHeight(false, scale) / 2.0F;
      float iconCenterX = x + 9;
      float iconCenterY = y + height / 2.0F;

      if (text.contains("Enabled")) {
         String moduleName = text.replace(" Enabled!", "");
         drawCheckmark(stack, iconCenterX, iconCenterY, new Color(0, 255, 0).getRGB());
         font.render(stack, moduleName, x + 17, textY, white, false, scale);
         font.render(stack, "Enabled", x + 17 + font.getWidth(moduleName, scale) + 3, textY, gray, false, scale);
      } else if (text.contains("Disabled")) {
         String moduleName = text.replace(" Disabled!", "");
         drawCross(stack, iconCenterX, iconCenterY, new Color(255, 80, 80).getRGB());
         font.render(stack, moduleName, x + 17, textY, white, false, scale);
         font.render(stack, "Disabled", x + 17 + font.getWidth(moduleName, scale) + 3, textY, gray, false, scale);
      } else {
         font.render(stack, text, x + 10, textY, white, false, scale);
      }
   }

   private void drawCheckmark(PoseStack stack, float cx, float cy, int color) {
      drawLine(stack, cx - 4, cy, cx - 1, cy + 3, 1.5F, color);
      drawLine(stack, cx - 1, cy + 3, cx + 4, cy - 3, 1.5F, color);
   }

   private void drawCross(PoseStack stack, float cx, float cy, int color) {
      drawLine(stack, cx - 3, cy - 3, cx + 3, cy + 3, 1.5F, color);
      drawLine(stack, cx + 3, cy - 3, cx - 3, cy + 3, 1.5F, color);
   }

   private void drawLine(PoseStack stack, float x1, float y1, float x2, float y2, float thickness, int color) {
      double dx = x2 - x1;
      double dy = y2 - y1;
      double len = Math.sqrt(dx * dx + dy * dy);
      if (len == 0.0) {
         return;
      }
      double nx = dx / len;
      double ny = dy / len;
      double hw = (double)thickness / 2.0;

      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;

      Matrix4f matrix = stack.last().pose();
      BufferBuilder buffer = Tesselator.getInstance().getBuilder();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      buffer.vertex(matrix, (float)((double)x1 - ny * hw), (float)((double)y1 + nx * hw), 0.0F).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, (float)((double)x1 + ny * hw), (float)((double)y1 - nx * hw), 0.0F).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, (float)((double)x2 + ny * hw), (float)((double)y2 - nx * hw), 0.0F).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, (float)((double)x2 - ny * hw), (float)((double)y2 + nx * hw), 0.0F).color(r, g, b, a).endVertex();
      BufferUploader.drawWithShader(buffer.end());
   }

   public float getWidth() {
      CustomTextRenderer font = Fonts.opensans;
      return font.getWidth(this.message, 0.45) + 25;
   }

   public float getHeight() {
      return 22;
   }

   public NotificationLevel getLevel() {
      return this.level;
   }

   public String getMessage() {
      return this.message;
   }

   public long getMaxAge() {
      return this.maxAge;
   }

   public long getCreateTime() {
      return this.createTime;
   }

   public SmoothAnimationTimer getWidthTimer() {
      return this.widthTimer;
   }

   public SmoothAnimationTimer getHeightTimer() {
      return this.heightTimer;
   }

   public void setLevel(NotificationLevel level) {
      this.level = level;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public void setMaxAge(long maxAge) {
      this.maxAge = maxAge;
   }

   public void setCreateTime(long createTime) {
      this.createTime = createTime;
   }

   public void setWidthTimer(SmoothAnimationTimer widthTimer) {
      this.widthTimer = widthTimer;
   }

   public void setHeightTimer(SmoothAnimationTimer heightTimer) {
      this.heightTimer = heightTimer;
   }
}