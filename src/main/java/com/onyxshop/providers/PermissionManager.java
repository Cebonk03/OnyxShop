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

   public PermissionManager(OnyxShop var1) {
      this.plugin = var1;
      var1.runTaskLater(this::init, 1L);
   }

   public PermissionsProvider getProvider() {
      return this.provider;
   }

   private void init() {
      for (String var2 : this.supported) {
         if (this.isEnabled(var2)) {
            try {
               PermissionsProvider var3 = this.getProvider(var2);
               var3.setup(this.plugin);
               this.provider = var3;
               break;
            } catch (PermissionsHookException var4) {
            }
         }
      }

      if (this.provider == null) {
         SendMessage.logDebugMessage(Lang.CANNOT_HOOK_PERMISSIONS.get());
      }
   }

   private PermissionsProvider getProvider(String var1) {
      switch (var1) {
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

   private boolean isEnabled(String var1) {
      return this.plugin.getServer().getPluginManager().isPluginEnabled(var1);
   }
}
