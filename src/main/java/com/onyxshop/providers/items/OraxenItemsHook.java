package com.onyxshop.providers.items;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemUpdater;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.ItemProvider;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class OraxenItemsHook implements ItemProvider, Listener {
   private static final ItemType TYPE = new ItemType(ItemProviderType.ORAXEN);
   private final OnyxShop plugin;
   private boolean isReady = false;

   public OraxenItemsHook(OnyxShop plugin) {
      this.plugin = plugin;
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler
   public void onItemsLoad(OraxenItemsLoadedEvent event) {
      SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", "Oraxen"));
      this.isReady = true;
      this.plugin.getItemManager().check();
      OraxenItemsLoadedEvent.getHandlerList().unregister(this);
   }

   @Override
   public ItemType getType() {
      return TYPE;
   }

   @Override
   public boolean isReady() {
      return this.isReady;
   }

   @Override
   public ItemStack getItem(String itemId) throws ItemLoadException {
      if (!this.isReady) {
         throw new ItemLoadException("Cannot create item from Oraxen because integration failed after 20 ticks");
      } else {
         try {
            ItemBuilder itemBuilder = OraxenItems.getItemById(itemId);
            if (itemBuilder == null) {
               throw new ItemLoadException(Lang.UNKNOWN_THIRD_PARTY_ITEM.get().replace("%id%", itemId).replace("%plugin%", "Oraxen"));
            } else {
               return ItemUpdater.updateItem(itemBuilder.build());
            }
         } catch (Exception exception) {
            String text;
            if (exception instanceof ArrayIndexOutOfBoundsException) {
               text = "Invalid syntax for Oraxen item, should be '<itemType>.<itemID>'. But got '" + itemId + "'";
            } else if (!(exception instanceof ItemLoadException)) {
               text = Lang.ERROR_CREATING_THIRD_PARTY_ITEM.get().getLegacy().replace("%id%", itemId).replace("%plugin%", "Oraxen");
               exception.printStackTrace();
            } else {
               text = exception.getMessage();
            }

            throw new ItemLoadException("Failed to load Oraxen item with reason: " + text);
         }
      }
   }

   @Override
   public String getItem(ItemStack itemStack) {
      return OraxenItems.getIdByItem(itemStack);
   }

   @Override
   public String getID(String text) {
      return text.contains(":") ? text.split(":")[1] : text;
   }
}
