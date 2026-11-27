package com.onyxshop.api.events;

import com.onyxshop.OnyxShop;
import com.onyxshop.api.objects.ExternalItems;

public class ItemProviderPreLoadEvent extends CustomEvent {
   private final OnyxShop plugin;

   public ItemProviderPreLoadEvent(OnyxShop plugin) {
      this.plugin = plugin;
   }

   public void registerExternal(ExternalItems externalItems) {
      this.plugin.getItemManager().registerExternalProvider(externalItems);
   }
}
