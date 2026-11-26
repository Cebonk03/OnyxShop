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

   public NexoItemsHook(OnyxShop var1) {
      this.plugin = var1;
      var1.getServer().getPluginManager().registerEvents(this, var1);
   }

   @EventHandler
   public void onItemsLoad(NexoItemsLoadedEvent var1) {
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
   public ItemStack getItem(String var1) throws ItemLoadException {
      if (!this.isReady) {
         throw new ItemLoadException("Cannot create item from " + this.getType().getName() + " because integration failed after 20 ticks");
      } else {
         try {
            ItemBuilder var2 = NexoItems.itemFromId(var1);
            if (var2 == null) {
               throw new ItemLoadException(Lang.UNKNOWN_THIRD_PARTY_ITEM.get().replace("%id%", var1).replace("%plugin%", this.getType().getName()));
            } else {
               return ItemUpdater.Companion.updateItem(var2.build());
            }
         } catch (Exception var4) {
            String var3;
            if (var4 instanceof ArrayIndexOutOfBoundsException) {
               var3 = "Invalid syntax for " + this.getType().getName() + " item, should be '<itemType>.<itemID>'. But got '" + var1 + "'";
            } else if (!(var4 instanceof ItemLoadException)) {
               var3 = Lang.ERROR_CREATING_THIRD_PARTY_ITEM.get().getLegacy().replace("%id%", var1).replace("%plugin%", this.getType().getName());
               var4.printStackTrace();
            } else {
               var3 = var4.getMessage();
            }

            throw new ItemLoadException("Failed to load " + this.getType().getName() + " item with reason: " + var3);
         }
      }
   }

   @Override
   public String getItem(ItemStack var1) {
      return NexoItems.idFromItem(var1);
   }

   @Override
   public String getID(String var1) {
      return var1.contains(":") ? var1.split(":")[1] : var1;
   }
}
