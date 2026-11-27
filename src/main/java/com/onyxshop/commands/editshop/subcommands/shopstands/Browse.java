package com.onyxshop.commands.editshop.subcommands.shopstands;

import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.Lang;
import com.onyxshop.objects.inventorys.StandBrowser;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Browse extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;

   public Browse(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
   }

   @Override
   public String getName() {
      return "browse";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_SHOPSTANDS_BROWSE_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_SHOPSTANDS_BROWSE_SUBCOMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop.shopstands." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (value instanceof Player) {
         new StandBrowser(this.plugin, (Player)value);
      }
   }

   @Override
   public List<String> getTabCompletion(String[] values) {
      return null;
   }
}
