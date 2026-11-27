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

   public DelayedLoadListener(OnyxShop plugin, Runnable task, boolean flag, boolean flag2, boolean flag3, boolean flag4) {
      SendMessage.infoMessage("Providers not enabled yet, waiting...");
      this.onComplete = task;
      if (flag) {
         this.waitFor.add(DelayedLoadListener.Type.ITEMS);
      }

      if (flag2) {
         this.waitFor.add(DelayedLoadListener.Type.SPAWNERS);
      }

      if (flag3) {
         this.waitFor.add(DelayedLoadListener.Type.ECONOMY);
      }

      if (flag4) {
         this.waitFor.add(DelayedLoadListener.Type.PLACEHOLDERS);
      }

      plugin.runTaskLater(this::fallBack, 100L);
   }

   @EventHandler
   public void onLoad(ProviderLoadEvent event) {
      this.loaded.add(event.getProviderType());
      if (this.loaded.containsAll(this.waitFor)) {
         this.enabled = true;
         this.onComplete.run();
         this.unregister();
      } else {
         SendMessage.logDebugMessage(event.getProviderType().name() + " provider loaded, waiting...");
      }
   }

   private void unregister() {
      ProviderLoadEvent.getHandlerList().unregister(this);
   }

   private void fallBack() {
      if (!this.enabled) {
         SendMessage.warnMessage(
            "Provider(s) "
               + Arrays.toString(this.waitFor.stream().filter(element -> !this.loaded.contains(element)).toArray())
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
