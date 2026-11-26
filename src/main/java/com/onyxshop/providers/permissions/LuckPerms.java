package com.onyxshop.providers.permissions;

import com.onyxshop.OnyxShop;
import com.onyxshop.providers.PermissionsProvider;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.exceptions.PermissionsHookException;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LuckPerms implements PermissionsProvider {
   private net.luckperms.api.LuckPerms api;
   private OnyxShop plugin;

   public LuckPerms() {
   }

   @Override
   public void setup(OnyxShop var1) throws PermissionsHookException {
      this.plugin = var1;
      RegisteredServiceProvider var2 = Bukkit.getServicesManager().getRegistration(net.luckperms.api.LuckPerms.class);
      if (var2 != null) {
         this.api = (net.luckperms.api.LuckPerms)var2.getProvider();
      } else {
         throw new PermissionsHookException("Failed to find LuckPerms provider from bukkit API");
      }
   }

   @Override
   public String getName() {
      return "LuckPerms";
   }

   @Override
   public String getGroupName(String var1) {
      for (Group var3 : this.api.getGroupManager().getLoadedGroups()) {
         if (!var3.getNodes().stream().noneMatch(var1x -> var1x.getKey().equalsIgnoreCase(var1) && var1x.getValue())) {
            return var3.getDisplayName() == null ? var3.getName() : ChatUtil.formatColors(var3.getDisplayName());
         }
      }

      return null;
   }
}
