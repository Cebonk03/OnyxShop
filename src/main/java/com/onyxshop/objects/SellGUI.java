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

   public void open(Player player) {
      this.inv = this.plugin.getMetaUtils().createInventory(this, size, TitleManager.get(TitleManager.Menu.SELL_GUI, player));
      staticItems.forEach((key, value) -> this.inv.setItem(key, value.getItem(player)));
      this.plugin.navBar.addSellGUINavBar(this.inv, player);
      player.openInventory(this.inv);
   }

   public void sellItems(Player player) {
      synchronized (this.getSellLock()) {
      Transaction.Result result = Transaction.Result.SUCCESS;
      HashMap<Integer, SmartStack> map = new HashMap<>();
      HashMap<ShopItem, Sold> map2 = new HashMap<>();
      int value = 0;

      for (int size2 = 0; size2 <= size - (this.plugin.navBar.isEnableSellGUINav() ? 10 : 1); size2++) {
         if (!staticItems.containsKey(size2)) {
            ItemStack itemStack = this.inv.getItem(size2);
            if (itemStack != null && itemStack.getType() != Material.AIR) {
               ShopItem shopItem = this.plugin.createItem.matchShopItem(itemStack, player, "sellgui");
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
                     if (amount <= 0) {
                        continue;
                     }
                  }

                  Sold sold = map2.computeIfAbsent(shopItem, key -> new Sold());
                  sold.addValues(amount, this.getPrices(shopItem, player, itemStack, amount, sold.getAmount()));
                  value += amount;
                  ItemStack itemStack2 = new ItemStack(itemStack);
                  itemStack2.setAmount(itemStack.getAmount() - amount);
                  if (!shopItem.isSellCommand() || shopItem.isRequireItem()) {
                     map.put(size2, new SmartStack(itemStack2));
                     sold.addIndex(size2);
                  }
               } else if (this.plugin.sellShulkers && XMaterial.isShulker(itemStack.getType())) {
                  Integer value2 = this.sellShulker(map2, map, player, itemStack, "sellgui", size2);
                  if (value2 != null) {
                     value += value2;
                  }
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
      HashMap<ShopItem, Integer> map4 = new HashMap<>();
      if (value > 0) {
         map3 = this.callPreTransactionEvent(this.getItems(map4, map2), this.getPrices(map2), player, value);
         if (map3 != null) {
            if (!map3.keySet().stream().allMatch(element -> element.getType() != EconomyType.ITEM) && !this.canPay(player, (Map<EcoType, Double>)map3)) {
               SendMessage.chatToPlayer(player, Lang.CANNOT_STORE_PAYMENT.get(player));
               result = Transaction.Result.CANT_STORE_PAYMENT;
            } else {
               this.removeItems(this.inv, map);
               map2.forEach((key, value2) -> {
                  if (key.isSellCommand()) {
                     CommandHelper.executeCommandItem(key, player, CommandType.SELL, value2.getPrices(), value2.getAmount());
                  }
               });
               this.plugin.getEcoHandler().deposit((Map<EcoType, Double>)map3, player);
               SendMessage.sendTransactionMessage(player, value, (Map<EcoType, Double>)map3, map4, Transaction.Type.SELL_GUI_SCREEN);
               this.sellItems(map4, player.getUniqueId());
            }
         } else {
            result = Transaction.Result.TRANSACTION_CANCELLED;
         }
      } else {
         result = Transaction.Result.NO_ITEMS_FOUND;
         SendMessage.chatToPlayer(player, Lang.NO_ITEM_FOUND.get(player));
      }

      this.addUnsoldItems(player);
      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(map4, (Map<EcoType, Double>)map3, player, value, Transaction.Type.SELL_ALL_COMMAND, result));
      }
   }

   private Object getSellLock() {
      return this.plugin.sellLimits && this.plugin.getLimitedSellProvider() != null ? this.plugin.getLimitedSellProvider().getSellLock() : this;
   }

   private void addUnsoldItems(Player player) {
      for (int size2 = 0; size2 <= size - (this.plugin.navBar.isEnableSellGUINav() ? 10 : 1); size2++) {
         if (!staticItems.containsKey(size2)) {
            ItemStack itemStack = this.inv.getItem(size2);
            if (itemStack != null && itemStack.getType() != Material.AIR) {
               for (ItemStack itemStack2 : player.getInventory().addItem(new ItemStack[]{itemStack}).values()) {
                  player.getWorld().dropItem(player.getLocation(), itemStack2);
               }
            }
         }
      }
   }

   private Integer sellShulker(Map<ShopItem, Sold> map, Map<Integer, SmartStack> map2, Player player, ItemStack itemStack, String shopId, int value) {
      HashMap<ShopItem, Sold> map3 = new HashMap<>();
      HashMap<Integer, ItemStack> map4 = new HashMap<>();
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

                     if (shopItem.getLimitedSellMode() != 0) {
                        int itemLimit = this.plugin.getLimitedSellProvider().getItemLimit(shopItem, player.getUniqueId(), true);
                        amount = Math.min(amount, itemLimit - (map3.getOrDefault(shopItem, new Sold()).getAmount() + map.getOrDefault(shopItem, new Sold()).getAmount()));
                        if (amount < 0) {
                           continue;
                        }
                     }

                     Sold sold = map3.computeIfAbsent(shopItem, key -> new Sold());
                     sold.addValues(amount, this.getPrices(shopItem, player, itemStack2, amount, sold.getAmount()));
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

   private Map<EcoType, Double> getPrices(ShopItem shopItem, Player player, ItemStack itemStack, int value, int value2) {
      return shopItem.isASellPricing()
         ? shopItem.getASellPrice()
            .getSellPrices(shopItem.getASellPrice().isAll() ? null : shopItem.getASellPrice().getTypes().get(0), player, itemStack, value, value2, shopItem.getSellModifiers())
         : Collections.singletonMap(shopItem.getEcoType(), shopItem.getSellPrice(player, itemStack, value, value2));
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

   private boolean canPay(Player player, Map<EcoType, Double> map) {
      for (EcoType ecoType : map.keySet().stream().filter(element -> element.getType() == EconomyType.ITEM).collect(Collectors.toList())) {
         ItemStack itemStack = ((ItemEconomy)this.plugin.getEcoHandler().getEcon(ecoType)).getItem();
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

   private Map<ShopItem, Integer> getItems(Map<ShopItem, Integer> map, Map<ShopItem, Sold> map2) {
      for (Entry entry : map2.entrySet()) {
         map.merge((ShopItem)entry.getKey(), ((Sold)entry.getValue()).getAmount(), Integer::sum);
      }

      return map;
   }

   private Map<EcoType, Double> getPrices(Map<ShopItem, Sold> map) {
      HashMap<EcoType, Double> map2 = new HashMap<>();

      for (Entry entry : map.entrySet()) {
         ((Sold)entry.getValue()).getPrices().forEach((key, value) -> map2.merge(key, value, Double::sum));
      }

      return map2;
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
         map.forEach((key, value) -> this.plugin.getDynamicPricingProvider().sellItem(key.getItemPath(), value));
      }
   }

   private Map<EcoType, Double> callPreTransactionEvent(Map<ShopItem, Integer> map, Map<EcoType, Double> map2, Player player, int value) {
      PreTransactionEvent preTransactionEvent = new PreTransactionEvent(map, map2, player, value, Transaction.Type.SELL_GUI_SCREEN);
      Bukkit.getPluginManager().callEvent(preTransactionEvent);
      return preTransactionEvent.isCancelled() ? null : preTransactionEvent.getPrices();
   }

   @Override
   public Inventory getInventory() {
      return this.inv;
   }

   private static void loadItems() {
      for (String sectionId : ConfigManager.getConfig().getConfigurationSection("sellgui-screen.items").getKeys(false)) {
         ArrayList slots = OnyxShop.getInstance()
            .calculateAmount
            .getSlots(ConfigManager.getConfig().getString("sellgui-screen.items." + sectionId + ".slot", null));
         if (slots == null) {
            SendMessage.warnMessage("Invalid slot for item, skipping...");
            SendMessage.errorItemConfig("sellgui-screen.items." + sectionId);
         } else {
            LocalizedItemStack localizedItemStack = OnyxShop.getInstance()
               .createItem
               .createFillItem(ConfigManager.getConfig().getConfigurationSection("sellgui-screen.items." + sectionId), null);

            for (int size2 = slots.size() - 1; size2 > -1; size2--) {
               int value = (Integer)slots.get(size2);
               if (value < (OnyxShop.getInstance().navBar.isEnableSellGUINav() ? size - 9 : size) && value >= 0) {
                  staticItems.put(value, localizedItemStack);
               } else {
                  SendMessage.warnMessage("Item slot " + value + " is out of bounds for menu size of " + size + ", skipping ...");
                  SendMessage.errorItemConfig("sellgui-screen.items." + sectionId);
               }
            }
         }
      }
   }

   public static boolean isFillItem(int value) {
      return staticItems.containsKey(value);
   }

   static {
      if (ConfigManager.getConfig().contains("sellgui-screen.items")) {
         loadItems();
      }
   }
}
