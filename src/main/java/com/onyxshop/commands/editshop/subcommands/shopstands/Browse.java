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

   public Browse(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
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
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop.shopstands." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var1 instanceof Player) {
         new StandBrowser(this.plugin, (Player)var1);
      }
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      return null;
   }
}
