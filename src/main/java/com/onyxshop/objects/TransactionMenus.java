package com.onyxshop.objects;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.CreateItem;
import com.onyxshop.util.Transaction;

public class TransactionMenus {
   private static TransactionScreen buyScreen;
   private static TransactionScreen sellScreen;
   private static TransactionScreen buyStacksScreen;
   private static TransactionScreen shopStandBuy;
   private static TransactionScreen shopStandSell;

   public TransactionMenus() {
   }

   public static void load(OnyxShop var0) {
      buyScreen = var0.createItem.getTransactionScreen(Transaction.Type.BUY_SCREEN);
      sellScreen = var0.createItem.getTransactionScreen(Transaction.Type.SELL_SCREEN);
      buyStacksScreen = var0.createItem.getTransactionScreen(Transaction.Type.BUY_STACKS_SCREEN);
      if (var0.shopStands) {
         shopStandBuy = var0.createItem.getTransactionScreen(Transaction.Type.SHOPSTAND_BUY_SCREEN);
         shopStandSell = var0.createItem.getTransactionScreen(Transaction.Type.SHOPSTAND_SELL_SCREEN);
      }
   }

   public static List<TransactionItem> getItems(Transaction.Type var0) {
      switch (var0) {
         case BUY_STACKS_SCREEN:
            return buyStacksScreen.getItems();
         case BUY_SCREEN:
            return buyScreen.getItems();
         case SELL_SCREEN:
            return sellScreen.getItems();
         case SHOPSTAND_BUY_SCREEN:
            return shopStandBuy.getItems();
         case SHOPSTAND_SELL_SCREEN:
            return shopStandSell.getItems();
         default:
            return null;
      }
   }

   public static CreateItem.TransactionItemAction getActionFromSlot(Transaction.Type var0, int var1) {
      try {
         TransactionItem var2 = getItems(var0)
            .stream()
            .filter(var1x -> var1x.getSlots().contains(var1) && var1x.getAction() != CreateItem.TransactionItemAction.NONE)
            .findFirst()
            .orElse(null);
         if (var2 != null) {
            return var2.getAction();
         }
      } catch (NullPointerException var3) {
      }

      return CreateItem.TransactionItemAction.NONE;
   }

   public static TransactionItem getItemFromSlot(Transaction.Type var0, int var1) {
      return getItems(var0).stream().filter(var1x -> var1x.getSlots().contains(var1)).findFirst().orElse(null);
   }

   public static TransactionItem getItemByType(Transaction.Type var0, CreateItem.TransactionItemType var1) {
      return getItems(var0).stream().filter(var1x -> var1x.getType() == var1).findFirst().orElse(null);
   }

   public static List<TransactionItem> getItemsByType(Transaction.Type var0, CreateItem.TransactionItemType var1) {
      return getItems(var0).stream().filter(var1x -> var1x.getType() == var1).collect(Collectors.toList());
   }

   public static List<TransactionItem> getItemsByAction(Transaction.Type var0, CreateItem.TransactionItemActionType... var1) {
      return getItems(var0).stream().filter(var1x -> Arrays.stream(var1).anyMatch(var1xx -> var1x.getAction().type == var1xx)).collect(Collectors.toList());
   }

   public static int getSize(Transaction.Type var0) {
      switch (var0) {
         case BUY_STACKS_SCREEN:
            return buyStacksScreen.getSize();
         case BUY_SCREEN:
            return buyScreen.getSize();
         case SELL_SCREEN:
            return sellScreen.getSize();
         case SHOPSTAND_BUY_SCREEN:
            return shopStandBuy.getSize();
         case SHOPSTAND_SELL_SCREEN:
            return shopStandSell.getSize();
         default:
            return 0;
      }
   }
}
