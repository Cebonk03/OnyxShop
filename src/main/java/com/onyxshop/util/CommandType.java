package com.onyxshop.util;

public enum CommandType {
   CLICK,
   BUY,
   SELL;

   private CommandType() {
   }

   public String getCompound() {
      switch (this) {
         case CLICK:
            return "click-commands";
         case BUY:
         default:
            return "buy-commands";
         case SELL:
            return "sell-commands";
      }
   }
}
