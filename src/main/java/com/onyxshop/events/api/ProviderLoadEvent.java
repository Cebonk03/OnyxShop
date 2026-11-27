package com.onyxshop.events.api;

import com.onyxshop.api.events.CustomEvent;
import com.onyxshop.events.delayed.DelayedLoadListener;

public class ProviderLoadEvent extends CustomEvent {
   private final DelayedLoadListener.Type providerType;

   public ProviderLoadEvent(DelayedLoadListener.Type type) {
      this.providerType = type;
   }

   public DelayedLoadListener.Type getProviderType() {
      return this.providerType;
   }
}
