package com.onyxshop.util.exceptions;

import com.onyxshop.files.Translatable;
import org.bukkit.ChatColor;

public class StandUnloadException extends Exception {
   public StandUnloadException(String text) {
      super(text);
   }

   public StandUnloadException(Translatable text) {
      super(ChatColor.stripColor(text.getLegacy()));
   }
}
