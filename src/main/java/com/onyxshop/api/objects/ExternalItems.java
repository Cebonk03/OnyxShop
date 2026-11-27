package com.onyxshop.api.objects;

import com.onyxshop.providers.items.ExternalItemsProvider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public abstract class ExternalItems extends ExternalItemsProvider {
   public ExternalItems(Plugin plugin, String text) {
      super(plugin, text);
   }

   @Override
   public abstract ItemStack getItem(String itemId);

   @Override
   public abstract String getItem(ItemStack itemStack);
}
