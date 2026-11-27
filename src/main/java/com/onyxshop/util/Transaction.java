package com.onyxshop.util;

import com.onyxshop.files.Lang;

public class Transaction {
   public Transaction() {
   }

   public static enum Mode {
      BUY(Lang.BOUGHT.get().getLegacy()),
      SELL(Lang.SOLD.get().getLegacy());

      private String name;

      private Mode(String text) {
         this.name = text;
      }

      public String getName() {
         return this.name;
      }

      public static Transaction.Mode getFromType(Transaction.Type type) {
         return type != Transaction.Type.BUY_SCREEN
               && type != Transaction.Type.BUY_STACKS_SCREEN
               && type != Transaction.Type.QUICK_BUY
               && type != Transaction.Type.SHOPSTAND_BUY_SCREEN
               && type != Transaction.Type.API_BUY
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

      private Type(String type) {
         this.name = type;
      }

      public String getName() {
         return this.name;
      }

      public Transaction.Type withCustomType(String type) {
         this.customType = type.toUpperCase().replace(" ", "_");
         this.name = this.name.replace("%type%", this.customType);
         return this;
      }

      public String enumName() {
         return this.name() + (this.customType != null ? "_" + this.customType : "");
      }

      public static Transaction.Type fromValue(String type) {
         if (type.startsWith("API")) {
            String[] values = type.split("_", 3);
            Transaction.Type type2 = valueOf(values[0] + "_" + values[1]);
            type2.name = type2.name.replace("%type%", type2.customType = values[2]);
            return type2;
         } else {
            return valueOf(type);
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
