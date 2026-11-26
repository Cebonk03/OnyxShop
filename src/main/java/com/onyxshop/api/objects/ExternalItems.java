package com.onyxshop.api.objects;

import com.onyxshop.providers.items.ExternalItemsProvider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public abstract class ExternalItems extends ExternalItemsProvider {
   public ExternalItems(Plugin var1, String var2) {
      super(var1, var2);
   }

   @Override
   public abstract ItemStack getItem(String var1);

   @Override
   public abstract String getItem(ItemStack var1);
}
