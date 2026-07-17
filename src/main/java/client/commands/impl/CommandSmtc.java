package client.commands.impl;

import client.commands.Command;
import client.commands.CommandInfo;
import client.utils.ChatUtils;
import client.utils.smtc.SmtcUtils;
import client.utils.smtc.SmtcUtils.SmtcInfo;

@CommandInfo(
   name = "smtc",
   description = "Get current Windows media playback info via SMTC",
   aliases = {"musicinfo", "nowplaying", "np"}
)
public class CommandSmtc extends Command {

   @Override
   public void onCommand(String[] args) {
      SmtcInfo info = SmtcUtils.getCurrentInfo();

      if (info == null || !info.isPlaying()) {
         ChatUtils.addChatMessage("§7No media is currently playing or SMTC not available");
         return;
      }

      ChatUtils.addChatMessage("§b=== Now Playing (SMTC) ===");
      ChatUtils.addChatMessage("§fTitle: §e" + info.title);
      if (info.duration > 0) {
         ChatUtils.addChatMessage("§fProgress: §a" + info.getFormattedPosition() + " §7/ §a" + info.getFormattedDuration());
         ChatUtils.addChatMessage("§f" + info.getProgressBar(20));
      }
      ChatUtils.addChatMessage("§fCover: §7" + (info.base64Cover.isEmpty() ? "None" : "Available (" + info.base64Cover.length() + " chars)"));
   }

   @Override
   public String[] onTab(String[] args) {
      return new String[0];
   }
}