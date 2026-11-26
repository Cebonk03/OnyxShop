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

   public EcoItemsHook(OnyxShop var1) {
      this.plugin = var1;
      if (var1.getServer().getPluginManager().isPluginEnabled(this.getType().getName())) {
         this.enable();
      } else {
         var1.runTaskLater(() -> {
            if (!var1.getServer().getPluginManager().isPluginEnabled(this.getType().getName())) {
               SendMessage.errorMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", this.getType().getName()));
            } else {
               this.enable();
            }
         }, 20L);
      }
   }

   private void enable() {
      try {
         Class var1 = Class.forName("com.willfp.ecoitems.items.EcoItems");
         Field var2 = var1.getDeclaredField("registry");
         var2.setAccessible(true);
         this.registry = (Registry<EcoItem>)var2.get(null);
      } catch (Exception var3) {
         var3.printStackTrace();
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
   public ItemStack getItem(String var1) {
      if (!this.isReady) {
         return null;
      } else {
         try {
            EcoItem var2 = (EcoItem)this.registry.get(var1);
            return var2 == null ? null : var2.getItemStack().clone();
         } catch (Exception var3) {
            SendMessage.warnMessage("A unknown error occurred while loading a item from EcoItems, this is a bug");
            return null;
         }
      }
   }

   @Override
   public String getItem(ItemStack var1) {
      return var1.hasItemMeta() && var1.getItemMeta().getPersistentDataContainer().has(key)
         ? (String)var1.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING)
         : null;
   }
}
