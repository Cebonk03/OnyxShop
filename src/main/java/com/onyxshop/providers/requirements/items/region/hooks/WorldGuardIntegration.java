package com.onyxshop.providers.requirements.items.region.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.requirements.items.region.RegionManager;
import com.onyxshop.providers.requirements.items.region.RegionProvider;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WorldGuardIntegration implements RegionProvider {
   private final OnyxShop plugin;

   public WorldGuardIntegration(OnyxShop plugin) {
      this.plugin = plugin;
   }

   @Override
   public void init(OnyxShop plugin, RegionManager regionManager) {
      if (!this.isReady()) {
         SendMessage.infoMessage(this.getName() + " found, waiting...");
         plugin.runTaskLater(() -> {
            if (plugin.getServer().getPluginManager().isPluginEnabled(this.getName())) {
               SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getName()));
            } else {
               SendMessage.warnMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", this.getName()));
            }

            regionManager.onLoad();
         }, 5L);
      } else {
         SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getName()));
      }
   }

   @Override
   public boolean isReady() {
      RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();

      for (World world : this.plugin.getServer().getWorlds()) {
         com.sk89q.worldguard.protection.managers.RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
         if (regionManager != null) {
            return true;
         }
      }

      return false;
   }

   @Override
   public String getName() {
      return "WorldGuard";
   }

   @Override
   public boolean isLoaded(String text) {
      RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();

      for (World world : this.plugin.getServer().getWorlds()) {
         com.sk89q.worldguard.protection.managers.RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
         if (regionManager != null && regionManager.hasRegion(text)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public String getRegionName(String name) {
      return name;
   }

   @Override
   public boolean isInsideRegion(Player player, String regionId) {
      Location location = BukkitAdapter.adapt(player.getLocation());
      RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
      com.sk89q.worldguard.protection.managers.RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
      if (regionManager == null) {
         return false;
      } else {
         ApplicableRegionSet applicableRegionSet = regionManager.getApplicableRegions(location.toVector().toBlockPoint());

         for (ProtectedRegion protectedRegion : applicableRegionSet.getRegions()) {
            if (protectedRegion.getId().equalsIgnoreCase(regionId)) {
               return true;
            }
         }

         return false;
      }
   }
}
