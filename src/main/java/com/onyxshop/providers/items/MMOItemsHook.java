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

   public MMOItemsHook(OnyxShop var1) {
      this.plugin = var1;
      if (var1.getServer().getPluginManager().isPluginEnabled("MMOItems")) {
         this.enable();
      } else {
         var1.runTaskLater(() -> {
            if (!var1.getServer().getPluginManager().isPluginEnabled("MMOItems")) {
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
   public ItemStack getItem(String var1) throws ItemLoadException {
      if (!this.isReady) {
         return null;
      } else {
         String[] var2 = var1.split("\\.");

         try {
            Type var3 = MMOItems.plugin.getTypes().get(var2[0]);
            if (var3 == null) {
               throw new ItemLoadException("Invalid MMOItems item type for '" + var2[0] + "." + var2[1] + "'");
            } else {
               MMOItem var6 = MMOItems.plugin.getMMOItem(var3, var2[1]);
               if (var6 == null) {
                  throw new ItemLoadException(Lang.UNKNOWN_THIRD_PARTY_ITEM.get().replace("%id%", var1).replace("%plugin%", "MMOItems"));
               } else {
                  return var6.newBuilder().build();
               }
            }
         } catch (Exception var5) {
            String var4;
            if (var5 instanceof ArrayIndexOutOfBoundsException) {
               var4 = "Invalid syntax for MMOItem, should be '<itemType>.<itemID>'. But got '" + var1 + "'";
            } else if (!(var5 instanceof ItemLoadException)) {
               var4 = Lang.ERROR_CREATING_THIRD_PARTY_ITEM.get().replace("%id%", var1).getLegacy().replace("%plugin%", "MMOItems");
               var5.printStackTrace();
            } else {
               var4 = var5.getMessage();
            }

            throw new ItemLoadException("Failed to load MMOItem with reason: " + var4);
         }
      }
   }

   @Override
   public String getItem(ItemStack var1) {
      String var2 = MMOItems.getTypeName(var1);
      if (var2 == null) {
         return null;
      } else {
         String var3 = MMOItems.getID(var1);
         return var3 == null ? null : var2 + "." + var3;
      }
   }

   @Override
   public String getID(String var1) {
      return var1.contains(":") ? var1.split(":")[1] : var1;
   }
}
