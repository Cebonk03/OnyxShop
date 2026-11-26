package com.onyxshop.events.api;

import com.onyxshop.api.events.CustomEvent;
import com.onyxshop.events.delayed.DelayedLoadListener;

public class ProviderLoadEvent extends CustomEvent {
   private final DelayedLoadListener.Type providerType;

   public ProviderLoadEvent(DelayedLoadListener.Type var1) {
      this.providerType = var1;
   }

   public DelayedLoadListener.Type getProviderType() {
      return this.providerType;
   }
}
