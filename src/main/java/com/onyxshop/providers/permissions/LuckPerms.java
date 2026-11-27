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
   public void setup(OnyxShop plugin) throws PermissionsHookException {
      this.plugin = plugin;
      RegisteredServiceProvider registeredServiceProvider = Bukkit.getServicesManager().getRegistration(net.luckperms.api.LuckPerms.class);
      if (registeredServiceProvider != null) {
         this.api = (net.luckperms.api.LuckPerms)registeredServiceProvider.getProvider();
      } else {
         throw new PermissionsHookException("Failed to find LuckPerms provider from bukkit API");
      }
   }

   @Override
   public String getName() {
      return "LuckPerms";
   }

   @Override
   public String getGroupName(String name) {
      for (Group group : this.api.getGroupManager().getLoadedGroups()) {
         if (!group.getNodes().stream().noneMatch(entry -> entry.getKey().equalsIgnoreCase(name) && entry.getValue())) {
            return group.getDisplayName() == null ? group.getName() : ChatUtil.formatColors(group.getDisplayName());
         }
      }

      return null;
   }
}
