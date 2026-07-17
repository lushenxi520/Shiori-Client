package client.modules.impl.render;

import client.Shiori;
import client.Version;
import client.events.api.EventTarget;
import client.events.impl.EventRender2D;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.modules.ModuleManager;
import client.ui.hud.HudElement;
import client.utils.RenderUtils;
import client.utils.SmoothAnimationTimer;
import client.utils.StencilUtils;
import client.utils.renderer.Fonts;
import client.utils.renderer.text.CustomTextRenderer;
import client.values.ValueBuilder;
import client.values.impl.BooleanValue;
import client.values.impl.FloatValue;
import client.values.impl.ModeValue;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;


@ModuleInfo(
   name = "HUD",
   description = "Displays information on your screen",
   category = Category.RENDER
)
public class HUD extends Module implements HudElement {
   public static final int headerColor = new Color(150, 45, 45, 255).getRGB();
   public static final int bodyColor = new Color(0, 0, 0, 120).getRGB();
   private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
   public BooleanValue moduleToggleSound = ValueBuilder.create(this, "Module Toggle Sound").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue notification = ValueBuilder.create(this, "Notification").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue arrayList = ValueBuilder.create(this, "Array List").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue prettyModuleName = ValueBuilder.create(this, "Pretty Module Name")
      .setOnUpdate(value -> Module.update = true)
      .setVisibility(this.arrayList::getCurrentValue)
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();
   public BooleanValue hideRenderModules = ValueBuilder.create(this, "Hide Render Modules")
      .setOnUpdate(value -> Module.update = true)
      .setVisibility(this.arrayList::getCurrentValue)
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();
   public BooleanValue rainbow = ValueBuilder.create(this, "Rainbow")
      .setDefaultBooleanValue(true)
      .setVisibility(this.arrayList::getCurrentValue)
      .build()
      .getBooleanValue();
   public FloatValue rainbowSpeed = ValueBuilder.create(this, "Rainbow Speed")
      .setVisibility(this.arrayList::getCurrentValue)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(20.0F)
      .setDefaultFloatValue(10.0F)
      .setFloatStep(0.1F)
      .build()
      .getFloatValue();
   public FloatValue rainbowOffset = ValueBuilder.create(this, "Rainbow Offset")
      .setVisibility(this.arrayList::getCurrentValue)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(20.0F)
      .setDefaultFloatValue(10.0F)
      .setFloatStep(0.1F)
      .build()
      .getFloatValue();
   public ModeValue arrayListDirection = ValueBuilder.create(this, "ArrayList Direction")
      .setVisibility(this.arrayList::getCurrentValue)
      .setDefaultModeIndex(0)
      .setModes("Right", "Left")
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
   public FloatValue arrayListSize = ValueBuilder.create(this, "ArrayList Size")
      .setVisibility(this.arrayList::getCurrentValue)
      .setDefaultFloatValue(0.4F)
      .setFloatStep(0.01F)
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(1.0F)
      .build()
      .getFloatValue();
   List<Module> renderModules;
   float width;

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
      float maxWidth = hudRenderedWidth;
      if (this.arrayListDirection.isCurrentMode("Right")) {
         this.xOffset.setCurrentValue(x - (mc.getWindow().getGuiScaledWidth() - maxWidth - 6.0F));
      } else {
         this.xOffset.setCurrentValue(x - 3.0F);
      }
   }

   @Override
   public void setHudY(float y) {
      this.yOffset.setCurrentValue(y);
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

   public String getModuleDisplayName(Module module) {
      String name = this.prettyModuleName.getCurrentValue() ? module.getPrettyName() : module.getName();
      return name + (module.getSuffix() == null ? "" : " §7" + module.getSuffix());
   }

   @EventTarget
   public void notification(EventRender2D e) {
      if (this.notification.getCurrentValue()) {
         Shiori.getInstance().getNotificationManager().onRender(e);
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      CustomTextRenderer font = Fonts.opensans;

      if (this.arrayList.getCurrentValue()) {
         e.getStack().pushPose();
         ModuleManager moduleManager = Shiori.getInstance().getModuleManager();
         if (update || this.renderModules == null) {
            this.renderModules = new ArrayList<>(moduleManager.getModules());
            if (this.hideRenderModules.getCurrentValue()) {
               this.renderModules.removeIf(modulex -> modulex.getCategory() == Category.RENDER);
            }

            this.renderModules.sort((o1, o2) -> {
               float o1Width = font.getWidth(this.getModuleDisplayName(o1), (double)this.arrayListSize.getCurrentValue());
               float o2Width = font.getWidth(this.getModuleDisplayName(o2), (double)this.arrayListSize.getCurrentValue());
               return Float.compare(o2Width, o1Width);
            });
         }

         float maxWidth = this.renderModules.isEmpty()
            ? 0.0F
            : font.getWidth(this.getModuleDisplayName(this.renderModules.get(0)), (double)this.arrayListSize.getCurrentValue());
         float arrayListX = this.arrayListDirection.isCurrentMode("Right")
            ? (float)mc.getWindow().getGuiScaledWidth() - maxWidth - 6.0F + this.xOffset.getCurrentValue()
            : 3.0F + this.xOffset.getCurrentValue();
         float arrayListY = this.yOffset.getCurrentValue();
         float height = 0.0F;
         double fontHeight = font.getHeight(true, (double)this.arrayListSize.getCurrentValue());

         for (Module module : this.renderModules) {
            SmoothAnimationTimer animation = module.getAnimation();
            if (module.isEnabled()) {
               animation.target = 100.0F;
            } else {
               animation.target = 0.0F;
            }

            animation.update(true);
            if (animation.value > 0.0F) {
               String displayName = this.getModuleDisplayName(module);
               float stringWidth = font.getWidth(displayName, (double)this.arrayListSize.getCurrentValue());
               float left = -stringWidth * (1.0F - animation.value / 100.0F);
               float right = maxWidth - stringWidth * (animation.value / 100.0F);
               float innerX = this.arrayListDirection.isCurrentMode("Left") ? left : right;
               
               int color = -1;
               if (this.rainbow.getCurrentValue()) {
                  color = RenderUtils.getRainbowOpaque(
                     (int)(-height * this.rainbowOffset.getCurrentValue()), 1.0F, 1.0F, (21.0F - this.rainbowSpeed.getCurrentValue()) * 1000.0F
                  );
               }

               float alpha = animation.value / 100.0F;
               font.setAlpha(alpha);
               font.render(
                  e.getStack(),
                  displayName,
                  (double)(arrayListX + innerX + 1.5F),
                  (double)(arrayListY + height + 1.0F),
                  new Color(color),
                  true,
                  (double)this.arrayListSize.getCurrentValue()
               );
               height += (float)((double)(animation.value / 100.0F) * fontHeight);
            }
         }

         font.setAlpha(1.0F);
         e.getStack().popPose();

         this.hudRenderedX = arrayListX;
         this.hudRenderedY = arrayListY;
         this.hudRenderedWidth = maxWidth + 6.0F;
         this.hudRenderedHeight = height;
      }
   }
}