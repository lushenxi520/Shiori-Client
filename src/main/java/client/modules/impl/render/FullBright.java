package client.modules.impl.render;

import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.values.ValueBuilder;
import client.values.impl.FloatValue;

@ModuleInfo(
   name = "FullBright",
   description = "Make your world brighter.",
   category = Category.RENDER
)
public class FullBright extends Module {
   public FloatValue brightness = ValueBuilder.create(this, "Brightness")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(1.0F)
      .build()
      .getFloatValue();
}
