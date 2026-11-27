package com.onyxshop.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.methodes.CreateItem;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.providers.UserManager;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.Transaction;
import com.onyxshop.util.scheduler.ScheduledTask;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TransactionMenu extends ShopInventory {
   private static final TransactionMenu.VisualLimitAction visualLimitAction = TransactionMenu.VisualLimitAction.get();
   private static final boolean checkInitialAmount = ConfigManager.getConfig().getBoolean("visual-transaction-limits.initial-amount");
   private final ShopItem shopItem;
   private final boolean disabledBackButton;
   private final String rootSection;
   private final TransactionMenu.SelectedItem selectedItem;
   private final List<ItemStack> CACHE = new ArrayList<>();
   private final Transaction.Mode transactionMode;
   private final Transaction.Type transactionType;
   private final Inventory inv;
   private final Player player;
   private ScheduledTask updateTask;
   private EcoType ecoType;
   private int itemStock;
   private int sellLimit;
   private int amount;

   public TransactionMenu(Player player, ShopItem shopItem, String playerName, boolean flag, Transaction.Mode mode, Transaction.Type type, int value) {
      this.shopItem = shopItem;
      this.player = player;
      this.rootSection = playerName;
      this.disabledBackButton = flag;
      this.transactionMode = mode;
      this.transactionType = type;
      this.ecoType = this.getEcoType();
      this.selectedItem = new TransactionMenu.SelectedItem(shopItem);
      this.amount = this.getInitialAmount(this.shopItem.isFixedStackSize() ? this.shopItem.getFixedStackSize() : this.shopItem.getStackSize());
      this.itemStock = shopItem.getLimitedStockMode() != 0
         ? OnyxShop.getInstance().getLimitedStockProvider().getItemStock(shopItem, player.getUniqueId(), true)
         : 0;
      this.sellLimit = shopItem.getLimitedSellMode() != 0 ? OnyxShop.getInstance().getLimitedSellProvider().getItemLimit(shopItem, player.getUniqueId(), true) : 0;
      this.inv = OnyxShop.getInstance()
         .getMetaUtils()
         .createInventory(this, CalculateAmount.getInvSlotsDef(UserManager.getUser(player), TransactionMenus.getSize(type)), this.getInvTitle());
      this.init();
      this.startUpdateTask();
      OnyxShop.getInstance().getSoundEffects().selectItem(this.player);
   }

   private void startUpdateTask() {
      long updateInterval = OnyxShop.getInstance().getSection(this.shopItem.section).getUpdateInterval();
      if (updateInterval > 0L) {
         this.updateTask = OnyxShop.getInstance().runTaskTimer(() -> {
            if (this.inv.getViewers().isEmpty()) {
               this.updateTask.cancel();
            } else {
               if (this.shopItem.isDynamicPricing() || this.shopItem.getLimitedStockMode() != 0 || this.shopItem.getLimitedSellMode() != 0) {
                  this.setSelectedItem(true);
               }
            }
         }, updateInterval, updateInterval);
      }
   }

   private int getInitialAmount(int amount) {
      int value;
      if (this.transactionMode == Transaction.Mode.BUY) {
         value = this.shopItem.isMinBuy(amount) ? Math.max(this.shopItem.getMinBuy(), amount) : amount;
      } else {
         if (this.transactionMode != Transaction.Mode.SELL) {
            return 1;
         }

         value = this.shopItem.isMinSell(amount) ? Math.max(this.shopItem.getMinSell(), amount) : amount;
      }

      return this.shopItem.isFixedStackSize() ? this.shopItem.getClosestStackSize(value) : value;
   }

   public EcoType getEcoType() {
      if (this.transactionMode == Transaction.Mode.BUY) {
         return !this.shopItem.isABuyPricing()
            ? this.shopItem.getEcoType()
            : (!this.shopItem.getABuyPrice().isAll() ? this.shopItem.getABuyPrice().getTypes().get(0) : null);
      } else {
         return !this.shopItem.isASellPricing()
            ? this.shopItem.getEcoType()
            : (!this.shopItem.getASellPrice().isAll() ? this.shopItem.getASellPrice().getTypes().get(0) : null);
      }
   }

   public void cycleEcoType() {
      this.ecoType = this.getNextEcoType();
      this.open(true, true);
   }

   public EcoType getCurrentEcoType() {
      return this.ecoType;
   }

   public void setEcoType(EcoType ecoType) {
      this.ecoType = ecoType;
   }

   public Player getPlayer() {
      return this.player;
   }

   private EcoType getNextEcoType() {
      List types = this.transactionMode == Transaction.Mode.BUY ? this.shopItem.getABuyPrice().getTypes() : this.shopItem.getASellPrice().getTypes();
      int index = types.indexOf(this.ecoType);
      return index + 1 == types.size() ? (EcoType)types.get(0) : (EcoType)types.get(index + 1);
   }

   public boolean isAPricing() {
      return this.transactionMode == Transaction.Mode.BUY ? this.shopItem.isABuyPricing() : this.shopItem.isASellPricing();
   }

   public boolean isAllPrices() {
      return this.isAPricing() && (this.transactionMode == Transaction.Mode.BUY ? this.shopItem.getABuyPrice().isAll() : this.shopItem.getASellPrice().isAll());
   }

   public double getPrice(int price) {
      return this.shopItem.getBuyPrice(this.player, price);
   }

   public int getLimitedStockMode() {
      return this.shopItem.getLimitedStockMode();
   }

   public String getRootSection() {
      return this.rootSection;
   }

   public boolean isDisabledBackButton() {
      return this.disabledBackButton;
   }

   public Transaction.Mode getTransactionMode() {
      return this.transactionMode;
   }

   public Transaction.Type getTransactionType() {
      return this.transactionType;
   }

   public int getItemStock() {
      return this.itemStock;
   }

   public void updateItemStock(int stock) {
      this.itemStock -= stock;
   }

   public void addItemStock(int stock) {
      this.itemStock += stock;
   }

   public int getSellLimit() {
      return this.sellLimit;
   }

   public void updateSellLimit(int limit) {
      this.sellLimit -= limit;
   }

   public String getItemPath() {
      return this.shopItem.getItemPath();
   }

   public ShopItem getShopItem() {
      return this.shopItem;
   }

   public int getMaxStackSize() {
      return this.shopItem.isFixedStackSize() ? this.shopItem.getFixedStackSize() : this.selectedItem.maxStackSize;
   }

   public int getAmount() {
      return this.amount;
   }

   public void setAmount(int amount) {
      this.amount = amount;
   }

   public void emptyCache() {
      for (ItemStack itemStack : this.CACHE) {
         this.player.getInventory().removeItem(new ItemStack[]{itemStack});
      }

      this.CACHE.clear();
   }

   public List<ItemStack> getCachedItems() {
      return new ArrayList<>(this.CACHE);
   }

   public int getTotalAmount() {
      return this.transactionType == Transaction.Type.BUY_STACKS_SCREEN ? this.amount * this.getMaxStackSize() : this.amount;
   }

   public Double getAvailableItems(int value) {
      this.CACHE.clear();
      if (this.shopItem.getLimitedSellMode() != 0 && OnyxShop.getInstance().sellLimits) {
         int itemLimit = OnyxShop.getInstance().getLimitedSellProvider().getItemLimit(this.shopItem, this.player.getUniqueId(), true);
         if (itemLimit < value) {
            value = itemLimit;
         }

         this.sellLimit = itemLimit;
         if (itemLimit <= 0) {
            return -1.0;
         }
      }

      if (this.shopItem.isSellCommand() && !this.shopItem.isRequireItem()) {
         this.amount = value;
         return this.shopItem.getSellPrice();
      } else {
         if (this.shopItem.isMaxSell(value)) {
            value = this.shopItem.getMaxSell();
         }

         int value2 = 0;
         double amount = 0.0;

         for (int index = 0; index < 36; index++) {
            ItemStack itemStack = this.player.getInventory().getItem(index);
            if (itemStack != null && !itemStack.getType().equals(Material.AIR) && this.shopItem.match(itemStack)) {
               if (value < itemStack.getAmount()) {
                  ItemStack itemStack2 = new ItemStack(itemStack);
                  itemStack2.setAmount(value);
                  this.CACHE.add(itemStack2);
                  amount += this.shopItem.getSellPrice(this.player, itemStack, value, value2);
                  value2 += value;
                  break;
               }

               this.CACHE.add(new ItemStack(itemStack));
               amount += this.shopItem.getSellPrice(this.player, itemStack, itemStack.getAmount(), value2);
               value2 += itemStack.getAmount();
               value -= itemStack.getAmount();
            }
         }

         if (this.shopItem.isMinSell(value2)) {
            return null;
         } else {
            this.amount = value2;
            return value2 == 0 ? null : amount;
         }
      }
   }

   public Map<EcoType, Double> getAvailableItemsA(int value) {
      this.CACHE.clear();
      if (this.shopItem.getLimitedSellMode() != 0 && OnyxShop.getInstance().sellLimits) {
         int itemLimit = OnyxShop.getInstance().getLimitedSellProvider().getItemLimit(this.shopItem, this.player.getUniqueId(), true);
         if (itemLimit < value) {
            value = itemLimit;
         }

         this.sellLimit = itemLimit;
         if (itemLimit <= 0) {
            return Collections.EMPTY_MAP;
         }
      }

      if (this.shopItem.isSellCommand() && !this.shopItem.isRequireItem()) {
         this.amount = value;
         return this.shopItem.getASellPrice().getSellPrices();
      } else {
         if (this.shopItem.isMaxSell(value)) {
            value = this.shopItem.getMaxSell();
         }

         int value2 = 0;
         HashMap<EcoType, Double> map = new HashMap<>();

         for (int index = 0; index < 36; index++) {
            ItemStack itemStack = this.player.getInventory().getItem(index);
            if (itemStack != null && !itemStack.getType().equals(Material.AIR) && this.shopItem.match(itemStack)) {
               if (value < itemStack.getAmount()) {
                  ItemStack itemStack2 = new ItemStack(itemStack);
                  itemStack2.setAmount(value);
                  this.CACHE.add(itemStack2);
                  this.shopItem
                     .getASellPrice()
                     .getSellPrices(this.ecoType, this.player, itemStack, value, value2, this.shopItem.getSellModifiers())
                     .forEach((key, value4) -> map.put(key, map.getOrDefault(key, 0.0) + value4));
                  value2 += value;
                  break;
               }

               this.CACHE.add(new ItemStack(itemStack));
               this.shopItem
                  .getASellPrice()
                  .getSellPrices(this.ecoType, this.player, itemStack, itemStack.getAmount(), value2, this.shopItem.getSellModifiers())
                  .forEach((key, value4) -> map.put(key, map.getOrDefault(key, 0.0) + value4));
               value2 += itemStack.getAmount();
               value -= itemStack.getAmount();
            }
         }

         if (this.shopItem.isMinSell(value2)) {
            return null;
         } else {
            this.amount = value2;
            return value2 == 0 ? null : map;
         }
      }
   }

   public int getFreeSpace() {
      int value = 0;
      int maxStackSize = this.shopItem.getItemToGive().getMaxStackSize();
      ArrayList values = new ArrayList<>(Arrays.asList(this.player.getInventory().getContents()));

      for (int index = 0; index < 36; index++) {
         ItemStack itemStack = (ItemStack)values.get(index);
         if (itemStack == null || itemStack.getType().equals(Material.AIR)) {
            value += maxStackSize;
         } else if (itemStack.isSimilar(this.shopItem.getItemToGive()) && itemStack.getAmount() < maxStackSize) {
            value += maxStackSize - itemStack.getAmount();
         }
      }

      return value;
   }

   public int getAmountToSell(int amount) {
      int totalAmount = this.getTotalAmount();
      if (totalAmount > amount && !OnyxShop.getInstance().dropItemsOnGround) {
         totalAmount = amount;
      }

      if (this.shopItem.isMaxBuy(totalAmount)) {
         totalAmount = this.shopItem.getMaxBuy();
      }

      if (this.shopItem.isMinBuy(totalAmount)) {
         totalAmount = this.shopItem.getMinBuy();
      }

      if (this.shopItem.getLimitedStockMode() != 0) {
         totalAmount = Math.min(
            totalAmount, this.itemStock = OnyxShop.getInstance().getLimitedStockProvider().getItemStock(this.shopItem, this.player.getUniqueId(), true)
         );
      }

      if (!OnyxShop.getInstance().dropItemsOnGround) {
         totalAmount = Math.min(totalAmount, amount);
      }

      return Math.max(0, totalAmount);
   }

   public void sendTransactionCompleteMessage(double amount) {
      SendMessage.sendTransactionMessage(
         this.player,
         this.getTotalAmount(),
         amount,
         this.shopItem,
         this.transactionMode,
         this.transactionType,
         this.transactionMode == Transaction.Mode.BUY
            ? this.shopItem.isBuyCommand() && !this.shopItem.isGiveItem()
            : this.shopItem.isSellCommand() && !this.shopItem.isRequireItem()
      );
   }

   public void sendTransactionCompleteMessage(double amount, int value) {
      SendMessage.sendTransactionMessage(
         this.player,
         value,
         amount,
         this.shopItem,
         this.transactionMode,
         this.transactionType,
         this.transactionMode == Transaction.Mode.BUY
            ? this.shopItem.isBuyCommand() && !this.shopItem.isGiveItem()
            : this.shopItem.isSellCommand() && !this.shopItem.isRequireItem()
      );
   }

   public void sendTransactionCompleteMessage(Map<EcoType, Double> map, int value) {
      SendMessage.sendTransactionMessage(
         this.player,
         value,
         map,
         this.shopItem,
         this.transactionMode,
         this.transactionType,
         this.transactionMode == Transaction.Mode.BUY
            ? this.shopItem.isBuyCommand() && !this.shopItem.isGiveItem()
            : this.shopItem.isSellCommand() && !this.shopItem.isRequireItem()
      );
   }

   private String getTransactionScreenType() {
      return this.transactionType.getName();
   }

   public Translatable getInvTitle() {
      Translatable text = null;
      switch (this.transactionType) {
         case SELL_SCREEN:
            text = TitleManager.get(TitleManager.Menu.SELL_SCREEN, this.player);
            break;
         case BUY_SCREEN:
            text = TitleManager.get(TitleManager.Menu.BUY_SCREEN, this.player);
            break;
         case BUY_STACKS_SCREEN:
            text = TitleManager.get(TitleManager.Menu.BUY_STACKS, this.player);
            break;
         case SHOPSTAND_BUY_SCREEN:
            text = TitleManager.get(TitleManager.Menu.SHOPSTAND_BUY_SCREEN, this.player);
            break;
         case SHOPSTAND_SELL_SCREEN:
            text = TitleManager.get(TitleManager.Menu.SHOPSTAND_SELL_SCREEN, this.player);
      }

      return text.replace("%item%", this.shopItem.getDisplayname())
         .replace("%material%", OnyxShop.getInstance().getMaterialName(this.shopItem.getItemToGive().getType().name()));
   }

   public boolean isPurchaseAble() {
      return this.transactionMode == Transaction.Mode.BUY ? this.shopItem.isBuyAble() : this.shopItem.isSellAble();
   }

   public Map<EcoType, Double> getPrices(int value) {
      return this.shopItem.isABuyPricing()
         ? this.shopItem.getABuyPrice().getBuyPrices(this.ecoType, this.player, value, this.shopItem.getBuyModifiers())
         : Collections.singletonMap(this.ecoType, this.shopItem.getBuyPrice(this.player, value));
   }

   private void updateButtons(int value) {
      TransactionItem transactionItem = TransactionMenus.getItemByType(this.transactionType, CreateItem.TransactionItemType.FILLITEM);
      int size = OnyxShop.getInstance().allowIllegalStacks ? 64 : this.selectedItem.maxStackSize;
      if (this.shopItem.isFixedStackSize()) {
         size = this.shopItem.getSmallestStackSize(size);
      }

      if (value > this.amount || !this.isMaxAmount(value)) {
         if (visualLimitAction != TransactionMenu.VisualLimitAction.NONE) {
            for (TransactionItem transactionItem2 : TransactionMenus.getItemsByType(this.transactionType, CreateItem.TransactionItemType.INCREASE_AMOUNT)) {
               if (value + transactionItem2.getAction().amount <= size && !this.isMaxAmount(value + transactionItem2.getAction().amount)) {
                  this.setItem(transactionItem2.getSlots(), transactionItem2.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
               } else if (visualLimitAction == TransactionMenu.VisualLimitAction.RENAME) {
                  this.setItem(
                     transactionItem2.getSlots(),
                     new ItemBuilder(transactionItem2.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
                        .withDisplayName(Lang.MAX_AMOUNT_REACHED.get(this.player))
                        .build()
                  );
               } else {
                  this.setItem(transactionItem2.getSlots(), transactionItem == null ? new ItemStack(Material.AIR) : transactionItem.getItem());
               }
            }

            for (TransactionItem transactionItem2 : TransactionMenus.getItemsByType(this.transactionType, CreateItem.TransactionItemType.SELECT_AMOUNT)) {
               if (value != transactionItem2.getAction().amount && !this.isMaxAmount(transactionItem2.getAction().amount)) {
                  this.setItem(transactionItem2.getSlots(), transactionItem2.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
               } else if (visualLimitAction == TransactionMenu.VisualLimitAction.RENAME) {
                  this.setItem(
                     transactionItem2.getSlots(),
                     new ItemBuilder(transactionItem2.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
                        .withDisplayName(Lang.MAX_AMOUNT_REACHED.get(this.player))
                        .build()
                  );
               } else {
                  this.setItem(transactionItem2.getSlots(), transactionItem == null ? new ItemStack(Material.AIR) : transactionItem.getItem());
               }
            }
         }

         value = value <= size && !this.isMaxAmount(value) ? value : (this.getMaxAmount() != 0 ? Math.min(size, this.getMaxAmount()) : size);
      }

      if (value < this.amount || !this.isMinAmount(value)) {
         if (visualLimitAction != TransactionMenu.VisualLimitAction.NONE) {
            for (TransactionItem transactionItem2 : TransactionMenus.getItemsByType(this.transactionType, CreateItem.TransactionItemType.DECREASE_AMOUNT)) {
               if (value - transactionItem2.getAction().amount >= 1 && !this.isMinAmount(value - transactionItem2.getAction().amount)) {
                  this.setItem(transactionItem2.getSlots(), transactionItem2.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
               } else if (visualLimitAction == TransactionMenu.VisualLimitAction.RENAME) {
                  this.setItem(
                     transactionItem2.getSlots(),
                     new ItemBuilder(transactionItem2.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
                        .withDisplayName(Lang.MIN_AMOUNT_REACHED.get(this.player))
                        .build()
                  );
               } else {
                  this.setItem(transactionItem2.getSlots(), transactionItem == null ? new ItemStack(Material.AIR) : transactionItem.getItem());
               }
            }

            for (TransactionItem transactionItem2 : TransactionMenus.getItemsByType(this.transactionType, CreateItem.TransactionItemType.SELECT_AMOUNT)) {
               if (value != transactionItem2.getAction().amount && !this.isMaxAmount(value + transactionItem2.getAction().amount)) {
                  this.setItem(transactionItem2.getSlots(), transactionItem2.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
               } else if (visualLimitAction == TransactionMenu.VisualLimitAction.RENAME) {
                  this.setItem(
                     transactionItem2.getSlots(),
                     new ItemBuilder(transactionItem2.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
                        .withDisplayName(Lang.MAX_AMOUNT_REACHED.get(this.player))
                        .build()
                  );
               } else {
                  this.setItem(transactionItem2.getSlots(), transactionItem == null ? new ItemStack(Material.AIR) : transactionItem.getItem());
               }
            }
         }

         value = value >= 1 && !this.isMinAmount(value) ? value : (this.getMinAmount() != 0 ? Math.max(1, this.getMinAmount()) : 1);
      }

      this.amount = value;
   }

   public void setConfirmTransactionItem() {
      TransactionItem transactionItem = TransactionMenus.getItemByType(this.transactionType, CreateItem.TransactionItemType.CONFIRM);
      if (transactionItem != null) {
         ItemStack itemStack = transactionItem.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount());
         if (this.isAPricing()
            && (
               this.transactionMode == Transaction.Mode.BUY
                  ? !this.shopItem.getABuyPrice().isAll() && this.shopItem.getABuyPrice().getTypes().size() != 1
                  : !this.shopItem.getASellPrice().isAll() && this.shopItem.getASellPrice().getTypes().size() != 1
            )
            && !Lang.RIGHT_CLICK_CHANGE_CURRENCY.get(this.player).getLegacy().isEmpty()) {
            ItemBuilder itemBuilder = new ItemBuilder(itemStack);
            itemBuilder.addLore(
               Lang.RIGHT_CLICK_CHANGE_CURRENCY
                  .get(this.player)
                  .replace("%currency%", OnyxShop.getInstance().getEcoHandler().getEcon(this.getNextEcoType()).getFriendly())
            );
            this.setItem(transactionItem.getSlots(), itemBuilder.build());
         } else {
            this.setItem(transactionItem.getSlots(), itemStack);
         }
      }
   }

   public void setSelectedItem(boolean flag) {
      TransactionItem transactionItem = TransactionMenus.getItemByType(this.transactionType, CreateItem.TransactionItemType.SELECTED_ITEM);
      if (transactionItem != null) {
         this.setItem(transactionItem.getSlots(), this.selectedItem.get());
      }
   }

   private void init() {
      TransactionItem transactionItem = TransactionMenus.getItemByType(this.transactionType, CreateItem.TransactionItemType.FILLITEM);
      int size = OnyxShop.getInstance().allowIllegalStacks ? 64 : this.selectedItem.maxStackSize;
      TransactionMenus.getItems(this.transactionType)
         .forEach(
            entry -> {
               switch (entry.getType()) {
                  case DECREASE_AMOUNT:
                     if (visualLimitAction != TransactionMenu.VisualLimitAction.NONE
                        && checkInitialAmount
                        && (
                           this.amount - entry.getAction().amount < 1
                              || this.isMinAmount(this.amount - entry.getAction().amount)
                              || !this.shopItem.isAllowQuantity()
                        )) {
                        if (visualLimitAction == TransactionMenu.VisualLimitAction.RENAME) {
                           this.setItem(
                              entry.getSlots(),
                              new ItemBuilder(entry.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
                                 .withDisplayName(Lang.MIN_AMOUNT_REACHED.get(this.player))
                                 .build()
                           );
                        } else {
                           this.setItem(entry.getSlots(), transactionItem == null ? new ItemStack(Material.AIR) : transactionItem.getItem());
                        }
                     } else {
                        this.setItem(entry.getSlots(), entry.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
                     }
                     break;
                  case INCREASE_AMOUNT:
                     if (visualLimitAction != TransactionMenu.VisualLimitAction.NONE
                        && checkInitialAmount
                        && (
                           this.amount + entry.getAction().amount > size
                              || this.isMaxAmount(this.amount + entry.getAction().amount)
                              || !this.shopItem.isAllowQuantity()
                        )) {
                        if (visualLimitAction == TransactionMenu.VisualLimitAction.RENAME) {
                           this.setItem(
                              entry.getSlots(),
                              new ItemBuilder(entry.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
                                 .withDisplayName(Lang.MAX_AMOUNT_REACHED.get(this.player))
                                 .build()
                           );
                        } else {
                           this.setItem(entry.getSlots(), transactionItem == null ? new ItemStack(Material.AIR) : transactionItem.getItem());
                        }
                     } else {
                        this.setItem(entry.getSlots(), entry.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
                     }
                     break;
                  case SELECT_AMOUNT:
                     if ((visualLimitAction == TransactionMenu.VisualLimitAction.NONE || !checkInitialAmount || this.amount != entry.getAction().amount)
                        && !this.isMaxAmount(entry.getAction().amount)
                        && this.shopItem.isAllowQuantity()) {
                        this.setItem(entry.getSlots(), entry.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
                     } else if (visualLimitAction == TransactionMenu.VisualLimitAction.RENAME) {
                        this.setItem(
                           entry.getSlots(),
                           new ItemBuilder(entry.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
                              .withDisplayName(Lang.MAX_AMOUNT_REACHED.get(this.player))
                              .build()
                        );
                     } else {
                        this.setItem(entry.getSlots(), transactionItem == null ? new ItemStack(Material.AIR) : transactionItem.getItem());
                     }
                     break;
                  case NORMAL:
                     this.setItem(entry.getSlots(), entry.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
               }
            }
         );
      if (transactionItem != null) {
         for (int index = 0; index < this.inv.getSize() - (OnyxShop.getInstance().navBar.isEnableTransactionNav() ? 9 : 0); index++) {
            if (this.inv.getItem(index) == null) {
               this.inv.setItem(index, transactionItem.getItem());
            }
         }
      }
   }

   private boolean isMaxAmount(int amount) {
      return this.transactionMode == Transaction.Mode.BUY
         ? (this.transactionType == Transaction.Type.BUY_STACKS_SCREEN ? this.shopItem.isMaxBuy(amount * this.getMaxStackSize()) : this.shopItem.isMaxBuy(amount))
         : this.shopItem.isMaxSell(amount);
   }

   private int getMaxAmount() {
      return this.transactionMode == Transaction.Mode.BUY
         ? (this.transactionType == Transaction.Type.BUY_STACKS_SCREEN ? this.shopItem.getMaxBuy() / this.getMaxStackSize() : this.shopItem.getMaxBuy())
         : this.shopItem.getMaxSell();
   }

   private boolean isMinAmount(int amount) {
      return this.transactionMode == Transaction.Mode.BUY
         ? (this.transactionType == Transaction.Type.BUY_STACKS_SCREEN ? this.shopItem.isMinBuy(amount * this.getMaxStackSize()) : this.shopItem.isMinBuy(amount))
         : this.shopItem.isMinSell(amount);
   }

   private int getMinAmount() {
      return this.transactionMode == Transaction.Mode.BUY
         ? (this.transactionType == Transaction.Type.BUY_STACKS_SCREEN ? this.shopItem.getMinBuy() / this.getMaxStackSize() : this.shopItem.getMinBuy())
         : this.shopItem.getMinSell();
   }

   public void updateAmount(CreateItem.TransactionItemAction transactionItemAction) {
      int amount = this.amount;
      switch (transactionItemAction.type) {
         case ADD:
            amount += transactionItemAction.amount;
            break;
         case REMOVE:
            amount -= transactionItemAction.amount;
            break;
         case SET:
            amount = transactionItemAction.amount;
      }

      if (this.shopItem.isFixedStackSize()) {
         if (amount > this.amount) {
            amount = this.shopItem.getLargestStackSize(amount);
         } else {
            amount = this.shopItem.getSmallestStackSize(amount);
         }
      }

      this.updateButtons(amount);
      this.setSelectedItem(false);
      this.setConfirmTransactionItem();
   }

   private void setItem(List<Integer> numbers, ItemStack itemStack) {
      numbers.forEach(entry -> this.inv.setItem(entry, itemStack));
   }

   public void open(boolean flag) {
      this.open(flag, false);
   }

   public void open(boolean flag, boolean flag2) {
      if (OnyxShop.getInstance().bedrock
         && OnyxShop.getInstance().bedrockMenus
         && UserManager.getUser(this.player).isBedrock()
         && OnyxShop.getInstance().getBedrockInventorys().isEnabledFor(this.player)) {
         OnyxShop.getInstance().getBedrockInventorys().openTransactionMenu(this);
      } else {
         if (this.shopItem.isDynamicPricing() || flag2) {
            TransactionMenus.getItemsByAction(
                  this.transactionType, CreateItem.TransactionItemActionType.INSTA_BUY, CreateItem.TransactionItemActionType.INSTA_SELL
               )
               .forEach(
                  entry -> this.setItem(entry.getSlots(), entry.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
               );
         }

         this.setSelectedItem(flag);
         this.setConfirmTransactionItem();
         if (this.transactionType != Transaction.Type.SHOPSTAND_BUY_SCREEN && this.transactionType != Transaction.Type.SHOPSTAND_SELL_SCREEN) {
            OnyxShop.getInstance().navBar.addTransactionNavBar(this.inv, this.transactionMode, this.player, this.shopItem, this.getEcoTypeSimple());
         } else {
            OnyxShop.getInstance().navBar.addShopStandsNavBar(this.inv, this.transactionMode, this.player, this.shopItem, this.getEcoTypeSimple());
         }

         this.open(this.player);
      }
   }

   private void open(Player player) {
      UserManager.getUser(player).setOpenNewGUI(true);
      player.openInventory(this.inv);
   }

   private EcoType getEcoTypeSimple() {
      return this.ecoType == null
         ? (this.transactionMode == Transaction.Mode.BUY ? this.shopItem.getABuyPrice().getTypes().get(0) : this.shopItem.getASellPrice().getTypes().get(0))
         : this.ecoType;
   }

   @Override
   public Inventory getInventory() {
      return null;
   }

   private class SelectedItem {
      final ItemStack item;
      final int maxStackSize;
      Translatable displayname;
      List<Translatable> lore;
      Set<Integer> placeholders;
      int pricingPlaceholder = -2;

      public SelectedItem(ShopItem shopItem) {
         TransactionItem transactionItem = TransactionMenus.getItemByType(TransactionMenu.this.transactionType, CreateItem.TransactionItemType.SELECTED_ITEM);
         this.item = shopItem.getShopItem().clone();
         this.maxStackSize = this.item.getMaxStackSize();
         if (transactionItem != null) {
            this.placeholders = this.getPlaceholders(transactionItem.getItem().getItemMeta());
         }
      }

      public ItemStack get() {
         this.item.setAmount(TransactionMenu.this.amount);
         this.parsePlaceholders();
         return this.item;
      }

      private void parsePlaceholders() {
         ItemMeta itemMeta = this.item.getItemMeta();
         if (this.displayname != null) {
            OnyxShop.getInstance()
               .getMetaUtils()
               .setDisplayName(
                  itemMeta,
                  this.translatePapi(this.displayname, -1)
                     .replace("%amount%", String.valueOf(TransactionMenu.this.getTotalAmount()))
                     .replace("%price%", this.formatPrice(this.displayname, -1))
               );
         }

         if (this.lore != null) {
            ArrayList values = new ArrayList<>(this.lore);

            for (int size = 0; size < values.size(); size++) {
               values.set(
                  size,
                  this.translatePapi((Translatable)values.get(size), size)
                     .replace("%amount%", String.valueOf(TransactionMenu.this.getTotalAmount()))
                     .replace("%price%", this.formatPrice((Translatable)values.get(size), size))
               );
            }

            this.addStockLore(values);
            OnyxShop.getInstance().getMetaUtils().setLore(itemMeta, values);
         }

         this.item.setItemMeta(itemMeta);
      }

      private String formatPrice(Translatable text, int price) {
         if (this.pricingPlaceholder != price) {
            return "0";
         } else if (TransactionMenu.this.transactionMode == Transaction.Mode.BUY) {
            return TransactionMenu.this.shopItem.isABuyPricing()
               ? TransactionMenu.this.shopItem
                  .getABuyPrice()
                  .formatBuyPrices(
                     text.getLegacy().split("%price%")[0],
                     TransactionMenu.this.ecoType,
                     TransactionMenu.this.player,
                     TransactionMenu.this.amount,
                     TransactionMenu.this.shopItem.getBuyModifiers()
                  )
               : OnyxShop.getInstance()
                  .formatPrice(
                     TransactionMenu.this.ecoType, TransactionMenu.this.shopItem.getBuyPrice(TransactionMenu.this.player, TransactionMenu.this.amount)
                  );
         } else {
            return TransactionMenu.this.shopItem.isASellPricing()
               ? TransactionMenu.this.shopItem
                  .getASellPrice()
                  .formatSellPrices(
                     text.getLegacy().split("%price%")[0],
                     TransactionMenu.this.ecoType,
                     TransactionMenu.this.player,
                     TransactionMenu.this.amount,
                     TransactionMenu.this.shopItem.getSellModifiers()
                  )
               : OnyxShop.getInstance()
                  .formatPrice(
                     TransactionMenu.this.ecoType,
                     TransactionMenu.this.shopItem.getSellPrice(TransactionMenu.this.player, this.item, TransactionMenu.this.amount, 0)
                  );
         }
      }

      private Translatable translatePapi(Translatable text, int value) {
         return !this.placeholders.contains(value) ? text : text.translate(TransactionMenu.this.player);
      }

      private void addStockLore(List<Translatable> values) {
         if (TransactionMenu.this.transactionMode == Transaction.Mode.BUY && TransactionMenu.this.shopItem.getLimitedStockMode() != 0) {
            values.add(
               OnyxShop.getInstance()
                  .getLimitedStockProvider()
                  .getStockLore(TransactionMenu.this.shopItem, TransactionMenu.this.player, TransactionMenu.this.itemStock)
            );
         } else if (TransactionMenu.this.transactionMode == Transaction.Mode.SELL && TransactionMenu.this.shopItem.getLimitedSellMode() != 0) {
            values.add(
               OnyxShop.getInstance()
                  .getLimitedSellProvider()
                  .getStockLore(TransactionMenu.this.shopItem, TransactionMenu.this.player, TransactionMenu.this.sellLimit)
            );
         }
      }

      private Set<Integer> getPlaceholders(ItemMeta itemMeta) {
         if (itemMeta.hasDisplayName()) {
            Translatable text = OnyxShop.getInstance().getMetaUtils().getDisplayName(itemMeta);
            if (text.toString().contains("%price%")) {
               this.pricingPlaceholder = -1;
            }

            this.displayname = text;
         }

         if (itemMeta.hasLore()) {
            List lore = OnyxShop.getInstance().getMetaUtils().getLore(itemMeta);

            for (int size = 0; size < lore.size(); size++) {
               if (((Translatable)lore.get(size)).toString().contains("%price%")) {
                  this.pricingPlaceholder = size;
                  break;
               }
            }

            this.lore = lore;
         }

         return PlaceholderAPIHook.getPlaceholders(itemMeta).keySet();
      }
   }

   private static enum VisualLimitAction {
      NONE,
      RENAME,
      REMOVE;

      private VisualLimitAction() {
      }

      public static TransactionMenu.VisualLimitAction get() {
         try {
            return valueOf(ConfigManager.getConfig().getString("visual-transaction-limits.action").toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException exception) {
            SendMessage.logDebugMessage(
               "Failed to load transaction visual limit for " + ConfigManager.getConfig().getString("visual-transaction-limits.action")
            );
            return REMOVE;
         }
      }
   }
}
