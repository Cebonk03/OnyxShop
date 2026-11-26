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

   public WorldGuardIntegration(OnyxShop var1) {
      this.plugin = var1;
   }

   @Override
   public void init(OnyxShop var1, RegionManager var2) {
      if (!this.isReady()) {
         SendMessage.infoMessage(this.getName() + " found, waiting...");
         var1.runTaskLater(() -> {
            if (var1.getServer().getPluginManager().isPluginEnabled(this.getName())) {
               SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getName()));
            } else {
               SendMessage.warnMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", this.getName()));
            }

            var2.onLoad();
         }, 5L);
      } else {
         SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getName()));
      }
   }

   @Override
   public boolean isReady() {
      RegionContainer var1 = WorldGuard.getInstance().getPlatform().getRegionContainer();

      for (World var3 : this.plugin.getServer().getWorlds()) {
         com.sk89q.worldguard.protection.managers.RegionManager var4 = var1.get(BukkitAdapter.adapt(var3));
         if (var4 != null) {
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
   public boolean isLoaded(String var1) {
      RegionContainer var2 = WorldGuard.getInstance().getPlatform().getRegionContainer();

      for (World var4 : this.plugin.getServer().getWorlds()) {
         com.sk89q.worldguard.protection.managers.RegionManager var5 = var2.get(BukkitAdapter.adapt(var4));
         if (var5 != null && var5.hasRegion(var1)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public String getRegionName(String var1) {
      return var1;
   }

   @Override
   public boolean isInsideRegion(Player var1, String var2) {
      Location var3 = BukkitAdapter.adapt(var1.getLocation());
      RegionContainer var4 = WorldGuard.getInstance().getPlatform().getRegionContainer();
      com.sk89q.worldguard.protection.managers.RegionManager var5 = var4.get(BukkitAdapter.adapt(var1.getWorld()));
      if (var5 == null) {
         return false;
      } else {
         ApplicableRegionSet var6 = var5.getApplicableRegions(var3.toVector().toBlockPoint());

         for (ProtectedRegion var8 : var6.getRegions()) {
            if (var8.getId().equalsIgnoreCase(var2)) {
               return true;
            }
         }

         return false;
      }
   }
}
