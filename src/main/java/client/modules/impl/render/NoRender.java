package client.modules.impl.render;

import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.values.ValueBuilder;
import client.values.impl.BooleanValue;

@ModuleInfo(
   name = "NoRender",
   description = "Disables rendering",
   category = Category.RENDER
)
public class NoRender extends Module {
   public BooleanValue disableEffects = ValueBuilder.create(this, "Disable Effects").setDefaultBooleanValue(true).build().getBooleanValue();
}
