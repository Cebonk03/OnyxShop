package com.onyxshop.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.api.events.PostTransactionEvent;
import com.onyxshop.api.events.PreTransactionEvent;
import com.onyxshop.commands.base.PluginCommands;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.ItemManager;
import com.onyxshop.providers.economys.ItemEconomy;
import com.onyxshop.util.CommandHelper;
import com.onyxshop.util.CommandType;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.EconomyType;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.SmartStack;
import com.onyxshop.util.Transaction;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.cache.Sold;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.util.StringUtil;

public class Sellall extends PluginCommands {
   private final boolean oldSellallCommand = ConfigManager.badYaml == null && ConfigManager.getConfig().getBoolean("old-sellall-command");

   public Sellall(OnyxShop plugin, List<String> shopIds, List<String> values) {
      super(
         plugin,
         (String)shopIds.get(0),
         "Sell all items from your inventory, hand items or specific items",
         "/" + (String)shopIds.remove(0) + " <inventory/item/hand> [qty]",
         null,
         shopIds,
         values
      );
   }

   @Override
   public boolean execute(CommandSender sender, String command, String[] values) {
      if (ConfigManager.badYaml != null) {
         SendMessage.sendMessage(sender, ChatColor.DARK_RED + "This command cannot be executed now, please fix the configuration formatting first!");
         return true;
      } else {
         if (sender instanceof Player) {
            Player player = (Player)sender;
            if (!this.canUse(player)) {
               return true;
            }

            if (values.length == 0) {
               if (this.oldSellallCommand) {
                  if (PermissionsCache.hasPermission(player, "OnyxShop.sellall")) {
                     this.sellInventory(player);
                  } else {
                     SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get(player));
                  }
               } else if (this.hasPermissions(player)) {
                  String commandUsages = this.getCommandUsages(player);
                  if (!commandUsages.isEmpty()) {
                     SendMessage.chatToPlayer(player, Lang.SELLALL_COMMAND_USAGES.get(player));
                     SendMessage.chatToPlayer(player, commandUsages);
                  } else {
                     SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get(player));
                  }
               } else {
                  SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get(player));
               }
            } else {
               String text = values[0].toUpperCase(Locale.ENGLISH);
               switch (text) {
                  case "INVENTORY":
                     if (PermissionsCache.hasPermission(player, "OnyxShop.sellall")) {
                        this.sellInventory(player);
                     } else {
                        SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get(player));
                     }
                     break;
                  case "HAND":
                     if (PermissionsCache.hasPermission(player, "OnyxShop.sellallhand")) {
                        int value = 0;
                        if (values.length >= 2) {
                           try {
                              value = Integer.parseInt(values[1]);
                              if (value <= 0) {
                                 throw new NumberFormatException();
                              }
                           } catch (NumberFormatException exception) {
                              SendMessage.chatToPlayer(player, Lang.NO_VALID_AMOUNT.get(player));
                              return true;
                           }
                        }

                        this.sellHand(player, value);
                     } else {
                        SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get(player));
                     }
                     break;
                  default:
                     if (PermissionsCache.hasPermission(player, "OnyxShop.sellallitem")) {
                        Sellall.SelectedMat selectedMat;
                        try {
                           selectedMat = new Sellall.SelectedMat(player, values[0]);
                        } catch (NullPointerException exception) {
                           return true;
                        }

                        int value;
                        if (values.length >= 2) {
                           try {
                              value = Integer.parseInt(values[1]);
                              if (value <= 0) {
                                 throw new NumberFormatException();
                              }
                           } catch (NumberFormatException exception) {
                              SendMessage.chatToPlayer(player, Lang.NO_VALID_AMOUNT.get(player));
                              return true;
                           }
                        } else {
                           value = Integer.MAX_VALUE;
                        }

                        this.removeItems(player, selectedMat, value);
                     } else {
                        SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get(player));
                     }
               }
            }
         } else {
            SendMessage.sendMessage(sender, Lang.REAL_PLAYER.get());
         }

         return true;
      }
   }

   private void sellInventory(Player player) {
      Transaction.Result result = Transaction.Result.SUCCESS;
      HashMap map = new HashMap();
      HashMap<ShopItem, Sold> map2 = new HashMap<>();
      int value = 0;

      for (int index = 0; index <= 35; index++) {
         ItemStack itemStack = player.getInventory().getItem(index);
         if (itemStack != null && itemStack.getType() != Material.AIR) {
            ShopItem shopItem = this.plugin.createItem.matchShopItem(itemStack, player, "sellall");
            if (shopItem != null && shopItem.isSellAble()) {
               int amount = itemStack.getAmount();
               if (shopItem.isMaxSell(map2.getOrDefault(shopItem, new Sold()).getAmount() + amount)) {
                  if (map2.getOrDefault(shopItem, new Sold()).getAmount() >= shopItem.getMaxSell()) {
                     continue;
                  }

                  amount = shopItem.getMaxSell() - map2.getOrDefault(shopItem, new Sold()).getAmount();
               }

               if (shopItem.getLimitedSellMode() != 0 && this.plugin.sellLimits) {
                  int itemLimit = this.plugin.getLimitedSellProvider().getItemLimit(shopItem, player.getUniqueId(), true);
                  amount = Math.min(amount, itemLimit - map2.getOrDefault(shopItem, new Sold()).getAmount());
                  if (amount < 0) {
                     continue;
                  }
               }

               Sold sold = map2.computeIfAbsent(shopItem, key -> new Sold());
               sold.addValues(amount, this.getPrice(shopItem, player, itemStack, amount, sold.getAmount()));
               value += amount;
               ItemStack itemStack2 = new ItemStack(itemStack);
               itemStack2.setAmount(itemStack2.getAmount() - amount);
               if (!shopItem.isSellCommand() || shopItem.isRequireItem()) {
                  map.put(index, new SmartStack(itemStack2));
                  sold.addIndex(index);
               }
            } else if (this.plugin.sellShulkers && XMaterial.isShulker(itemStack.getType())) {
               Integer value3 = this.sellShulker(map2, map, player, itemStack, "sellall", index);
               if (value3 != null) {
                  value += value3;
               }
            }
         }
      }

      for (Entry<ShopItem, Sold> entry : new HashSet<>(map2.entrySet())) {
         if (((ShopItem)entry.getKey()).isMinSell(((Sold)entry.getValue()).getAmount())) {
            map2.remove(entry.getKey());
            value -= ((Sold)entry.getValue()).getAmount();
            ((Sold)entry.getValue()).getIndexes().forEach(map::remove);
         }
      }

      Map<EcoType, Double> map3 = new HashMap<>();
      HashMap map4 = new HashMap();
      if (value > 0) {
         map3 = this.callPreTransactionEvent(this.getItems(map4, map2), this.getPrices((Map<EcoType, Double>)map3, map2), player, value);
         if (map3 != null) {
            if (!map3.keySet().stream().allMatch(element -> element.getType() != EconomyType.ITEM) && !this.canPay(player, (Map<EcoType, Double>)map3)) {
               SendMessage.chatToPlayer(player, Lang.CANNOT_STORE_PAYMENT.get(player));
               result = Transaction.Result.CANT_STORE_PAYMENT;
            } else {
               this.removeItems(player.getInventory(), map);
               map2.forEach((key, value2) -> {
                  if (key.isSellCommand()) {
                     CommandHelper.executeCommandItem(key, player, CommandType.SELL, value2.getPrices(), value2.getAmount());
                  }
               });
               this.plugin.getEcoHandler().deposit((Map<EcoType, Double>)map3, player);
               SendMessage.sendTransactionMessage(player, value, (Map<EcoType, Double>)map3, map4, Transaction.Type.SELL_ALL_COMMAND);
               this.sellItems(map4, player.getUniqueId());
            }
         } else {
            result = Transaction.Result.TRANSACTION_CANCELLED;
         }
      } else {
         result = Transaction.Result.NO_ITEMS_FOUND;
         SendMessage.chatToPlayer(player, Lang.NO_ITEM_FOUND.get(player));
      }

      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(map4, (Map<EcoType, Double>)map3, player, value, Transaction.Type.SELL_ALL_COMMAND, result));
   }

   private void sellHand(Player player, int value) {
      Transaction.Result result = Transaction.Result.SUCCESS;
      ItemStack itemStack = this.plugin.versionHandler.getItemInHand(player);
      ShopItem shopItem = null;
      Object value2 = new HashMap();
      if (itemStack != null && itemStack.getType() != Material.AIR) {
         shopItem = this.plugin.createItem.matchShopItem(itemStack, player, "sellallhand");
         if (shopItem != null) {
            if (shopItem.isSellAble()) {
               if (value == 0 || value >= itemStack.getAmount()) {
                  value = itemStack.getAmount();
               }

               if (shopItem.getLimitedSellMode() != 0 && this.plugin.sellLimits) {
                  int itemLimit = this.plugin.getLimitedSellProvider().getItemLimit(shopItem, player.getUniqueId(), true);
                  value = Math.min(itemLimit, value);
               }

               if (shopItem.isMaxSell(value)) {
                  value = shopItem.getMaxSell();
               }

               if (value > 0) {
                  if (!shopItem.isMinSell(value)) {
                     value2 = this.callPreTransactionEvent(Collections.singletonMap(shopItem, value), this.getPrices(shopItem, player, itemStack, value), player, value);
                     if (value2 != null) {
                        if (shopItem.getEcoType().getType() == EconomyType.ITEM && !this.canPay(player, (Map<EcoType, Double>)value2)) {
                           SendMessage.chatToPlayer(player, Lang.CANNOT_STORE_PAYMENT.get(player));
                           result = Transaction.Result.CANT_STORE_PAYMENT;
                        } else {
                           if (!shopItem.isSellCommand() || shopItem.isRequireItem()) {
                              if (value == itemStack.getAmount()) {
                                 if (this.plugin.version <= 110) {
                                    player.getInventory().removeItem(new ItemStack[]{itemStack});
                                 } else {
                                    itemStack.setAmount(0);
                                 }
                              } else {
                                 itemStack.setAmount(itemStack.getAmount() - value);
                              }
                           }

                           this.plugin.getEcoHandler().deposit((Map<EcoType, Double>)value2, player);
                           SendMessage.sendTransactionMessage(
                              player,
                              value,
                              (Map<EcoType, Double>)value2,
                              shopItem,
                              Transaction.Mode.SELL,
                              Transaction.Type.SELL_ALL_COMMAND,
                              shopItem.isSellCommand() && !shopItem.isRequireItem()
                           );
                           this.sellItems(shopItem, value, player.getUniqueId());
                           CommandHelper.executeCommandItem(shopItem, player, CommandType.SELL, value2.toString(), value);
                        }
                     } else {
                        result = Transaction.Result.TRANSACTION_CANCELLED;
                     }
                  } else {
                     SendMessage.chatToPlayer(
                        player,
                        Lang.NOT_ENOUGH_ITEMS_TO_SELL
                           .get(player)
                           .replace("%min-sell%", String.valueOf(shopItem.getMinSell()))
                           .replace("%quantity%", String.valueOf(value))
                     );
                     result = Transaction.Result.NOT_ENOUGH_ITEMS;
                  }
               } else {
                  result = Transaction.Result.REACHED_SELL_LIMIT;
                  SendMessage.chatToPlayer(player, Lang.LIMITED_ITEM_SELL_AMOUNT.get(player));
               }
            } else {
               result = Transaction.Result.NEGATIVE_ITEM_PRICE;
               SendMessage.chatToPlayer(player, Lang.CANNOT_SELL_ITEM.get(player));
            }
         } else if (this.plugin.sellShulkers && XMaterial.isShulker(itemStack.getType())) {
            HashMap map = new HashMap();
            HashMap<ShopItem, Sold> map2 = new HashMap<>();
            Integer slot = this.sellShulker(map2, map, player, itemStack, "sellallhand", player.getInventory().getHeldItemSlot());
            if (slot != null) {
               Map<EcoType, Double> prices = this.getPrices(map2);
               if (!prices.keySet().stream().allMatch(element -> element.getType() != EconomyType.ITEM) && !this.canPay(player, prices)) {
                  SendMessage.chatToPlayer(player, Lang.CANNOT_STORE_PAYMENT.get(player));
                  result = Transaction.Result.CANT_STORE_PAYMENT;
               } else {
                  Map<ShopItem, Integer> items = this.getItems(map2);
                  prices = this.callPreTransactionEvent(items, prices, player, slot);
                  if (prices != null) {
                     this.removeItems(player.getInventory(), map);
                     this.plugin.getEcoHandler().deposit(prices, player);
                     map2.forEach((key, value3) -> {
                        if (key.isSellCommand()) {
                           CommandHelper.executeCommandItem(key, player, CommandType.SELL, value3.getPrices(), value3.getAmount());
                        }
                     });
                     SendMessage.sendTransactionMessage(player, slot, prices, items, Transaction.Type.SELL_ALL_COMMAND);
                  } else {
                     result = Transaction.Result.TRANSACTION_CANCELLED;
                  }
               }
            } else {
               result = Transaction.Result.NO_ITEMS_FOUND;
               SendMessage.chatToPlayer(player, Lang.NO_ITEM_FOUND.get(player));
            }
         } else {
            result = Transaction.Result.NO_ITEMS_FOUND;
            SendMessage.chatToPlayer(player, Lang.NO_ITEM_FOUND.get(player));
         }
      } else {
         result = Transaction.Result.NO_ITEMS_FOUND;
         SendMessage.chatToPlayer(player, Lang.CANNOT_SELL_AIR.get(player));
      }

      Bukkit.getPluginManager()
         .callEvent(
            new PostTransactionEvent(Collections.singletonMap(shopItem, value), (Map<EcoType, Double>)value2, player, value, Transaction.Type.SELL_ALL_COMMAND, result)
         );
   }

   private void removeItems(Player player, Sellall.SelectedMat selectedMat, int value) {
      Transaction.Result result = Transaction.Result.SUCCESS;
      HashMap map = new HashMap();
      HashMap<ShopItem, Sold> map2 = new HashMap<>();
      int value2 = 0;

      for (int index = 0; index < 36; index++) {
         ItemStack itemStack = player.getInventory().getItem(index);
         if (itemStack != null && !itemStack.getType().equals(Material.AIR) && selectedMat.match(itemStack)) {
            ShopItem shopItem = this.plugin.createItem.matchShopItem(itemStack, player, "sellallitem");
            if (shopItem != null && shopItem.isSellAble()) {
               int amount = itemStack.getAmount();
               if (shopItem.isMaxSell(map2.getOrDefault(shopItem, new Sold()).getAmount() + amount)) {
                  if (map2.getOrDefault(shopItem, new Sold()).getAmount() >= shopItem.getMaxSell()) {
                     continue;
                  }

                  amount = shopItem.getMaxSell() - map2.getOrDefault(shopItem, new Sold()).getAmount();
               }

               if (shopItem.getLimitedSellMode() != 0 && this.plugin.sellLimits) {
                  int itemLimit = this.plugin.getLimitedSellProvider().getItemLimit(shopItem, player.getUniqueId(), true);
                  amount = Math.min(amount, itemLimit - map2.getOrDefault(shopItem, new Sold()).getAmount());
                  if (amount < 0) {
                     continue;
                  }
               }

               if (amount > value) {
                  amount = value;
               }

               Sold sold = map2.computeIfAbsent(shopItem, key -> new Sold());
               sold.addValues(amount, this.getPrice(shopItem, player, itemStack, amount, sold.getAmount()));
               value2 += amount;
               value -= amount;
               ItemStack itemStack2 = new ItemStack(itemStack);
               itemStack2.setAmount(itemStack.getAmount() - amount);
               if (!shopItem.isSellCommand() || shopItem.isRequireItem()) {
                  map.put(index, new SmartStack(itemStack2));
                  sold.addIndex(index);
               }

               if (value <= 0) {
                  break;
               }
            } else if (this.plugin.sellShulkers && XMaterial.isShulker(itemStack.getType())) {
               Integer value4 = this.sellShulker(map2, map, player, itemStack, "sellallitem", index);
               if (value4 != null) {
                  value2 += value4;
               }
            }
         }
      }

      for (Entry<ShopItem, Sold> entry : new HashSet<>(map2.entrySet())) {
         if (((ShopItem)entry.getKey()).isMinSell(((Sold)entry.getValue()).getAmount())) {
            value2 -= ((Sold)entry.getValue()).getAmount();
            map2.remove(entry.getKey());
            ((Sold)entry.getValue()).getIndexes().forEach(map::remove);
         }
      }

      Map<EcoType, Double> map3 = new HashMap<>();
      HashMap map4 = new HashMap();
      if (value2 > 0) {
         map3 = this.callPreTransactionEvent(this.getItems(map4, map2), this.getPrices((Map<EcoType, Double>)map3, map2), player, value2);
         if (map3 != null) {
            if (!map3.keySet().stream().allMatch(element -> element.getType() != EconomyType.ITEM) && !this.canPay(player, (Map<EcoType, Double>)map3)) {
               SendMessage.chatToPlayer(player, Lang.CANNOT_STORE_PAYMENT.get(player));
               result = Transaction.Result.CANT_STORE_PAYMENT;
            } else {
               this.removeItems(player.getInventory(), map);
               map2.forEach((key, value3) -> {
                  if (key.isSellCommand()) {
                     CommandHelper.executeCommandItem(key, player, CommandType.SELL, value3.getPrices(), value3.getAmount());
                  }
               });
               if (map4.size() != 1) {
                  SendMessage.sendTransactionMessage(player, value2, (Map<EcoType, Double>)map3, map4, Transaction.Type.SELL_ALL_COMMAND);
               } else {
                  ShopItem shopItem = (ShopItem)map4.keySet().stream().findFirst().get();
                  SendMessage.sendTransactionMessage(
                     player,
                     value2,
                     (Map<EcoType, Double>)map3,
                     shopItem,
                     Transaction.Mode.SELL,
                     Transaction.Type.SELL_ALL_COMMAND,
                     shopItem.isSellCommand() && !shopItem.isRequireItem()
                  );
               }

               this.plugin.getEcoHandler().deposit((Map<EcoType, Double>)map3, player);
               this.sellItems(map4, player.getUniqueId());
            }
         } else {
            result = Transaction.Result.TRANSACTION_CANCELLED;
         }
      } else {
         result = Transaction.Result.NO_ITEMS_FOUND;
         SendMessage.chatToPlayer(player, Lang.NO_ITEM_FOUND.get(player));
      }

      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(map4, (Map<EcoType, Double>)map3, player, value2, Transaction.Type.SELL_ALL_COMMAND, result));
   }

   private Integer sellShulker(Map<ShopItem, Sold> map, Map<Integer, SmartStack> map2, Player player, ItemStack itemStack, String shopId, int value) {
      HashMap<ShopItem, Sold> map3 = new HashMap<>();
      HashMap map4 = new HashMap();
      int value2 = 0;
      if (itemStack.getItemMeta() instanceof BlockStateMeta) {
         BlockStateMeta blockStateMeta = (BlockStateMeta)itemStack.getItemMeta();
         if (blockStateMeta.getBlockState() instanceof ShulkerBox) {
            Inventory inventory = ((ShulkerBox)blockStateMeta.getBlockState()).getInventory();
            if (inventory.isEmpty()) {
               return null;
            }

            for (int index = 0; index < 27; index++) {
               ItemStack itemStack2 = inventory.getItem(index);
               if (itemStack2 != null && !itemStack2.getType().equals(Material.AIR)) {
                  ShopItem shopItem = this.plugin.createItem.matchShopItem(itemStack2, player, shopId);
                  if (shopItem != null && shopItem.isSellAble()) {
                     int amount = itemStack2.getAmount();
                     if (shopItem.isMaxSell(map3.getOrDefault(shopItem, new Sold()).getAmount() + map.getOrDefault(shopItem, new Sold()).getAmount() + amount)) {
                        if (map3.getOrDefault(shopItem, new Sold()).getAmount() + map.getOrDefault(shopItem, new Sold()).getAmount() >= shopItem.getMaxSell()) {
                           continue;
                        }

                        amount = shopItem.getMaxSell() - (map3.getOrDefault(shopItem, new Sold()).getAmount() + map.getOrDefault(shopItem, new Sold()).getAmount());
                     }

                     if (shopItem.getLimitedSellMode() != 0 && this.plugin.sellLimits) {
                        int itemLimit = this.plugin.getLimitedSellProvider().getItemLimit(shopItem, player.getUniqueId(), true);
                        amount = Math.min(amount, itemLimit - (map3.getOrDefault(shopItem, new Sold()).getAmount() + map.getOrDefault(shopItem, new Sold()).getAmount()));
                        if (amount < 0) {
                           continue;
                        }
                     }

                     Sold sold = map3.computeIfAbsent(shopItem, key -> new Sold());
                     sold.addValues(amount, this.getPrice(shopItem, player, itemStack2, amount, sold.getAmount()));
                     value2 += amount;
                     ItemStack itemStack3 = new ItemStack(itemStack2);
                     itemStack3.setAmount(itemStack2.getAmount() - amount);
                     if (!shopItem.isSellCommand() || shopItem.isRequireItem()) {
                        map4.put(index, itemStack3);
                        sold.addIndex(index);
                     }
                  }
               }
            }

            for (Entry<ShopItem, Sold> entry : new HashSet<>(map3.entrySet())) {
               if (((ShopItem)entry.getKey()).isMinSell(((Sold)entry.getValue()).getAmount())) {
                  map3.remove(entry.getKey());
                  value2 -= ((Sold)entry.getValue()).getAmount();
                  ((Sold)entry.getValue()).getIndexes().forEach(map4::remove);
               }
            }

            if (!map3.isEmpty()) {
               ItemStack itemStack2 = new ItemStack(itemStack);
               this.emptyShulker(new SmartStack.Shulker(itemStack2, map4));
               ShopItem shopItem = this.plugin.createItem.matchShopItem(itemStack2);
               if (shopItem != null && shopItem.getLimitedSellMode() != 0 && this.plugin.getLimitedSellProvider().getItemLimit(shopItem, player.getUniqueId(), true) > 0) {
                  value2++;
                  map2.put(value, new SmartStack(new ItemStack(Material.AIR)));
                  map.put(
                     new ShopItem.Shulker(
                        shopItem, itemStack2, map3.entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> ((Sold)entry.getValue()).getAmount()))
                     ),
                     map3.computeIfAbsent(shopItem, key -> new Sold())
                        .addValues(
                           1,
                           shopItem.isASellPricing()
                              ? shopItem.getASellPrice().getSellPrices(null, player, 1, shopItem.getSellModifiers())
                              : Collections.singletonMap(shopItem.getEcoType(), shopItem.getSellPrice(player, 1))
                        )
                  );
               } else {
                  map2.put(value, new SmartStack.Shulker(itemStack, map4));
                  map3.forEach((key, value3) -> map.merge(key, value3, Sold::addValue));
               }

               return value2;
            }
         }
      }

      return null;
   }

   private void removeItems(Inventory inventory, Map<Integer, SmartStack> map) {
      map.forEach((key, value) -> {
         if (value instanceof SmartStack.Shulker) {
            this.emptyShulker((SmartStack.Shulker)value);
         } else if (value.getItem().getAmount() != 0) {
            inventory.setItem(key, value.getItem());
         } else {
            inventory.clear(key);
         }
      });
   }

   private void emptyShulker(SmartStack.Shulker shulker) {
      ItemStack itemStack = shulker.getItem();
      BlockStateMeta blockStateMeta = (BlockStateMeta)itemStack.getItemMeta();
      ShulkerBox shulkerBox = (ShulkerBox)blockStateMeta.getBlockState();
      shulker.getItems().forEach((key, value) -> {
         if (value.getAmount() != 0) {
            shulkerBox.getInventory().setItem(key, value);
         } else {
            shulkerBox.getInventory().clear(key);
         }
      });
      blockStateMeta.setBlockState(shulkerBox);
      itemStack.setItemMeta(blockStateMeta);
   }

   private void sellItems(ShopItem shopItem, int value, UUID uniqueId) {
      if (this.plugin.sellLimits && shopItem.getLimitedSellMode() != 0) {
         this.plugin.getLimitedSellProvider().sellItems(shopItem, uniqueId, value, true);
      }

      if (this.plugin.itemStock && shopItem.isRefillStock()) {
         this.plugin.getLimitedStockProvider().sellItems(shopItem, uniqueId, value);
      }

      if (this.plugin.dynamicPricing && shopItem.isDynamicPricing()) {
         this.plugin.runTaskAsync(() -> this.plugin.getDynamicPricingProvider().sellItem(shopItem.getItemPath(), value));
      }
   }

   private void sellItems(Map<ShopItem, Integer> map, UUID uniqueId) {
      if (this.plugin.sellLimits) {
         map.forEach((key, value) -> {
            if (key.getLimitedSellMode() != 0) {
               this.plugin.getLimitedSellProvider().sellItems(key, uniqueId, value, true);
            }
         });
      }

      if (this.plugin.itemStock) {
         map.forEach((key, value) -> {
            if (key.isRefillStock()) {
               this.plugin.getLimitedStockProvider().sellItems(key, uniqueId, value);
            }
         });
      }

      if (this.plugin.dynamicPricing) {
         map.forEach((key, value) -> {
            if (key.isDynamicPricing()) {
               this.plugin.getDynamicPricingProvider().sellItem(key.getItemPath(), value);
            }
         });
      }
   }

   private double callPreTransactionEvent(ShopItem shopItem, Player player, int value, double amount) {
      PreTransactionEvent preTransactionEvent = new PreTransactionEvent(shopItem, player, value, amount, Transaction.Type.SELL_ALL_COMMAND);
      Bukkit.getPluginManager().callEvent(preTransactionEvent);
      amount = preTransactionEvent.getPrice();
      return preTransactionEvent.isCancelled() ? -1.0 : amount;
   }

   private Map<EcoType, Double> callPreTransactionEvent(Map<ShopItem, Integer> map, Map<EcoType, Double> map2, Player player, int value) {
      PreTransactionEvent preTransactionEvent = new PreTransactionEvent(map, map2, player, value, Transaction.Type.SELL_ALL_COMMAND);
      Bukkit.getPluginManager().callEvent(preTransactionEvent);
      return preTransactionEvent.isCancelled() ? null : preTransactionEvent.getPrices();
   }

   private String getCommandUsages(Player player) {
      StringBuilder builder = new StringBuilder();
      if (PermissionsCache.hasPermission(player, "OnyxShop.sellall")) {
         builder.append(Lang.SELLALL_INVENTORY_COMMAND_USAGE.get(player).getLegacy()).append("\n");
      }

      if (PermissionsCache.hasPermission(player, "OnyxShop.sellallhand")) {
         builder.append(Lang.SELLALL_HAND_COMMAND_USAGE.get(player).getLegacy()).append("\n");
      }

      if (PermissionsCache.hasPermission(player, "OnyxShop.sellallitem")) {
         builder.append(Lang.SELLALL_ITEM_COMMAND_USAGE.get(player).getLegacy()).append("\n");
      }

      return builder.toString();
   }

   private boolean hasPermissions(Player player) {
      return PermissionsCache.hasPermission(player, "OnyxShop.sellall")
         || PermissionsCache.hasPermission(player, "OnyxShop.sellallhand")
         || PermissionsCache.hasPermission(player, "OnyxShop.sellallitem");
   }

   private boolean canPay(Player player, Map<EcoType, Double> map) {
      for (EcoType ecoType : map.keySet().stream().filter(element -> element.getType() == EconomyType.ITEM).collect(Collectors.toList())) {
         ItemStack itemStack = ((ItemEconomy)OnyxShop.getInstance().getEcoHandler().getEcon(ecoType)).getItem();
         int value = 0;
         int maxStackSize = itemStack.getMaxStackSize();

         for (int index = 0; index < 36; index++) {
            ItemStack itemStack2 = player.getInventory().getItem(index);
            if (itemStack2 == null || itemStack2.getType().equals(Material.AIR)) {
               value += maxStackSize;
            } else if (itemStack2.isSimilar(itemStack) && itemStack2.getAmount() < maxStackSize) {
               value += maxStackSize - itemStack2.getAmount();
            }
         }

         if ((Double)map.get(ecoType) > value) {
            return false;
         }
      }

      return true;
   }

   private Map<EcoType, Double> getPrice(ShopItem shopItem, Player player, ItemStack itemStack, int price, int value) {
      return shopItem.isASellPricing()
         ? shopItem.getASellPrice()
            .getSellPrices(shopItem.getASellPrice().isAll() ? null : shopItem.getASellPrice().getTypes().get(0), player, itemStack, price, value, shopItem.getSellModifiers())
         : Collections.singletonMap(shopItem.getEcoType(), shopItem.getSellPrice(player, itemStack, price, value));
   }

   private Map<EcoType, Double> getPrices(ShopItem shopItem, Player player, ItemStack itemStack, int value) {
      return this.getPrices(shopItem, player, itemStack, value, 0);
   }

   private Map<EcoType, Double> getPrices(ShopItem shopItem, Player player, ItemStack itemStack, int value, int value2) {
      return shopItem.isASellPricing()
         ? shopItem.getASellPrice()
            .getSellPrices(shopItem.getASellPrice().isAll() ? null : shopItem.getASellPrice().getTypes().get(0), player, itemStack, value, value2, shopItem.getSellModifiers())
         : Collections.singletonMap(shopItem.getEcoType(), shopItem.getSellPrice(player, itemStack, value, value2));
   }

   private boolean canPay(Player player, EcoType ecoType, double amount) {
      int value = 0;
      int maxStackSize = ((ItemEconomy)this.plugin.getEcoHandler().getEcon(ecoType)).getItem().getMaxStackSize();

      for (int index = 0; index < 36; index++) {
         ItemStack itemStack = player.getInventory().getItem(index);
         if (itemStack == null || itemStack.getType().equals(Material.AIR)) {
            value += maxStackSize;
         } else if (itemStack.isSimilar(((ItemEconomy)this.plugin.getEcoHandler().getEcon(ecoType)).getItem()) && itemStack.getAmount() < maxStackSize) {
            value += maxStackSize - itemStack.getAmount();
         }
      }

      return value >= amount;
   }

   private Map<EcoType, Double> getPrices(Map<EcoType, Double> map, Map<ShopItem, Sold> map2) {
      for (Entry entry : map2.entrySet()) {
         ((Sold)entry.getValue()).getPrices().forEach((key, value) -> map.merge(key, value, Double::sum));
      }

      return map;
   }

   private Map<ShopItem, Integer> getItems(Map<ShopItem, Integer> map, Map<ShopItem, Sold> map2) {
      for (Entry entry : map2.entrySet()) {
         map.merge((ShopItem)entry.getKey(), ((Sold)entry.getValue()).getAmount(), Integer::sum);
      }

      return map;
   }

   private Map<EcoType, Double> getPrices(Map<ShopItem, Sold> map) {
      HashMap<EcoType, Double> map2 = new HashMap<>();

      for (Entry<ShopItem, Sold> entry : map.entrySet()) {
         entry.getValue().getPrices().forEach((key, value) -> map2.merge(key, value, Double::sum));
      }

      return map2;
   }

   private Map<ShopItem, Integer> getItems(Map<ShopItem, Sold> map) {
      HashMap<ShopItem, Integer> map2 = new HashMap<>();

      for (Entry<ShopItem, Sold> entry : map.entrySet()) {
         map2.put(entry.getKey(), map2.getOrDefault(entry.getKey(), 0) + entry.getValue().getAmount());
      }

      return map2;
   }

   @Override
   public List<String> tabComplete(CommandSender sender, String command, String[] values) {
      if (PermissionsCache.hasPermission(sender, "OnyxShop.sellall")
         && PermissionsCache.hasPermission(sender, "OnyxShop.sellallhand")
         && PermissionsCache.hasPermission(sender, "OnyxShop.sellallitem")) {
         if (values.length == 1) {
            ArrayList values2 = new ArrayList<>(this.plugin.getSupportedMatNames());
            values2.addAll(this.plugin.getItemManager().getAllLoaded());
            values2.add("hand");
            values2.add("inventory");
            if (!values[0].isEmpty()) {
               ArrayList values3 = new ArrayList();
               StringUtil.copyPartialMatches(values[0], values2, values3);
               Collections.sort(values3);
               return values3;
            } else {
               return values2;
            }
         } else if (values.length != 2) {
            return Collections.emptyList();
         } else if (!values[1].isEmpty()) {
            ArrayList values2 = new ArrayList();
            StringUtil.copyPartialMatches(values[1], Arrays.asList("8", "16", "32", "64", "128", "256", "512", "1024"), values2);
            Collections.sort(values2);
            return values2;
         } else {
            return Arrays.asList("8", "16", "32", "64", "128", "256", "512", "1024");
         }
      } else {
         return Collections.emptyList();
      }
   }

   private final class SelectedMat {
      private final Object item;

      public SelectedMat(Player player, String materialName) {
         ItemManager.CustomItem customItem = Sellall.this.plugin.getItemManager().getRegistered(materialName);
         if (customItem != null) {
            this.item = customItem;
         } else {
            Optional optional = XMaterial.matchXMaterial(materialName);
            if (optional.isPresent()) {
               XMaterial xMaterial = (XMaterial)optional.get();
               if (!xMaterial.isSupported()) {
                  SendMessage.warnMessage(player, Lang.MATERIAL_NOT_SUPPORTED.get(player) + " (" + materialName + ")");
                  throw new NullPointerException();
               } else if (xMaterial == XMaterial.AIR) {
                  SendMessage.warnMessage(player, Lang.CANNOT_SELL_AIR.get(player));
                  throw new NullPointerException();
               } else {
                  this.item = xMaterial;
               }
            } else {
               SendMessage.warnMessage(player, Lang.INVALID_ITEM_TYPE.get(player).replace("%type%", materialName));
               throw new NullPointerException();
            }
         }
      }

      public boolean match(ItemStack itemStack) {
         return this.item instanceof ItemManager.CustomItem
            ? ((ItemManager.CustomItem)this.item).match(itemStack)
            : XMaterial.matchXMaterial(itemStack).equals(this.item);
      }
   }
}
