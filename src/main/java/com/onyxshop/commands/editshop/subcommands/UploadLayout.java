package com.onyxshop.commands.editshop.subcommands;

import java.util.List;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.methodes.MarketplaceIntegration;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UploadLayout extends SubCommad {
   public UploadLayout() {
   }

   @Override
   public String getName() {
      return "uploadLayout";
   }

   @Override
   public String getDescription() {
      return "§aUpload your current shop layout.";
   }

   @Override
   public String getSyntax() {
      return "§a/editshop uploadLayout";
   }

   @Override
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var1 instanceof Player) {
         new MarketplaceIntegration().startUpload((Player)var1);
      }
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      return null;
   }
}
