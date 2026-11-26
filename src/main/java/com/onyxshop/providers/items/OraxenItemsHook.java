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

   public OraxenItemsHook(OnyxShop var1) {
      this.plugin = var1;
      var1.getServer().getPluginManager().registerEvents(this, var1);
   }

   @EventHandler
   public void onItemsLoad(OraxenItemsLoadedEvent var1) {
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
   public ItemStack getItem(String var1) throws ItemLoadException {
      if (!this.isReady) {
         throw new ItemLoadException("Cannot create item from Oraxen because integration failed after 20 ticks");
      } else {
         try {
            ItemBuilder var2 = OraxenItems.getItemById(var1);
            if (var2 == null) {
               throw new ItemLoadException(Lang.UNKNOWN_THIRD_PARTY_ITEM.get().replace("%id%", var1).replace("%plugin%", "Oraxen"));
            } else {
               return ItemUpdater.updateItem(var2.build());
            }
         } catch (Exception var4) {
            String var3;
            if (var4 instanceof ArrayIndexOutOfBoundsException) {
               var3 = "Invalid syntax for Oraxen item, should be '<itemType>.<itemID>'. But got '" + var1 + "'";
            } else if (!(var4 instanceof ItemLoadException)) {
               var3 = Lang.ERROR_CREATING_THIRD_PARTY_ITEM.get().getLegacy().replace("%id%", var1).replace("%plugin%", "Oraxen");
               var4.printStackTrace();
            } else {
               var3 = var4.getMessage();
            }

            throw new ItemLoadException("Failed to load Oraxen item with reason: " + var3);
         }
      }
   }

   @Override
   public String getItem(ItemStack var1) {
      return OraxenItems.getIdByItem(var1);
   }

   @Override
   public String getID(String var1) {
      return var1.contains(":") ? var1.split(":")[1] : var1;
   }
}
