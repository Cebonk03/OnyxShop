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

   public Sellall(OnyxShop var1, List<String> var2, List<String> var3) {
      super(
         var1,
         (String)var2.get(0),
         "Sell all items from your inventory, hand items or specific items",
         "/" + (String)var2.remove(0) + " <inventory/item/hand> [qty]",
         null,
         var2,
         var3
      );
   }

   @Override
   public boolean execute(CommandSender var1, String var2, String[] var3) {
      if (ConfigManager.badYaml != null) {
         SendMessage.sendMessage(var1, ChatColor.DARK_RED + "This command cannot be executed now, please fix the configuration formatting first!");
         return true;
      } else {
         if (var1 instanceof Player) {
            Player var4 = (Player)var1;
            if (!this.canUse(var4)) {
               return true;
            }

            if (var3.length == 0) {
               if (this.oldSellallCommand) {
                  if (PermissionsCache.hasPermission(var4, "OnyxShop.sellall")) {
                     this.sellInventory(var4);
                  } else {
                     SendMessage.chatToPlayer(var4, Lang.NO_PERMISSIONS.get(var4));
                  }
               } else if (this.hasPermissions(var4)) {
                  String var5 = this.getCommandUsages(var4);
                  if (!var5.isEmpty()) {
                     SendMessage.chatToPlayer(var4, Lang.SELLALL_COMMAND_USAGES.get(var4));
                     SendMessage.chatToPlayer(var4, var5);
                  } else {
                     SendMessage.chatToPlayer(var4, Lang.NO_PERMISSIONS.get(var4));
                  }
               } else {
                  SendMessage.chatToPlayer(var4, Lang.NO_PERMISSIONS.get(var4));
               }
            } else {
               String var13 = var3[0].toUpperCase(Locale.ENGLISH);
               switch (var13) {
                  case "INVENTORY":
                     if (PermissionsCache.hasPermission(var4, "OnyxShop.sellall")) {
                        this.sellInventory(var4);
                     } else {
                        SendMessage.chatToPlayer(var4, Lang.NO_PERMISSIONS.get(var4));
                     }
                     break;
                  case "HAND":
                     if (PermissionsCache.hasPermission(var4, "OnyxShop.sellallhand")) {
                        int var14 = 0;
                        if (var3.length >= 2) {
                           try {
                              var14 = Integer.parseInt(var3[1]);
                              if (var14 <= 0) {
                                 throw new NumberFormatException();
                              }
                           } catch (NumberFormatException var12) {
                              SendMessage.chatToPlayer(var4, Lang.NO_VALID_AMOUNT.get(var4));
                              return true;
                           }
                        }

                        this.sellHand(var4, var14);
                     } else {
                        SendMessage.chatToPlayer(var4, Lang.NO_PERMISSIONS.get(var4));
                     }
                     break;
                  default:
                     if (PermissionsCache.hasPermission(var4, "OnyxShop.sellallitem")) {
                        Sellall.SelectedMat var7;
                        try {
                           var7 = new Sellall.SelectedMat(var4, var3[0]);
                        } catch (NullPointerException var11) {
                           return true;
                        }

                        int var8;
                        if (var3.length >= 2) {
                           try {
                              var8 = Integer.parseInt(var3[1]);
                              if (var8 <= 0) {
                                 throw new NumberFormatException();
                              }
                           } catch (NumberFormatException var10) {
                              SendMessage.chatToPlayer(var4, Lang.NO_VALID_AMOUNT.get(var4));
                              return true;
                           }
                        } else {
                           var8 = Integer.MAX_VALUE;
                        }

                        this.removeItems(var4, var7, var8);
                     } else {
                        SendMessage.chatToPlayer(var4, Lang.NO_PERMISSIONS.get(var4));
                     }
               }
            }
         } else {
            SendMessage.sendMessage(var1, Lang.REAL_PLAYER.get());
         }

         return true;
      }
   }

   private void sellInventory(Player var1) {
      Transaction.Result var2 = Transaction.Result.SUCCESS;
      HashMap var3 = new HashMap();
      HashMap<ShopItem, Sold> var4 = new HashMap<>();
      int var5 = 0;

      for (int var6 = 0; var6 <= 35; var6++) {
         ItemStack var7 = var1.getInventory().getItem(var6);
         if (var7 != null && var7.getType() != Material.AIR) {
            ShopItem var8 = this.plugin.createItem.matchShopItem(var7, var1, "sellall");
            if (var8 != null && var8.isSellAble()) {
               int var16 = var7.getAmount();
               if (var8.isMaxSell(var4.getOrDefault(var8, new Sold()).getAmount() + var16)) {
                  if (var4.getOrDefault(var8, new Sold()).getAmount() >= var8.getMaxSell()) {
                     continue;
                  }

                  var16 = var8.getMaxSell() - var4.getOrDefault(var8, new Sold()).getAmount();
               }

               if (var8.getLimitedSellMode() != 0 && this.plugin.sellLimits) {
                  int var10 = this.plugin.getLimitedSellProvider().getItemLimit(var8, var1.getUniqueId(), true);
                  var16 = Math.min(var16, var10 - var4.getOrDefault(var8, new Sold()).getAmount());
                  if (var16 < 0) {
                     continue;
                  }
               }

               Sold var17 = var4.computeIfAbsent(var8, var0 -> new Sold());
               var17.addValues(var16, this.getPrice(var8, var1, var7, var16, var17.getAmount()));
               var5 += var16;
               ItemStack var11 = new ItemStack(var7);
               var11.setAmount(var11.getAmount() - var16);
               if (!var8.isSellCommand() || var8.isRequireItem()) {
                  var3.put(var6, new SmartStack(var11));
                  var17.addIndex(var6);
               }
            } else if (this.plugin.sellShulkers && XMaterial.isShulker(var7.getType())) {
               Integer var9 = this.sellShulker(var4, var3, var1, var7, "sellall", var6);
               if (var9 != null) {
                  var5 += var9;
               }
            }
         }
      }

      for (Entry<ShopItem, Sold> var14 : new HashSet<>(var4.entrySet())) {
         if (((ShopItem)var14.getKey()).isMinSell(((Sold)var14.getValue()).getAmount())) {
            var4.remove(var14.getKey());
            var5 -= ((Sold)var14.getValue()).getAmount();
            ((Sold)var14.getValue()).getIndexes().forEach(var3::remove);
         }
      }

      Map<EcoType, Double> var13 = new HashMap<>();
      HashMap var15 = new HashMap();
      if (var5 > 0) {
         var13 = this.callPreTransactionEvent(this.getItems(var15, var4), this.getPrices((Map<EcoType, Double>)var13, var4), var1, var5);
         if (var13 != null) {
            if (!var13.keySet().stream().allMatch(var0 -> var0.getType() != EconomyType.ITEM) && !this.canPay(var1, (Map<EcoType, Double>)var13)) {
               SendMessage.chatToPlayer(var1, Lang.CANNOT_STORE_PAYMENT.get(var1));
               var2 = Transaction.Result.CANT_STORE_PAYMENT;
            } else {
               this.removeItems(var1.getInventory(), var3);
               var4.forEach((var1x, var2x) -> {
                  if (var1x.isSellCommand()) {
                     CommandHelper.executeCommandItem(var1x, var1, CommandType.SELL, var2x.getPrices(), var2x.getAmount());
                  }
               });
               this.plugin.getEcoHandler().deposit((Map<EcoType, Double>)var13, var1);
               SendMessage.sendTransactionMessage(var1, var5, (Map<EcoType, Double>)var13, var15, Transaction.Type.SELL_ALL_COMMAND);
               this.plugin.runTaskAsync(() -> this.sellItems(var15, var1.getUniqueId()));
            }
         } else {
            var2 = Transaction.Result.TRANSACTION_CANCELLED;
         }
      } else {
         var2 = Transaction.Result.NO_ITEMS_FOUND;
         SendMessage.chatToPlayer(var1, Lang.NO_ITEM_FOUND.get(var1));
      }

      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(var15, (Map<EcoType, Double>)var13, var1, var5, Transaction.Type.SELL_ALL_COMMAND, var2));
   }

   private void sellHand(Player var1, int var2) {
      Transaction.Result var3 = Transaction.Result.SUCCESS;
      ItemStack var4 = this.plugin.versionHandler.getItemInHand(var1);
      ShopItem var5 = null;
      Object var6 = new HashMap();
      if (var4 != null && var4.getType() != Material.AIR) {
         var5 = this.plugin.createItem.matchShopItem(var4, var1, "sellallhand");
         if (var5 != null) {
            if (var5.isSellAble()) {
               if (var2 == 0 || var2 >= var4.getAmount()) {
                  var2 = var4.getAmount();
               }

               if (var5.getLimitedSellMode() != 0 && this.plugin.sellLimits) {
                  int var7 = this.plugin.getLimitedSellProvider().getItemLimit(var5, var1.getUniqueId(), true);
                  var2 = Math.min(var7, var2);
               }

               if (var5.isMaxSell(var2)) {
                  var2 = var5.getMaxSell();
               }

               if (var2 > 0) {
                  if (!var5.isMinSell(var2)) {
                     var6 = this.callPreTransactionEvent(Collections.singletonMap(var5, var2), this.getPrices(var5, var1, var4, var2), var1, var2);
                     if (var6 != null) {
                        if (var5.getEcoType().getType() == EconomyType.ITEM && !this.canPay(var1, (Map<EcoType, Double>)var6)) {
                           SendMessage.chatToPlayer(var1, Lang.CANNOT_STORE_PAYMENT.get(var1));
                           var3 = Transaction.Result.CANT_STORE_PAYMENT;
                        } else {
                           if (!var5.isSellCommand() || var5.isRequireItem()) {
                              if (var2 == var4.getAmount()) {
                                 if (this.plugin.version <= 110) {
                                    var1.getInventory().removeItem(new ItemStack[]{var4});
                                 } else {
                                    var4.setAmount(0);
                                 }
                              } else {
                                 var4.setAmount(var4.getAmount() - var2);
                              }
                           }

                           this.plugin.getEcoHandler().deposit((Map<EcoType, Double>)var6, var1);
                           SendMessage.sendTransactionMessage(
                              var1,
                              var2,
                              (Map<EcoType, Double>)var6,
                              var5,
                              Transaction.Mode.SELL,
                              Transaction.Type.SELL_ALL_COMMAND,
                              var5.isSellCommand() && !var5.isRequireItem()
                           );
                           this.sellItems(var5, var2, var1.getUniqueId());
                           CommandHelper.executeCommandItem(var5, var1, CommandType.SELL, var6.toString(), var2);
                        }
                     } else {
                        var3 = Transaction.Result.TRANSACTION_CANCELLED;
                     }
                  } else {
                     SendMessage.chatToPlayer(
                        var1,
                        Lang.NOT_ENOUGH_ITEMS_TO_SELL
                           .get(var1)
                           .replace("%min-sell%", String.valueOf(var5.getMinSell()))
                           .replace("%quantity%", String.valueOf(var2))
                     );
                     var3 = Transaction.Result.NOT_ENOUGH_ITEMS;
                  }
               } else {
                  var3 = Transaction.Result.REACHED_SELL_LIMIT;
                  SendMessage.chatToPlayer(var1, Lang.LIMITED_ITEM_SELL_AMOUNT.get(var1));
               }
            } else {
               var3 = Transaction.Result.NEGATIVE_ITEM_PRICE;
               SendMessage.chatToPlayer(var1, Lang.CANNOT_SELL_ITEM.get(var1));
            }
         } else if (this.plugin.sellShulkers && XMaterial.isShulker(var4.getType())) {
            HashMap var12 = new HashMap();
            HashMap<ShopItem, Sold> var8 = new HashMap<>();
            Integer var9 = this.sellShulker(var8, var12, var1, var4, "sellallhand", var1.getInventory().getHeldItemSlot());
            if (var9 != null) {
               Map<EcoType, Double> var10 = this.getPrices(var8);
               if (!var10.keySet().stream().allMatch(var0 -> var0.getType() != EconomyType.ITEM) && !this.canPay(var1, var10)) {
                  SendMessage.chatToPlayer(var1, Lang.CANNOT_STORE_PAYMENT.get(var1));
                  var3 = Transaction.Result.CANT_STORE_PAYMENT;
               } else {
                  Map<ShopItem, Integer> var11 = this.getItems(var8);
                  var10 = this.callPreTransactionEvent(var11, var10, var1, var9);
                  if (var10 != null) {
                     this.removeItems(var1.getInventory(), var12);
                     this.plugin.getEcoHandler().deposit(var10, var1);
                     var8.forEach((var1x, var2x) -> {
                        if (var1x.isSellCommand()) {
                           CommandHelper.executeCommandItem(var1x, var1, CommandType.SELL, var2x.getPrices(), var2x.getAmount());
                        }
                     });
                     SendMessage.sendTransactionMessage(var1, var9, var10, var11, Transaction.Type.SELL_ALL_COMMAND);
                  } else {
                     var3 = Transaction.Result.TRANSACTION_CANCELLED;
                  }
               }
            } else {
               var3 = Transaction.Result.NO_ITEMS_FOUND;
               SendMessage.chatToPlayer(var1, Lang.NO_ITEM_FOUND.get(var1));
            }
         } else {
            var3 = Transaction.Result.NO_ITEMS_FOUND;
            SendMessage.chatToPlayer(var1, Lang.NO_ITEM_FOUND.get(var1));
         }
      } else {
         var3 = Transaction.Result.NO_ITEMS_FOUND;
         SendMessage.chatToPlayer(var1, Lang.CANNOT_SELL_AIR.get(var1));
      }

      Bukkit.getPluginManager()
         .callEvent(
            new PostTransactionEvent(Collections.singletonMap(var5, var2), (Map<EcoType, Double>)var6, var1, var2, Transaction.Type.SELL_ALL_COMMAND, var3)
         );
   }

   private void removeItems(Player var1, Sellall.SelectedMat var2, int var3) {
      Transaction.Result var4 = Transaction.Result.SUCCESS;
      HashMap var5 = new HashMap();
      HashMap<ShopItem, Sold> var6 = new HashMap<>();
      int var7 = 0;

      for (int var8 = 0; var8 < 36; var8++) {
         ItemStack var9 = var1.getInventory().getItem(var8);
         if (var9 != null && !var9.getType().equals(Material.AIR) && var2.match(var9)) {
            ShopItem var10 = this.plugin.createItem.matchShopItem(var9, var1, "sellallitem");
            if (var10 != null && var10.isSellAble()) {
               int var19 = var9.getAmount();
               if (var10.isMaxSell(var6.getOrDefault(var10, new Sold()).getAmount() + var19)) {
                  if (var6.getOrDefault(var10, new Sold()).getAmount() >= var10.getMaxSell()) {
                     continue;
                  }

                  var19 = var10.getMaxSell() - var6.getOrDefault(var10, new Sold()).getAmount();
               }

               if (var10.getLimitedSellMode() != 0 && this.plugin.sellLimits) {
                  int var12 = this.plugin.getLimitedSellProvider().getItemLimit(var10, var1.getUniqueId(), true);
                  var19 = Math.min(var19, var12 - var6.getOrDefault(var10, new Sold()).getAmount());
                  if (var19 < 0) {
                     continue;
                  }
               }

               if (var19 > var3) {
                  var19 = var3;
               }

               Sold var20 = var6.computeIfAbsent(var10, var0 -> new Sold());
               var20.addValues(var19, this.getPrice(var10, var1, var9, var19, var20.getAmount()));
               var7 += var19;
               var3 -= var19;
               ItemStack var13 = new ItemStack(var9);
               var13.setAmount(var9.getAmount() - var19);
               if (!var10.isSellCommand() || var10.isRequireItem()) {
                  var5.put(var8, new SmartStack(var13));
                  var20.addIndex(var8);
               }

               if (var3 <= 0) {
                  break;
               }
            } else if (this.plugin.sellShulkers && XMaterial.isShulker(var9.getType())) {
               Integer var11 = this.sellShulker(var6, var5, var1, var9, "sellallitem", var8);
               if (var11 != null) {
                  var7 += var11;
               }
            }
         }
      }

      for (Entry<ShopItem, Sold> var16 : new HashSet<>(var6.entrySet())) {
         if (((ShopItem)var16.getKey()).isMinSell(((Sold)var16.getValue()).getAmount())) {
            var7 -= ((Sold)var16.getValue()).getAmount();
            var6.remove(var16.getKey());
            ((Sold)var16.getValue()).getIndexes().forEach(var5::remove);
         }
      }

      Map<EcoType, Double> var15 = new HashMap<>();
      HashMap var17 = new HashMap();
      if (var7 > 0) {
         var15 = this.callPreTransactionEvent(this.getItems(var17, var6), this.getPrices((Map<EcoType, Double>)var15, var6), var1, var7);
         if (var15 != null) {
            if (!var15.keySet().stream().allMatch(var0 -> var0.getType() != EconomyType.ITEM) && !this.canPay(var1, (Map<EcoType, Double>)var15)) {
               SendMessage.chatToPlayer(var1, Lang.CANNOT_STORE_PAYMENT.get(var1));
               var4 = Transaction.Result.CANT_STORE_PAYMENT;
            } else {
               this.removeItems(var1.getInventory(), var5);
               var6.forEach((var1x, var2x) -> {
                  if (var1x.isSellCommand()) {
                     CommandHelper.executeCommandItem(var1x, var1, CommandType.SELL, var2x.getPrices(), var2x.getAmount());
                  }
               });
               if (var17.size() != 1) {
                  SendMessage.sendTransactionMessage(var1, var7, (Map<EcoType, Double>)var15, var17, Transaction.Type.SELL_ALL_COMMAND);
               } else {
                  ShopItem var18 = (ShopItem)var17.keySet().stream().findFirst().get();
                  SendMessage.sendTransactionMessage(
                     var1,
                     var7,
                     (Map<EcoType, Double>)var15,
                     var18,
                     Transaction.Mode.SELL,
                     Transaction.Type.SELL_ALL_COMMAND,
                     var18.isSellCommand() && !var18.isRequireItem()
                  );
               }

               this.plugin.getEcoHandler().deposit((Map<EcoType, Double>)var15, var1);
               this.plugin.runTaskAsync(() -> this.sellItems(var17, var1.getUniqueId()));
            }
         } else {
            var4 = Transaction.Result.TRANSACTION_CANCELLED;
         }
      } else {
         var4 = Transaction.Result.NO_ITEMS_FOUND;
         SendMessage.chatToPlayer(var1, Lang.NO_ITEM_FOUND.get(var1));
      }

      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(var17, (Map<EcoType, Double>)var15, var1, var7, Transaction.Type.SELL_ALL_COMMAND, var4));
   }

   private Integer sellShulker(Map<ShopItem, Sold> var1, Map<Integer, SmartStack> var2, Player var3, ItemStack var4, String var5, int var6) {
      HashMap<ShopItem, Sold> var7 = new HashMap<>();
      HashMap var8 = new HashMap();
      int var9 = 0;
      if (var4.getItemMeta() instanceof BlockStateMeta) {
         BlockStateMeta var10 = (BlockStateMeta)var4.getItemMeta();
         if (var10.getBlockState() instanceof ShulkerBox) {
            Inventory var11 = ((ShulkerBox)var10.getBlockState()).getInventory();
            if (var11.isEmpty()) {
               return null;
            }

            for (int var12 = 0; var12 < 27; var12++) {
               ItemStack var13 = var11.getItem(var12);
               if (var13 != null && !var13.getType().equals(Material.AIR)) {
                  ShopItem var14 = this.plugin.createItem.matchShopItem(var13, var3, var5);
                  if (var14 != null && var14.isSellAble()) {
                     int var15 = var13.getAmount();
                     if (var14.isMaxSell(var7.getOrDefault(var14, new Sold()).getAmount() + var1.getOrDefault(var14, new Sold()).getAmount() + var15)) {
                        if (var7.getOrDefault(var14, new Sold()).getAmount() + var1.getOrDefault(var14, new Sold()).getAmount() >= var14.getMaxSell()) {
                           continue;
                        }

                        var15 = var14.getMaxSell() - (var7.getOrDefault(var14, new Sold()).getAmount() + var1.getOrDefault(var14, new Sold()).getAmount());
                     }

                     if (var14.getLimitedSellMode() != 0 && this.plugin.sellLimits) {
                        int var16 = this.plugin.getLimitedSellProvider().getItemLimit(var14, var3.getUniqueId(), true);
                        var15 = Math.min(var15, var16 - (var7.getOrDefault(var14, new Sold()).getAmount() + var1.getOrDefault(var14, new Sold()).getAmount()));
                        if (var15 < 0) {
                           continue;
                        }
                     }

                     Sold var22 = var7.computeIfAbsent(var14, var0 -> new Sold());
                     var22.addValues(var15, this.getPrice(var14, var3, var13, var15, var22.getAmount()));
                     var9 += var15;
                     ItemStack var17 = new ItemStack(var13);
                     var17.setAmount(var13.getAmount() - var15);
                     if (!var14.isSellCommand() || var14.isRequireItem()) {
                        var8.put(var12, var17);
                        var22.addIndex(var12);
                     }
                  }
               }
            }

            for (Entry<ShopItem, Sold> var20 : new HashSet<>(var7.entrySet())) {
               if (((ShopItem)var20.getKey()).isMinSell(((Sold)var20.getValue()).getAmount())) {
                  var7.remove(var20.getKey());
                  var9 -= ((Sold)var20.getValue()).getAmount();
                  ((Sold)var20.getValue()).getIndexes().forEach(var8::remove);
               }
            }

            if (!var7.isEmpty()) {
               ItemStack var19 = new ItemStack(var4);
               this.emptyShulker(new SmartStack.Shulker(var19, var8));
               ShopItem var21 = this.plugin.createItem.matchShopItem(var19);
               if (var21 != null && var21.getLimitedSellMode() != 0 && this.plugin.getLimitedSellProvider().getItemLimit(var21, var3.getUniqueId(), true) > 0) {
                  var9++;
                  var2.put(var6, new SmartStack(new ItemStack(Material.AIR)));
                  var1.put(
                     new ShopItem.Shulker(
                        var21, var19, var7.entrySet().stream().collect(Collectors.toMap(Entry::getKey, var0 -> ((Sold)var0.getValue()).getAmount()))
                     ),
                     var7.computeIfAbsent(var21, var0 -> new Sold())
                        .addValues(
                           1,
                           var21.isASellPricing()
                              ? var21.getASellPrice().getSellPrices(null, var3, 1, var21.getSellModifiers())
                              : Collections.singletonMap(var21.getEcoType(), var21.getSellPrice(var3, 1))
                        )
                  );
               } else {
                  var2.put(var6, new SmartStack.Shulker(var4, var8));
                  var7.forEach((var1x, var2x) -> var1.merge(var1x, var2x, Sold::addValue));
               }

               return var9;
            }
         }
      }

      return null;
   }

   private void removeItems(Inventory var1, Map<Integer, SmartStack> var2) {
      var2.forEach((var2x, var3) -> {
         if (var3 instanceof SmartStack.Shulker) {
            this.emptyShulker((SmartStack.Shulker)var3);
         } else if (var3.getItem().getAmount() != 0) {
            var1.setItem(var2x, var3.getItem());
         } else {
            var1.clear(var2x);
         }
      });
   }

   private void emptyShulker(SmartStack.Shulker var1) {
      ItemStack var2 = var1.getItem();
      BlockStateMeta var3 = (BlockStateMeta)var2.getItemMeta();
      ShulkerBox var4 = (ShulkerBox)var3.getBlockState();
      var1.getItems().forEach((var1x, var2x) -> {
         if (var2x.getAmount() != 0) {
            var4.getInventory().setItem(var1x, var2x);
         } else {
            var4.getInventory().clear(var1x);
         }
      });
      var3.setBlockState(var4);
      var2.setItemMeta(var3);
   }

   private void sellItems(ShopItem var1, int var2, UUID var3) {
      this.plugin.runTaskAsync(() -> {
         if (this.plugin.sellLimits && var1.getLimitedSellMode() != 0) {
            this.plugin.getLimitedSellProvider().sellItems(var1, var3, var2, true);
         }

         if (this.plugin.itemStock && var1.isRefillStock()) {
            this.plugin.getLimitedStockProvider().sellItems(var1, var3, var2);
         }

         if (this.plugin.dynamicPricing && var1.isDynamicPricing()) {
            this.plugin.getDynamicPricingProvider().sellItem(var1.getItemPath(), var2);
         }
      });
   }

   private void sellItems(Map<ShopItem, Integer> var1, UUID var2) {
      if (this.plugin.sellLimits) {
         var1.forEach((var2x, var3) -> {
            if (var2x.getLimitedSellMode() != 0) {
               this.plugin.getLimitedSellProvider().sellItems(var2x, var2, var3, true);
            }
         });
      }

      if (this.plugin.itemStock) {
         var1.forEach((var2x, var3) -> {
            if (var2x.isRefillStock()) {
               this.plugin.getLimitedStockProvider().sellItems(var2x, var2, var3);
            }
         });
      }

      if (this.plugin.dynamicPricing) {
         var1.forEach((var1x, var2x) -> {
            if (var1x.isDynamicPricing()) {
               this.plugin.getDynamicPricingProvider().sellItem(var1x.getItemPath(), var2x);
            }
         });
      }
   }

   private double callPreTransactionEvent(ShopItem var1, Player var2, int var3, double var4) {
      PreTransactionEvent var6 = new PreTransactionEvent(var1, var2, var3, var4, Transaction.Type.SELL_ALL_COMMAND);
      Bukkit.getPluginManager().callEvent(var6);
      var4 = var6.getPrice();
      return var6.isCancelled() ? -1.0 : var4;
   }

   private Map<EcoType, Double> callPreTransactionEvent(Map<ShopItem, Integer> var1, Map<EcoType, Double> var2, Player var3, int var4) {
      PreTransactionEvent var5 = new PreTransactionEvent(var1, var2, var3, var4, Transaction.Type.SELL_ALL_COMMAND);
      Bukkit.getPluginManager().callEvent(var5);
      return var5.isCancelled() ? null : var5.getPrices();
   }

   private String getCommandUsages(Player var1) {
      StringBuilder var2 = new StringBuilder();
      if (PermissionsCache.hasPermission(var1, "OnyxShop.sellall")) {
         var2.append(Lang.SELLALL_INVENTORY_COMMAND_USAGE.get(var1).getLegacy()).append("\n");
      }

      if (PermissionsCache.hasPermission(var1, "OnyxShop.sellallhand")) {
         var2.append(Lang.SELLALL_HAND_COMMAND_USAGE.get(var1).getLegacy()).append("\n");
      }

      if (PermissionsCache.hasPermission(var1, "OnyxShop.sellallitem")) {
         var2.append(Lang.SELLALL_ITEM_COMMAND_USAGE.get(var1).getLegacy()).append("\n");
      }

      return var2.toString();
   }

   private boolean hasPermissions(Player var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.sellall")
         || PermissionsCache.hasPermission(var1, "OnyxShop.sellallhand")
         || PermissionsCache.hasPermission(var1, "OnyxShop.sellallitem");
   }

   private boolean canPay(Player var1, Map<EcoType, Double> var2) {
      for (EcoType var4 : var2.keySet().stream().filter(var0 -> var0.getType() == EconomyType.ITEM).collect(Collectors.toList())) {
         ItemStack var5 = ((ItemEconomy)OnyxShop.getInstance().getEcoHandler().getEcon(var4)).getItem();
         int var6 = 0;
         int var7 = var5.getMaxStackSize();

         for (int var8 = 0; var8 < 36; var8++) {
            ItemStack var9 = var1.getInventory().getItem(var8);
            if (var9 == null || var9.getType().equals(Material.AIR)) {
               var6 += var7;
            } else if (var9.isSimilar(var5) && var9.getAmount() < var7) {
               var6 += var7 - var9.getAmount();
            }
         }

         if ((Double)var2.get(var4) > var6) {
            return false;
         }
      }

      return true;
   }

   private Map<EcoType, Double> getPrice(ShopItem var1, Player var2, ItemStack var3, int var4, int var5) {
      return var1.isASellPricing()
         ? var1.getASellPrice()
            .getSellPrices(var1.getASellPrice().isAll() ? null : var1.getASellPrice().getTypes().get(0), var2, var3, var4, var5, var1.getSellModifiers())
         : Collections.singletonMap(var1.getEcoType(), var1.getSellPrice(var2, var3, var4, var5));
   }

   private Map<EcoType, Double> getPrices(ShopItem var1, Player var2, ItemStack var3, int var4) {
      return this.getPrices(var1, var2, var3, var4, 0);
   }

   private Map<EcoType, Double> getPrices(ShopItem var1, Player var2, ItemStack var3, int var4, int var5) {
      return var1.isASellPricing()
         ? var1.getASellPrice()
            .getSellPrices(var1.getASellPrice().isAll() ? null : var1.getASellPrice().getTypes().get(0), var2, var3, var4, var5, var1.getSellModifiers())
         : Collections.singletonMap(var1.getEcoType(), var1.getSellPrice(var2, var3, var4, var5));
   }

   private boolean canPay(Player var1, EcoType var2, double var3) {
      int var5 = 0;
      int var6 = ((ItemEconomy)this.plugin.getEcoHandler().getEcon(var2)).getItem().getMaxStackSize();

      for (int var7 = 0; var7 < 36; var7++) {
         ItemStack var8 = var1.getInventory().getItem(var7);
         if (var8 == null || var8.getType().equals(Material.AIR)) {
            var5 += var6;
         } else if (var8.isSimilar(((ItemEconomy)this.plugin.getEcoHandler().getEcon(var2)).getItem()) && var8.getAmount() < var6) {
            var5 += var6 - var8.getAmount();
         }
      }

      return var5 >= var3;
   }

   private Map<EcoType, Double> getPrices(Map<EcoType, Double> var1, Map<ShopItem, Sold> var2) {
      for (Entry var4 : var2.entrySet()) {
         ((Sold)var4.getValue()).getPrices().forEach((var1x, var2x) -> var1.merge(var1x, var2x, Double::sum));
      }

      return var1;
   }

   private Map<ShopItem, Integer> getItems(Map<ShopItem, Integer> var1, Map<ShopItem, Sold> var2) {
      for (Entry var4 : var2.entrySet()) {
         var1.merge((ShopItem)var4.getKey(), ((Sold)var4.getValue()).getAmount(), Integer::sum);
      }

      return var1;
   }

   private Map<EcoType, Double> getPrices(Map<ShopItem, Sold> var1) {
      HashMap<EcoType, Double> var2 = new HashMap<>();

      for (Entry<ShopItem, Sold> var4 : var1.entrySet()) {
         var4.getValue().getPrices().forEach((var1x, var2x) -> var2.merge(var1x, var2x, Double::sum));
      }

      return var2;
   }

   private Map<ShopItem, Integer> getItems(Map<ShopItem, Sold> var1) {
      HashMap<ShopItem, Integer> var2 = new HashMap<>();

      for (Entry<ShopItem, Sold> var4 : var1.entrySet()) {
         var2.put(var4.getKey(), var2.getOrDefault(var4.getKey(), 0) + var4.getValue().getAmount());
      }

      return var2;
   }

   @Override
   public List<String> tabComplete(CommandSender var1, String var2, String[] var3) {
      if (PermissionsCache.hasPermission(var1, "OnyxShop.sellall")
         && PermissionsCache.hasPermission(var1, "OnyxShop.sellallhand")
         && PermissionsCache.hasPermission(var1, "OnyxShop.sellallitem")) {
         if (var3.length == 1) {
            ArrayList var6 = new ArrayList<>(this.plugin.getSupportedMatNames());
            var6.addAll(this.plugin.getItemManager().getAllLoaded());
            var6.add("hand");
            var6.add("inventory");
            if (!var3[0].isEmpty()) {
               ArrayList var5 = new ArrayList();
               StringUtil.copyPartialMatches(var3[0], var6, var5);
               Collections.sort(var5);
               return var5;
            } else {
               return var6;
            }
         } else if (var3.length != 2) {
            return Collections.emptyList();
         } else if (!var3[1].isEmpty()) {
            ArrayList var4 = new ArrayList();
            StringUtil.copyPartialMatches(var3[1], Arrays.asList("8", "16", "32", "64", "128", "256", "512", "1024"), var4);
            Collections.sort(var4);
            return var4;
         } else {
            return Arrays.asList("8", "16", "32", "64", "128", "256", "512", "1024");
         }
      } else {
         return Collections.emptyList();
      }
   }

   private final class SelectedMat {
      private final Object item;

      public SelectedMat(Player nullx, String nullxx) {
         ItemManager.CustomItem var4 = Sellall.this.plugin.getItemManager().getRegistered(nullxx);
         if (var4 != null) {
            this.item = var4;
         } else {
            Optional var5 = XMaterial.matchXMaterial(nullxx);
            if (var5.isPresent()) {
               XMaterial var6 = (XMaterial)var5.get();
               if (!var6.isSupported()) {
                  SendMessage.warnMessage(nullx, Lang.MATERIAL_NOT_SUPPORTED.get(nullx) + " (" + nullxx + ")");
                  throw new NullPointerException();
               } else if (var6 == XMaterial.AIR) {
                  SendMessage.warnMessage(nullx, Lang.CANNOT_SELL_AIR.get(nullx));
                  throw new NullPointerException();
               } else {
                  this.item = var6;
               }
            } else {
               SendMessage.warnMessage(nullx, Lang.INVALID_ITEM_TYPE.get(nullx).replace("%type%", nullxx));
               throw new NullPointerException();
            }
         }
      }

      public boolean match(ItemStack var1) {
         return this.item instanceof ItemManager.CustomItem
            ? ((ItemManager.CustomItem)this.item).match(var1)
            : XMaterial.matchXMaterial(var1).equals(this.item);
      }
   }
}
