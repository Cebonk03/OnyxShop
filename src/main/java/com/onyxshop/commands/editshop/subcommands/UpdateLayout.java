package com.onyxshop.commands.editshop.subcommands;

import java.util.List;
import java.util.UUID;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.methodes.MarketplaceIntegration;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UpdateLayout extends SubCommad {
   public UpdateLayout() {
   }

   @Override
   public String getName() {
      return "updateLayout";
   }

   @Override
   public String getDescription() {
      return "§aUpdate an existing shop layout.";
   }

   @Override
   public String getSyntax() {
      return "§a/editshop updateLayout <layoutID>";
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

            new MarketplaceIntegration().startUpdate((Player)value, uniqueId.toString());
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
