package com.onyxshop.commands.editshop.subcommands;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.methodes.MarketplaceIntegration;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InstallLayout extends SubCommad {
   public InstallLayout() {
   }

   @Override
   public String getName() {
      return "installLayout";
   }

   @Override
   public String getDescription() {
      return "§aInstall a custom premade shop layout.";
   }

   @Override
   public String getSyntax() {
      return "§a/editshop installLayout <layoutID>";
   }

   @Override
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (value instanceof Player) {
         if (values.length > 1) {
            UUID uniqueId;
            try {
               uniqueId = UUID.fromString(values[1]);
            } catch (IllegalArgumentException exception) {
               SendMessage.sendMessage(value, ChatColor.RED + "Invalid layout ID for " + values[1]);
               return;
            }

            new MarketplaceIntegration().downloadLayout((Player)value, uniqueId.toString(), Arrays.asList(values).contains("-unsafe"));
         } else {
            SendMessage.sendMessage(value, this.getSyntax());
         }
      }
   }

   @Override
   public List<String> getTabCompletion(String[] values) {
      return null;
   }
}
