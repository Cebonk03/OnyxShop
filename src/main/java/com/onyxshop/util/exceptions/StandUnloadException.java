package com.onyxshop.util.exceptions;

import com.onyxshop.files.Translatable;
import org.bukkit.ChatColor;

public class StandUnloadException extends Exception {
   public StandUnloadException(String var1) {
      super(var1);
   }

   public StandUnloadException(Translatable var1) {
      super(ChatColor.stripColor(var1.getLegacy()));
   }
}
