package client.modules.impl.render;

import client.events.api.EventTarget;
import client.events.impl.EventRender2D;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.utils.ChatUtils;
import client.utils.renderer.BlurUtils;
import client.utils.renderer.ShadowUtils;
import client.values.ValueBuilder;
import client.values.impl.BooleanValue;
import client.values.impl.FloatValue;
import org.lwjgl.opengl.GL11;

@ModuleInfo(
   name = "PostProcess",
   description = "Post process effects",
   category = Category.RENDER
)
public class PostProcess extends Module {
   private boolean disableBlur = false;
   private boolean disableBloom = false;
   private final BooleanValue blur = ValueBuilder.create(this, "Blur").setDefaultBooleanValue(true).build().getBooleanValue();
   private final FloatValue blurFPS = ValueBuilder.create(this, "Blur FPS")
      .setVisibility(this.blur::getCurrentValue)
      .setFloatStep(1.0F)
      .setDefaultFloatValue(90.0F)
      .setMinFloatValue(15.0F)
      .setMaxFloatValue(120.0F)
      .build()
      .getFloatValue();
   private final FloatValue strength = ValueBuilder.create(this, "Blur Strength")
      .setVisibility(this.blur::getCurrentValue)
      .setDefaultFloatValue(2.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(19.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   private final BooleanValue bloom = ValueBuilder.create(this, "Bloom").setDefaultBooleanValue(true).build().getBooleanValue();
   private final FloatValue bloomFPS = ValueBuilder.create(this, "Bloom FPS")
      .setVisibility(this.bloom::getCurrentValue)
      .setFloatStep(1.0F)
      .setDefaultFloatValue(90.0F)
      .setMinFloatValue(15.0F)
      .setMaxFloatValue(120.0F)
      .build()
      .getFloatValue();

   @EventTarget(0)
   public void onRender(EventRender2D e) {
      if (this.blur.getCurrentValue() && !this.disableBlur) {
         GL11.glGetError();
         BlurUtils.onRenderAfterWorld(e, this.blurFPS.getCurrentValue(), (int)this.strength.getCurrentValue());
         if (GL11.glGetError() != 0) {
            ChatUtils.addChatMessage("Disabling blur due to error.");
            this.disableBlur = true;
         }
      }

      if (this.bloom.getCurrentValue() && !this.disableBloom) {
         GL11.glGetError();
         ShadowUtils.onRenderAfterWorld(e, this.bloomFPS.getCurrentValue());
         if (GL11.glGetError() != 0) {
            ChatUtils.addChatMessage("Disabling bloom due to error.");
            this.disableBloom = true;
         }
      }
   }
}
