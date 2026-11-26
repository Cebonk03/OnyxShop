package com.onyxshop.util;

import com.onyxshop.files.Lang;

public class Transaction {
   public Transaction() {
   }

   public static enum Mode {
      BUY(Lang.BOUGHT.get().getLegacy()),
      SELL(Lang.SOLD.get().getLegacy());

      private String name;

      private Mode(String nullxx) {
         this.name = nullxx;
      }

      public String getName() {
         return this.name;
      }

      public static Transaction.Mode getFromType(Transaction.Type var0) {
         return var0 != Transaction.Type.BUY_SCREEN
               && var0 != Transaction.Type.BUY_STACKS_SCREEN
               && var0 != Transaction.Type.QUICK_BUY
               && var0 != Transaction.Type.SHOPSTAND_BUY_SCREEN
               && var0 != Transaction.Type.API_BUY
            ? SELL
            : BUY;
      }
   }

   public static enum Result {
      SUCCESS,
      SUCCESS_COMMANDS_EXECUTED,
      ITEM_ERROR,
      DISPLAY_ITEM,
      NEGATIVE_ITEM_PRICE,
      NO_PERMISSIONS,
      REQUIREMENTS_FAILED,
      INSUFFICIENT_FUNDS,
      NO_INVENTORY_SPACE,
      TRANSACTION_CANCELLED,
      NO_ITEMS_FOUND,
      NOT_ENOUGH_ITEMS,
      TO_MANY_ITEMS,
      NO_ITEM_STOCK_LEFT,
      REACHED_SELL_LIMIT,
      CANT_STORE_PAYMENT;

      private Result() {
      }
   }

   public static enum Type {
      SELL_GUI_SCREEN(Lang.SELLGUI_SCREEN.get().getLegacy()),
      SELL_ALL_COMMAND(Lang.SELLALL_COMMAND.get().getLegacy()),
      SELL_ALL_SCREEN(Lang.SELLALL_SCREEN.get().getLegacy()),
      SELL_SCREEN(Lang.SELL_SCREEN.get().getLegacy()),
      BUY_SCREEN(Lang.BUY_SCREEN.get().getLegacy()),
      BUY_STACKS_SCREEN(Lang.BUYSTACKS_SCREEN.get().getLegacy()),
      QUICK_SELL(Lang.QUICK_SELL.get().getLegacy()),
      QUICK_BUY(Lang.QUICK_BUY.get().getLegacy()),
      SHOPSTAND_BUY_SCREEN(Lang.SHOPSTAND_BUY_SCREEN.get().getLegacy()),
      SHOPSTAND_SELL_SCREEN(Lang.SHOPSTAND_SELL_SCREEN.get().getLegacy()),
      AUTO_SELL_CHEST(Lang.AUTO_SELL_CHEST.get().getLegacy()),
      API_BUY("API buy (%type%)"),
      API_SELL("API sell (%type%)");

      private String name;
      private String customType;

      private Type(String nullxx) {
         this.name = nullxx;
      }

      public String getName() {
         return this.name;
      }

      public Transaction.Type withCustomType(String var1) {
         this.customType = var1.toUpperCase().replace(" ", "_");
         this.name = this.name.replace("%type%", this.customType);
         return this;
      }

      public String enumName() {
         return this.name() + (this.customType != null ? "_" + this.customType : "");
      }

      public static Transaction.Type fromValue(String var0) {
         if (var0.startsWith("API")) {
            String[] var1 = var0.split("_", 3);
            Transaction.Type var2 = valueOf(var1[0] + "_" + var1[1]);
            var2.name = var2.name.replace("%type%", var2.customType = var1[2]);
            return var2;
         } else {
            return valueOf(var0);
         }
      }

      public Transaction.Mode getTransactionMode() {
         return Transaction.Mode.getFromType(this);
      }

      public String getMode() {
         return Transaction.Mode.getFromType(this).getName();
      }
   }
}
