package com.onyxshop.providers.requirements.items.region;

import com.onyxshop.OnyxShop;
import org.bukkit.entity.Player;

public interface RegionProvider {
   void init(OnyxShop plugin, RegionManager regionManager);

   boolean isReady();

   String getName();

   boolean isLoaded(String text);

   String getRegionName(String name);

   boolean isInsideRegion(Player player, String playerName);
}
