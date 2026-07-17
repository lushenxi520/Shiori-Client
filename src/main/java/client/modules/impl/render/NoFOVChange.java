package client.modules.impl.render;

import client.events.api.EventTarget;
import client.events.impl.EventUpdateFoV;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;

@ModuleInfo(
   name = "NoFOVChange",
   description = "Disables field of view changes",
   category = Category.RENDER
)
public class NoFOVChange extends Module {

   @EventTarget
   public void onFov(EventUpdateFoV event) {
      event.setFov(1.0F);
   }
}