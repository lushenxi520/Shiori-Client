package client.modules.impl.misc;

import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventHandlePacket;
import client.events.impl.EventMotion;
import client.events.impl.EventRespawn;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.utils.ChatUtils;
import client.utils.TimeHelper;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

@ModuleInfo(
   name = "Helper",
   description = "Bed wars Info helper",
   category = Category.MISC
)
public class Helper extends Module {
   TimeHelper diamond = new TimeHelper();
   TimeHelper emerald = new TimeHelper();
   private int dia;
   private int eme = 1;
   private boolean startgame = false;

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (this.diamond.delay(30000.0) && this.startgame) {
            this.dia++;
            ChatUtils.addChatMessage("第" + this.dia + "波钻石刷新");
            this.diamond.reset();
         }

         if (this.emerald.delay(60000.0) && this.startgame) {
            this.eme++;
            ChatUtils.addChatMessage("第" + this.eme + "波绿宝石刷新");
            this.emerald.reset();
         }
      }
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      this.dia = 0;
      this.eme = 0;
   }

   @EventTarget(0)
   public void onPacket(EventHandlePacket e) {
      try {
         if (mc.player != null && !e.isCancelled() && e.getPacket() instanceof ClientboundSystemChatPacket) {
            ClientboundSystemChatPacket s02 = (ClientboundSystemChatPacket)e.getPacket();
            String words = s02.content().getString();
            if (words.contains("游戏准备开始")) {
               this.diamond.reset();
               this.emerald.reset();
               this.startgame = true;
            }

            if (words.contains("游戏结束")) {
               this.startgame = false;
            }
         }
      } catch (Exception var4) {
         var4.printStackTrace();
      }
   }
}
