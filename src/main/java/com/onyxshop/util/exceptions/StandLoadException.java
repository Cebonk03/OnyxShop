package com.onyxshop.util.exceptions;

import com.onyxshop.files.Translatable;
import org.bukkit.ChatColor;

public class StandLoadException extends Exception {
   public StandLoadException(String text) {
      super(text);
   }

   public StandLoadException(Translatable text) {
      super(ChatColor.stripColor(text.getLegacy()));
   }
}
