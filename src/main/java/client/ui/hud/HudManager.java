package client.ui.hud;

import client.Shiori;
import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventRunTicks;
import client.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

public class HudManager {

   @EventTarget
   public void onTick(EventRunTicks event) {
      if (event.getType() != EventType.PRE) return;

      Minecraft mc = Minecraft.getInstance();
      if (mc.screen != null && !(mc.screen instanceof ChatScreen)) {
         for (Module module : Shiori.getInstance().getModuleManager().getModules()) {
            if (module instanceof HudElement hud && module.isEnabled()) {
               if (hud.isHudDragging()) {
                  hud.stopDragging();
               }
            }
         }
      }
   }
}