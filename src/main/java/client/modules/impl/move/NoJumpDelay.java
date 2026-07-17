package client.modules.impl.move;

import inject.mixin.O.accessors.LivingEntityAccessor;
import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventMotion;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;

@ModuleInfo(
   name = "NoJumpDelay",
   description = "Removes the delay when jumping",
   category = Category.MOVEMENT
)
public class NoJumpDelay extends Module {
   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         ((LivingEntityAccessor)mc.player).setNoJumpDelay(0);
      }
   }
}
