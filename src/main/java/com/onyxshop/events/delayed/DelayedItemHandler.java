package com.onyxshop.events.delayed;

import java.util.ArrayList;
import java.util.Collection;
import com.onyxshop.OnyxShop;
import com.onyxshop.events.api.ProviderLoadEvent;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.economys.ItemEconomy;
import com.onyxshop.util.exceptions.EconomyLoadException;
import com.onyxshop.util.exceptions.ItemRequirementLoadException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DelayedItemHandler implements Listener {
   private final Collection<DelayedItemProvider> PROVIDERS = new ArrayList<>();
   private final OnyxShop plugin;

   public DelayedItemHandler(OnyxShop var1) {
      this.plugin = var1;
      this.plugin.getServer().getPluginManager().registerEvents(this, var1);
   }

   public void registerProvider(DelayedItemProvider var1) {
      this.PROVIDERS.add(var1);
   }

   @EventHandler
   public void onLoad(ProviderLoadEvent var1) {
      if (var1.getProviderType() == DelayedLoadListener.Type.ITEMS) {
         for (DelayedItemProvider var3 : this.PROVIDERS) {
            try {
               var3.load();
            } catch (EconomyLoadException var5) {
               SendMessage.errorMessage("Failed to load item economy after waiting for item providers at " + var3.getPath() + ", reason:\n" + var5.getMessage());
            } catch (ItemRequirementLoadException var6) {
               SendMessage.errorMessage(
                  "Failed to load item requirement for shop item '" + var3.getPath() + "' after waiting for item providers with reason: " + var6.getMessage()
               );
            }
         }

         ProviderLoadEvent.getHandlerList().unregister(this);
         if (this.PROVIDERS.stream().anyMatch(var0 -> var0 instanceof ItemEconomy)) {
            this.plugin.getEcoHandler().mayContinue();
         }
      }
   }
}
