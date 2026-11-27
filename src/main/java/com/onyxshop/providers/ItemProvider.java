package com.onyxshop.providers;

import com.onyxshop.util.ItemType;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.inventory.ItemStack;

public interface ItemProvider {
   ItemType getType();

   boolean isReady();

   ItemStack getItem(String itemId) throws ItemLoadException;

   String getItem(ItemStack itemStack);

   default String getID(String itemID) {
      return itemID;
   }
}
