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
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var1 instanceof Player) {
         if (var2.length > 1) {
            UUID var3;
            try {
               var3 = UUID.fromString(var2[1]);
            } catch (IllegalArgumentException var5) {
               SendMessage.sendMessage(var1, ChatColor.RED + "Invalid layout ID for " + var2[1]);
               return;
            }

            new MarketplaceIntegration().downloadLayout((Player)var1, var3.toString(), Arrays.asList(var2).contains("-unsafe"));
         } else {
            SendMessage.sendMessage(var1, this.getSyntax());
         }
      }
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      return null;
   }
}
