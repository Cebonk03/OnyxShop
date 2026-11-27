package com.onyxshop.commands;

import java.util.Collections;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.base.PluginCommands;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class SellGUI extends PluginCommands {
   public SellGUI(OnyxShop plugin, List<String> shopIds, List<String> values) {
      super(plugin, (String)shopIds.get(0), "Opens the sell GUI", "/" + (String)shopIds.remove(0), "OnyxShop.sellgui", shopIds, values);
   }

   @Override
   public boolean execute(CommandSender sender, String command, String[] values) {
      if (!PermissionsCache.hasPermission(sender, "OnyxShop.sellgui")) {
         SendMessage.sendMessage(sender, Lang.NO_PERMISSIONS.get());
         return true;
      } else if (ConfigManager.badYaml != null) {
         SendMessage.sendMessage(sender, ChatColor.DARK_RED + "This command cannot be executed now, please fix the configuration formatting first!");
         return true;
      } else {
         if (sender instanceof Player) {
            Player player = (Player)sender;
            if (!this.canUse(player)) {
               return true;
            }

            new com.onyxshop.objects.SellGUI().open(player);
         } else if ((sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender) && values.length > 0) {
            Player player = this.plugin.getServer().getPlayer(values[0]);
            if (player != null) {
               new com.onyxshop.objects.SellGUI().open(player);
            } else {
               SendMessage.sendMessage(sender, Lang.PLAYER_NOT_ONLINE.get());
            }
         }

         return true;
      }
   }

   @Override
   public List<String> tabComplete(CommandSender sender, String command, String[] values) {
      return Collections.emptyList();
   }
}
