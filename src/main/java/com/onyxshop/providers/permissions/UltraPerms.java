package com.onyxshop.providers.permissions;

import me.TechsCode.UltraPermissions.UltraPermissions;
import me.TechsCode.UltraPermissions.UltraPermissionsAPI;
import me.TechsCode.UltraPermissions.storage.objects.Group;
import com.onyxshop.OnyxShop;
import com.onyxshop.providers.PermissionsProvider;

public class UltraPerms implements PermissionsProvider {
   private UltraPermissionsAPI api;

   public UltraPerms() {
   }

   @Override
   public void setup(OnyxShop plugin) {
      this.api = UltraPermissions.getAPI();
   }

   @Override
   public String getName() {
      return "UltraPermissions";
   }

   @Override
   public String getGroupName(String name) {
      for (Group group : this.api.getGroups()) {
         if (group.getPermissions().stream().anyMatch(entry -> entry.getKey().equalsIgnoreCase(name))) {
            return group.getName();
         }
      }

      return null;
   }
}
