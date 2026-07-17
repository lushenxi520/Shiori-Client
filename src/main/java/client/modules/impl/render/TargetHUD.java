package client.modules.impl.render;

import client.events.api.EventTarget;
import client.events.impl.EventPacket;
import client.events.impl.EventRender2D;
import client.events.impl.EventShader;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.modules.impl.combat.Killaura;
import client.ui.hud.HudElement;
import client.utils.RenderUtils;
import client.utils.SmoothAnimationTimer;
import client.utils.StencilUtils;
import client.utils.renderer.Fonts;
import client.utils.renderer.SkijaRenderer;
import client.values.ValueBuilder;
import client.values.impl.FloatValue;
import client.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector4f;

@ModuleInfo(
   name = "TargetHUD",
   description = "Displays information about your current target",
   category = Category.RENDER
)
public class TargetHUD extends Module implements HudElement {
   public static final Map<String, AtomicInteger> playerHealthMap = new HashMap<>();
   private static final Color COLOR_PANEL_BG = new Color(0, 0, 0, 80);
   private static final Color COLOR_HEALTH_BG = new Color(0, 0, 0, 100);
   private static final Color COLOR_HEALTH_BAR = new Color(0, 150, 255);
   private static final Color COLOR_HEALTH_BAR2 = new Color(0, 100, 255);
   private static final Color COLOR_HEALTH_LAG = new Color(99, 99, 99, 120);
   private static final Color BOX_HEADER_COLOR = new Color(150, 45, 45, 255);
   private static final Color BOX_BODY_COLOR = new Color(0, 0, 0, 50);

   private final SmoothAnimationTimer healthAnim = new SmoothAnimationTimer();
   private final SmoothAnimationTimer healthLagAnim = new SmoothAnimationTimer();
   private final SmoothAnimationTimer scaleAnim = new SmoothAnimationTimer();
   private final SmoothAnimationTimer fadeAnim = new SmoothAnimationTimer();
   private final SmoothAnimationTimer slideAnim = new SmoothAnimationTimer();
   private final SmoothAnimationTimer contentAnim = new SmoothAnimationTimer();
   private final SmoothAnimationTimer boxHealthAnim = new SmoothAnimationTimer();
   private final SmoothAnimationTimer boxSlideAnim = new SmoothAnimationTimer();
   private final ItemStack[] equipmentSlots = new ItemStack[4];

   private float lastHealth;
   private float healthDelta;
   private int lastHurtTime;
   private boolean visible = false;
   private LivingEntity currentTarget;
   private long lastActiveTime = 0L;
   private Vector4f blurMatrix;

   private boolean hudDragging;
   private float hudDragOffsetX;
   private float hudDragOffsetY;
   private float hudRenderedX;
   private float hudRenderedY;
   private float hudRenderedWidth;
   private float hudRenderedHeight;

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
      float margin = 4.0f;
      this.xOffset.setCurrentValue(x - margin);
      this.position.setCurrentValue(0);
   }

   @Override
   public void setHudY(float y) {
      float margin = 4.0f;
      this.yOffset.setCurrentValue(y - margin);
      this.position.setCurrentValue(0);
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

   public ModeValue styleMode = ValueBuilder.create(this, "Style")
      .setDefaultModeIndex(0)
      .setModes("Round", "Box")
      .build()
      .getModeValue();

   public ModeValue position = ValueBuilder.create(this, "Position")
      .setVisibility(() -> false)
      .setDefaultModeIndex(4)
      .setModes("Top Left", "Top Right", "Bottom Left", "Bottom Right", "Crosshair")
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

   public TargetHUD() {
      this.scaleAnim.setCurrentValue(1.0f);
      this.fadeAnim.setCurrentValue(0.0f);
      this.slideAnim.setCurrentValue(5.0f);
      this.contentAnim.setCurrentValue(0.0f);
      this.boxHealthAnim.setCurrentValue(0.0f);
      this.boxSlideAnim.setCurrentValue(-120.0f);
      for (int i = 0; i < this.equipmentSlots.length; i++) {
         this.equipmentSlots[i] = ItemStack.EMPTY;
      }
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      Packet<?> packet = event.getPacket();
      if (packet instanceof ClientboundSetScorePacket scorePacket) {
         if (mc.level != null && mc.player != null
               && ("belowHealth".equals(scorePacket.getObjectiveName())
               || "health".equals(scorePacket.getObjectiveName()))
               && !scorePacket.getOwner().equals(mc.player.getGameProfile().getName())) {
            playerHealthMap.computeIfAbsent(scorePacket.getOwner(), s -> new AtomicInteger())
                  .set(scorePacket.getScore());
         }
      }
   }

   @EventTarget
   public void onShader(EventShader e) {
      if (this.blurMatrix != null) {
         RenderUtils.drawRoundedRect(e.getStack(), this.blurMatrix.x(), this.blurMatrix.y(),
               this.blurMatrix.z(), this.blurMatrix.w(), 5.0f, -1);
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      if (mc.level == null || mc.player == null) {
         return;
      }

      for (AbstractClientPlayer player : mc.level.players()) {
         if (player == mc.player || !playerHealthMap.containsKey(player.getName().getString())) {
            continue;
         }
         player.setHealth(Math.max(1, playerHealthMap.get(player.getName().getString()).get()));
      }

      LivingEntity target = null;
      if (mc.screen instanceof ChatScreen) {
         target = mc.player;
      } else if (Killaura.aimingTarget instanceof LivingEntity le) {
         target = le;
      } else if (Killaura.target instanceof LivingEntity le) {
         target = le;
      }

      if (target != null) {
         if (!Mth.equal(this.lastHealth, target.getHealth())) {
            this.healthDelta = target.getHealth() - this.lastHealth;
            this.lastHealth = target.getHealth();
         }
         float currentHealth = Math.min(target.getHealth(), 20.0f);
         float maxHealth = Math.min(target.getMaxHealth(), 20.0f);
         float ratio = maxHealth > 0.0f ? currentHealth / maxHealth : 0.0f;
         this.healthAnim.animate(ratio, 0.5f);
         this.healthLagAnim.animate(ratio, 1.5f);
         this.boxHealthAnim.animate(ratio, 0.5f);
      } else {
         this.healthDelta = 0.0f;
      }

      this.healthAnim.tick();
      this.healthLagAnim.tick();
      this.boxHealthAnim.tick();

      if (this.styleMode.isCurrentMode("Round")) {
         renderRoundStyle(e, target);
      } else if (this.styleMode.isCurrentMode("Box")) {
         renderBoxStyle(e, target);
      }
   }


   private void renderBoxStyle(EventRender2D e, LivingEntity target) {
      float panelWidth = 120.0f;
      float panelHeight = 42.0f;
      float[] pos = getPosition(panelWidth, panelHeight);
      float x = pos[0];
      float y = pos[1];

      boolean hasTarget = target != null;
      long now = System.currentTimeMillis();

      if (hasTarget) {
         this.lastActiveTime = now;
         if (this.currentTarget != target) {
            this.currentTarget = target;
         }
      }

      long timeSinceActive = now - this.lastActiveTime;
      if (!hasTarget && timeSinceActive > 2000L) {
         this.blurMatrix = null;
         return;
      }

      float headSize = 26.0f;
      float headX = x + 5.0f;
      float headY = y + (panelHeight - headSize) / 2.0f;
      float contentX = x + headSize + 9.0f;

      this.blurMatrix = new Vector4f(x, y, panelWidth, panelHeight);

      SkijaRenderer.beginFrame();
      Canvas canvas = SkijaRenderer.getCanvas();
      if (canvas == null) {
         SkijaRenderer.endFrame();
         return;
      }

      SkijaRenderer.fillRect(canvas, x, y, panelWidth, panelHeight, BOX_BODY_COLOR.getRGB());

      SkijaRenderer.drawHorizontalRainbow(canvas, x, y, panelWidth, panelHeight, 0.55f, 0.95f, 0.3f, 0.25f, 100);

      float healthRatio = this.boxHealthAnim.getValueF();
      if (healthRatio > 0.0f) {
         canvas.save();
         canvas.clipRect(io.github.humbleui.types.Rect.makeXYWH(x, y, panelWidth * healthRatio, panelHeight));
         SkijaRenderer.drawHorizontalRainbow(canvas, x, y, panelWidth, panelHeight, 0.55f, 0.95f, 0.35f, 0.35f, 140);
         canvas.restore();
      }

      String displayName = this.currentTarget != null
            ? (this.currentTarget == mc.player
                  ? NameProtect.getName(mc.player.getName().getString())
                  : this.currentTarget.getName().getString())
            : "";
      Font nameFont = SkijaRenderer.getHarmonyFont(11.0f);
      float nameHeight = SkijaRenderer.getFontHeight(nameFont);
      float centeredNameY = y + (panelHeight - nameHeight) / 2.0f;
      SkijaRenderer.drawText(canvas, displayName, contentX, centeredNameY, nameFont, BOX_HEADER_COLOR.getRGB());

      float glowSize = 4.0f;
      float glowAlpha = 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 600.0);
      int glowColor = new Color(0.55f, 0.65f, 0.85f, glowAlpha).getRGB();
      SkijaRenderer.drawGlowRect(canvas, x, y, panelWidth, panelHeight, glowColor, glowSize);

      SkijaRenderer.endFrame();

      this.hudRenderedX = x;
      this.hudRenderedY = y;
      this.hudRenderedWidth = panelWidth;
      this.hudRenderedHeight = panelHeight;

      if (this.currentTarget != null) {
         PoseStack poseStack = e.getStack();
         poseStack.pushPose();
         drawPlayerHead(e.getGuiGraphics(), poseStack, this.currentTarget, headX, headY, headSize, 1.0f);
         poseStack.popPose();
      }
   }


   private void renderRoundStyle(EventRender2D e, LivingEntity target) {
      float panelWidth = 120.0f;
      float panelHeight = 38.0f;
      float[] pos = getPosition(panelWidth, panelHeight);
      float x = pos[0];
      float y = pos[1];

      float headBoxSize = 30.0f;
      float headPadding = 4.0f;
      float contentX = x + 4.0f + headBoxSize + headPadding;
      float contentWidth = panelWidth - (contentX - x) - 3.0f;
      float nameY = (float) (y + 3.0f + 2.0f);
      float fontHeight = (float) Fonts.harmony.getHeight(true, 0.45f);
      float healthH = 4.0f;
      float healthY = (float) (nameY + fontHeight + 1.0f);
      float healthW = contentWidth - 2.0f;
      float equipY = healthY + healthH + 2.0f;

      boolean hasTarget = target != null;
      long now = System.currentTimeMillis();
      boolean targetChanged = false;

      if (hasTarget) {
         this.lastActiveTime = now;
         if (this.currentTarget != target) {
            this.currentTarget = target;
            targetChanged = true;
         }
      }

      boolean shouldShow = hasTarget || now - this.lastActiveTime < 300L;
      if (shouldShow != this.visible) {
         this.visible = shouldShow;
         if (this.visible) {
            this.fadeAnim.animate(1.0f, 0.35f);
            this.slideAnim.setCurrentValue(5.0f);
            this.slideAnim.setStartTime(0L);
            this.contentAnim.setCurrentValue(0.0f);
            this.contentAnim.setStartTime(0L);
            this.scaleAnim.setCurrentValue(1.0f);
         } else {
            this.fadeAnim.animate(0.0f, 0.15f);
            this.slideAnim.animate(5.0f, 0.15f);
            this.contentAnim.animate(0.0f, 0.15f);
         }
      } else if (targetChanged && this.visible) {
         this.fadeAnim.animate(1.0f, 0.35f);
         this.slideAnim.setCurrentValue(5.0f);
         this.slideAnim.setStartTime(0L);
         this.contentAnim.setCurrentValue(0.0f);
         this.contentAnim.setStartTime(0L);
         this.scaleAnim.setCurrentValue(1.0f);
      }

      this.fadeAnim.tick();
      if (this.fadeAnim.isAnimating() && this.visible) {
         if (this.fadeAnim.getProgress() >= 0.08f && this.slideAnim.getStartTime() == 0L) {
            this.slideAnim.animate(0.0f, 0.3f);
         }
         if (this.fadeAnim.getProgress() >= 0.15f && this.contentAnim.getStartTime() == 0L) {
            this.contentAnim.animate(1.0f, 0.4f);
         }
      }
      if (this.slideAnim.getStartTime() != 0L) {
         this.slideAnim.tick();
      }
      if (this.contentAnim.getStartTime() != 0L) {
         this.contentAnim.tick();
      }

      float fade = this.fadeAnim.getValueF();
      if (fade <= 0.01f) {
         return;
      }

      PoseStack poseStack = e.getStack();
      poseStack.pushPose();

      this.blurMatrix = new Vector4f(x, y, panelWidth, panelHeight);

      RenderUtils.fillBound(poseStack, x, y, panelWidth, panelHeight, BOX_BODY_COLOR.getRGB());

      StencilUtils.write(false);
      RenderUtils.drawRoundedRect(poseStack, x, y, panelWidth, panelHeight, 5.0f, -1);
      StencilUtils.erase(true);
      RenderUtils.fillBound(poseStack, x, y, panelWidth, panelHeight,
            new Color(0, 0, 0, (int) (COLOR_PANEL_BG.getAlpha() * fade)).getRGB());
      StencilUtils.dispose();

      if (hasTarget && target.hurtTime > this.lastHurtTime) {
         this.scaleAnim.setCurrentValue(0.7f);
         this.scaleAnim.animate(1.0f, 1.5f);
      }
      if (hasTarget) {
         this.lastHurtTime = target.hurtTime;
      }
      this.scaleAnim.tick();

      float scaleValue = this.scaleAnim.getValueF();
      float minScale = Math.max(0.7f, fade);
      float combinedScale = scaleValue * minScale;
      float headSize = headBoxSize * combinedScale;
      float headX = x + 4.0f + (headBoxSize - headSize) / 2.0f;
      float headY = y + (panelHeight - headSize) / 2.0f;

      if (this.currentTarget != null) {
         drawPlayerHead(e.getGuiGraphics(), poseStack, this.currentTarget, headX, headY, headSize, fade);
      }

      float slideOff = this.slideAnim.getValueF();
      String displayName = this.currentTarget != null
            ? (this.currentTarget == mc.player
                  ? NameProtect.getName(mc.player.getName().getString())
                  : this.currentTarget.getName().getString())
            : "";
      Fonts.harmony.render(poseStack, displayName,
            contentX, nameY + 1.0f + slideOff,
            new Color(255, 255, 255, (int) (255.0f * fade)), true, 0.45f);

      RenderUtils.drawRoundedRect(poseStack, contentX, healthY, healthW, healthH, 3.0f,
            new Color(0, 0, 0, (int) (COLOR_HEALTH_BG.getAlpha() * fade)).getRGB());

      float lagWidth = this.healthLagAnim.getValueF() * healthW;
      RenderUtils.drawRoundedRect(poseStack, contentX, healthY, lagWidth, healthH, 3.0f,
            new Color(COLOR_HEALTH_LAG.getRed(), COLOR_HEALTH_LAG.getGreen(),
                  COLOR_HEALTH_LAG.getBlue(), (int) (COLOR_HEALTH_LAG.getAlpha() * fade)).getRGB());

      float contentVal = this.contentAnim.getValueF();
      float barWidth = this.healthAnim.getValueF() * healthW * contentVal;
      Color barColor1 = new Color(COLOR_HEALTH_BAR.getRed(), COLOR_HEALTH_BAR.getGreen(),
            COLOR_HEALTH_BAR.getBlue(), (int) (255.0f * fade));

      RenderUtils.drawRoundedRect(poseStack, contentX, healthY, barWidth, healthH, 3.0f, barColor1.getRGB());

      if (this.currentTarget != null) {
         this.equipmentSlots[0] = this.currentTarget.getItemBySlot(EquipmentSlot.HEAD);
         this.equipmentSlots[1] = this.currentTarget.getItemBySlot(EquipmentSlot.CHEST);
         this.equipmentSlots[2] = this.currentTarget.getItemBySlot(EquipmentSlot.LEGS);
         this.equipmentSlots[3] = this.currentTarget.getItemBySlot(EquipmentSlot.FEET);
      }

      float itemX = contentX;
      float itemScale = 0.8f;
      float itemSize = 16.0f * itemScale;
      float itemGap = 2.0f;

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      for (ItemStack itemStack : this.equipmentSlots) {
         if (itemStack != null && !itemStack.isEmpty()) {
            PoseStack itemPose = new PoseStack();
            itemPose.pushPose();
            itemPose.translate(itemX, equipY, 0.0f);
            itemPose.scale(itemScale, itemScale, 1.0f);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, fade);
            e.getGuiGraphics().renderItem(itemStack, 0, 0);
            itemPose.popPose();
         }
         itemX += itemSize + itemGap;
      }
      RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
      RenderSystem.disableBlend();

      this.hudRenderedX = x;
      this.hudRenderedY = y;
      this.hudRenderedWidth = panelWidth;
      this.hudRenderedHeight = panelHeight;

      poseStack.popPose();
   }

   private void drawPlayerHead(GuiGraphics graphics, PoseStack poseStack, LivingEntity entity, float x, float y, float size, float alpha) {
      boolean isHit = entity.hurtTime > 0;
      int bgColor = isHit ? new Color(200, 50, 50, 200).getRGB() : new Color(50, 50, 50, 200).getRGB();
      RenderUtils.drawRoundedRect(poseStack, x, y, size, size, size * 0.17f, bgColor);

      ResourceLocation skinTexture = getEntitySkinTexture(entity);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

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
         if (entity instanceof Player) {
            graphics.blit(
                  skinTexture,
                  (int) x,
                  (int) y,
                  (int) size,
                  (int) size,
                  40, 8, 8, 8,
                  64, 64
            );
         }
      } catch (Exception ex) {
         RenderUtils.drawRoundedRect(poseStack, x, y, size, size, size * 0.17f, new Color(150, 150, 150, 255).getRGB());
      }

      RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
      RenderSystem.disableBlend();
   }

   private ResourceLocation getEntitySkinTexture(LivingEntity entity) {
      if (entity instanceof AbstractClientPlayer) {
         return ((AbstractClientPlayer) entity).getSkinTextureLocation();
      } else if (entity instanceof Player player) {
         PlayerInfo playerInfo = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(player.getUUID()) : null;
         if (playerInfo != null) {
            return playerInfo.getSkinLocation();
         }
         return DefaultPlayerSkin.getDefaultSkin(player.getUUID());
      } else {
         return mc.player != null ? mc.player.getSkinTextureLocation() : DefaultPlayerSkin.getDefaultSkin();
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
            x = screenWidth - width - margin + xo;
            y = screenHeight - height - margin + yo;
            break;
         case "Crosshair":
         default:
            x = screenWidth / 2.0f + 10.0f + xo;
            y = screenHeight / 2.0f + 10.0f + yo;
            break;
      }

      return new float[] { x, y };
   }
}