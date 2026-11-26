package com.onyxshop.util.exceptions;

import com.onyxshop.files.Translatable;
import org.bukkit.ChatColor;

public class StandLoadException extends Exception {
   public StandLoadException(String var1) {
      super(var1);
   }

   public StandLoadException(Translatable var1) {
      super(ChatColor.stripColor(var1.getLegacy()));
   }
}
