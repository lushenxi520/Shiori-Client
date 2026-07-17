package client.modules.impl.move;

import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventMotion;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;

@ModuleInfo(
   name = "Sprint",
   description = "Automatically sprints",
   category = Category.MOVEMENT
)
public class Sprint extends Module {
   @EventTarget(0)
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         mc.options.keySprint.setDown(true);
         mc.options.toggleSprint().set(false);
      }
   }

   @Override
   public void onDisable() {
      mc.options.keySprint.setDown(false);
   }
}
