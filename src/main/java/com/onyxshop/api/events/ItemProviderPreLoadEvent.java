package com.onyxshop.api.events;

import com.onyxshop.OnyxShop;
import com.onyxshop.api.objects.ExternalItems;

public class ItemProviderPreLoadEvent extends CustomEvent {
   private final OnyxShop plugin;

   public ItemProviderPreLoadEvent(OnyxShop var1) {
      this.plugin = var1;
   }

   public void registerExternal(ExternalItems var1) {
      this.plugin.getItemManager().registerExternalProvider(var1);
   }
}
