package client.modules.impl.render;

import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventRenderScoreboard;
import client.events.impl.EventRenderTabOverlay;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@ModuleInfo(
   name = "ServerNameSpoof",
   description = "Spoof the server name",
   category = Category.RENDER
)
public class ScoreboardSpoof extends Module {
   @EventTarget
   public void onRenderScoreboard(EventRenderScoreboard e) {
      String string = e.getComponent().getString();
      if (string.contains("布吉岛")) {
         MutableComponent textComponent = Component.literal("§d§l我爱你");
         textComponent.setStyle(e.getComponent().getStyle());
         e.setComponent(textComponent);
      }
   }

   @EventTarget
   public void onRenderTab(EventRenderTabOverlay e) {
      String string = e.getComponent().getString();
      if (string.contains("布吉岛")) {
         if (e.getType() == EventType.HEADER) {
            e.setComponent(Component.literal("§d§l我爱你"));
         } else if (e.getType() == EventType.FOOTER) {
            e.setComponent(Component.literal(""));
         }
      }
   }
}
