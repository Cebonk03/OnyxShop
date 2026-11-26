package com.onyxshop.events.delayed;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.events.api.ProviderLoadEvent;
import com.onyxshop.methodes.SendMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DelayedLoadListener implements Listener {
   private boolean enabled;
   private final Runnable onComplete;
   private final Set<DelayedLoadListener.Type> loaded = new HashSet<>();
   private final Set<DelayedLoadListener.Type> waitFor = new HashSet<>();

   public DelayedLoadListener(OnyxShop var1, Runnable var2, boolean var3, boolean var4, boolean var5, boolean var6) {
      SendMessage.infoMessage("Providers not enabled yet, waiting...");
      this.onComplete = var2;
      if (var3) {
         this.waitFor.add(DelayedLoadListener.Type.ITEMS);
      }

      if (var4) {
         this.waitFor.add(DelayedLoadListener.Type.SPAWNERS);
      }

      if (var5) {
         this.waitFor.add(DelayedLoadListener.Type.ECONOMY);
      }

      if (var6) {
         this.waitFor.add(DelayedLoadListener.Type.PLACEHOLDERS);
      }

      var1.runTaskLater(this::fallBack, 100L);
   }

   @EventHandler
   public void onLoad(ProviderLoadEvent var1) {
      this.loaded.add(var1.getProviderType());
      if (this.loaded.containsAll(this.waitFor)) {
         this.enabled = true;
         this.onComplete.run();
         this.unregister();
      } else {
         SendMessage.logDebugMessage(var1.getProviderType().name() + " provider loaded, waiting...");
      }
   }

   private void unregister() {
      ProviderLoadEvent.getHandlerList().unregister(this);
   }

   private void fallBack() {
      if (!this.enabled) {
         SendMessage.warnMessage(
            "Provider(s) "
               + Arrays.toString(this.waitFor.stream().filter(var1 -> !this.loaded.contains(var1)).toArray())
               + " not enabled yet after waiting for server to finish, loading now..."
         );
         this.onComplete.run();
      }
   }

   public static enum Type {
      ITEMS,
      SPAWNERS,
      ECONOMY,
      PLACEHOLDERS;

      private Type() {
      }
   }
}
