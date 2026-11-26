package com.onyxshop.providers.permissions;

import java.util.Locale;
import com.onyxshop.OnyxShop;
import com.onyxshop.providers.PermissionsProvider;
import com.onyxshop.util.exceptions.PermissionsHookException;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultPerms implements PermissionsProvider {
   private Permission api;

   public VaultPerms() {
   }

   @Override
   public void setup(OnyxShop var1) throws PermissionsHookException {
      RegisteredServiceProvider var2 = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
      if (var2 != null) {
         this.api = (Permission)var2.getProvider();
      } else {
         throw new PermissionsHookException("Failed to find Vault Permissions provider from bukkit API");
      }
   }

   @Override
   public String getName() {
      return this.api.getName();
   }

   @Override
   public String getGroupName(String var1) {
      for (String var5 : this.api.getGroups()) {
         if (this.api.groupHas((String)null, var5, var1.toLowerCase(Locale.ENGLISH))) {
            return var5;
         }
      }

      return null;
   }
}
