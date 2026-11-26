package com.onyxshop.events.api;

import com.onyxshop.api.events.CustomEvent;

public class ProvidersLoadEvent extends CustomEvent {
   final boolean wait;

   public ProvidersLoadEvent(boolean var1) {
      this.wait = var1;
   }
}
