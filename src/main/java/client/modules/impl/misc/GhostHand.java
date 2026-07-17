package client.modules.impl.misc;

import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;

@ModuleInfo(
   name = "GhostHand",
   description = "Allows you to interact with chests through blocks",
   category = Category.MISC
)
public class GhostHand extends Module {
   public static boolean enabled = false;

   @Override
   public void onEnable() {
      enabled = true;
   }

   @Override
   public void onDisable() {
      enabled = false;
   }
}