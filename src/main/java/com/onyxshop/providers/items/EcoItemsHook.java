package com.onyxshop.providers.items;

import com.willfp.eco.core.registry.Registry;
import com.willfp.ecoitems.items.EcoItem;
import java.lang.reflect.Field;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.ItemProvider;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class EcoItemsHook implements ItemProvider, Listener {
   private static final ItemType TYPE = new ItemType(ItemProviderType.ECOITEMS);
   private static final NamespacedKey key = new NamespacedKey("ecoitems", "item");
   private final OnyxShop plugin;
   private boolean isReady = false;
   private Registry<EcoItem> registry;

   public EcoItemsHook(OnyxShop plugin) {
      this.plugin = plugin;
      if (plugin.getServer().getPluginManager().isPluginEnabled(this.getType().getName())) {
         this.enable();
      } else {
         plugin.runTaskLater(() -> {
            if (!plugin.getServer().getPluginManager().isPluginEnabled(this.getType().getName())) {
               SendMessage.errorMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", this.getType().getName()));
            } else {
               this.enable();
            }
         }, 20L);
      }
   }

   private void enable() {
      try {
         Class type = Class.forName("com.willfp.ecoitems.items.EcoItems");
         Field field = type.getDeclaredField("registry");
         field.setAccessible(true);
         this.registry = (Registry<EcoItem>)field.get(null);
      } catch (Exception exception) {
         exception.printStackTrace();
         throw new NullPointerException(
            "Failed to register EcoItems hook, this version of EcoItems("
               + Bukkit.getPluginManager().getPlugin("EcoItems").getDescription().getVersion()
               + ") is not supported..."
         );
      }

      SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getType().getName()));
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
   public ItemStack getItem(String itemId) {
      if (!this.isReady) {
         return null;
      } else {
         try {
            EcoItem ecoItem = (EcoItem)this.registry.get(itemId);
            return ecoItem == null ? null : ecoItem.getItemStack().clone();
         } catch (Exception exception) {
            SendMessage.warnMessage("A unknown error occurred while loading a item from EcoItems, this is a bug");
            return null;
         }
      }
   }

   @Override
   public String getItem(ItemStack itemStack) {
      return itemStack.hasItemMeta() && itemStack.getItemMeta().getPersistentDataContainer().has(key)
         ? (String)itemStack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING)
         : null;
   }
}
