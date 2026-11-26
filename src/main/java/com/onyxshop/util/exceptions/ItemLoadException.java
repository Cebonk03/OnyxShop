package com.onyxshop.util.exceptions;

import com.onyxshop.files.Translatable;
import org.bukkit.ChatColor;

public class ItemLoadException extends CustomException {
   public ItemLoadException(String var1) {
      super(var1);
   }

   public ItemLoadException(Translatable var1) {
      super(ChatColor.stripColor(var1.getLegacy()));
   }
}
