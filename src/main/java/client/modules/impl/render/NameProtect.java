package client.modules.impl.render;

import client.events.api.EventTarget;
import client.events.impl.EventRenderTabOverlay;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

@ModuleInfo(
   name = "NameProtect",
   description = "Protect your name",
   category = Category.RENDER
)
public class NameProtect extends Module {
   public static NameProtect instance;

   public NameProtect() {
      instance = this;
   }

   public static String getName(String string) {
      if (!instance.isEnabled() || mc.player == null) {
         return string;
      } else {
         return string.contains(mc.player.getName().getString()) ? StringUtils.replace(string, mc.player.getName().getString(), "§dShiori§7") : string;
      }
   }

   @EventTarget
   public void onRenderTab(EventRenderTabOverlay e) {
      e.setComponent(Component.literal(getName(e.getComponent().getString())));
   }
}
