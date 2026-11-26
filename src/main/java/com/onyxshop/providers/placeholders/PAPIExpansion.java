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

   public PAPIExpansion(PlaceholderProvider var1) {
      this.provider = var1;
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

   public String onRequest(OfflinePlayer var1, String var2) {
      if (var2.startsWith("buyprice_")) {
         String var14 = var2.replace("buyprice_", "").replace(">", ".");
         return !var14.contains(".") ? "§cInvalid item path for '" + var14 + "'" : this.provider.getItemBuyPrice(var14.split("\\.", 2));
      } else if (var2.startsWith("sellprice_")) {
         String var13 = var2.replace("sellprice_", "").replace(">", ".");
         return !var13.contains(".") ? "§cInvalid item path for '" + var13 + "'" : this.provider.getItemSellPrice(var13.split("\\.", 2));
      } else if (var2.startsWith("discount_")) {
         if (!OnyxShop.getInstance().discountsActive) {
            return "§cDiscounts not enabled";
         } else {
            String var12 = var2.replace("discount_", "");
            return var1 != null && var1.isOnline() ? this.provider.getDiscount(var1.getPlayer(), var12) : "§cInvalid player";
         }
      } else if (var2.startsWith("sell-multiplier_")) {
         if (!OnyxShop.getInstance().multipliers) {
            return "§cSell multipliers not enabled";
         } else {
            String var11 = var2.replace("sell-multiplier_", "");
            return var1 != null && var1.isOnline() ? this.provider.getMultiplier(var1.getPlayer(), var11) : "§cInvalid player";
         }
      } else if (var2.startsWith("shop_rotation_")) {
         String var10 = var2.replace("shop_rotation_", "");
         return this.provider.getShopRotation(var10);
      } else if (var2.startsWith("item_purchases_")) {
         SimplePair var9 = this.getData(var2.replace("item_purchases_", ""));
         if (var9 == null) {
            return "§cInvalid/missing timeframe, use day/week/month";
         } else {
            return var9.value == null
               ? "§cInvalid/missing item path"
               : this.provider.getTotalTransactions(Transaction.Mode.BUY, (TransactionLog.TimeFrame)var9.key, (String)var9.value);
         }
      } else if (var2.startsWith("item_sales_")) {
         SimplePair var8 = this.getData(var2.replace("item_sales_", ""));
         if (var8 == null) {
            return "§cInvalid/missing timeframe, use day/week/month";
         } else {
            return var8.value == null
               ? "§cInvalid/missing item path"
               : this.provider.getTotalTransactions(Transaction.Mode.SELL, (TransactionLog.TimeFrame)var8.key, (String)var8.value);
         }
      } else if (var2.startsWith("player_purchases_")) {
         PAPIExpansion.PlayerData var7 = this.getPlayerData(var2.substring("player_purchases_".length()));
         return var7 == null
            ? "§cInvalid/missing player, timeframe (day/week/month) or item path"
            : this.provider.getTotalTransactions(Transaction.Mode.BUY, var7.timeFrame, var7.path == null ? "" : var7.path, var7.uuid);
      } else if (var2.startsWith("player_sales_")) {
         PAPIExpansion.PlayerData var6 = this.getPlayerData(var2.substring("player_sales_".length()));
         return var6 == null
            ? "§cInvalid/missing player, timeframe (day/week/month) or item path"
            : this.provider.getTotalTransactions(Transaction.Mode.SELL, var6.timeFrame, var6.path == null ? "" : var6.path, var6.uuid);
      } else {
         PlaceholderProvider.PlaceholderCache var3 = this.provider.getPlaceholderCache();
         if (var3 == null) {
            return "§cLoading cache...";
         } else {
            try {
               SimplePair var4 = this.getPlaceholder(var2.toUpperCase().split("_", 2));
               if (var4 == null) {
                  return "Unknown placeholder";
               } else {
                  switch ((PAPIExpansion.PlaceholderType)var4.key) {
                     case PLAYER_MOST_BOUGHT_ITEMS_CURRENT_MONTH:
                        return var3.getTopPlayerBoughtItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)var4.value).index).player.name;
                     case PLAYER_MOST_BOUGHT_ITEMS_CURRENT_MONTH_AMOUNT:
                        return String.valueOf(var3.getTopPlayerBoughtItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)var4.value).index).amount);
                     case PLAYER_MOST_BOUGHT_ITEMS_CURRENT_WEEK:
                        return var3.getTopPlayerBoughtItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)var4.value).index).player.name;
                     case PLAYER_MOST_BOUGHT_ITEMS_CURRENT_WEEK_AMOUNT:
                        return String.valueOf(var3.getTopPlayerBoughtItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)var4.value).index).amount);
                     case PLAYER_MOST_BOUGHT_ITEMS_CURRENT_DAY:
                        return var3.getTopPlayerBoughtItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)var4.value).index).player.name;
                     case PLAYER_MOST_BOUGHT_ITEMS_CURRENT_DAY_AMOUNT:
                        return String.valueOf(var3.getTopPlayerBoughtItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)var4.value).index).amount);
                     case PLAYER_MOST_SOLD_ITEMS_CURRENT_MONTH:
                        return var3.getTopPlayerSoldItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)var4.value).index).player.name;
                     case PLAYER_MOST_SOLD_ITEMS_CURRENT_MONTH_AMOUNT:
                        return String.valueOf(var3.getTopPlayerSoldItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)var4.value).index).amount);
                     case PLAYER_MOST_SOLD_ITEMS_CURRENT_WEEK:
                        return var3.getTopPlayerSoldItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)var4.value).index).player.name;
                     case PLAYER_MOST_SOLD_ITEMS_CURRENT_WEEK_AMOUNT:
                        return String.valueOf(var3.getTopPlayerSoldItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)var4.value).index).amount);
                     case PLAYER_MOST_SOLD_ITEMS_CURRENT_DAY:
                        return var3.getTopPlayerSoldItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)var4.value).index).player.name;
                     case PLAYER_MOST_SOLD_ITEMS_CURRENT_DAY_AMOUNT:
                        return String.valueOf(var3.getTopPlayerSoldItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)var4.value).index).amount);
                     case MOST_BOUGHT_ITEM_CURRENT_MONTH:
                        return var3.getTopBoughtItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)var4.value).index).item.name;
                     case MOST_BOUGHT_ITEM_CURRENT_MONTH_AMOUNT:
                        return String.valueOf(var3.getTopBoughtItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)var4.value).index).amount);
                     case MOST_BOUGHT_ITEM_CURRENT_WEEK:
                        return var3.getTopBoughtItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)var4.value).index).item.name;
                     case MOST_BOUGHT_ITEM_CURRENT_WEEK_AMOUNT:
                        return String.valueOf(var3.getTopBoughtItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)var4.value).index).amount);
                     case MOST_BOUGHT_ITEM_CURRENT_DAY:
                        return var3.getTopBoughtItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)var4.value).index).item.name;
                     case MOST_BOUGHT_ITEM_CURRENT_DAY_AMOUNT:
                        return String.valueOf(var3.getTopBoughtItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)var4.value).index).amount);
                     case MOST_SOLD_ITEM_CURRENT_MONTH:
                        return var3.getTopSoldItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)var4.value).index).item.name;
                     case MOST_SOLD_ITEM_CURRENT_MONTH_AMOUNT:
                        return String.valueOf(var3.getTopSoldItemsCurrentMonth().get(((PAPIExpansion.SimpleIndex)var4.value).index).amount);
                     case MOST_SOLD_ITEM_CURRENT_WEEK:
                        return var3.getTopSoldItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)var4.value).index).item.name;
                     case MOST_SOLD_ITEM_CURRENT_WEEK_AMOUNT:
                        return String.valueOf(var3.getTopSoldItemsCurrentWeek().get(((PAPIExpansion.SimpleIndex)var4.value).index).amount);
                     case MOST_SOLD_ITEM_CURRENT_DAY:
                        return var3.getTopSoldItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)var4.value).index).item.name;
                     case MOST_SOLD_ITEM_CURRENT_DAY_AMOUNT:
                        return String.valueOf(var3.getTopSoldItemsCurrentDay().get(((PAPIExpansion.SimpleIndex)var4.value).index).amount);
                     default:
                        return "Unknown placeholder";
                  }
               }
            } catch (IndexOutOfBoundsException var5) {
               return "null";
            }
         }
      }
   }

   private PAPIExpansion.PlayerData getPlayerData(String var1) {
      String[] var2 = var1.split("_");
      if (var2.length < 2) {
         return null;
      } else {
         for (int var3 = 1; var3 < var2.length; var3++) {
            TransactionLog.TimeFrame var4;
            try {
               var4 = TransactionLog.TimeFrame.valueOf(var2[var3].toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException var7) {
               continue;
            }

            UUID var5 = this.getPlayerUUID(String.join("_", Arrays.copyOfRange(var2, 0, var3)));
            if (var5 != null) {
               String var6 = var3 + 1 < var2.length ? String.join("_", Arrays.copyOfRange(var2, var3 + 1, var2.length)).replace(">", ".") : null;
               return new PAPIExpansion.PlayerData(var5, var4, var6);
            }
         }

         return null;
      }
   }

   private UUID getPlayerUUID(String var1) {
      try {
         return UUID.fromString(var1);
      } catch (IllegalArgumentException var3) {
         return Bukkit.getOfflinePlayer(var1).getUniqueId();
      }
   }

   private SimplePair<TransactionLog.TimeFrame, String> getData(String var1) {
      String[] var2 = var1.split("_", 2);

      try {
         TransactionLog.TimeFrame var3 = TransactionLog.TimeFrame.valueOf(var2[0].toUpperCase(Locale.ENGLISH));
         return new SimplePair<>(var3, var2.length > 1 ? var2[1].replace(">", ".") : null);
      } catch (IllegalArgumentException var4) {
         return null;
      }
   }

   private SimplePair<PAPIExpansion.PlaceholderType, PAPIExpansion.SimpleIndex> getPlaceholder(String... var1) {
      try {
         PAPIExpansion.SimpleIndex var2 = PAPIExpansion.SimpleIndex.get(var1[0]);
         PAPIExpansion.PlaceholderType var3 = PAPIExpansion.PlaceholderType.valueOf(var2 == null ? String.join("_", var1) : var1[1]);
         return new SimplePair<>(var3, var2 == null ? PAPIExpansion.SimpleIndex.FIRST : var2);
      } catch (IllegalArgumentException var4) {
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

      PlayerData(UUID var1, TransactionLog.TimeFrame var2, String var3) {
         this.uuid = var1;
         this.timeFrame = var2;
         this.path = var3;
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

      private SimpleIndex(int nullxx) {
         this.index = nullxx;
      }

      public static PAPIExpansion.SimpleIndex get(String var0) {
         try {
            return valueOf(var0);
         } catch (IllegalArgumentException var2) {
            return null;
         }
      }
   }
}
