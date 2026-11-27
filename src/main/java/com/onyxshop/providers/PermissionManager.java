package com.onyxshop.providers;

import java.util.Arrays;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.permissions.LuckPerms;
import com.onyxshop.providers.permissions.UltraPerms;
import com.onyxshop.providers.permissions.VaultPerms;
import com.onyxshop.util.exceptions.PermissionsHookException;

public class PermissionManager {
   private final OnyxShop plugin;
   private final List<String> supported = Arrays.asList("LuckPerms", "UltraPermissions", "Vault");
   private PermissionsProvider provider;

   public PermissionManager(OnyxShop plugin) {
      this.plugin = plugin;
      plugin.runTaskLater(this::init, 1L);
   }

   public PermissionsProvider getProvider() {
      return this.provider;
   }

   private void init() {
      for (String text : this.supported) {
         if (this.isEnabled(text)) {
            try {
               PermissionsProvider permissionsProvider = this.getProvider(text);
               permissionsProvider.setup(this.plugin);
               this.provider = permissionsProvider;
               break;
            } catch (PermissionsHookException exception) {
            }
         }
      }

      if (this.provider == null) {
         SendMessage.logDebugMessage(Lang.CANNOT_HOOK_PERMISSIONS.get());
      }
   }

   private PermissionsProvider getProvider(String text) {
      switch (text) {
         case "LuckPerms":
            return new LuckPerms();
         case "UltraPermissions":
            return new UltraPerms();
         case "Vault":
            return new VaultPerms();
         default:
            return null;
      }
   }

   private boolean isEnabled(String text) {
      return this.plugin.getServer().getPluginManager().isPluginEnabled(text);
   }
}
