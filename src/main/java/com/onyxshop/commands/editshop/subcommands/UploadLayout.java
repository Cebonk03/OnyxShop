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
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (value instanceof Player) {
         new MarketplaceIntegration().startUpload((Player)value);
      }
   }

   @Override
   public List<String> getTabCompletion(String[] values) {
      return null;
   }
}
