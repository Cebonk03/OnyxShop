package com.onyxshop.providers.requirements.items.region;

import java.util.HashMap;
import java.util.Map;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.requirements.items.region.hooks.WorldGuardIntegration;
import com.onyxshop.util.exceptions.ItemRequirementLoadException;
import org.bukkit.plugin.PluginManager;

public class RegionManager {
   private final RegionProvider provider;
   private final Map<RegionRequirement, String> requirements = new HashMap<>();

   public RegionManager(OnyxShop plugin) {
      PluginManager pluginManager = plugin.getServer().getPluginManager();
      this.provider = this.getProvider(plugin, pluginManager);
      if (this.provider != null) {
         this.provider.init(plugin, this);
      }
   }

   private RegionProvider getProvider(OnyxShop plugin, PluginManager pluginManager) {
      return pluginManager.getPlugin("WorldGuard") != null ? new WorldGuardIntegration(plugin) : null;
   }

   public RegionRequirement addRequirement(RegionRequirement regionRequirement, String text) {
      this.requirements.put(regionRequirement, text);
      return regionRequirement;
   }

   public RegionProvider getProvider() throws ItemRequirementLoadException {
      if (this.provider != null) {
         return this.provider;
      } else {
         throw new ItemRequirementLoadException("Failed to enable integration with supported region plugin");
      }
   }

   public void onLoad() {
      if (!this.provider.isReady()) {
         SendMessage.errorMessage("Failed to enable region integration with '" + this.provider.getName() + "'");
      }

      for (RegionRequirement regionRequirement : this.requirements.keySet()) {
         String text = this.requirements.get(regionRequirement);
         if (!this.provider.isReady()) {
            SendMessage.warnMessage("Failed to load region item requirement: " + this.provider.getName() + " integration failed to load");
            SendMessage.errorShops(text.split("\\.")[0], text.split("\\.", 2)[1]);
         } else {
            if (!this.provider.isLoaded(regionRequirement.getRegionID())) {
               SendMessage.warnMessage(
                  "Issue while loading region item requirement: Region using ID '"
                     + regionRequirement.getRegionID()
                     + "' was not found in "
                     + this.provider.getName()
                     + ", is this supposed to be a region which hasn't been created yet? Else the name of the region is incorrect, continuing anyway..."
               );
               SendMessage.errorShops(text.split("\\.")[0], text.split("\\.", 2)[1]);
            }

            regionRequirement.onLoad();
         }
      }
   }
}
