package com.onyxshop.providers.requirements.items.region;

import com.onyxshop.OnyxShop;
import org.bukkit.entity.Player;

public interface RegionProvider {
   void init(OnyxShop var1, RegionManager var2);

   boolean isReady();

   String getName();

   boolean isLoaded(String var1);

   String getRegionName(String var1);

   boolean isInsideRegion(Player var1, String var2);
}
