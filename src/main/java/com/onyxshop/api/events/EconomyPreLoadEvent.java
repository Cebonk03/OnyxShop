package com.onyxshop.api.events;

import com.onyxshop.OnyxShop;
import com.onyxshop.api.objects.ExternalEconomy;

public class EconomyPreLoadEvent extends CustomEvent {
   private final OnyxShop plugin;

   public EconomyPreLoadEvent(OnyxShop plugin) {
      this.plugin = plugin;
   }

   public void registerExternal(ExternalEconomy externalEconomy) {
      this.plugin.getEcoHandler().registerExternalProvider(externalEconomy);
   }
}
