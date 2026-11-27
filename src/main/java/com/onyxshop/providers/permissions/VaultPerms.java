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
   public void setup(OnyxShop plugin) throws PermissionsHookException {
      RegisteredServiceProvider registeredServiceProvider = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
      if (registeredServiceProvider != null) {
         this.api = (Permission)registeredServiceProvider.getProvider();
      } else {
         throw new PermissionsHookException("Failed to find Vault Permissions provider from bukkit API");
      }
   }

   @Override
   public String getName() {
      return this.api.getName();
   }

   @Override
   public String getGroupName(String name) {
      for (String text : this.api.getGroups()) {
         if (this.api.groupHas((String)null, text, name.toLowerCase(Locale.ENGLISH))) {
            return text;
         }
      }

      return null;
   }
}
