package com.onyxshop.providers.items;

import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.ItemProvider;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import com.onyxshop.util.exceptions.ItemLoadException;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class MMOItemsHook implements ItemProvider, Listener {
   private static final ItemType TYPE = new ItemType(ItemProviderType.MMO_ITEMS);
   private final OnyxShop plugin;
   private boolean isReady = false;

   public MMOItemsHook(OnyxShop plugin) {
      this.plugin = plugin;
      if (plugin.getServer().getPluginManager().isPluginEnabled("MMOItems")) {
         this.enable();
      } else {
         plugin.runTaskLater(() -> {
            if (!plugin.getServer().getPluginManager().isPluginEnabled("MMOItems")) {
               SendMessage.errorMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", "MMOItems"));
            }

            this.enable();
         }, 20L);
      }
   }

   private void enable() {
      SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", "MMOItems"));
      this.isReady = true;
      this.plugin.getItemManager().check();
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
         return null;
      } else {
         String[] values = itemId.split("\\.");

         try {
            Type type = MMOItems.plugin.getTypes().get(values[0]);
            if (type == null) {
               throw new ItemLoadException("Invalid MMOItems item type for '" + values[0] + "." + values[1] + "'");
            } else {
               MMOItem mmoItem = MMOItems.plugin.getMMOItem(type, values[1]);
               if (mmoItem == null) {
                  throw new ItemLoadException(Lang.UNKNOWN_THIRD_PARTY_ITEM.get().replace("%id%", itemId).replace("%plugin%", "MMOItems"));
               } else {
                  return mmoItem.newBuilder().build();
               }
            }
         } catch (Exception exception) {
            String text;
            if (exception instanceof ArrayIndexOutOfBoundsException) {
               text = "Invalid syntax for MMOItem, should be '<itemType>.<itemID>'. But got '" + itemId + "'";
            } else if (!(exception instanceof ItemLoadException)) {
               text = Lang.ERROR_CREATING_THIRD_PARTY_ITEM.get().replace("%id%", itemId).getLegacy().replace("%plugin%", "MMOItems");
               exception.printStackTrace();
            } else {
               text = exception.getMessage();
            }

            throw new ItemLoadException("Failed to load MMOItem with reason: " + text);
         }
      }
   }

   @Override
   public String getItem(ItemStack itemStack) {
      String typeName = MMOItems.getTypeName(itemStack);
      if (typeName == null) {
         return null;
      } else {
         String itemId = MMOItems.getID(itemStack);
         return itemId == null ? null : typeName + "." + itemId;
      }
   }

   @Override
   public String getID(String text) {
      return text.contains(":") ? text.split(":")[1] : text;
   }
}
