package com.onyxshop.providers.placeholders;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.TransactionLog;
import com.onyxshop.providers.placeholders.handler.PlaceholderProvider;
import com.onyxshop.util.SimplePair;
import com.onyxshop.util.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class PAPIExpansion extends PlaceholderExpansion {
   private final PlaceholderProvider provider;

   public PAPIExpansion(PlaceholderProvider placeholderProvider) {
      this.provider = placeholderProvider;
   }

   public String getAuthor() {
      return "OnyxShop";
   }

   public String getName() {
      return "OnyxShop";
   }

   public String getIdentifier() {
      return "onyxshop";
   }

   public String getVersion() {
      return "1.3.0";
   }

   public boolean persist() {
      return true;
   }

   public String onRequest(OfflinePlayer offlinePlayer, String params) {
      if (params.startsWith("buyprice_")) {
         String text = params.replace("buyprice_", "").replace(">", ".");
         return !text.contains(".") ? "§cInvalid item path for '" + text + "'" : this.provider.getItemBuyPrice(text.split("\\.", 2));
      } else if (params.startsWith("sellprice_")) {
         String text = params.replace("sellprice_", "").replace(">", ".");
         return !text.contains(".") ? "§cInvalid item path for '" + text + "'" : this.provider.getItemSellPrice(text.split("\\.", 2));
      } else if (params.startsWith("discount_")) {
         if (!OnyxShop.getInstance().discountsActive) {
            return "§cDiscounts not enabled";
         } else {
            String text = params.replace("discount_", "");
            return offlinePlayer != null && offlinePlayer.isOnline() ? this.provider.getDiscount(offlinePlayer.getPlayer(), text) : "§cInvalid player";
         }
      } else if (params.startsWith("sell-multiplier_")) {
         if (!OnyxShop.getInstance().multipliers) {
            return "§cSell multipliers not enabled";
         } else {
            String text = params.replace("sell-multiplier_", "");
            return offlinePlayer != null && offlinePlayer.isOnline() ? this.provider.getMultiplier(offlinePlayer.getPlayer(), text) : "§cInvalid player";
         }
      } else if (params.startsWith("shop_rotation_")) {
         String shopId = params.replace("shop_rotation_", "");
         return this.provider.getShopRotation(shopId);
      } else if (params.startsWith("item_purchases_")) {
         SimplePair simplePair = this.getData(params.replace("item_purchases_", ""));
         if (simplePair == null) {
            return "§cInvalid/missing timeframe, use day/week/month";
         } else {
            return simplePair.value == null
               ? "§cInvalid/missing item path"
               : this.provider.getTotalTransactions(Transaction.Mode.BUY, (TransactionLog.TimeFrame)simplePair.key, (String)simplePair.value);
         }
      } else if (params.startsWith("item_sales_")) {
         SimplePair simplePair = this.getData(params.replace("item_sales_", ""));
         if (simplePair == null) {
            return "§cInvalid/missing timeframe, use day/week/month";
         } else {
            return simplePair.value == null
               ? "§cInvalid/missing item path"
               : this.provider.getTotalTransactions(Transaction.Mode.SELL, (TransactionLog.TimeFrame)simplePair.key, (String)simplePair.value);
         }
      } else if (params.startsWith("player_purchases_")) {
         PAPIExpansion.PlayerData playerData = this.getPlayerData(params.substring("player_purchases_".length()));
         return playerData == null
            ? "§cInvalid/missing player, timeframe (day/week/month) or item path"
            : this.provider.getTotalTransactions(Transaction.Mode.BUY, playerData.timeFrame, playerData.path == null ? "" : playerData.path, playerData.uuid);
      } else if (params.startsWith("player_sales_")) {
         PAPIExpansion.PlayerData playerData = this.getPlayerData(params.substring("player_sales_".length()));
         return playerData == null
            ? "§cInvalid/missing player, timeframe (day/week/month) or item path"
            : this.provider.getTotalTransactions(Transaction.Mode.SELL, playerData.timeFrame, playerData.path == null ? "" : playerData.path, playerData.uuid);
      } else {
         PlaceholderProvider.PlaceholderCache placeholderCache = this.provider.getPlaceholderCache();
         if (placeholderCache == null) {
            return "§cLoading cache...";
         } else {
            try {
               SimplePair simplePair = this.getPlaceholder(params.toUpperCase().split("_", 2));
               if (simplePair == null) {
                  return "Unknown placeholder";
               } else {
                  switch ((PAPIExpansion.PlaceholderType)simplePair.key) {
                     case PLAYER_MOST_BOUGHT_ITEMS_CURRENT_MONTH:
                        return placeholderCache.getTopPlayerBoughtItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).player.name;
                     case PLAYER_MOST_BOUGHT_ITEMS_CURRENT_MONTH_AMOUNT:
                        return String.valueOf(placeholderCache.getTopPlayerBoughtItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).amount);
                     case PLAYER_MOST_BOUGHT_ITEMS_CURRENT_WEEK:
                        return placeholderCache.getTopPlayerBoughtItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).player.name;
                     case PLAYER_MOST_BOUGHT_ITEMS_CURRENT_WEEK_AMOUNT:
                        return String.valueOf(placeholderCache.getTopPlayerBoughtItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).amount);
                     case PLAYER_MOST_BOUGHT_ITEMS_CURRENT_DAY:
                        return placeholderCache.getTopPlayerBoughtItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).player.name;
                     case PLAYER_MOST_BOUGHT_ITEMS_CURRENT_DAY_AMOUNT:
                        return String.valueOf(placeholderCache.getTopPlayerBoughtItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).amount);
                     case PLAYER_MOST_SOLD_ITEMS_CURRENT_MONTH:
                        return placeholderCache.getTopPlayerSoldItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).player.name;
                     case PLAYER_MOST_SOLD_ITEMS_CURRENT_MONTH_AMOUNT:
                        return String.valueOf(placeholderCache.getTopPlayerSoldItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).amount);
                     case PLAYER_MOST_SOLD_ITEMS_CURRENT_WEEK:
                        return placeholderCache.getTopPlayerSoldItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).player.name;
                     case PLAYER_MOST_SOLD_ITEMS_CURRENT_WEEK_AMOUNT:
                        return String.valueOf(placeholderCache.getTopPlayerSoldItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).amount);
                     case PLAYER_MOST_SOLD_ITEMS_CURRENT_DAY:
                        return placeholderCache.getTopPlayerSoldItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).player.name;
                     case PLAYER_MOST_SOLD_ITEMS_CURRENT_DAY_AMOUNT:
                        return String.valueOf(placeholderCache.getTopPlayerSoldItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).amount);
                     case MOST_BOUGHT_ITEM_CURRENT_MONTH:
                        return placeholderCache.getTopBoughtItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).item.name;
                     case MOST_BOUGHT_ITEM_CURRENT_MONTH_AMOUNT:
                        return String.valueOf(placeholderCache.getTopBoughtItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).amount);
                     case MOST_BOUGHT_ITEM_CURRENT_WEEK:
                        return placeholderCache.getTopBoughtItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).item.name;
                     case MOST_BOUGHT_ITEM_CURRENT_WEEK_AMOUNT:
                        return String.valueOf(placeholderCache.getTopBoughtItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).amount);
                     case MOST_BOUGHT_ITEM_CURRENT_DAY:
                        return placeholderCache.getTopBoughtItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).item.name;
                     case MOST_BOUGHT_ITEM_CURRENT_DAY_AMOUNT:
                        return String.valueOf(placeholderCache.getTopBoughtItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).amount);
                     case MOST_SOLD_ITEM_CURRENT_MONTH:
                        return placeholderCache.getTopSoldItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).item.name;
                     case MOST_SOLD_ITEM_CURRENT_MONTH_AMOUNT:
                        return String.valueOf(placeholderCache.getTopSoldItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).amount);
                     case MOST_SOLD_ITEM_CURRENT_WEEK:
                        return placeholderCache.getTopSoldItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).item.name;
                     case MOST_SOLD_ITEM_CURRENT_WEEK_AMOUNT:
                        return String.valueOf(placeholderCache.getTopSoldItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).amount);
                     case MOST_SOLD_ITEM_CURRENT_DAY:
                        return placeholderCache.getTopSoldItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).item.name;
                     case MOST_SOLD_ITEM_CURRENT_DAY_AMOUNT:
                        return String.valueOf(placeholderCache.getTopSoldItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)simplePair.value).index).amount);
                     default:
                        return "Unknown placeholder";
                  }
               }
            } catch (IndexOutOfBoundsException exception) {
               return "null";
            }
         }
      }
   }

   private PAPIExpansion.PlayerData getPlayerData(String params) {
      String[] values = params.split("_");
      if (values.length < 2) {
         return null;
      } else {
         for (int index = 1; index < values.length; index++) {
            TransactionLog.TimeFrame timeFrame;
            try {
               timeFrame = TransactionLog.TimeFrame.valueOf(values[index].toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException exception) {
               continue;
            }

            UUID playerUUID = this.getPlayerUUID(String.join("_", Arrays.copyOfRange(values, 0, index)));
            if (playerUUID != null) {
               String text = index + 1 < values.length ? String.join("_", Arrays.copyOfRange(values, index + 1, values.length)).replace(">", ".") : null;
               return new PAPIExpansion.PlayerData(playerUUID, timeFrame, text);
            }
         }

         return null;
      }
   }

   private UUID getPlayerUUID(String params) {
      try {
         return UUID.fromString(params);
      } catch (IllegalArgumentException exception) {
         return Bukkit.getOfflinePlayer(params).getUniqueId();
      }
   }

   private SimplePair<TransactionLog.TimeFrame, String> getData(String text) {
      String[] values = text.split("_", 2);

      try {
         TransactionLog.TimeFrame timeFrame = TransactionLog.TimeFrame.valueOf(values[0].toUpperCase(Locale.ENGLISH));
         return new SimplePair<>(timeFrame, values.length > 1 ? values[1].replace(">", ".") : null);
      } catch (IllegalArgumentException exception) {
         return null;
      }
   }

   private SimplePair<PAPIExpansion.PlaceholderType, PAPIExpansion.SimpleIndex> getPlaceholder(String... types) {
      try {
         PAPIExpansion.SimpleIndex simpleIndex = PAPIExpansion.SimpleIndex.get(types[0]);
         PAPIExpansion.PlaceholderType placeholderType = PAPIExpansion.PlaceholderType.valueOf(simpleIndex == null ? String.join("_", types) : types[1]);
         return new SimplePair<>(placeholderType, simpleIndex == null ? PAPIExpansion.SimpleIndex.FIRST : simpleIndex);
      } catch (IllegalArgumentException exception) {
         return null;
      }
   }

   private static enum PlaceholderType {
      PLAYER_MOST_BOUGHT_ITEMS_CURRENT_MONTH,
      PLAYER_MOST_BOUGHT_ITEMS_CURRENT_MONTH_AMOUNT,
      PLAYER_MOST_BOUGHT_ITEMS_CURRENT_WEEK,
      PLAYER_MOST_BOUGHT_ITEMS_CURRENT_WEEK_AMOUNT,
      PLAYER_MOST_BOUGHT_ITEMS_CURRENT_DAY,
      PLAYER_MOST_BOUGHT_ITEMS_CURRENT_DAY_AMOUNT,
      PLAYER_MOST_SOLD_ITEMS_CURRENT_MONTH,
      PLAYER_MOST_SOLD_ITEMS_CURRENT_MONTH_AMOUNT,
      PLAYER_MOST_SOLD_ITEMS_CURRENT_WEEK,
      PLAYER_MOST_SOLD_ITEMS_CURRENT_WEEK_AMOUNT,
      PLAYER_MOST_SOLD_ITEMS_CURRENT_DAY,
      PLAYER_MOST_SOLD_ITEMS_CURRENT_DAY_AMOUNT,
      MOST_BOUGHT_ITEM_CURRENT_MONTH,
      MOST_BOUGHT_ITEM_CURRENT_MONTH_AMOUNT,
      MOST_BOUGHT_ITEM_CURRENT_WEEK,
      MOST_BOUGHT_ITEM_CURRENT_WEEK_AMOUNT,
      MOST_BOUGHT_ITEM_CURRENT_DAY,
      MOST_BOUGHT_ITEM_CURRENT_DAY_AMOUNT,
      MOST_SOLD_ITEM_CURRENT_MONTH,
      MOST_SOLD_ITEM_CURRENT_MONTH_AMOUNT,
      MOST_SOLD_ITEM_CURRENT_WEEK,
      MOST_SOLD_ITEM_CURRENT_WEEK_AMOUNT,
      MOST_SOLD_ITEM_CURRENT_DAY,
      MOST_SOLD_ITEM_CURRENT_DAY_AMOUNT;

      private PlaceholderType() {
      }
   }

   private static final class PlayerData {
      final UUID uuid;
      final TransactionLog.TimeFrame timeFrame;
      final String path;

      PlayerData(UUID playerId, TransactionLog.TimeFrame timeFrame, String text) {
         this.uuid = playerId;
         this.timeFrame = timeFrame;
         this.path = text;
      }
   }

   private static enum SimpleIndex {
      FIRST(0),
      SECOND(1),
      THIRD(2),
      FOURTH(3),
      FIFTH(4),
      SIXTH(5),
      SEVENTH(6),
      EIGHTH(7),
      NINTH(8),
      TENTH(9);

      public final int index;

      private SimpleIndex(int index) {
         this.index = index;
      }

      public static PAPIExpansion.SimpleIndex get(String text) {
         try {
            return valueOf(text);
         } catch (IllegalArgumentException exception) {
            return null;
         }
      }
   }
}
