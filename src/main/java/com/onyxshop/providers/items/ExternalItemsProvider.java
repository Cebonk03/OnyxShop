package com.onyxshop.providers.items;

import com.onyxshop.providers.ItemProvider;
import com.onyxshop.util.ExternalItemType;
import com.onyxshop.util.ItemType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class ExternalItemsProvider implements ItemProvider {
   private final ItemType TYPE;

   public ExternalItemsProvider(Plugin plugin, String text) {
      this.TYPE = new ExternalItemType(plugin, text);
   }

   @Override
   public final ItemType getType() {
      return this.TYPE;
   }

   @Override
   public final boolean isReady() {
      return true;
   }

   @Override
   public ItemStack getItem(String itemId) {
      return null;
   }

   @Override
   public String getItem(ItemStack itemStack) {
      return "";
   }
}
