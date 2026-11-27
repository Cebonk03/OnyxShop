package com.onyxshop.providers.items;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import com.nexomc.nexo.items.ItemBuilder;
import com.nexomc.nexo.items.ItemUpdater;
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

public class NexoItemsHook implements ItemProvider, Listener {
   private static final ItemType TYPE = new ItemType(ItemProviderType.NEXO);
   private final OnyxShop plugin;
   private boolean isReady = false;

   public NexoItemsHook(OnyxShop plugin) {
      this.plugin = plugin;
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler
   public void onItemsLoad(NexoItemsLoadedEvent event) {
      SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getType().getName()));
      this.isReady = true;
      this.plugin.getItemManager().check();
      NexoItemsLoadedEvent.getHandlerList().unregister(this);
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
         throw new ItemLoadException("Cannot create item from " + this.getType().getName() + " because integration failed after 20 ticks");
      } else {
         try {
            ItemBuilder itemBuilder = NexoItems.itemFromId(itemId);
            if (itemBuilder == null) {
               throw new ItemLoadException(Lang.UNKNOWN_THIRD_PARTY_ITEM.get().replace("%id%", itemId).replace("%plugin%", this.getType().getName()));
            } else {
               return ItemUpdater.Companion.updateItem(itemBuilder.build());
            }
         } catch (Exception exception) {
            String text;
            if (exception instanceof ArrayIndexOutOfBoundsException) {
               text = "Invalid syntax for " + this.getType().getName() + " item, should be '<itemType>.<itemID>'. But got '" + itemId + "'";
            } else if (!(exception instanceof ItemLoadException)) {
               text = Lang.ERROR_CREATING_THIRD_PARTY_ITEM.get().getLegacy().replace("%id%", itemId).replace("%plugin%", this.getType().getName());
               exception.printStackTrace();
            } else {
               text = exception.getMessage();
            }

            throw new ItemLoadException("Failed to load " + this.getType().getName() + " item with reason: " + text);
         }
      }
   }

   @Override
   public String getItem(ItemStack itemStack) {
      return NexoItems.idFromItem(itemStack);
   }

   @Override
   public String getID(String text) {
      return text.contains(":") ? text.split(":")[1] : text;
   }
}
