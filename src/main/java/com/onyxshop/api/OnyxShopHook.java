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

   public OnyxShopHook(OnyxShop plugin2) {
      plugin = plugin2;
   }

   public static boolean isSellAble(ShopItem shopItem) {
      return shopItem.isSellAble();
   }

   @Deprecated
   @Nullable
   public static Double getItemSellPrice(ItemStack itemStack) {
      ShopItem shopItem = plugin.createItem.matchShopItem(itemStack);
      return shopItem == null ? null : shopItem.getSellPrice(itemStack);
   }

   @Deprecated
   @Nullable
   public static Double getItemSellPrice(Player player, ItemStack itemStack) {
      ShopItem shopItem = plugin.createItem.matchShopItem(player, itemStack, Transaction.Mode.SELL);
      return shopItem == null ? null : shopItem.getSellPrice(player, itemStack);
   }

   @Deprecated
   @Nullable
   public static Double getItemBuyPrice(ItemStack itemStack) {
      ShopItem shopItem = plugin.createItem.matchShopItem(itemStack);
      return shopItem == null ? null : shopItem.getBuyPrice(itemStack.getAmount());
   }

   @Deprecated
   @Nullable
   public static Double getItemBuyPrice(Player player, ItemStack itemStack) {
      ShopItem shopItem = plugin.createItem.matchShopItem(player, itemStack, Transaction.Mode.BUY);
      return shopItem == null ? null : shopItem.getBuyPrice(player, itemStack.getAmount());
   }

   public static Double getItemSellPrice(ShopItem shopItem, ItemStack itemStack) {
      return shopItem.getSellPrice(itemStack);
   }

   public static Double getItemSellPrice(ShopItem shopItem, ItemStack itemStack, int price, int value) {
      return shopItem.getSellPrice(itemStack, price, value);
   }

   public static Double getItemSellPrice(ShopItem shopItem, ItemStack itemStack, Player player) {
      return shopItem.getSellPrice(player, itemStack);
   }

   public static Double getItemSellPrice(ShopItem shopItem, ItemStack itemStack, Player player, int price, int value) {
      return shopItem.getSellPrice(player, itemStack, price, value);
   }

   public static Optional<SellPrice> getSellPrice(OfflinePlayer offlinePlayer, ItemStack itemStack) {
      Objects.requireNonNull(itemStack, "Item cannot be null");
      Objects.requireNonNull(offlinePlayer, "Player cannot be null");
      boolean online = offlinePlayer.isOnline();
      int amount = itemStack.getAmount();
      ShopItem shopItem = !online ? getShopItem(itemStack) : plugin.createItem.matchShopItem((Player)offlinePlayer, itemStack, Transaction.Mode.SELL);
      if (shopItem == null || !shopItem.isSellAble()) {
         return Optional.empty();
      } else if (shopItem.isMaxSell(amount)) {
         return Optional.empty();
      } else if (shopItem.isMinSell(amount)) {
         return Optional.empty();
      } else {
         if (shopItem.getLimitedSellMode() != 0) {
            int sellLimit = getSellLimit(shopItem, offlinePlayer.getUniqueId());
            if (amount > sellLimit) {
               return Optional.empty();
            }
         }

         SellPrice sellPrice;
         if (shopItem.isASellPricing()) {
            Map prices = !online
               ? shopItem.getASellPrice().getSellPrices(shopItem.getASellPrice().isAll() ? null : shopItem.getASellPrice().getTypes().get(0), itemStack, amount, 0)
               : shopItem.getASellPrice()
                  .getSellPrices(
                     shopItem.getASellPrice().isAll() ? null : shopItem.getASellPrice().getTypes().get(0), (Player)offlinePlayer, itemStack, amount, 0, shopItem.getSellModifiers()
                  );
            sellPrice = new SellPrice(offlinePlayer, amount, shopItem, prices);
         } else {
            double amount2 = !online ? shopItem.getSellPrice(itemStack, amount, 0) : shopItem.getSellPrice((Player)offlinePlayer, itemStack, amount, 0);
            sellPrice = new SellPrice(offlinePlayer, amount, shopItem, shopItem.getEcoType(), amount2);
         }

         return Optional.of(sellPrice);
      }
   }

   public static SellPrices getSellPrices(OfflinePlayer offlinePlayer, ItemStack... playerNames) {
      Objects.requireNonNull(offlinePlayer, "Player cannot be null");
      Objects.requireNonNull(playerNames, "Items cannot be null");
      boolean online = offlinePlayer.isOnline();
      HashMap<ShopItem, Sold> map = new HashMap<>();
      HashMap<EcoType, Double> map2 = new HashMap<>();

      for (ItemStack itemStack : playerNames) {
         if (itemStack != null && itemStack.getType() != Material.AIR) {
            ShopItem shopItem = !online ? plugin.createItem.matchShopItem(itemStack) : plugin.createItem.matchShopItem((Player)offlinePlayer, itemStack, Transaction.Mode.SELL);
            if (shopItem != null && shopItem.isSellAble()) {
               Sold sold = map.computeIfAbsent(shopItem, key -> new Sold());
               int amount = sold.getAmount();
               if (!shopItem.isMaxSell(itemStack.getAmount() + amount)) {
                  if (shopItem.getLimitedSellMode() != 0) {
                     int sellLimit = getSellLimit(shopItem, offlinePlayer.getUniqueId());
                     if (itemStack.getAmount() + amount > sellLimit) {
                        continue;
                     }
                  }

                  Map<EcoType, Double> map3;
                  if (shopItem.isASellPricing()) {
                     map3 = !online
                        ? shopItem.getASellPrice()
                           .getSellPrices(shopItem.getASellPrice().isAll() ? null : shopItem.getASellPrice().getTypes().get(0), itemStack, itemStack.getAmount(), amount)
                        : shopItem.getASellPrice()
                           .getSellPrices(
                              shopItem.getASellPrice().isAll() ? null : shopItem.getASellPrice().getTypes().get(0),
                              (Player)offlinePlayer,
                              itemStack,
                              itemStack.getAmount(),
                              amount,
                              shopItem.getSellModifiers()
                           );
                     map3.forEach((key, value) -> map2.merge(key, value, Double::sum));
                  } else {
                     double price = !online ? shopItem.getSellPrice(itemStack, itemStack.getAmount(), amount) : shopItem.getSellPrice((Player)offlinePlayer, itemStack, itemStack.getAmount(), amount);
                     map2.merge(shopItem.getEcoType(), price, Double::sum);
                     map3 = Collections.singletonMap(shopItem.getEcoType(), price);
                  }

                  sold.addValues(itemStack.getAmount(), map3);
               }
            }
         }
      }

      for (Entry<ShopItem, Sold> entry : new HashSet<>(map.entrySet())) {
         if (((ShopItem)entry.getKey()).isMinSell(((Sold)entry.getValue()).getAmount())) {
            map.remove(entry.getKey());
            ((Sold)entry.getValue()).getPrices().forEach((key, value) -> map2.compute(key, (key2, value2) -> {
               double amount = value2 - value;
               return amount <= 0.0 ? null : amount;
            }));
         }
      }

      return new SellPrices(offlinePlayer, map.entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> ((Sold)entry.getValue()).getAmount())), map2);
   }

   public static SellPrices getCutSellPrices(OfflinePlayer offlinePlayer, ItemStack[] playerNames, boolean flag) {
      Objects.requireNonNull(offlinePlayer, "Player cannot be null");
      Objects.requireNonNull(playerNames, "Items cannot be null");
      boolean online = offlinePlayer.isOnline();
      HashMap<ShopItem, Sold> map = new HashMap<>();
      HashMap<EcoType, Double> map2 = new HashMap<>();

      for (int index = 0; index < playerNames.length; index++) {
         ItemStack itemStack = playerNames[index];
         if (itemStack != null && itemStack.getType() != Material.AIR) {
            ShopItem shopItem = !online ? plugin.createItem.matchShopItem(itemStack) : plugin.createItem.matchShopItem((Player)offlinePlayer, itemStack, Transaction.Mode.SELL);
            if (shopItem != null && shopItem.isSellAble()) {
               Sold sold = map.computeIfAbsent(shopItem, key -> new Sold());
               int amount = sold.getAmount();
               int maxSell = getMaxSell(shopItem, itemStack.getAmount(), amount);
               if (maxSell >= 1) {
                  maxSell = getSellLimit(shopItem, offlinePlayer.getUniqueId(), maxSell, amount);
                  if (maxSell >= 1) {
                     Map<EcoType, Double> map3;
                     if (shopItem.isASellPricing()) {
                        map3 = !online
                           ? shopItem.getASellPrice()
                              .getSellPrices(shopItem.getASellPrice().isAll() ? null : shopItem.getASellPrice().getTypes().get(0), itemStack, maxSell, amount)
                           : shopItem.getASellPrice()
                              .getSellPrices(
                                 shopItem.getASellPrice().isAll() ? null : shopItem.getASellPrice().getTypes().get(0),
                                 (Player)offlinePlayer,
                                 itemStack,
                                 maxSell,
                                 amount,
                                 shopItem.getSellModifiers()
                              );
                        map3.forEach((key, value2) -> map2.merge(key, value2, Double::sum));
                     } else {
                        double price = !online ? shopItem.getSellPrice(itemStack, maxSell, amount) : shopItem.getSellPrice((Player)offlinePlayer, itemStack, maxSell, amount);
                        map2.merge(shopItem.getEcoType(), price, Double::sum);
                        map3 = Collections.singletonMap(shopItem.getEcoType(), price);
                     }

                     sold.addValues(maxSell, map3);
                     if (!shopItem.isSellCommand() || shopItem.isRequireItem()) {
                        sold.addIndex(index);
                     }
                  }
               }
            }
         }
      }

      for (Entry<ShopItem, Sold> entry : new HashSet<>(map.entrySet())) {
         if (((ShopItem)entry.getKey()).isMinSell(((Sold)entry.getValue()).getAmount())) {
            map.remove(entry.getKey());
            ((Sold)entry.getValue()).getPrices().forEach((key, value) -> map2.compute(key, (key2, value2) -> {
               double amount = value2 - value;
               return amount <= 0.0 ? null : amount;
            }));
         } else if (flag) {
            int amount = ((Sold)entry.getValue()).getAmount();

            for (Integer value : ((Sold)entry.getValue()).getIndexes()) {
               if (amount < playerNames[value].getAmount()) {
                  ItemStack itemStack = new ItemStack(playerNames[value]);
                  itemStack.setAmount(itemStack.getAmount() - amount);
                  playerNames[value] = itemStack;
                  break;
               }

               amount -= playerNames[value].getAmount();
               playerNames[value] = null;
            }
         }
      }

      return new SellPrices(offlinePlayer, map.entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> ((Sold)entry.getValue()).getAmount())), map2);
   }

   @Deprecated
   public static TransactionResult sellItem(Player player, ShopItem shopItem, int value) {
      return sellItem(player, shopItem, value, false, false, true, false);
   }

   public static TransactionResult sellItem(Player player, ShopItem shopItem, int value, @NotNull String itemId) {
      return sellItem(player, shopItem, value, false, false, true, false, itemId);
   }

   @Deprecated
   public static TransactionResult sellItem(Player player, ShopItem shopItem, int value, boolean flag, boolean flag2) {
      return sellItem(player, shopItem, value, flag, flag2, true, false);
   }

   public static TransactionResult sellItem(Player player, ShopItem shopItem, int value, boolean flag, boolean flag2, @NotNull String itemId) {
      return sellItem(player, shopItem, value, flag, flag2, true, false, itemId);
   }

   @Deprecated
   public static TransactionResult sellItem(Player player, ShopItem shopItem, int value, boolean flag, boolean flag2, boolean flag3, boolean flag4) {
      return sellItem(player, shopItem, value, flag, flag2, flag3, flag4, "Unknown");
   }

   public static TransactionResult sellItem(Player player, ShopItem shopItem, int value, boolean flag, boolean flag2, boolean flag3, boolean flag4, @NotNull String itemId) {
      Objects.requireNonNull(player, "Player cannot be null");
      Objects.requireNonNull(shopItem, "ShopItem cannot be null");
      if (value < 1) {
         throw new NullPointerException("Amount cannot be less then 1");
      } else if (!player.isOnline()) {
         throw new NullPointerException("Target player should be online");
      } else if (shopItem.hasItemError()) {
         return new TransactionResult(Transaction.Result.ITEM_ERROR);
      } else if (shopItem.isDisplayItem()) {
         return new TransactionResult(Transaction.Result.DISPLAY_ITEM);
      } else if (!shopItem.isSellAble()) {
         return new TransactionResult(Transaction.Result.NEGATIVE_ITEM_PRICE);
      } else if (!flag || PermissionsCache.hasPermission(player, "OnyxShop.shop." + shopItem.section) && shopItem.canPurchase(player, "shop", !flag4)) {
         if (flag2 && !shopItem.meetsRequirements(player, !flag4)) {
            return new TransactionResult(Transaction.Result.REQUIREMENTS_FAILED);
         } else {
            ArrayList<ItemStack> itemStacks = null;
            HashMap<EcoType, Double> map = new HashMap<>();
            if (flag3
               ? PlayerItems.canSell(shopItem, player)
               : (itemStacks = PlayerItems.getSellAble(shopItem, player, value, false)).stream().mapToInt(ItemStack::getAmount).sum() >= value) {
               if (shopItem.getLimitedSellMode() != 0) {
                  int sellLimit = getSellLimit(shopItem, player.getUniqueId());
                  if (flag3) {
                     value = Math.min(value, sellLimit);
                  }

                  if (flag3 ? value <= 0 : sellLimit < value) {
                     if (flag4) {
                        SendMessage.chatToPlayer(player, Lang.LIMITED_ITEM_SELL_AMOUNT.get(player));
                     }

                     callPostS(shopItem, player, value, map, Transaction.Result.REACHED_SELL_LIMIT);
                     return new TransactionResult(Transaction.Result.REACHED_SELL_LIMIT);
                  }
               }

               value = flag3 ? (itemStacks = PlayerItems.getSellAble(shopItem, player, value, true)).stream().mapToInt(ItemStack::getAmount).sum() : value;
               if (!flag3 && shopItem.isMaxSell(value)) {
                  callPostS(shopItem, player, value, map, Transaction.Result.TO_MANY_ITEMS);
                  return new TransactionResult(Transaction.Result.TO_MANY_ITEMS);
               } else if (shopItem.isMinSell(value)) {
                  if (flag4) {
                     SendMessage.chatToPlayer(
                        player,
                        Lang.NOT_ENOUGH_ITEMS_TO_SELL
                           .get(player)
                           .replace("%min-sell%", String.valueOf(shopItem.getMinSell()))
                           .replace("%quantity%", String.valueOf(value))
                     );
                  }

                  callPostS(shopItem, player, value, map, Transaction.Result.NOT_ENOUGH_ITEMS);
                  return new TransactionResult(Transaction.Result.NOT_ENOUGH_ITEMS);
               } else {
                  int value2 = 0;

                  for (ItemStack itemStack : itemStacks) {
                     if (shopItem.isASellPricing()) {
                        for (Entry<EcoType, Double> entry : shopItem.getASellPrice()
                           .getSellPrices(null, player, itemStack, itemStack.getAmount(), value2 += itemStack.getAmount(), shopItem.getSellModifiers())
                           .entrySet()) {
                           map.merge((EcoType)entry.getKey(), (Double)entry.getValue(), Double::sum);
                        }
                     } else {
                        map.merge(shopItem.getEcoType(), shopItem.getSellPrice(player, itemStack, itemStack.getAmount(), value2 += itemStack.getAmount()), Double::sum);
                     }
                  }

                  if (map.entrySet()
                     .stream()
                     .anyMatch(
                        entry -> entry.getKey().getType() == EconomyType.ITEM
                           && !PlayerItems.canPay(player, entry.getKey(), entry.getValue())
                     )) {
                     if (flag4) {
                        SendMessage.chatToPlayer(player, Lang.CANNOT_STORE_PAYMENT.get(player));
                     }

                     callPostS(shopItem, player, value, map, Transaction.Result.CANT_STORE_PAYMENT);
                     return new TransactionResult(Transaction.Result.CANT_STORE_PAYMENT);
                  } else {
                     PreTransactionEvent preTransactionEvent = new PreTransactionEvent(Collections.singletonMap(shopItem, value), map, player, value, Transaction.Type.API_SELL);
                     Bukkit.getPluginManager().callEvent(preTransactionEvent);
                     Map prices = preTransactionEvent.getPrices();
                     if (preTransactionEvent.isCancelled()) {
                        callPostS(shopItem, player, value, prices, Transaction.Result.TRANSACTION_CANCELLED);
                        return new TransactionResult(Transaction.Result.TRANSACTION_CANCELLED);
                     } else {
                        plugin.getEcoHandler().deposit(prices, player);
                        if (shopItem.isSellCommand()) {
                           CommandHelper.executeCommandItem(shopItem, player, CommandType.SELL, prices.toString(), value);
                        }

                        if (!shopItem.isSellCommand() || shopItem.isRequireItem()) {
                           itemStacks.forEach(entry -> player.getInventory().removeItem(new ItemStack[]{entry}));
                        }

                        if (shopItem.getLimitedSellMode() != 0) {
                           sellItemLimit(shopItem, player.getUniqueId(), value);
                        }

                        if (shopItem.isRefillStock()) {
                           sellItemStock(shopItem, player.getUniqueId(), value);
                        }

                        if (shopItem.isDynamicPricing()) {
                           sellItem(shopItem, value);
                        }

                        if (flag4) {
                           SendMessage.sendTransactionMessage(
                              player,
                              value,
                              prices,
                              shopItem,
                              Transaction.Mode.SELL,
                              Transaction.Type.API_SELL.withCustomType(itemId),
                              shopItem.isSellCommand() && !shopItem.isRequireItem()
                           );
                        }

                        callPostS(shopItem, player, value, prices, shopItem.isSellCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS);
                        return new TransactionResult(
                           shopItem.isSellCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS, prices, value
                        );
                     }
                  }
               }
            } else {
               if (flag4) {
                  SendMessage.chatToPlayer(player, Lang.NO_ITEM_FOUND.get(player));
               }

               callPostS(shopItem, player, value, map, Transaction.Result.NO_ITEMS_FOUND);
               return new TransactionResult(Transaction.Result.NO_ITEMS_FOUND);
            }
         }
      } else {
         return new TransactionResult(Transaction.Result.NO_PERMISSIONS);
      }
   }

   public static boolean isBuyAble(ShopItem shopItem) {
      return shopItem.isBuyAble();
   }

   @Deprecated
   public static Double getItemBuyPrice(ShopItem shopItem, ItemStack itemStack) {
      return shopItem.getBuyPrice(itemStack.getAmount());
   }

   @Deprecated
   public static Double getItemBuyPrice(ShopItem shopItem, ItemStack itemStack, Player player) {
      return shopItem.getBuyPrice(player, itemStack.getAmount());
   }

   public static double getItemBuyPrice(ShopItem shopItem, int price) {
      return shopItem.getBuyPrice(price);
   }

   public static double getItemBuyPrice(ShopItem shopItem, Player player, int price) {
      return shopItem.getBuyPrice(player, price);
   }

   public static Optional<BuyPrice> getBuyPrice(OfflinePlayer offlinePlayer, ItemStack itemStack) {
      Objects.requireNonNull(itemStack, "Item cannot be null");
      Objects.requireNonNull(offlinePlayer, "Player cannot be null");
      boolean online = offlinePlayer.isOnline();
      int amount = itemStack.getAmount();
      ShopItem shopItem = !online ? getShopItem(itemStack) : plugin.createItem.matchShopItem((Player)offlinePlayer, itemStack, Transaction.Mode.BUY);
      if (shopItem == null || !shopItem.isBuyAble()) {
         return Optional.empty();
      } else if (shopItem.isMaxBuy(itemStack.getAmount())) {
         return Optional.empty();
      } else if (shopItem.isMinBuy(itemStack.getAmount())) {
         return Optional.empty();
      } else {
         if (shopItem.getLimitedStockMode() != 0) {
            int itemStock = getItemStock(shopItem, offlinePlayer.getUniqueId());
            if (amount > itemStock) {
               return Optional.empty();
            }
         }

         BuyPrice buyPrice;
         if (shopItem.isABuyPricing()) {
            Map prices = !online
               ? shopItem.getABuyPrice().getBuyPrices(shopItem.getABuyPrice().isAll() ? null : shopItem.getABuyPrice().getTypes().get(0), amount)
               : shopItem.getABuyPrice()
                  .getBuyPrices(shopItem.getABuyPrice().isAll() ? null : shopItem.getABuyPrice().getTypes().get(0), (Player)offlinePlayer, amount, shopItem.getBuyModifiers());
            buyPrice = new BuyPrice(offlinePlayer, amount, shopItem, prices);
         } else {
            double amount2 = !online ? shopItem.getBuyPrice(amount) : shopItem.getBuyPrice((Player)offlinePlayer, amount);
            buyPrice = new BuyPrice(offlinePlayer, amount, shopItem, shopItem.getEcoType(), amount2);
         }

         return Optional.of(buyPrice);
      }
   }

   public static boolean hasMultipleBuyPrices(ShopItem shopItem) {
      return shopItem.isABuyPricing();
   }

   public static AdvancedBuyPrice getMultipleBuyPrices(ShopItem shopItem) {
      return new AdvancedBuyPrice(shopItem);
   }

   public static boolean hasMultipleSellPrices(ShopItem shopItem) {
      return shopItem.isASellPricing();
   }

   public static AdvancedSellPrice getMultipleSellPrices(ShopItem shopItem) {
      return new AdvancedSellPrice(shopItem);
   }

   @Deprecated
   public static TransactionResult purchaseItem(Player player, ShopItem shopItem, int value) {
      return purchaseItem(player, shopItem, value, false, false, true, false);
   }

   public static TransactionResult purchaseItem(Player player, ShopItem shopItem, int value, @NotNull String itemId) {
      return purchaseItem(player, shopItem, value, false, false, true, false, itemId);
   }

   @Deprecated
   public static TransactionResult purchaseItem(Player player, ShopItem shopItem, int value, boolean flag, boolean flag2) {
      return purchaseItem(player, shopItem, value, flag, flag2, true, false);
   }

   public static TransactionResult purchaseItem(Player player, ShopItem shopItem, int value, boolean flag, boolean flag2, @NotNull String itemId) {
      return purchaseItem(player, shopItem, value, flag, flag2, true, false, itemId);
   }

   @Deprecated
   public static TransactionResult purchaseItem(Player player, ShopItem shopItem, int value, boolean flag, boolean flag2, boolean flag3, boolean flag4) {
      return purchaseItem(player, shopItem, value, flag, flag2, flag3, flag4, "Unknown");
   }

   public static TransactionResult purchaseItem(
      Player player, ShopItem shopItem, int value, boolean flag, boolean flag2, boolean flag3, boolean flag4, @NotNull String itemId
   ) {
      Objects.requireNonNull(player, "Player cannot be null");
      Objects.requireNonNull(shopItem, "ShopItem cannot be null");
      if (value < 1) {
         throw new NullPointerException("Amount cannot be less then 1");
      } else if (!player.isOnline()) {
         throw new NullPointerException("Target player should be online");
      } else if (shopItem.hasItemError()) {
         return new TransactionResult(Transaction.Result.ITEM_ERROR);
      } else if (shopItem.isDisplayItem()) {
         return new TransactionResult(Transaction.Result.DISPLAY_ITEM);
      } else if (!shopItem.isBuyAble()) {
         return new TransactionResult(Transaction.Result.NEGATIVE_ITEM_PRICE);
      } else if (!flag || PermissionsCache.hasPermission(player, "OnyxShop.shop." + shopItem.section) && shopItem.canPurchase(player, "shop", !flag4)) {
         if (flag2 && !shopItem.meetsRequirements(player, !flag4)) {
            return new TransactionResult(Transaction.Result.REQUIREMENTS_FAILED);
         } else {
            HashMap map = new HashMap();
            if (flag3 ? PlayerItems.canBuy(player, shopItem) : PlayerItems.getFinalAmount(shopItem, player, value, false) >= value) {
               value = flag3 ? PlayerItems.getFinalAmount(shopItem, player, value, true) : value;
               if (!flag3 && shopItem.isMaxBuy(value)) {
                  callPostB(shopItem, player, value, map, Transaction.Result.TO_MANY_ITEMS);
                  return new TransactionResult(Transaction.Result.TO_MANY_ITEMS);
               } else if (shopItem.isMinBuy(value)) {
                  if (flag4) {
                     SendMessage.chatToPlayer(player, Lang.NOT_ENOUGH_ITEMS_TO_BUY.get(player).replace("%min-buy%", String.valueOf(shopItem.getMinBuy())));
                  }

                  callPostB(shopItem, player, value, map, Transaction.Result.NOT_ENOUGH_ITEMS);
                  return new TransactionResult(Transaction.Result.NOT_ENOUGH_ITEMS);
               } else {
                  if (shopItem.getLimitedStockMode() != 0) {
                     int itemStock = getItemStock(shopItem, player.getUniqueId());
                     if (flag3) {
                        value = Math.min(value, itemStock);
                     }

                     if (flag3 ? value <= 0 : itemStock < value) {
                        if (flag4) {
                           SendMessage.chatToPlayer(player, Lang.NOT_ENOUGH_STOCK_AVAILABLE.get(player));
                           plugin.getSoundEffects().insufficientStock(player);
                        }

                        callPostB(shopItem, player, value, map, Transaction.Result.NO_ITEM_STOCK_LEFT);
                        return new TransactionResult(Transaction.Result.NO_ITEM_STOCK_LEFT);
                     }
                  }

                  PreTransactionEvent preTransactionEvent = new PreTransactionEvent(
                     Collections.singletonMap(shopItem, value),
                     shopItem.isABuyPricing()
                        ? shopItem.getABuyPrice().getBuyPrices(null, player, value, shopItem.getBuyModifiers())
                        : Collections.singletonMap(shopItem.getEcoType(), shopItem.getBuyPrice(player, value)),
                     player,
                     value,
                     Transaction.Type.API_BUY
                  );
                  Bukkit.getPluginManager().callEvent(preTransactionEvent);
                  Map prices = preTransactionEvent.getPrices();
                  if (preTransactionEvent.isCancelled()) {
                     callPostB(shopItem, player, value, prices, Transaction.Result.TRANSACTION_CANCELLED);
                     return new TransactionResult(Transaction.Result.TRANSACTION_CANCELLED);
                  } else if (!arePricesValid(prices)) {
                     callPostB(shopItem, player, value, prices, Transaction.Result.NEGATIVE_ITEM_PRICE);
                     return new TransactionResult(Transaction.Result.NEGATIVE_ITEM_PRICE, prices, value);
                  } else {
                     Map map2 = plugin.getEcoHandler().has(prices, player);
                     if (map2.size() != 0) {
                        if (flag4) {
                           SendMessage.chatToPlayer(
                              player,
                              Lang.INSUFFICIENT_MONEY
                                 .get(player)
                                 .replace(
                                    "%currency%",
                                    shopItem.isABuyPricing()
                                       ? plugin.formatPrices(
                                          Lang.INSUFFICIENT_MONEY.get(player).getLegacy().split("%currency%")[0], map2.keySet(), shopItem.getABuyPrice().isAll()
                                       )
                                       : plugin.getEcoHandler().getEcon(shopItem.getEcoType()).getFriendly()
                                 )
                           );
                           plugin.getSoundEffects().insufficientFunds(player);
                        }

                        callPostB(shopItem, player, value, prices, Transaction.Result.INSUFFICIENT_FUNDS);
                        return new TransactionResult(Transaction.Result.INSUFFICIENT_FUNDS);
                     } else if (!plugin.getEcoHandler().withdraw(prices, player)) {
                        if (flag4) {
                           SendMessage.chatToPlayer(
                              player,
                              Lang.INSUFFICIENT_MONEY
                                 .get(player)
                                 .replace(
                                    "%currency%",
                                    shopItem.isABuyPricing()
                                       ? plugin.formatPrices(
                                          Lang.INSUFFICIENT_MONEY.get(player).getLegacy().split("%currency%")[0], prices.keySet(), shopItem.getABuyPrice().isAll()
                                       )
                                       : plugin.getEcoHandler().getEcon(shopItem.getEcoType()).getFriendly()
                                 )
                           );
                           plugin.getSoundEffects().insufficientFunds(player);
                        }

                        callPostB(shopItem, player, value, prices, Transaction.Result.INSUFFICIENT_FUNDS);
                        return new TransactionResult(Transaction.Result.INSUFFICIENT_FUNDS, prices, value);
                     } else {
                        if (shopItem.isBuyCommand()) {
                           CommandHelper.executeCommandItem(shopItem, player, CommandType.BUY, prices.toString(), value);
                        }

                        if (!shopItem.isBuyCommand() || shopItem.isGiveItem()) {
                           PlayerItems.giveItems(player, shopItem, value);
                        }

                        if (shopItem.getLimitedStockMode() != 0) {
                           buyItemStock(shopItem, player.getUniqueId(), value);
                        }

                        if (shopItem.isDynamicPricing()) {
                           buyItem(shopItem, value);
                        }

                        if (flag4) {
                           SendMessage.sendTransactionMessage(
                              player,
                              value,
                              prices,
                              shopItem,
                              Transaction.Mode.BUY,
                              Transaction.Type.API_BUY.withCustomType(itemId),
                              shopItem.isBuyCommand() && !shopItem.isGiveItem()
                           );
                        }

                        callPostB(shopItem, player, value, prices, shopItem.isBuyCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS);
                        return new TransactionResult(
                           shopItem.isBuyCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS, prices, value
                        );
                     }
                  }
               }
            } else {
               if (flag4) {
                  SendMessage.chatToPlayer(player, Lang.MORE_SPACE_NEEDED.get(player));
               }

               callPostB(shopItem, player, value, map, Transaction.Result.NO_INVENTORY_SPACE);
               return new TransactionResult(Transaction.Result.NO_INVENTORY_SPACE);
            }
         }
      } else {
         return new TransactionResult(Transaction.Result.NO_PERMISSIONS);
      }
   }

   public static void setSectionPriceModifier(UUID uniqueId, String sectionId, PriceModifierType priceModifierType, double price) {
      plugin.getModifierManager().setCommandSectionModifier(uniqueId, sectionId, priceModifierType, price < 0.0 ? 0.0 : price);
   }

   public static void setItemPriceModifier(UUID uniqueId, String itemId, PriceModifierType priceModifierType, double price) {
      plugin.getModifierManager().setCommandItemModifier(uniqueId, itemId, priceModifierType, price < 0.0 ? 0.0 : price);
   }

   public static void removeSectionModifier(UUID uniqueId, String sectionId, PriceModifierType priceModifierType) {
      plugin.getModifierManager().removeCommandSectionModifier(uniqueId, sectionId, priceModifierType);
   }

   public static void removeItemModifier(UUID uniqueId, String itemId, PriceModifierType priceModifierType) {
      plugin.getModifierManager().removeCommandItemModifier(uniqueId, itemId, priceModifierType);
   }

   @Nullable
   public static PlayerPriceModifier getPlayerPriceModifiers(UUID playerId) {
      com.onyxshop.objects.PlayerPriceModifier playerPriceModifier = plugin.getModifierManager().getCommandModifiersForPlayer(playerId);
      return playerPriceModifier != null ? new PlayerPriceModifier(playerPriceModifier) : null;
   }

   @Nullable
   public static PlayerPriceModifier loadPlayerPriceModifiers(UUID playerId) {
      com.onyxshop.objects.PlayerPriceModifier playerPriceModifier = plugin.getModifierManager().loadCommandModifiersForPlayer(playerId);
      return playerPriceModifier != null ? new PlayerPriceModifier(playerPriceModifier) : null;
   }

   public static EconomyProvider getEcon(@Nullable EcoType ecoType) {
      return plugin.getEcoHandler().getEcon(ecoType);
   }

   public static void logTransaction(UUID uniqueId, ShopItem shopItem, Map<EcoType, Double> map, int value, Transaction.Mode mode, @NotNull String shopId) {
      plugin.getTransactionLog()
         .log(uniqueId, shopItem, map, value, mode, (mode == Transaction.Mode.BUY ? Transaction.Type.API_BUY : Transaction.Type.API_SELL).withCustomType(shopId));
   }

   public static void logTransaction(UUID uniqueId, Map<ShopItem, Integer> map, Map<EcoType, Double> map2, int value, Transaction.Mode mode, @NotNull String shopId) {
      plugin.getTransactionLog()
         .log(uniqueId, map, map2, value, mode, (mode == Transaction.Mode.BUY ? Transaction.Type.API_BUY : Transaction.Type.API_SELL).withCustomType(shopId));
   }

   @Deprecated
   public static void buyItem(ItemStack itemStack, int value) {
      if (plugin.dynamicPricing) {
         plugin.getDynamicPricingProvider().buyItem(plugin.createItem.matchShopItem(itemStack).getItemPath(), value);
      }
   }

   @Deprecated
   public static void sellItem(ItemStack itemStack, int value) {
      if (plugin.dynamicPricing) {
         plugin.getDynamicPricingProvider().sellItem(plugin.createItem.matchShopItem(itemStack).getItemPath(), value);
      }
   }

   public static void buyItem(ShopItem shopItem, int value) {
      if (plugin.dynamicPricing) {
         plugin.getDynamicPricingProvider().buyItem(shopItem.getItemPath(), value);
      }
   }

   public static void sellItem(ShopItem shopItem, int value) {
      if (plugin.dynamicPricing) {
         plugin.getDynamicPricingProvider().sellItem(shopItem.getItemPath(), value);
      }
   }

   public static void openTransactionScreen(Player player, String playerName, String text, Transaction.Mode mode) {
      ShopSection section = plugin.getSection(playerName);
      if (section == null) {
         throw new NullPointerException("A section with name '" + playerName + "' does not exist");
      } else {
         ShopItem shopItem = section.getShopItem(playerName + "." + text);
         if (shopItem == null) {
            throw new NullPointerException("The shop item at section " + playerName + " with index " + text + " does not exist");
         } else {
            openTransactionScreen(player, shopItem, mode);
         }
      }
   }

   public static void openTransactionScreen(Player player, ShopItem shopItem, Transaction.Mode mode) {
      new TransactionMenu(player, shopItem, null, true, mode, mode == Transaction.Mode.BUY ? Transaction.Type.BUY_SCREEN : Transaction.Type.SELL_SCREEN, 1)
         .open(false);
   }

   @Nullable
   public static ShopItem getShopItem(ItemStack itemStack) {
      return plugin.createItem.matchShopItem(itemStack);
   }

   @Nullable
   public static ShopItem getShopItem(Player player, ItemStack itemStack) {
      return plugin.createItem.matchShopItem(player, itemStack);
   }

   @Nullable
   public static ShopItem getShopItem(String shopId) {
      try {
         ShopSection section = plugin.getSection(shopId.split("\\.")[0]);
         return section.getShopItem(shopId);
      } catch (Exception exception) {
         return null;
      }
   }

   @Nullable
   public static ShopSection getShopSection(String shopId) {
      return plugin.getSection(shopId);
   }

   public static List<String> getShopSections() {
      return plugin.getShopSections();
   }

   public static Map<String, ShopSection> getSections() {
      return plugin.getSections();
   }

   public static int getItemStock(ShopItem shopItem, @Nullable UUID uniqueId) {
      return plugin.getLimitedStockProvider().getItemStock(shopItem, uniqueId, true);
   }

   public static Long getItemStockRestockTime(ShopItem shopItem, @Nullable UUID uniqueId) {
      return plugin.getLimitedStockProvider().getTimeLeftToRestock(shopItem, uniqueId);
   }

   public static int buyItemStock(ShopItem shopItem, @Nullable UUID uniqueId, int stock) {
      return plugin.getLimitedStockProvider().buyItemsSafe(shopItem, uniqueId, stock);
   }

   public static void sellItemStock(ShopItem shopItem, @Nullable UUID uniqueId, int stock) {
      plugin.getLimitedStockProvider().sellItems(shopItem, uniqueId, stock);
   }

   public static int getSellLimit(ShopItem shopItem, @Nullable UUID uniqueId) {
      return plugin.getLimitedSellProvider().getItemLimit(shopItem, uniqueId, true);
   }

   public static Long getSellLimitRestockTime(ShopItem shopItem, @Nullable UUID uniqueId) {
      return plugin.getLimitedSellProvider().getTimeLeftToRestock(shopItem, uniqueId);
   }

   public static int sellItemLimit(ShopItem shopItem, @Nullable UUID uniqueId, int limit) {
      return plugin.getLimitedSellProvider().sellItemsSafe(shopItem, uniqueId, limit, true);
   }

   public static boolean hasPermissions(ShopItem shopItem, Player player) {
      return shopItem.hasPermission(player, "shop", true);
   }

   public static boolean hasPermissions(ShopItem shopItem, Player player, String shopId) {
      return shopItem.hasPermission(player, shopId, true);
   }

   private static int getMaxSell(ShopItem shopItem, int value, int value2) {
      if (shopItem.getMaxSell() > 0) {
         if (shopItem.isMaxSell(value2)) {
            return -1;
         }

         value = Math.min(value, shopItem.getMaxSell() - value2);
      }

      return value;
   }

   private static int getSellLimit(ShopItem shopItem, UUID uniqueId, int limit, int value) {
      if (shopItem.getLimitedSellMode() != 0) {
         int sellLimit = getSellLimit(shopItem, uniqueId);
         if (sellLimit <= value) {
            return -1;
         }

         if (sellLimit < limit + value) {
            limit = sellLimit - value;
         }
      }

      return limit;
   }

   private static boolean arePricesValid(Map<EcoType, Double> map) {
      for (Double amount : map.values()) {
         if (amount == null || !(amount >= 0.0)) {
            return false;
         }
      }

      return true;
   }

   private static void callPostB(ShopItem shopItem, Player player, int value, Map<EcoType, Double> map, Transaction.Result result) {
      callPost(shopItem, player, value, map, Transaction.Type.API_BUY, result);
   }

   private static void callPostS(ShopItem shopItem, Player player, int value, Map<EcoType, Double> map, Transaction.Result result) {
      callPost(shopItem, player, value, map, Transaction.Type.API_SELL, result);
   }

   private static void callPost(ShopItem shopItem, Player player, int value, Map<EcoType, Double> map, Transaction.Type type, Transaction.Result result) {
      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(Collections.singletonMap(shopItem, value), map, player, value, type, result));
   }
}
