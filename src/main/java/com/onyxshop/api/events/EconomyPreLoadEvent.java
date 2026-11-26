package com.onyxshop.api.events;

import com.onyxshop.OnyxShop;
import com.onyxshop.api.objects.ExternalEconomy;

public class EconomyPreLoadEvent extends CustomEvent {
   private final OnyxShop plugin;

   public EconomyPreLoadEvent(OnyxShop var1) {
      this.plugin = var1;
   }

   public void registerExternal(ExternalEconomy var1) {
      this.plugin.getEcoHandler().registerExternalProvider(var1);
   }
}
