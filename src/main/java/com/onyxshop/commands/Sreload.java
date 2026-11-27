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
   public Sreload(OnyxShop plugin, List<String> shopIds) {
      super(plugin, "sreload", "Reloads the plugin", "/sreload", "OnyxShop.reload", shopIds);
   }

   @Override
   public boolean execute(CommandSender sender, String command, String[] values) {
      if (!PermissionsCache.hasPermission(sender, "OnyxShop.reload")) {
         SendMessage.sendMessage(sender, Lang.NO_PERMISSIONS.get());
         return true;
      } else if (sender instanceof Player) {
         Player player = (Player)sender;
         if (this.hasAccessInWorld(player)) {
            long startTime = System.currentTimeMillis();
            this.plugin.getConfigManager().reload();
            Lang.CONFIGS_RELOADED.reload();
            this.plugin.getMetaUtils().update();
            this.plugin.getConfigManager().validate();
            if (ConfigManager.badYaml == null) {
               this.plugin.startupReload.setupPluginVersion();
               if (!this.plugin.getEcoHandler().relead()) {
                  SendMessage.chatToPlayer(player, Lang.ERROR_OCCURRED_WHILE_RELOADING.get(player));
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
               SendMessage.infoMessage(player, Lang.DONE.get(player).replace("%millis%", String.valueOf(System.currentTimeMillis() - startTime)));
            } else {
               SendMessage.chatToPlayer(player, Lang.ERROR_OCCURRED_WHILE_RELOADING.get(player));
               SendMessage.errorMessage("Skipping all settings and item loading because the configuration files could not be loaded...");
            }
         }
      } else if (sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender || sender instanceof RemoteConsoleCommandSender) {
         long startTime = System.currentTimeMillis();
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
            SendMessage.infoMessage(Lang.DONE.get().replace("%millis%", String.valueOf(System.currentTimeMillis() - startTime)));
         } else {
            SendMessage.errorMessage("Skipping all settings and item loading because the configuration files could not be loaded...");
         }
      }

      return true;
   }

   @Override
   public List<String> tabComplete(CommandSender sender, String command, String[] values) {
      return Collections.emptyList();
   }
}
