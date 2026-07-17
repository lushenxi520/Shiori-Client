package client.modules.impl.render;

import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventMotion;
import client.events.impl.EventPacket;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.values.ValueBuilder;
import client.values.impl.FloatValue;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;

@ModuleInfo(
   name = "TimeChanger",
   description = "Change the time of the world",
   category = Category.RENDER
)
public class TimeChanger extends Module {
   FloatValue time = ValueBuilder.create(this, "World Time")
      .setDefaultFloatValue(8000.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(24000.0F)
      .build()
      .getFloatValue();

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         mc.level.setDayTime((long)this.time.getCurrentValue());
      }
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (event.getPacket() instanceof ClientboundSetTimePacket) {
         event.setCancelled(true);
      }
   }
}
