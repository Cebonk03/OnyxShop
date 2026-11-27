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

   public DelayedItemHandler(OnyxShop plugin) {
      this.plugin = plugin;
      this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void registerProvider(DelayedItemProvider delayedItemProvider) {
      this.PROVIDERS.add(delayedItemProvider);
   }

   @EventHandler
   public void onLoad(ProviderLoadEvent event) {
      if (event.getProviderType() == DelayedLoadListener.Type.ITEMS) {
         for (DelayedItemProvider delayedItemProvider : this.PROVIDERS) {
            try {
               delayedItemProvider.load();
            } catch (EconomyLoadException exception) {
               SendMessage.errorMessage("Failed to load item economy after waiting for item providers at " + delayedItemProvider.getPath() + ", reason:\n" + exception.getMessage());
            } catch (ItemRequirementLoadException exception) {
               SendMessage.errorMessage(
                  "Failed to load item requirement for shop item '" + delayedItemProvider.getPath() + "' after waiting for item providers with reason: " + exception.getMessage()
               );
            }
         }

         ProviderLoadEvent.getHandlerList().unregister(this);
         if (this.PROVIDERS.stream().anyMatch(element -> element instanceof ItemEconomy)) {
            this.plugin.getEcoHandler().mayContinue();
         }
      }
   }
}
