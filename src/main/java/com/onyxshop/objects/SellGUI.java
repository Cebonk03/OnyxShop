package com.onyxshop.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.api.events.PostTransactionEvent;
import com.onyxshop.api.events.PreTransactionEvent;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.providers.economys.ItemEconomy;
import com.onyxshop.util.CommandHelper;
import com.onyxshop.util.CommandType;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.EconomyType;
import com.onyxshop.util.LocalizedItemStack;
import com.onyxshop.util.SmartStack;
import com.onyxshop.util.Transaction;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.cache.Sold;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

public class SellGUI extends ShopInventory {
   private Inventory inv;
   private final OnyxShop plugin = OnyxShop.getInstance();
   private static final Map<Integer, LocalizedItemStack> staticItems = new HashMap<>();
   private static final int size = Math.max(1, ConfigManager.getConfig().getInt("sellgui-screen.gui-rows", 6)) * 9;

   public SellGUI() {
   }

   public void open(Player var1) {
      this.inv = this.plugin.getMetaUtils().createInventory(this, size, TitleManager.get(TitleManager.Menu.SELL_GUI, var1));
      staticItems.forEach((var2, var3) -> this.inv.setItem(var2, var3.getItem(var1)));
      this.plugin.navBar.addSellGUINavBar(this.inv, var1);
      var1.openInventory(this.inv);
   }

   public void sellItems(Player var1) {
      Transaction.Result var2 = Transaction.Result.SUCCESS;
      HashMap<Integer, SmartStack> var3 = new HashMap<>();
      HashMap<ShopItem, Sold> var4 = new HashMap<>();
      int var5 = 0;

      for (int var6 = 0; var6 <= size - (this.plugin.navBar.isEnableSellGUINav() ? 10 : 1); var6++) {
         if (!staticItems.containsKey(var6)) {
            ItemStack var7 = this.inv.getItem(var6);
            if (var7 != null && var7.getType() != Material.AIR) {
               ShopItem var8 = this.plugin.createItem.matchShopItem(var7, var1, "sellgui");
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
                     if (var16 <= 0) {
                        continue;
                     }
                  }

                  Sold var17 = var4.computeIfAbsent(var8, var0 -> new Sold());
                  var17.addValues(var16, this.getPrices(var8, var1, var7, var16, var17.getAmount()));
                  var5 += var16;
                  ItemStack var11 = new ItemStack(var7);
                  var11.setAmount(var7.getAmount() - var16);
                  if (!var8.isSellCommand() || var8.isRequireItem()) {
                     var3.put(var6, new SmartStack(var11));
                     var17.addIndex(var6);
                  }
               } else if (this.plugin.sellShulkers && XMaterial.isShulker(var7.getType())) {
                  Integer var9 = this.sellShulker(var4, var3, var1, var7, "sellgui", var6);
                  if (var9 != null) {
                     var5 += var9;
                  }
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
      HashMap<ShopItem, Integer> var15 = new HashMap<>();
      if (var5 > 0) {
         var13 = this.callPreTransactionEvent(this.getItems(var15, var4), this.getPrices(var4), var1, var5);
         if (var13 != null) {
            if (!var13.keySet().stream().allMatch(var0 -> var0.getType() != EconomyType.ITEM) && !this.canPay(var1, (Map<EcoType, Double>)var13)) {
               SendMessage.chatToPlayer(var1, Lang.CANNOT_STORE_PAYMENT.get(var1));
               var2 = Transaction.Result.CANT_STORE_PAYMENT;
            } else {
               this.removeItems(this.inv, var3);
               var4.forEach((var1x, var2x) -> {
                  if (var1x.isSellCommand()) {
                     CommandHelper.executeCommandItem(var1x, var1, CommandType.SELL, var2x.getPrices(), var2x.getAmount());
                  }
               });
               this.plugin.getEcoHandler().deposit((Map<EcoType, Double>)var13, var1);
               SendMessage.sendTransactionMessage(var1, var5, (Map<EcoType, Double>)var13, var15, Transaction.Type.SELL_GUI_SCREEN);
               this.plugin.runTaskAsync(() -> this.sellItems(var15, var1.getUniqueId()));
            }
         } else {
            var2 = Transaction.Result.TRANSACTION_CANCELLED;
         }
      } else {
         var2 = Transaction.Result.NO_ITEMS_FOUND;
         SendMessage.chatToPlayer(var1, Lang.NO_ITEM_FOUND.get(var1));
      }

      this.addUnsoldItems(var1);
      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(var15, (Map<EcoType, Double>)var13, var1, var5, Transaction.Type.SELL_ALL_COMMAND, var2));
   }

   private void addUnsoldItems(Player var1) {
      for (int var2 = 0; var2 <= size - (this.plugin.navBar.isEnableSellGUINav() ? 10 : 1); var2++) {
         if (!staticItems.containsKey(var2)) {
            ItemStack var3 = this.inv.getItem(var2);
            if (var3 != null && var3.getType() != Material.AIR) {
               for (ItemStack var5 : var1.getInventory().addItem(new ItemStack[]{var3}).values()) {
                  var1.getWorld().dropItem(var1.getLocation(), var5);
               }
            }
         }
      }
   }

   private Integer sellShulker(Map<ShopItem, Sold> var1, Map<Integer, SmartStack> var2, Player var3, ItemStack var4, String var5, int var6) {
      HashMap<ShopItem, Sold> var7 = new HashMap<>();
      HashMap<Integer, ItemStack> var8 = new HashMap<>();
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

                     if (var14.getLimitedSellMode() != 0) {
                        int var16 = this.plugin.getLimitedSellProvider().getItemLimit(var14, var3.getUniqueId(), true);
                        var15 = Math.min(var15, var16 - (var7.getOrDefault(var14, new Sold()).getAmount() + var1.getOrDefault(var14, new Sold()).getAmount()));
                        if (var15 < 0) {
                           continue;
                        }
                     }

                     Sold var22 = var7.computeIfAbsent(var14, var0 -> new Sold());
                     var22.addValues(var15, this.getPrices(var14, var3, var13, var15, var22.getAmount()));
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

   private Map<EcoType, Double> getPrices(ShopItem var1, Player var2, ItemStack var3, int var4, int var5) {
      return var1.isASellPricing()
         ? var1.getASellPrice()
            .getSellPrices(var1.getASellPrice().isAll() ? null : var1.getASellPrice().getTypes().get(0), var2, var3, var4, var5, var1.getSellModifiers())
         : Collections.singletonMap(var1.getEcoType(), var1.getSellPrice(var2, var3, var4, var5));
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

   private boolean canPay(Player var1, Map<EcoType, Double> var2) {
      for (EcoType var4 : var2.keySet().stream().filter(var0 -> var0.getType() == EconomyType.ITEM).collect(Collectors.toList())) {
         ItemStack var5 = ((ItemEconomy)this.plugin.getEcoHandler().getEcon(var4)).getItem();
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

   private Map<ShopItem, Integer> getItems(Map<ShopItem, Integer> var1, Map<ShopItem, Sold> var2) {
      for (Entry var4 : var2.entrySet()) {
         var1.merge((ShopItem)var4.getKey(), ((Sold)var4.getValue()).getAmount(), Integer::sum);
      }

      return var1;
   }

   private Map<EcoType, Double> getPrices(Map<ShopItem, Sold> var1) {
      HashMap<EcoType, Double> var2 = new HashMap<>();

      for (Entry var4 : var1.entrySet()) {
         ((Sold)var4.getValue()).getPrices().forEach((var1x, var2x) -> var2.merge(var1x, var2x, Double::sum));
      }

      return var2;
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
         var1.forEach((var1x, var2x) -> this.plugin.getDynamicPricingProvider().sellItem(var1x.getItemPath(), var2x));
      }
   }

   private Map<EcoType, Double> callPreTransactionEvent(Map<ShopItem, Integer> var1, Map<EcoType, Double> var2, Player var3, int var4) {
      PreTransactionEvent var5 = new PreTransactionEvent(var1, var2, var3, var4, Transaction.Type.SELL_GUI_SCREEN);
      Bukkit.getPluginManager().callEvent(var5);
      return var5.isCancelled() ? null : var5.getPrices();
   }

   @Override
   public Inventory getInventory() {
      return this.inv;
   }

   private static void loadItems() {
      for (String var1 : ConfigManager.getConfig().getConfigurationSection("sellgui-screen.items").getKeys(false)) {
         ArrayList var2 = OnyxShop.getInstance()
            .calculateAmount
            .getSlots(ConfigManager.getConfig().getString("sellgui-screen.items." + var1 + ".slot", null));
         if (var2 == null) {
            SendMessage.warnMessage("Invalid slot for item, skipping...");
            SendMessage.errorItemConfig("sellgui-screen.items." + var1);
         } else {
            LocalizedItemStack var3 = OnyxShop.getInstance()
               .createItem
               .createFillItem(ConfigManager.getConfig().getConfigurationSection("sellgui-screen.items." + var1), null);

            for (int var4 = var2.size() - 1; var4 > -1; var4--) {
               int var5 = (Integer)var2.get(var4);
               if (var5 < (OnyxShop.getInstance().navBar.isEnableSellGUINav() ? size - 9 : size) && var5 >= 0) {
                  staticItems.put(var5, var3);
               } else {
                  SendMessage.warnMessage("Item slot " + var5 + " is out of bounds for menu size of " + size + ", skipping ...");
                  SendMessage.errorItemConfig("sellgui-screen.items." + var1);
               }
            }
         }
      }
   }

   public static boolean isFillItem(int var0) {
      return staticItems.containsKey(var0);
   }

   static {
      if (ConfigManager.getConfig().contains("sellgui-screen.items")) {
         loadItems();
      }
   }
}
