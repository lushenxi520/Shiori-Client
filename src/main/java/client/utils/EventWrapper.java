package client.utils;

import client.Shiori;
import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventClientChat;
import client.events.impl.EventMotion;
import client.events.impl.EventRespawn;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.RenderGuiEvent.Post;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventWrapper {
   @SubscribeEvent
   public void onRender(Post e) {
   }

   @SubscribeEvent
   public void onClientChat(ClientChatEvent e) {
      EventClientChat event = new EventClientChat(e.getMessage());
      Shiori.getInstance().getEventManager().call(event);
      if (event.isCancelled()) {
         e.setCanceled(true);
      }
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE && Minecraft.getInstance().player.tickCount <= 1) {
         Shiori.getInstance().getEventManager().call(new EventRespawn());
      }
   }
}
