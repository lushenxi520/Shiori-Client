package client.commands.impl;

import client.Shiori;
import client.commands.Command;
import client.commands.CommandInfo;
import client.exceptions.NoSuchModuleException;
import client.modules.Module;
import client.utils.ChatUtils;

@CommandInfo(
   name = "toggle",
   description = "Toggle a module",
   aliases = {"t"}
)
public class CommandToggle extends Command {
   @Override
   public void onCommand(String[] args) {
      if (args.length == 1) {
         String moduleName = args[0];

         try {
            Module module = Shiori.getInstance().getModuleManager().getModule(moduleName);
            if (module != null) {
               module.toggle();
            } else {
               ChatUtils.addChatMessage("Invalid module.");
            }
         } catch (NoSuchModuleException var4) {
            ChatUtils.addChatMessage("Invalid module.");
         }
      }
   }

   @Override
   public String[] onTab(String[] args) {
      return Shiori.getInstance()
         .getModuleManager()
         .getModules()
         .stream()
         .map(Module::getName)
         .filter(name -> name.toLowerCase().startsWith(args.length == 0 ? "" : args[0].toLowerCase()))
         .toArray(String[]::new);
   }
}
