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

   public static void load(OnyxShop plugin) {
      buyScreen = plugin.createItem.getTransactionScreen(Transaction.Type.BUY_SCREEN);
      sellScreen = plugin.createItem.getTransactionScreen(Transaction.Type.SELL_SCREEN);
      buyStacksScreen = plugin.createItem.getTransactionScreen(Transaction.Type.BUY_STACKS_SCREEN);
      if (plugin.shopStands) {
         shopStandBuy = plugin.createItem.getTransactionScreen(Transaction.Type.SHOPSTAND_BUY_SCREEN);
         shopStandSell = plugin.createItem.getTransactionScreen(Transaction.Type.SHOPSTAND_SELL_SCREEN);
      }
   }

   public static List<TransactionItem> getItems(Transaction.Type type) {
      switch (type) {
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

   public static CreateItem.TransactionItemAction getActionFromSlot(Transaction.Type type, int slot) {
      try {
         TransactionItem transactionItem = getItems(type)
            .stream()
            .filter(element -> element.getSlots().contains(slot) && element.getAction() != CreateItem.TransactionItemAction.NONE)
            .findFirst()
            .orElse(null);
         if (transactionItem != null) {
            return transactionItem.getAction();
         }
      } catch (NullPointerException exception) {
      }

      return CreateItem.TransactionItemAction.NONE;
   }

   public static TransactionItem getItemFromSlot(Transaction.Type type, int slot) {
      return getItems(type).stream().filter(element -> element.getSlots().contains(slot)).findFirst().orElse(null);
   }

   public static TransactionItem getItemByType(Transaction.Type type, CreateItem.TransactionItemType transactionItemType) {
      return getItems(type).stream().filter(element -> element.getType() == transactionItemType).findFirst().orElse(null);
   }

   public static List<TransactionItem> getItemsByType(Transaction.Type type, CreateItem.TransactionItemType transactionItemType) {
      return getItems(type).stream().filter(element -> element.getType() == transactionItemType).collect(Collectors.toList());
   }

   public static List<TransactionItem> getItemsByAction(Transaction.Type type, CreateItem.TransactionItemActionType... itemIds) {
      return getItems(type).stream().filter(element -> Arrays.stream(itemIds).anyMatch(element2 -> element.getAction().type == element2)).collect(Collectors.toList());
   }

   public static int getSize(Transaction.Type type) {
      switch (type) {
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
