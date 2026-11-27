package com.onyxshop.util.exceptions;

import com.onyxshop.files.Translatable;
import org.bukkit.ChatColor;

public class ItemLoadException extends CustomException {
   public ItemLoadException(String itemId) {
      super(itemId);
   }

   public ItemLoadException(Translatable text) {
      super(ChatColor.stripColor(text.getLegacy()));
   }
}
