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

   public RegionManager(OnyxShop var1) {
      PluginManager var2 = var1.getServer().getPluginManager();
      this.provider = this.getProvider(var1, var2);
      if (this.provider != null) {
         this.provider.init(var1, this);
      }
   }

   private RegionProvider getProvider(OnyxShop var1, PluginManager var2) {
      return var2.getPlugin("WorldGuard") != null ? new WorldGuardIntegration(var1) : null;
   }

   public RegionRequirement addRequirement(RegionRequirement var1, String var2) {
      this.requirements.put(var1, var2);
      return var1;
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

      for (RegionRequirement var2 : this.requirements.keySet()) {
         String var3 = this.requirements.get(var2);
         if (!this.provider.isReady()) {
            SendMessage.warnMessage("Failed to load region item requirement: " + this.provider.getName() + " integration failed to load");
            SendMessage.errorShops(var3.split("\\.")[0], var3.split("\\.", 2)[1]);
         } else {
            if (!this.provider.isLoaded(var2.getRegionID())) {
               SendMessage.warnMessage(
                  "Issue while loading region item requirement: Region using ID '"
                     + var2.getRegionID()
                     + "' was not found in "
                     + this.provider.getName()
                     + ", is this supposed to be a region which hasn't been created yet? Else the name of the region is incorrect, continuing anyway..."
               );
               SendMessage.errorShops(var3.split("\\.")[0], var3.split("\\.", 2)[1]);
            }

            var2.onLoad();
         }
      }
   }
}
