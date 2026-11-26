package com.onyxshop.commands;

import java.util.Collections;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.base.PluginCommand;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

public class Sreload extends PluginCommand {
   public Sreload(OnyxShop var1, List<String> var2) {
      super(var1, "sreload", "Reloads the plugin", "/sreload", "OnyxShop.reload", var2);
   }

   @Override
   public boolean execute(CommandSender var1, String var2, String[] var3) {
      if (!PermissionsCache.hasPermission(var1, "OnyxShop.reload")) {
         SendMessage.sendMessage(var1, Lang.NO_PERMISSIONS.get());
         return true;
      } else if (var1 instanceof Player) {
         Player var4 = (Player)var1;
         if (this.hasAccessInWorld(var4)) {
            long var5 = System.currentTimeMillis();
            this.plugin.getConfigManager().reload();
            Lang.CONFIGS_RELOADED.reload();
            this.plugin.getMetaUtils().update();
            this.plugin.getConfigManager().validate();
            if (ConfigManager.badYaml == null) {
               this.plugin.startupReload.setupPluginVersion();
               if (!this.plugin.getEcoHandler().relead()) {
                  SendMessage.chatToPlayer(var4, Lang.ERROR_OCCURRED_WHILE_RELOADING.get(var4));
                  return true;
               }

               SendMessage.init();
               this.plugin.updateClickMappings();
               this.plugin.getSpawnerManager().init();
               this.plugin.getSpawnerManager().initDelayed();
               if (this.plugin.getBedrockInventorys() != null) {
                  this.plugin.getBedrockInventorys().reload();
               }

               this.plugin.startupReload.checkDebugMode();
               this.plugin.startupReload.loadInventoryTitles();
               this.plugin.getProviderManager().reloadProviders();
               this.plugin.startupReload.loadItems(true);
               this.plugin.getSoundEffects().reload();
               this.plugin.reloadModifiers();
               this.plugin.reloadPlayerData();
               this.plugin.startupReload.updateAvailable();
               SendMessage.infoMessage(var4, Lang.DONE.get(var4).replace("%millis%", String.valueOf(System.currentTimeMillis() - var5)));
            } else {
               SendMessage.chatToPlayer(var4, Lang.ERROR_OCCURRED_WHILE_RELOADING.get(var4));
               SendMessage.errorMessage("Skipping all settings and item loading because the configuration files could not be loaded...");
            }
         }
      } else if (var1 instanceof ConsoleCommandSender || var1 instanceof BlockCommandSender || var1 instanceof RemoteConsoleCommandSender) {
         long var7 = System.currentTimeMillis();
         this.plugin.getConfigManager().reload();
         Lang.CONFIGS_RELOADED.reload();
         this.plugin.getMetaUtils().update();
         this.plugin.getConfigManager().validate();
         if (ConfigManager.badYaml == null) {
            this.plugin.startupReload.setupPluginVersion();
            if (!this.plugin.getEcoHandler().relead()) {
               return true;
            }

            SendMessage.init();
            this.plugin.updateClickMappings();
            this.plugin.getSpawnerManager().init();
            this.plugin.getSpawnerManager().initDelayed();
            if (this.plugin.getBedrockInventorys() != null) {
               this.plugin.getBedrockInventorys().reload();
            }

            this.plugin.startupReload.checkDebugMode();
            this.plugin.startupReload.loadInventoryTitles();
            this.plugin.getProviderManager().reloadProviders();
            this.plugin.startupReload.loadItems(true);
            this.plugin.getSoundEffects().reload();
            this.plugin.reloadModifiers();
            this.plugin.reloadPlayerData();
            this.plugin.startupReload.updateAvailable();
            SendMessage.infoMessage(Lang.DONE.get().replace("%millis%", String.valueOf(System.currentTimeMillis() - var7)));
         } else {
            SendMessage.errorMessage("Skipping all settings and item loading because the configuration files could not be loaded...");
         }
      }

      return true;
   }

   @Override
   public List<String> tabComplete(CommandSender var1, String var2, String[] var3) {
      return Collections.emptyList();
   }
}
