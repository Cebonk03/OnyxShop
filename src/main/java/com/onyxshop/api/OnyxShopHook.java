package com.onyxshop.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.api.events.PostTransactionEvent;
import com.onyxshop.api.events.PreTransactionEvent;
import com.onyxshop.api.objects.BuyPrice;
import com.onyxshop.api.objects.PlayerPriceModifier;
import com.onyxshop.api.objects.SellPrice;
import com.onyxshop.api.objects.SellPrices;
import com.onyxshop.api.objects.TransactionResult;
import com.onyxshop.api.prices.AdvancedBuyPrice;
import com.onyxshop.api.prices.AdvancedSellPrice;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.PriceModifierType;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.TransactionMenu;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.providers.EconomyProvider;
import com.onyxshop.util.CommandHelper;
import com.onyxshop.util.CommandType;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.EconomyType;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.PlayerItems;
import com.onyxshop.util.Transaction;
import com.onyxshop.util.cache.Sold;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OnyxShopHook {
   private static OnyxShop plugin;

   public OnyxShopHook(OnyxShop var1) {
      plugin = var1;
   }

   public static boolean isSellAble(ShopItem var0) {
      return var0.isSellAble();
   }

   @Deprecated
   @Nullable
   public static Double getItemSellPrice(ItemStack var0) {
      ShopItem var1 = plugin.createItem.matchShopItem(var0);
      return var1 == null ? null : var1.getSellPrice(var0);
   }

   @Deprecated
   @Nullable
   public static Double getItemSellPrice(Player var0, ItemStack var1) {
      ShopItem var2 = plugin.createItem.matchShopItem(var0, var1, Transaction.Mode.SELL);
      return var2 == null ? null : var2.getSellPrice(var0, var1);
   }

   @Deprecated
   @Nullable
   public static Double getItemBuyPrice(ItemStack var0) {
      ShopItem var1 = plugin.createItem.matchShopItem(var0);
      return var1 == null ? null : var1.getBuyPrice(var0.getAmount());
   }

   @Deprecated
   @Nullable
   public static Double getItemBuyPrice(Player var0, ItemStack var1) {
      ShopItem var2 = plugin.createItem.matchShopItem(var0, var1, Transaction.Mode.BUY);
      return var2 == null ? null : var2.getBuyPrice(var0, var1.getAmount());
   }

   public static Double getItemSellPrice(ShopItem var0, ItemStack var1) {
      return var0.getSellPrice(var1);
   }

   public static Double getItemSellPrice(ShopItem var0, ItemStack var1, int var2, int var3) {
      return var0.getSellPrice(var1, var2, var3);
   }

   public static Double getItemSellPrice(ShopItem var0, ItemStack var1, Player var2) {
      return var0.getSellPrice(var2, var1);
   }

   public static Double getItemSellPrice(ShopItem var0, ItemStack var1, Player var2, int var3, int var4) {
      return var0.getSellPrice(var2, var1, var3, var4);
   }

   public static Optional<SellPrice> getSellPrice(OfflinePlayer var0, ItemStack var1) {
      Objects.requireNonNull(var1, "Item cannot be null");
      Objects.requireNonNull(var0, "Player cannot be null");
      boolean var2 = var0.isOnline();
      int var3 = var1.getAmount();
      ShopItem var4 = !var2 ? getShopItem(var1) : plugin.createItem.matchShopItem((Player)var0, var1, Transaction.Mode.SELL);
      if (var4 == null || !var4.isSellAble()) {
         return Optional.empty();
      } else if (var4.isMaxSell(var3)) {
         return Optional.empty();
      } else if (var4.isMinSell(var3)) {
         return Optional.empty();
      } else {
         if (var4.getLimitedSellMode() != 0) {
            int var5 = getSellLimit(var4, var0.getUniqueId());
            if (var3 > var5) {
               return Optional.empty();
            }
         }

         SellPrice var8;
         if (var4.isASellPricing()) {
            Map var6 = !var2
               ? var4.getASellPrice().getSellPrices(var4.getASellPrice().isAll() ? null : var4.getASellPrice().getTypes().get(0), var1, var3, 0)
               : var4.getASellPrice()
                  .getSellPrices(
                     var4.getASellPrice().isAll() ? null : var4.getASellPrice().getTypes().get(0), (Player)var0, var1, var3, 0, var4.getSellModifiers()
                  );
            var8 = new SellPrice(var0, var3, var4, var6);
         } else {
            double var9 = !var2 ? var4.getSellPrice(var1, var3, 0) : var4.getSellPrice((Player)var0, var1, var3, 0);
            var8 = new SellPrice(var0, var3, var4, var4.getEcoType(), var9);
         }

         return Optional.of(var8);
      }
   }

   public static SellPrices getSellPrices(OfflinePlayer var0, ItemStack... var1) {
      Objects.requireNonNull(var0, "Player cannot be null");
      Objects.requireNonNull(var1, "Items cannot be null");
      boolean var2 = var0.isOnline();
      HashMap<ShopItem, Sold> var3 = new HashMap<>();
      HashMap<EcoType, Double> var4 = new HashMap<>();

      for (ItemStack var8 : var1) {
         if (var8 != null && var8.getType() != Material.AIR) {
            ShopItem var9 = !var2 ? plugin.createItem.matchShopItem(var8) : plugin.createItem.matchShopItem((Player)var0, var8, Transaction.Mode.SELL);
            if (var9 != null && var9.isSellAble()) {
               Sold var10 = var3.computeIfAbsent(var9, var0x -> new Sold());
               int var11 = var10.getAmount();
               if (!var9.isMaxSell(var8.getAmount() + var11)) {
                  if (var9.getLimitedSellMode() != 0) {
                     int var12 = getSellLimit(var9, var0.getUniqueId());
                     if (var8.getAmount() + var11 > var12) {
                        continue;
                     }
                  }

                  Map<EcoType, Double> var17;
                  if (var9.isASellPricing()) {
                     var17 = !var2
                        ? var9.getASellPrice()
                           .getSellPrices(var9.getASellPrice().isAll() ? null : var9.getASellPrice().getTypes().get(0), var8, var8.getAmount(), var11)
                        : var9.getASellPrice()
                           .getSellPrices(
                              var9.getASellPrice().isAll() ? null : var9.getASellPrice().getTypes().get(0),
                              (Player)var0,
                              var8,
                              var8.getAmount(),
                              var11,
                              var9.getSellModifiers()
                           );
                     var17.forEach((var1x, var2x) -> var4.merge(var1x, var2x, Double::sum));
                  } else {
                     double var13 = !var2 ? var9.getSellPrice(var8, var8.getAmount(), var11) : var9.getSellPrice((Player)var0, var8, var8.getAmount(), var11);
                     var4.merge(var9.getEcoType(), var13, Double::sum);
                     var17 = Collections.singletonMap(var9.getEcoType(), var13);
                  }

                  var10.addValues(var8.getAmount(), var17);
               }
            }
         }
      }

      for (Entry<ShopItem, Sold> var16 : new HashSet<>(var3.entrySet())) {
         if (((ShopItem)var16.getKey()).isMinSell(((Sold)var16.getValue()).getAmount())) {
            var3.remove(var16.getKey());
            ((Sold)var16.getValue()).getPrices().forEach((var1x, var2x) -> var4.compute(var1x, (var1xx, var2xx) -> {
               double var3x = var2xx - var2x;
               return var3x <= 0.0 ? null : var3x;
            }));
         }
      }

      return new SellPrices(var0, var3.entrySet().stream().collect(Collectors.toMap(Entry::getKey, var0x -> ((Sold)var0x.getValue()).getAmount())), var4);
   }

   public static SellPrices getCutSellPrices(OfflinePlayer var0, ItemStack[] var1, boolean var2) {
      Objects.requireNonNull(var0, "Player cannot be null");
      Objects.requireNonNull(var1, "Items cannot be null");
      boolean var3 = var0.isOnline();
      HashMap<ShopItem, Sold> var4 = new HashMap<>();
      HashMap<EcoType, Double> var5 = new HashMap<>();

      for (int var6 = 0; var6 < var1.length; var6++) {
         ItemStack var7 = var1[var6];
         if (var7 != null && var7.getType() != Material.AIR) {
            ShopItem var8 = !var3 ? plugin.createItem.matchShopItem(var7) : plugin.createItem.matchShopItem((Player)var0, var7, Transaction.Mode.SELL);
            if (var8 != null && var8.isSellAble()) {
               Sold var9 = var4.computeIfAbsent(var8, var0x -> new Sold());
               int var10 = var9.getAmount();
               int var11 = getMaxSell(var8, var7.getAmount(), var10);
               if (var11 >= 1) {
                  var11 = getSellLimit(var8, var0.getUniqueId(), var11, var10);
                  if (var11 >= 1) {
                     Map<EcoType, Double> var12;
                     if (var8.isASellPricing()) {
                        var12 = !var3
                           ? var8.getASellPrice()
                              .getSellPrices(var8.getASellPrice().isAll() ? null : var8.getASellPrice().getTypes().get(0), var7, var11, var10)
                           : var8.getASellPrice()
                              .getSellPrices(
                                 var8.getASellPrice().isAll() ? null : var8.getASellPrice().getTypes().get(0),
                                 (Player)var0,
                                 var7,
                                 var11,
                                 var10,
                                 var8.getSellModifiers()
                              );
                        var12.forEach((var1x, var2x) -> var5.merge(var1x, var2x, Double::sum));
                     } else {
                        double var13 = !var3 ? var8.getSellPrice(var7, var11, var10) : var8.getSellPrice((Player)var0, var7, var11, var10);
                        var5.merge(var8.getEcoType(), var13, Double::sum);
                        var12 = Collections.singletonMap(var8.getEcoType(), var13);
                     }

                     var9.addValues(var11, var12);
                     if (!var8.isSellCommand() || var8.isRequireItem()) {
                        var9.addIndex(var6);
                     }
                  }
               }
            }
         }
      }

      for (Entry<ShopItem, Sold> var16 : new HashSet<>(var4.entrySet())) {
         if (((ShopItem)var16.getKey()).isMinSell(((Sold)var16.getValue()).getAmount())) {
            var4.remove(var16.getKey());
            ((Sold)var16.getValue()).getPrices().forEach((var1x, var2x) -> var5.compute(var1x, (var1xx, var2xx) -> {
               double var3x = var2xx - var2x;
               return var3x <= 0.0 ? null : var3x;
            }));
         } else if (var2) {
            int var17 = ((Sold)var16.getValue()).getAmount();

            for (Integer var19 : ((Sold)var16.getValue()).getIndexes()) {
               if (var17 < var1[var19].getAmount()) {
                  ItemStack var21 = new ItemStack(var1[var19]);
                  var21.setAmount(var21.getAmount() - var17);
                  var1[var19] = var21;
                  break;
               }

               var17 -= var1[var19].getAmount();
               var1[var19] = null;
            }
         }
      }

      return new SellPrices(var0, var4.entrySet().stream().collect(Collectors.toMap(Entry::getKey, var0x -> ((Sold)var0x.getValue()).getAmount())), var5);
   }

   @Deprecated
   public static TransactionResult sellItem(Player var0, ShopItem var1, int var2) {
      return sellItem(var0, var1, var2, false, false, true, false);
   }

   public static TransactionResult sellItem(Player var0, ShopItem var1, int var2, @NotNull String var3) {
      return sellItem(var0, var1, var2, false, false, true, false, var3);
   }

   @Deprecated
   public static TransactionResult sellItem(Player var0, ShopItem var1, int var2, boolean var3, boolean var4) {
      return sellItem(var0, var1, var2, var3, var4, true, false);
   }

   public static TransactionResult sellItem(Player var0, ShopItem var1, int var2, boolean var3, boolean var4, @NotNull String var5) {
      return sellItem(var0, var1, var2, var3, var4, true, false, var5);
   }

   @Deprecated
   public static TransactionResult sellItem(Player var0, ShopItem var1, int var2, boolean var3, boolean var4, boolean var5, boolean var6) {
      return sellItem(var0, var1, var2, var3, var4, var5, var6, "Unknown");
   }

   public static TransactionResult sellItem(Player var0, ShopItem var1, int var2, boolean var3, boolean var4, boolean var5, boolean var6, @NotNull String var7) {
      Objects.requireNonNull(var0, "Player cannot be null");
      Objects.requireNonNull(var1, "ShopItem cannot be null");
      if (var2 < 1) {
         throw new NullPointerException("Amount cannot be less then 1");
      } else if (!var0.isOnline()) {
         throw new NullPointerException("Target player should be online");
      } else if (var1.hasItemError()) {
         return new TransactionResult(Transaction.Result.ITEM_ERROR);
      } else if (var1.isDisplayItem()) {
         return new TransactionResult(Transaction.Result.DISPLAY_ITEM);
      } else if (!var1.isSellAble()) {
         return new TransactionResult(Transaction.Result.NEGATIVE_ITEM_PRICE);
      } else if (!var3 || PermissionsCache.hasPermission(var0, "OnyxShop.shop." + var1.section) && var1.canPurchase(var0, "shop", !var6)) {
         if (var4 && !var1.meetsRequirements(var0, !var6)) {
            return new TransactionResult(Transaction.Result.REQUIREMENTS_FAILED);
         } else {
            ArrayList<ItemStack> var8 = null;
            HashMap<EcoType, Double> var9 = new HashMap<>();
            if (var5
               ? PlayerItems.canSell(var1, var0)
               : (var8 = PlayerItems.getSellAble(var1, var0, var2, false)).stream().mapToInt(ItemStack::getAmount).sum() >= var2) {
               if (var1.getLimitedSellMode() != 0) {
                  int var10 = getSellLimit(var1, var0.getUniqueId());
                  if (var5) {
                     var2 = Math.min(var2, var10);
                  }

                  if (var5 ? var2 <= 0 : var10 < var2) {
                     if (var6) {
                        SendMessage.chatToPlayer(var0, Lang.LIMITED_ITEM_SELL_AMOUNT.get(var0));
                     }

                     callPostS(var1, var0, var2, var9, Transaction.Result.REACHED_SELL_LIMIT);
                     return new TransactionResult(Transaction.Result.REACHED_SELL_LIMIT);
                  }
               }

               var2 = var5 ? (var8 = PlayerItems.getSellAble(var1, var0, var2, true)).stream().mapToInt(ItemStack::getAmount).sum() : var2;
               if (!var5 && var1.isMaxSell(var2)) {
                  callPostS(var1, var0, var2, var9, Transaction.Result.TO_MANY_ITEMS);
                  return new TransactionResult(Transaction.Result.TO_MANY_ITEMS);
               } else if (var1.isMinSell(var2)) {
                  if (var6) {
                     SendMessage.chatToPlayer(
                        var0,
                        Lang.NOT_ENOUGH_ITEMS_TO_SELL
                           .get(var0)
                           .replace("%min-sell%", String.valueOf(var1.getMinSell()))
                           .replace("%quantity%", String.valueOf(var2))
                     );
                  }

                  callPostS(var1, var0, var2, var9, Transaction.Result.NOT_ENOUGH_ITEMS);
                  return new TransactionResult(Transaction.Result.NOT_ENOUGH_ITEMS);
               } else {
                  int var17 = 0;

                  for (ItemStack var12 : var8) {
                     if (var1.isASellPricing()) {
                        for (Entry<EcoType, Double> var14 : var1.getASellPrice()
                           .getSellPrices(null, var0, var12, var12.getAmount(), var17 += var12.getAmount(), var1.getSellModifiers())
                           .entrySet()) {
                           var9.merge((EcoType)var14.getKey(), (Double)var14.getValue(), Double::sum);
                        }
                     } else {
                        var9.merge(var1.getEcoType(), var1.getSellPrice(var0, var12, var12.getAmount(), var17 += var12.getAmount()), Double::sum);
                     }
                  }

                  if (var9.entrySet()
                     .stream()
                     .anyMatch(
                        var1x -> var1x.getKey().getType() == EconomyType.ITEM
                           && !PlayerItems.canPay(var0, var1x.getKey(), var1x.getValue())
                     )) {
                     if (var6) {
                        SendMessage.chatToPlayer(var0, Lang.CANNOT_STORE_PAYMENT.get(var0));
                     }

                     callPostS(var1, var0, var2, var9, Transaction.Result.CANT_STORE_PAYMENT);
                     return new TransactionResult(Transaction.Result.CANT_STORE_PAYMENT);
                  } else {
                     PreTransactionEvent var18 = new PreTransactionEvent(Collections.singletonMap(var1, var2), var9, var0, var2, Transaction.Type.API_SELL);
                     Bukkit.getPluginManager().callEvent(var18);
                     Map var16 = var18.getPrices();
                     if (var18.isCancelled()) {
                        callPostS(var1, var0, var2, var16, Transaction.Result.TRANSACTION_CANCELLED);
                        return new TransactionResult(Transaction.Result.TRANSACTION_CANCELLED);
                     } else {
                        plugin.getEcoHandler().deposit(var16, var0);
                        if (var1.isSellCommand()) {
                           CommandHelper.executeCommandItem(var1, var0, CommandType.SELL, var16.toString(), var2);
                        }

                        if (!var1.isSellCommand() || var1.isRequireItem()) {
                           var8.forEach(var1x -> var0.getInventory().removeItem(new ItemStack[]{var1x}));
                        }

                        if (var1.getLimitedSellMode() != 0) {
                           sellItemLimit(var1, var0.getUniqueId(), var2);
                        }

                        if (var1.isRefillStock()) {
                           sellItemStock(var1, var0.getUniqueId(), var2);
                        }

                        if (var1.isDynamicPricing()) {
                           sellItem(var1, var2);
                        }

                        if (var6) {
                           SendMessage.sendTransactionMessage(
                              var0,
                              var2,
                              var16,
                              var1,
                              Transaction.Mode.SELL,
                              Transaction.Type.API_SELL.withCustomType(var7),
                              var1.isSellCommand() && !var1.isRequireItem()
                           );
                        }

                        callPostS(var1, var0, var2, var16, var1.isSellCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS);
                        return new TransactionResult(
                           var1.isSellCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS, var16, var2
                        );
                     }
                  }
               }
            } else {
               if (var6) {
                  SendMessage.chatToPlayer(var0, Lang.NO_ITEM_FOUND.get(var0));
               }

               callPostS(var1, var0, var2, var9, Transaction.Result.NO_ITEMS_FOUND);
               return new TransactionResult(Transaction.Result.NO_ITEMS_FOUND);
            }
         }
      } else {
         return new TransactionResult(Transaction.Result.NO_PERMISSIONS);
      }
   }

   public static boolean isBuyAble(ShopItem var0) {
      return var0.isBuyAble();
   }

   @Deprecated
   public static Double getItemBuyPrice(ShopItem var0, ItemStack var1) {
      return var0.getBuyPrice(var1.getAmount());
   }

   @Deprecated
   public static Double getItemBuyPrice(ShopItem var0, ItemStack var1, Player var2) {
      return var0.getBuyPrice(var2, var1.getAmount());
   }

   public static double getItemBuyPrice(ShopItem var0, int var1) {
      return var0.getBuyPrice(var1);
   }

   public static double getItemBuyPrice(ShopItem var0, Player var1, int var2) {
      return var0.getBuyPrice(var1, var2);
   }

   public static Optional<BuyPrice> getBuyPrice(OfflinePlayer var0, ItemStack var1) {
      Objects.requireNonNull(var1, "Item cannot be null");
      Objects.requireNonNull(var0, "Player cannot be null");
      boolean var2 = var0.isOnline();
      int var3 = var1.getAmount();
      ShopItem var4 = !var2 ? getShopItem(var1) : plugin.createItem.matchShopItem((Player)var0, var1, Transaction.Mode.BUY);
      if (var4 == null || !var4.isBuyAble()) {
         return Optional.empty();
      } else if (var4.isMaxBuy(var1.getAmount())) {
         return Optional.empty();
      } else if (var4.isMinBuy(var1.getAmount())) {
         return Optional.empty();
      } else {
         if (var4.getLimitedStockMode() != 0) {
            int var5 = getItemStock(var4, var0.getUniqueId());
            if (var3 > var5) {
               return Optional.empty();
            }
         }

         BuyPrice var8;
         if (var4.isABuyPricing()) {
            Map var6 = !var2
               ? var4.getABuyPrice().getBuyPrices(var4.getABuyPrice().isAll() ? null : var4.getABuyPrice().getTypes().get(0), var3)
               : var4.getABuyPrice()
                  .getBuyPrices(var4.getABuyPrice().isAll() ? null : var4.getABuyPrice().getTypes().get(0), (Player)var0, var3, var4.getBuyModifiers());
            var8 = new BuyPrice(var0, var3, var4, var6);
         } else {
            double var9 = !var2 ? var4.getBuyPrice(var3) : var4.getBuyPrice((Player)var0, var3);
            var8 = new BuyPrice(var0, var3, var4, var4.getEcoType(), var9);
         }

         return Optional.of(var8);
      }
   }

   public static boolean hasMultipleBuyPrices(ShopItem var0) {
      return var0.isABuyPricing();
   }

   public static AdvancedBuyPrice getMultipleBuyPrices(ShopItem var0) {
      return new AdvancedBuyPrice(var0);
   }

   public static boolean hasMultipleSellPrices(ShopItem var0) {
      return var0.isASellPricing();
   }

   public static AdvancedSellPrice getMultipleSellPrices(ShopItem var0) {
      return new AdvancedSellPrice(var0);
   }

   @Deprecated
   public static TransactionResult purchaseItem(Player var0, ShopItem var1, int var2) {
      return purchaseItem(var0, var1, var2, false, false, true, false);
   }

   public static TransactionResult purchaseItem(Player var0, ShopItem var1, int var2, @NotNull String var3) {
      return purchaseItem(var0, var1, var2, false, false, true, false, var3);
   }

   @Deprecated
   public static TransactionResult purchaseItem(Player var0, ShopItem var1, int var2, boolean var3, boolean var4) {
      return purchaseItem(var0, var1, var2, var3, var4, true, false);
   }

   public static TransactionResult purchaseItem(Player var0, ShopItem var1, int var2, boolean var3, boolean var4, @NotNull String var5) {
      return purchaseItem(var0, var1, var2, var3, var4, true, false, var5);
   }

   @Deprecated
   public static TransactionResult purchaseItem(Player var0, ShopItem var1, int var2, boolean var3, boolean var4, boolean var5, boolean var6) {
      return purchaseItem(var0, var1, var2, var3, var4, var5, var6, "Unknown");
   }

   public static TransactionResult purchaseItem(
      Player var0, ShopItem var1, int var2, boolean var3, boolean var4, boolean var5, boolean var6, @NotNull String var7
   ) {
      Objects.requireNonNull(var0, "Player cannot be null");
      Objects.requireNonNull(var1, "ShopItem cannot be null");
      if (var2 < 1) {
         throw new NullPointerException("Amount cannot be less then 1");
      } else if (!var0.isOnline()) {
         throw new NullPointerException("Target player should be online");
      } else if (var1.hasItemError()) {
         return new TransactionResult(Transaction.Result.ITEM_ERROR);
      } else if (var1.isDisplayItem()) {
         return new TransactionResult(Transaction.Result.DISPLAY_ITEM);
      } else if (!var1.isBuyAble()) {
         return new TransactionResult(Transaction.Result.NEGATIVE_ITEM_PRICE);
      } else if (!var3 || PermissionsCache.hasPermission(var0, "OnyxShop.shop." + var1.section) && var1.canPurchase(var0, "shop", !var6)) {
         if (var4 && !var1.meetsRequirements(var0, !var6)) {
            return new TransactionResult(Transaction.Result.REQUIREMENTS_FAILED);
         } else {
            HashMap var8 = new HashMap();
            if (var5 ? PlayerItems.canBuy(var0, var1) : PlayerItems.getFinalAmount(var1, var0, var2, false) >= var2) {
               var2 = var5 ? PlayerItems.getFinalAmount(var1, var0, var2, true) : var2;
               if (!var5 && var1.isMaxBuy(var2)) {
                  callPostB(var1, var0, var2, var8, Transaction.Result.TO_MANY_ITEMS);
                  return new TransactionResult(Transaction.Result.TO_MANY_ITEMS);
               } else if (var1.isMinBuy(var2)) {
                  if (var6) {
                     SendMessage.chatToPlayer(var0, Lang.NOT_ENOUGH_ITEMS_TO_BUY.get(var0).replace("%min-buy%", String.valueOf(var1.getMinBuy())));
                  }

                  callPostB(var1, var0, var2, var8, Transaction.Result.NOT_ENOUGH_ITEMS);
                  return new TransactionResult(Transaction.Result.NOT_ENOUGH_ITEMS);
               } else {
                  if (var1.getLimitedStockMode() != 0) {
                     int var9 = getItemStock(var1, var0.getUniqueId());
                     if (var5) {
                        var2 = Math.min(var2, var9);
                     }

                     if (var5 ? var2 <= 0 : var9 < var2) {
                        if (var6) {
                           SendMessage.chatToPlayer(var0, Lang.NOT_ENOUGH_STOCK_AVAILABLE.get(var0));
                           plugin.getSoundEffects().insufficientStock(var0);
                        }

                        callPostB(var1, var0, var2, var8, Transaction.Result.NO_ITEM_STOCK_LEFT);
                        return new TransactionResult(Transaction.Result.NO_ITEM_STOCK_LEFT);
                     }
                  }

                  PreTransactionEvent var13 = new PreTransactionEvent(
                     Collections.singletonMap(var1, var2),
                     var1.isABuyPricing()
                        ? var1.getABuyPrice().getBuyPrices(null, var0, var2, var1.getBuyModifiers())
                        : Collections.singletonMap(var1.getEcoType(), var1.getBuyPrice(var0, var2)),
                     var0,
                     var2,
                     Transaction.Type.API_BUY
                  );
                  Bukkit.getPluginManager().callEvent(var13);
                  Map var12 = var13.getPrices();
                  if (var13.isCancelled()) {
                     callPostB(var1, var0, var2, var12, Transaction.Result.TRANSACTION_CANCELLED);
                     return new TransactionResult(Transaction.Result.TRANSACTION_CANCELLED);
                  } else if (!arePricesValid(var12)) {
                     callPostB(var1, var0, var2, var12, Transaction.Result.NEGATIVE_ITEM_PRICE);
                     return new TransactionResult(Transaction.Result.NEGATIVE_ITEM_PRICE, var12, var2);
                  } else {
                     Map var10 = plugin.getEcoHandler().has(var12, var0);
                     if (var10.size() != 0) {
                        if (var6) {
                           SendMessage.chatToPlayer(
                              var0,
                              Lang.INSUFFICIENT_MONEY
                                 .get(var0)
                                 .replace(
                                    "%currency%",
                                    var1.isABuyPricing()
                                       ? plugin.formatPrices(
                                          Lang.INSUFFICIENT_MONEY.get(var0).getLegacy().split("%currency%")[0], var10.keySet(), var1.getABuyPrice().isAll()
                                       )
                                       : plugin.getEcoHandler().getEcon(var1.getEcoType()).getFriendly()
                                 )
                           );
                           plugin.getSoundEffects().insufficientFunds(var0);
                        }

                        callPostB(var1, var0, var2, var12, Transaction.Result.INSUFFICIENT_FUNDS);
                        return new TransactionResult(Transaction.Result.INSUFFICIENT_FUNDS);
                     } else if (!plugin.getEcoHandler().withdraw(var12, var0)) {
                        if (var6) {
                           SendMessage.chatToPlayer(
                              var0,
                              Lang.INSUFFICIENT_MONEY
                                 .get(var0)
                                 .replace(
                                    "%currency%",
                                    var1.isABuyPricing()
                                       ? plugin.formatPrices(
                                          Lang.INSUFFICIENT_MONEY.get(var0).getLegacy().split("%currency%")[0], var12.keySet(), var1.getABuyPrice().isAll()
                                       )
                                       : plugin.getEcoHandler().getEcon(var1.getEcoType()).getFriendly()
                                 )
                           );
                           plugin.getSoundEffects().insufficientFunds(var0);
                        }

                        callPostB(var1, var0, var2, var12, Transaction.Result.INSUFFICIENT_FUNDS);
                        return new TransactionResult(Transaction.Result.INSUFFICIENT_FUNDS, var12, var2);
                     } else {
                        if (var1.isBuyCommand()) {
                           CommandHelper.executeCommandItem(var1, var0, CommandType.BUY, var12.toString(), var2);
                        }

                        if (!var1.isBuyCommand() || var1.isGiveItem()) {
                           PlayerItems.giveItems(var0, var1, var2);
                        }

                        if (var1.getLimitedStockMode() != 0) {
                           buyItemStock(var1, var0.getUniqueId(), var2);
                        }

                        if (var1.isDynamicPricing()) {
                           buyItem(var1, var2);
                        }

                        if (var6) {
                           SendMessage.sendTransactionMessage(
                              var0,
                              var2,
                              var12,
                              var1,
                              Transaction.Mode.BUY,
                              Transaction.Type.API_BUY.withCustomType(var7),
                              var1.isBuyCommand() && !var1.isGiveItem()
                           );
                        }

                        callPostB(var1, var0, var2, var12, var1.isBuyCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS);
                        return new TransactionResult(
                           var1.isBuyCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS, var12, var2
                        );
                     }
                  }
               }
            } else {
               if (var6) {
                  SendMessage.chatToPlayer(var0, Lang.MORE_SPACE_NEEDED.get(var0));
               }

               callPostB(var1, var0, var2, var8, Transaction.Result.NO_INVENTORY_SPACE);
               return new TransactionResult(Transaction.Result.NO_INVENTORY_SPACE);
            }
         }
      } else {
         return new TransactionResult(Transaction.Result.NO_PERMISSIONS);
      }
   }

   public static void setSectionPriceModifier(UUID var0, String var1, PriceModifierType var2, double var3) {
      plugin.getModifierManager().setCommandSectionModifier(var0, var1, var2, var3 < 0.0 ? 0.0 : var3);
   }

   public static void setItemPriceModifier(UUID var0, String var1, PriceModifierType var2, double var3) {
      plugin.getModifierManager().setCommandItemModifier(var0, var1, var2, var3 < 0.0 ? 0.0 : var3);
   }

   public static void removeSectionModifier(UUID var0, String var1, PriceModifierType var2) {
      plugin.getModifierManager().removeCommandSectionModifier(var0, var1, var2);
   }

   public static void removeItemModifier(UUID var0, String var1, PriceModifierType var2) {
      plugin.getModifierManager().removeCommandItemModifier(var0, var1, var2);
   }

   @Nullable
   public static PlayerPriceModifier getPlayerPriceModifiers(UUID var0) {
      com.onyxshop.objects.PlayerPriceModifier var1 = plugin.getModifierManager().getCommandModifiersForPlayer(var0);
      return var1 != null ? new PlayerPriceModifier(var1) : null;
   }

   @Nullable
   public static PlayerPriceModifier loadPlayerPriceModifiers(UUID var0) {
      com.onyxshop.objects.PlayerPriceModifier var1 = plugin.getModifierManager().loadCommandModifiersForPlayer(var0);
      return var1 != null ? new PlayerPriceModifier(var1) : null;
   }

   public static EconomyProvider getEcon(@Nullable EcoType var0) {
      return plugin.getEcoHandler().getEcon(var0);
   }

   public static void logTransaction(UUID var0, ShopItem var1, Map<EcoType, Double> var2, int var3, Transaction.Mode var4, @NotNull String var5) {
      plugin.getTransactionLog()
         .log(var0, var1, var2, var3, var4, (var4 == Transaction.Mode.BUY ? Transaction.Type.API_BUY : Transaction.Type.API_SELL).withCustomType(var5));
   }

   public static void logTransaction(UUID var0, Map<ShopItem, Integer> var1, Map<EcoType, Double> var2, int var3, Transaction.Mode var4, @NotNull String var5) {
      plugin.getTransactionLog()
         .log(var0, var1, var2, var3, var4, (var4 == Transaction.Mode.BUY ? Transaction.Type.API_BUY : Transaction.Type.API_SELL).withCustomType(var5));
   }

   @Deprecated
   public static void buyItem(ItemStack var0, int var1) {
      if (plugin.dynamicPricing) {
         plugin.getDynamicPricingProvider().buyItem(plugin.createItem.matchShopItem(var0).getItemPath(), var1);
      }
   }

   @Deprecated
   public static void sellItem(ItemStack var0, int var1) {
      if (plugin.dynamicPricing) {
         plugin.getDynamicPricingProvider().sellItem(plugin.createItem.matchShopItem(var0).getItemPath(), var1);
      }
   }

   public static void buyItem(ShopItem var0, int var1) {
      if (plugin.dynamicPricing) {
         plugin.getDynamicPricingProvider().buyItem(var0.getItemPath(), var1);
      }
   }

   public static void sellItem(ShopItem var0, int var1) {
      if (plugin.dynamicPricing) {
         plugin.getDynamicPricingProvider().sellItem(var0.getItemPath(), var1);
      }
   }

   public static void openTransactionScreen(Player var0, String var1, String var2, Transaction.Mode var3) {
      ShopSection var4 = plugin.getSection(var1);
      if (var4 == null) {
         throw new NullPointerException("A section with name '" + var1 + "' does not exist");
      } else {
         ShopItem var5 = var4.getShopItem(var1 + "." + var2);
         if (var5 == null) {
            throw new NullPointerException("The shop item at section " + var1 + " with index " + var2 + " does not exist");
         } else {
            openTransactionScreen(var0, var5, var3);
         }
      }
   }

   public static void openTransactionScreen(Player var0, ShopItem var1, Transaction.Mode var2) {
      new TransactionMenu(var0, var1, null, true, var2, var2 == Transaction.Mode.BUY ? Transaction.Type.BUY_SCREEN : Transaction.Type.SELL_SCREEN, 1)
         .open(false);
   }

   @Nullable
   public static ShopItem getShopItem(ItemStack var0) {
      return plugin.createItem.matchShopItem(var0);
   }

   @Nullable
   public static ShopItem getShopItem(Player var0, ItemStack var1) {
      return plugin.createItem.matchShopItem(var0, var1);
   }

   @Nullable
   public static ShopItem getShopItem(String var0) {
      try {
         ShopSection var1 = plugin.getSection(var0.split("\\.")[0]);
         return var1.getShopItem(var0);
      } catch (Exception var2) {
         return null;
      }
   }

   @Nullable
   public static ShopSection getShopSection(String var0) {
      return plugin.getSection(var0);
   }

   public static List<String> getShopSections() {
      return plugin.getShopSections();
   }

   public static Map<String, ShopSection> getSections() {
      return plugin.getSections();
   }

   public static int getItemStock(ShopItem var0, @Nullable UUID var1) {
      return plugin.getLimitedStockProvider().getItemStock(var0, var1, true);
   }

   public static Long getItemStockRestockTime(ShopItem var0, @Nullable UUID var1) {
      return plugin.getLimitedStockProvider().getTimeLeftToRestock(var0, var1);
   }

   public static int buyItemStock(ShopItem var0, @Nullable UUID var1, int var2) {
      return plugin.getLimitedStockProvider().buyItemsSafe(var0, var1, var2);
   }

   public static void sellItemStock(ShopItem var0, @Nullable UUID var1, int var2) {
      plugin.getLimitedStockProvider().sellItems(var0, var1, var2);
   }

   public static int getSellLimit(ShopItem var0, @Nullable UUID var1) {
      return plugin.getLimitedSellProvider().getItemLimit(var0, var1, true);
   }

   public static Long getSellLimitRestockTime(ShopItem var0, @Nullable UUID var1) {
      return plugin.getLimitedSellProvider().getTimeLeftToRestock(var0, var1);
   }

   public static int sellItemLimit(ShopItem var0, @Nullable UUID var1, int var2) {
      return plugin.getLimitedSellProvider().sellItemsSafe(var0, var1, var2, true);
   }

   public static boolean hasPermissions(ShopItem var0, Player var1) {
      return var0.hasPermission(var1, "shop", true);
   }

   public static boolean hasPermissions(ShopItem var0, Player var1, String var2) {
      return var0.hasPermission(var1, var2, true);
   }

   private static int getMaxSell(ShopItem var0, int var1, int var2) {
      if (var0.getMaxSell() > 0) {
         if (var0.isMaxSell(var2)) {
            return -1;
         }

         var1 = Math.min(var1, var0.getMaxSell() - var2);
      }

      return var1;
   }

   private static int getSellLimit(ShopItem var0, UUID var1, int var2, int var3) {
      if (var0.getLimitedSellMode() != 0) {
         int var4 = getSellLimit(var0, var1);
         if (var4 <= var3) {
            return -1;
         }

         if (var4 < var2 + var3) {
            var2 = var4 - var3;
         }
      }

      return var2;
   }

   private static boolean arePricesValid(Map<EcoType, Double> var0) {
      for (Double var1 : var0.values()) {
         if (var1 == null || !(var1 >= 0.0)) {
            return false;
         }
      }

      return true;
   }

   private static void callPostB(ShopItem var0, Player var1, int var2, Map<EcoType, Double> var3, Transaction.Result var4) {
      callPost(var0, var1, var2, var3, Transaction.Type.API_BUY, var4);
   }

   private static void callPostS(ShopItem var0, Player var1, int var2, Map<EcoType, Double> var3, Transaction.Result var4) {
      callPost(var0, var1, var2, var3, Transaction.Type.API_SELL, var4);
   }

   private static void callPost(ShopItem var0, Player var1, int var2, Map<EcoType, Double> var3, Transaction.Type var4, Transaction.Result var5) {
      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(Collections.singletonMap(var0, var2), var3, var1, var2, var4, var5));
   }
}
