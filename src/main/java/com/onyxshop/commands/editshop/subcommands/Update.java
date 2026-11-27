package com.onyxshop.commands.editshop.subcommands;

import java.util.List;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.methodes.UpdateHandler;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Update extends SubCommad {
   public Update() {
   }

   @Override
   public String getName() {
      return "update";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_UPDATE_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_UPDATE_SUBCOMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (value instanceof Player) {
         if (!ConfigManager.getConfig().getBoolean("update-checker.enabled", false)
            || ConfigManager.getConfig().getString("update-checker.endpoint", "").isEmpty()) {
            SendMessage.errorMessage(value, Lang.UPDATE_DISABLED.get());
            return;
         }

         UpdateHandler.checkForUpdate((Player)value);
      }
   }

   @Override
   public List<String> getTabCompletion(String[] values) {
      return null;
   }
}
