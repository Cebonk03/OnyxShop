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
   public SellGUI(OnyxShop var1, List<String> var2, List<String> var3) {
      super(var1, (String)var2.get(0), "Opens the sell GUI", "/" + (String)var2.remove(0), "OnyxShop.sellgui", var2, var3);
   }

   @Override
   public boolean execute(CommandSender var1, String var2, String[] var3) {
      if (!PermissionsCache.hasPermission(var1, "OnyxShop.sellgui")) {
         SendMessage.sendMessage(var1, Lang.NO_PERMISSIONS.get());
         return true;
      } else if (ConfigManager.badYaml != null) {
         SendMessage.sendMessage(var1, ChatColor.DARK_RED + "This command cannot be executed now, please fix the configuration formatting first!");
         return true;
      } else {
         if (var1 instanceof Player) {
            Player var4 = (Player)var1;
            if (!this.canUse(var4)) {
               return true;
            }

            new com.onyxshop.objects.SellGUI().open(var4);
         } else if ((var1 instanceof ConsoleCommandSender || var1 instanceof BlockCommandSender) && var3.length > 0) {
            Player var5 = this.plugin.getServer().getPlayer(var3[0]);
            if (var5 != null) {
               new com.onyxshop.objects.SellGUI().open(var5);
            } else {
               SendMessage.sendMessage(var1, Lang.PLAYER_NOT_ONLINE.get());
            }
         }

         return true;
      }
   }

   @Override
   public List<String> tabComplete(CommandSender var1, String var2, String[] var3) {
      return Collections.emptyList();
   }
}
