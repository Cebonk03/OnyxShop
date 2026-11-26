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
   public void setup(OnyxShop var1) {
      this.api = UltraPermissions.getAPI();
   }

   @Override
   public String getName() {
      return "UltraPermissions";
   }

   @Override
   public String getGroupName(String var1) {
      for (Group var3 : this.api.getGroups()) {
         if (var3.getPermissions().stream().anyMatch(var1x -> var1x.getKey().equalsIgnoreCase(var1))) {
            return var3.getName();
         }
      }

      return null;
   }
}
