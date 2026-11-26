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

   public TransactionMenu(Player var1, ShopItem var2, String var3, boolean var4, Transaction.Mode var5, Transaction.Type var6, int var7) {
      this.shopItem = var2;
      this.player = var1;
      this.rootSection = var3;
      this.disabledBackButton = var4;
      this.transactionMode = var5;
      this.transactionType = var6;
      this.ecoType = this.getEcoType();
      this.selectedItem = new TransactionMenu.SelectedItem(var2);
      this.amount = this.getInitialAmount(this.shopItem.isFixedStackSize() ? this.shopItem.getFixedStackSize() : this.shopItem.getStackSize());
      this.itemStock = var2.getLimitedStockMode() != 0
         ? OnyxShop.getInstance().getLimitedStockProvider().getItemStock(var2, var1.getUniqueId(), true)
         : 0;
      this.sellLimit = var2.getLimitedSellMode() != 0 ? OnyxShop.getInstance().getLimitedSellProvider().getItemLimit(var2, var1.getUniqueId(), true) : 0;
      this.inv = OnyxShop.getInstance()
         .getMetaUtils()
         .createInventory(this, CalculateAmount.getInvSlotsDef(UserManager.getUser(var1), TransactionMenus.getSize(var6)), this.getInvTitle());
      this.init();
      this.startUpdateTask();
      OnyxShop.getInstance().getSoundEffects().selectItem(this.player);
   }

   private void startUpdateTask() {
      long var1 = OnyxShop.getInstance().getSection(this.shopItem.section).getUpdateInterval();
      if (var1 > 0L) {
         this.updateTask = OnyxShop.getInstance().runTaskTimer(() -> {
            if (this.inv.getViewers().isEmpty()) {
               this.updateTask.cancel();
            } else {
               if (this.shopItem.isDynamicPricing() || this.shopItem.getLimitedStockMode() != 0 || this.shopItem.getLimitedSellMode() != 0) {
                  this.setSelectedItem(true);
               }
            }
         }, var1, var1);
      }
   }

   private int getInitialAmount(int var1) {
      int var2;
      if (this.transactionMode == Transaction.Mode.BUY) {
         var2 = this.shopItem.isMinBuy(var1) ? Math.max(this.shopItem.getMinBuy(), var1) : var1;
      } else {
         if (this.transactionMode != Transaction.Mode.SELL) {
            return 1;
         }

         var2 = this.shopItem.isMinSell(var1) ? Math.max(this.shopItem.getMinSell(), var1) : var1;
      }

      return this.shopItem.isFixedStackSize() ? this.shopItem.getClosestStackSize(var2) : var2;
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

   public void setEcoType(EcoType var1) {
      this.ecoType = var1;
   }

   public Player getPlayer() {
      return this.player;
   }

   private EcoType getNextEcoType() {
      List var1 = this.transactionMode == Transaction.Mode.BUY ? this.shopItem.getABuyPrice().getTypes() : this.shopItem.getASellPrice().getTypes();
      int var2 = var1.indexOf(this.ecoType);
      return var2 + 1 == var1.size() ? (EcoType)var1.get(0) : (EcoType)var1.get(var2 + 1);
   }

   public boolean isAPricing() {
      return this.transactionMode == Transaction.Mode.BUY ? this.shopItem.isABuyPricing() : this.shopItem.isASellPricing();
   }

   public boolean isAllPrices() {
      return this.isAPricing() && (this.transactionMode == Transaction.Mode.BUY ? this.shopItem.getABuyPrice().isAll() : this.shopItem.getASellPrice().isAll());
   }

   public double getPrice(int var1) {
      return this.shopItem.getBuyPrice(this.player, var1);
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

   public void updateItemStock(int var1) {
      this.itemStock -= var1;
   }

   public void addItemStock(int var1) {
      this.itemStock += var1;
   }

   public int getSellLimit() {
      return this.sellLimit;
   }

   public void updateSellLimit(int var1) {
      this.sellLimit -= var1;
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

   public void setAmount(int var1) {
      this.amount = var1;
   }

   public void emptyCache() {
      for (ItemStack var2 : this.CACHE) {
         this.player.getInventory().removeItem(new ItemStack[]{var2});
      }

      this.CACHE.clear();
   }

   public List<ItemStack> getCachedItems() {
      return new ArrayList<>(this.CACHE);
   }

   public int getTotalAmount() {
      return this.transactionType == Transaction.Type.BUY_STACKS_SCREEN ? this.amount * this.getMaxStackSize() : this.amount;
   }

   public Double getAvailableItems(int var1) {
      this.CACHE.clear();
      if (this.shopItem.getLimitedSellMode() != 0 && OnyxShop.getInstance().sellLimits) {
         int var2 = OnyxShop.getInstance().getLimitedSellProvider().getItemLimit(this.shopItem, this.player.getUniqueId(), true);
         if (var2 < var1) {
            var1 = var2;
         }

         this.sellLimit = var2;
         if (var2 <= 0) {
            return -1.0;
         }
      }

      if (this.shopItem.isSellCommand() && !this.shopItem.isRequireItem()) {
         this.amount = var1;
         return this.shopItem.getSellPrice();
      } else {
         if (this.shopItem.isMaxSell(var1)) {
            var1 = this.shopItem.getMaxSell();
         }

         int var8 = 0;
         double var3 = 0.0;

         for (int var5 = 0; var5 < 36; var5++) {
            ItemStack var6 = this.player.getInventory().getItem(var5);
            if (var6 != null && !var6.getType().equals(Material.AIR) && this.shopItem.match(var6)) {
               if (var1 < var6.getAmount()) {
                  ItemStack var7 = new ItemStack(var6);
                  var7.setAmount(var1);
                  this.CACHE.add(var7);
                  var3 += this.shopItem.getSellPrice(this.player, var6, var1, var8);
                  var8 += var1;
                  break;
               }

               this.CACHE.add(new ItemStack(var6));
               var3 += this.shopItem.getSellPrice(this.player, var6, var6.getAmount(), var8);
               var8 += var6.getAmount();
               var1 -= var6.getAmount();
            }
         }

         if (this.shopItem.isMinSell(var8)) {
            return null;
         } else {
            this.amount = var8;
            return var8 == 0 ? null : var3;
         }
      }
   }

   public Map<EcoType, Double> getAvailableItemsA(int var1) {
      this.CACHE.clear();
      if (this.shopItem.getLimitedSellMode() != 0 && OnyxShop.getInstance().sellLimits) {
         int var2 = OnyxShop.getInstance().getLimitedSellProvider().getItemLimit(this.shopItem, this.player.getUniqueId(), true);
         if (var2 < var1) {
            var1 = var2;
         }

         this.sellLimit = var2;
         if (var2 <= 0) {
            return Collections.EMPTY_MAP;
         }
      }

      if (this.shopItem.isSellCommand() && !this.shopItem.isRequireItem()) {
         this.amount = var1;
         return this.shopItem.getASellPrice().getSellPrices();
      } else {
         if (this.shopItem.isMaxSell(var1)) {
            var1 = this.shopItem.getMaxSell();
         }

         int var7 = 0;
         HashMap<EcoType, Double> var3 = new HashMap<>();

         for (int var4 = 0; var4 < 36; var4++) {
            ItemStack var5 = this.player.getInventory().getItem(var4);
            if (var5 != null && !var5.getType().equals(Material.AIR) && this.shopItem.match(var5)) {
               if (var1 < var5.getAmount()) {
                  ItemStack var6 = new ItemStack(var5);
                  var6.setAmount(var1);
                  this.CACHE.add(var6);
                  this.shopItem
                     .getASellPrice()
                     .getSellPrices(this.ecoType, this.player, var5, var1, var7, this.shopItem.getSellModifiers())
                     .forEach((var1x, var2x) -> var3.put(var1x, var3.getOrDefault(var1x, 0.0) + var2x));
                  var7 += var1;
                  break;
               }

               this.CACHE.add(new ItemStack(var5));
               this.shopItem
                  .getASellPrice()
                  .getSellPrices(this.ecoType, this.player, var5, var5.getAmount(), var7, this.shopItem.getSellModifiers())
                  .forEach((var1x, var2x) -> var3.put(var1x, var3.getOrDefault(var1x, 0.0) + var2x));
               var7 += var5.getAmount();
               var1 -= var5.getAmount();
            }
         }

         if (this.shopItem.isMinSell(var7)) {
            return null;
         } else {
            this.amount = var7;
            return var7 == 0 ? null : var3;
         }
      }
   }

   public int getFreeSpace() {
      int var1 = 0;
      int var2 = this.shopItem.getItemToGive().getMaxStackSize();
      ArrayList var3 = new ArrayList<>(Arrays.asList(this.player.getInventory().getContents()));

      for (int var4 = 0; var4 < 36; var4++) {
         ItemStack var5 = (ItemStack)var3.get(var4);
         if (var5 == null || var5.getType().equals(Material.AIR)) {
            var1 += var2;
         } else if (var5.isSimilar(this.shopItem.getItemToGive()) && var5.getAmount() < var2) {
            var1 += var2 - var5.getAmount();
         }
      }

      return var1;
   }

   public int getAmountToSell(int var1) {
      int var2 = this.getTotalAmount();
      if (var2 > var1 && !OnyxShop.getInstance().dropItemsOnGround) {
         var2 = var1;
      }

      if (this.shopItem.isMaxBuy(var2)) {
         var2 = this.shopItem.getMaxBuy();
      }

      if (this.shopItem.isMinBuy(var2)) {
         var2 = this.shopItem.getMinBuy();
      }

      if (this.shopItem.getLimitedStockMode() != 0) {
         var2 = Math.min(
            var2, this.itemStock = OnyxShop.getInstance().getLimitedStockProvider().getItemStock(this.shopItem, this.player.getUniqueId(), true)
         );
      }

      if (!OnyxShop.getInstance().dropItemsOnGround) {
         var2 = Math.min(var2, var1);
      }

      return Math.max(0, var2);
   }

   public void sendTransactionCompleteMessage(double var1) {
      SendMessage.sendTransactionMessage(
         this.player,
         this.getTotalAmount(),
         var1,
         this.shopItem,
         this.transactionMode,
         this.transactionType,
         this.transactionMode == Transaction.Mode.BUY
            ? this.shopItem.isBuyCommand() && !this.shopItem.isGiveItem()
            : this.shopItem.isSellCommand() && !this.shopItem.isRequireItem()
      );
   }

   public void sendTransactionCompleteMessage(double var1, int var3) {
      SendMessage.sendTransactionMessage(
         this.player,
         var3,
         var1,
         this.shopItem,
         this.transactionMode,
         this.transactionType,
         this.transactionMode == Transaction.Mode.BUY
            ? this.shopItem.isBuyCommand() && !this.shopItem.isGiveItem()
            : this.shopItem.isSellCommand() && !this.shopItem.isRequireItem()
      );
   }

   public void sendTransactionCompleteMessage(Map<EcoType, Double> var1, int var2) {
      SendMessage.sendTransactionMessage(
         this.player,
         var2,
         var1,
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
      Translatable var1 = null;
      switch (this.transactionType) {
         case SELL_SCREEN:
            var1 = TitleManager.get(TitleManager.Menu.SELL_SCREEN, this.player);
            break;
         case BUY_SCREEN:
            var1 = TitleManager.get(TitleManager.Menu.BUY_SCREEN, this.player);
            break;
         case BUY_STACKS_SCREEN:
            var1 = TitleManager.get(TitleManager.Menu.BUY_STACKS, this.player);
            break;
         case SHOPSTAND_BUY_SCREEN:
            var1 = TitleManager.get(TitleManager.Menu.SHOPSTAND_BUY_SCREEN, this.player);
            break;
         case SHOPSTAND_SELL_SCREEN:
            var1 = TitleManager.get(TitleManager.Menu.SHOPSTAND_SELL_SCREEN, this.player);
      }

      return var1.replace("%item%", this.shopItem.getDisplayname())
         .replace("%material%", OnyxShop.getInstance().getMaterialName(this.shopItem.getItemToGive().getType().name()));
   }

   public boolean isPurchaseAble() {
      return this.transactionMode == Transaction.Mode.BUY ? this.shopItem.isBuyAble() : this.shopItem.isSellAble();
   }

   public Map<EcoType, Double> getPrices(int var1) {
      return this.shopItem.isABuyPricing()
         ? this.shopItem.getABuyPrice().getBuyPrices(this.ecoType, this.player, var1, this.shopItem.getBuyModifiers())
         : Collections.singletonMap(this.ecoType, this.shopItem.getBuyPrice(this.player, var1));
   }

   private void updateButtons(int var1) {
      TransactionItem var2 = TransactionMenus.getItemByType(this.transactionType, CreateItem.TransactionItemType.FILLITEM);
      int var3 = OnyxShop.getInstance().allowIllegalStacks ? 64 : this.selectedItem.maxStackSize;
      if (this.shopItem.isFixedStackSize()) {
         var3 = this.shopItem.getSmallestStackSize(var3);
      }

      if (var1 > this.amount || !this.isMaxAmount(var1)) {
         if (visualLimitAction != TransactionMenu.VisualLimitAction.NONE) {
            for (TransactionItem var5 : TransactionMenus.getItemsByType(this.transactionType, CreateItem.TransactionItemType.INCREASE_AMOUNT)) {
               if (var1 + var5.getAction().amount <= var3 && !this.isMaxAmount(var1 + var5.getAction().amount)) {
                  this.setItem(var5.getSlots(), var5.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
               } else if (visualLimitAction == TransactionMenu.VisualLimitAction.RENAME) {
                  this.setItem(
                     var5.getSlots(),
                     new ItemBuilder(var5.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
                        .withDisplayName(Lang.MAX_AMOUNT_REACHED.get(this.player))
                        .build()
                  );
               } else {
                  this.setItem(var5.getSlots(), var2 == null ? new ItemStack(Material.AIR) : var2.getItem());
               }
            }

            for (TransactionItem var9 : TransactionMenus.getItemsByType(this.transactionType, CreateItem.TransactionItemType.SELECT_AMOUNT)) {
               if (var1 != var9.getAction().amount && !this.isMaxAmount(var9.getAction().amount)) {
                  this.setItem(var9.getSlots(), var9.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
               } else if (visualLimitAction == TransactionMenu.VisualLimitAction.RENAME) {
                  this.setItem(
                     var9.getSlots(),
                     new ItemBuilder(var9.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
                        .withDisplayName(Lang.MAX_AMOUNT_REACHED.get(this.player))
                        .build()
                  );
               } else {
                  this.setItem(var9.getSlots(), var2 == null ? new ItemStack(Material.AIR) : var2.getItem());
               }
            }
         }

         var1 = var1 <= var3 && !this.isMaxAmount(var1) ? var1 : (this.getMaxAmount() != 0 ? Math.min(var3, this.getMaxAmount()) : var3);
      }

      if (var1 < this.amount || !this.isMinAmount(var1)) {
         if (visualLimitAction != TransactionMenu.VisualLimitAction.NONE) {
            for (TransactionItem var10 : TransactionMenus.getItemsByType(this.transactionType, CreateItem.TransactionItemType.DECREASE_AMOUNT)) {
               if (var1 - var10.getAction().amount >= 1 && !this.isMinAmount(var1 - var10.getAction().amount)) {
                  this.setItem(var10.getSlots(), var10.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
               } else if (visualLimitAction == TransactionMenu.VisualLimitAction.RENAME) {
                  this.setItem(
                     var10.getSlots(),
                     new ItemBuilder(var10.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
                        .withDisplayName(Lang.MIN_AMOUNT_REACHED.get(this.player))
                        .build()
                  );
               } else {
                  this.setItem(var10.getSlots(), var2 == null ? new ItemStack(Material.AIR) : var2.getItem());
               }
            }

            for (TransactionItem var11 : TransactionMenus.getItemsByType(this.transactionType, CreateItem.TransactionItemType.SELECT_AMOUNT)) {
               if (var1 != var11.getAction().amount && !this.isMaxAmount(var1 + var11.getAction().amount)) {
                  this.setItem(var11.getSlots(), var11.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
               } else if (visualLimitAction == TransactionMenu.VisualLimitAction.RENAME) {
                  this.setItem(
                     var11.getSlots(),
                     new ItemBuilder(var11.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
                        .withDisplayName(Lang.MAX_AMOUNT_REACHED.get(this.player))
                        .build()
                  );
               } else {
                  this.setItem(var11.getSlots(), var2 == null ? new ItemStack(Material.AIR) : var2.getItem());
               }
            }
         }

         var1 = var1 >= 1 && !this.isMinAmount(var1) ? var1 : (this.getMinAmount() != 0 ? Math.max(1, this.getMinAmount()) : 1);
      }

      this.amount = var1;
   }

   public void setConfirmTransactionItem() {
      TransactionItem var1 = TransactionMenus.getItemByType(this.transactionType, CreateItem.TransactionItemType.CONFIRM);
      if (var1 != null) {
         ItemStack var2 = var1.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount());
         if (this.isAPricing()
            && (
               this.transactionMode == Transaction.Mode.BUY
                  ? !this.shopItem.getABuyPrice().isAll() && this.shopItem.getABuyPrice().getTypes().size() != 1
                  : !this.shopItem.getASellPrice().isAll() && this.shopItem.getASellPrice().getTypes().size() != 1
            )
            && !Lang.RIGHT_CLICK_CHANGE_CURRENCY.get(this.player).getLegacy().isEmpty()) {
            ItemBuilder var3 = new ItemBuilder(var2);
            var3.addLore(
               Lang.RIGHT_CLICK_CHANGE_CURRENCY
                  .get(this.player)
                  .replace("%currency%", OnyxShop.getInstance().getEcoHandler().getEcon(this.getNextEcoType()).getFriendly())
            );
            this.setItem(var1.getSlots(), var3.build());
         } else {
            this.setItem(var1.getSlots(), var2);
         }
      }
   }

   public void setSelectedItem(boolean var1) {
      TransactionItem var2 = TransactionMenus.getItemByType(this.transactionType, CreateItem.TransactionItemType.SELECTED_ITEM);
      if (var2 != null) {
         this.setItem(var2.getSlots(), this.selectedItem.get());
      }
   }

   private void init() {
      TransactionItem var1 = TransactionMenus.getItemByType(this.transactionType, CreateItem.TransactionItemType.FILLITEM);
      int var2 = OnyxShop.getInstance().allowIllegalStacks ? 64 : this.selectedItem.maxStackSize;
      TransactionMenus.getItems(this.transactionType)
         .forEach(
            var3x -> {
               switch (var3x.getType()) {
                  case DECREASE_AMOUNT:
                     if (visualLimitAction != TransactionMenu.VisualLimitAction.NONE
                        && checkInitialAmount
                        && (
                           this.amount - var3x.getAction().amount < 1
                              || this.isMinAmount(this.amount - var3x.getAction().amount)
                              || !this.shopItem.isAllowQuantity()
                        )) {
                        if (visualLimitAction == TransactionMenu.VisualLimitAction.RENAME) {
                           this.setItem(
                              var3x.getSlots(),
                              new ItemBuilder(var3x.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
                                 .withDisplayName(Lang.MIN_AMOUNT_REACHED.get(this.player))
                                 .build()
                           );
                        } else {
                           this.setItem(var3x.getSlots(), var1 == null ? new ItemStack(Material.AIR) : var1.getItem());
                        }
                     } else {
                        this.setItem(var3x.getSlots(), var3x.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
                     }
                     break;
                  case INCREASE_AMOUNT:
                     if (visualLimitAction != TransactionMenu.VisualLimitAction.NONE
                        && checkInitialAmount
                        && (
                           this.amount + var3x.getAction().amount > var2
                              || this.isMaxAmount(this.amount + var3x.getAction().amount)
                              || !this.shopItem.isAllowQuantity()
                        )) {
                        if (visualLimitAction == TransactionMenu.VisualLimitAction.RENAME) {
                           this.setItem(
                              var3x.getSlots(),
                              new ItemBuilder(var3x.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
                                 .withDisplayName(Lang.MAX_AMOUNT_REACHED.get(this.player))
                                 .build()
                           );
                        } else {
                           this.setItem(var3x.getSlots(), var1 == null ? new ItemStack(Material.AIR) : var1.getItem());
                        }
                     } else {
                        this.setItem(var3x.getSlots(), var3x.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
                     }
                     break;
                  case SELECT_AMOUNT:
                     if ((visualLimitAction == TransactionMenu.VisualLimitAction.NONE || !checkInitialAmount || this.amount != var3x.getAction().amount)
                        && !this.isMaxAmount(var3x.getAction().amount)
                        && this.shopItem.isAllowQuantity()) {
                        this.setItem(var3x.getSlots(), var3x.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
                     } else if (visualLimitAction == TransactionMenu.VisualLimitAction.RENAME) {
                        this.setItem(
                           var3x.getSlots(),
                           new ItemBuilder(var3x.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
                              .withDisplayName(Lang.MAX_AMOUNT_REACHED.get(this.player))
                              .build()
                        );
                     } else {
                        this.setItem(var3x.getSlots(), var1 == null ? new ItemStack(Material.AIR) : var1.getItem());
                     }
                     break;
                  case NORMAL:
                     this.setItem(var3x.getSlots(), var3x.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()));
               }
            }
         );
      if (var1 != null) {
         for (int var3 = 0; var3 < this.inv.getSize() - (OnyxShop.getInstance().navBar.isEnableTransactionNav() ? 9 : 0); var3++) {
            if (this.inv.getItem(var3) == null) {
               this.inv.setItem(var3, var1.getItem());
            }
         }
      }
   }

   private boolean isMaxAmount(int var1) {
      return this.transactionMode == Transaction.Mode.BUY
         ? (this.transactionType == Transaction.Type.BUY_STACKS_SCREEN ? this.shopItem.isMaxBuy(var1 * this.getMaxStackSize()) : this.shopItem.isMaxBuy(var1))
         : this.shopItem.isMaxSell(var1);
   }

   private int getMaxAmount() {
      return this.transactionMode == Transaction.Mode.BUY
         ? (this.transactionType == Transaction.Type.BUY_STACKS_SCREEN ? this.shopItem.getMaxBuy() / this.getMaxStackSize() : this.shopItem.getMaxBuy())
         : this.shopItem.getMaxSell();
   }

   private boolean isMinAmount(int var1) {
      return this.transactionMode == Transaction.Mode.BUY
         ? (this.transactionType == Transaction.Type.BUY_STACKS_SCREEN ? this.shopItem.isMinBuy(var1 * this.getMaxStackSize()) : this.shopItem.isMinBuy(var1))
         : this.shopItem.isMinSell(var1);
   }

   private int getMinAmount() {
      return this.transactionMode == Transaction.Mode.BUY
         ? (this.transactionType == Transaction.Type.BUY_STACKS_SCREEN ? this.shopItem.getMinBuy() / this.getMaxStackSize() : this.shopItem.getMinBuy())
         : this.shopItem.getMinSell();
   }

   public void updateAmount(CreateItem.TransactionItemAction var1) {
      int var2 = this.amount;
      switch (var1.type) {
         case ADD:
            var2 += var1.amount;
            break;
         case REMOVE:
            var2 -= var1.amount;
            break;
         case SET:
            var2 = var1.amount;
      }

      if (this.shopItem.isFixedStackSize()) {
         if (var2 > this.amount) {
            var2 = this.shopItem.getLargestStackSize(var2);
         } else {
            var2 = this.shopItem.getSmallestStackSize(var2);
         }
      }

      this.updateButtons(var2);
      this.setSelectedItem(false);
      this.setConfirmTransactionItem();
   }

   private void setItem(List<Integer> var1, ItemStack var2) {
      var1.forEach(var2x -> this.inv.setItem(var2x, var2));
   }

   public void open(boolean var1) {
      this.open(var1, false);
   }

   public void open(boolean var1, boolean var2) {
      if (OnyxShop.getInstance().bedrock
         && OnyxShop.getInstance().bedrockMenus
         && UserManager.getUser(this.player).isBedrock()
         && OnyxShop.getInstance().getBedrockInventorys().isEnabledFor(this.player)) {
         OnyxShop.getInstance().getBedrockInventorys().openTransactionMenu(this);
      } else {
         if (this.shopItem.isDynamicPricing() || var2) {
            TransactionMenus.getItemsByAction(
                  this.transactionType, CreateItem.TransactionItemActionType.INSTA_BUY, CreateItem.TransactionItemActionType.INSTA_SELL
               )
               .forEach(
                  var1x -> this.setItem(var1x.getSlots(), var1x.getItem(this.shopItem, this.player, this.transactionType, this.ecoType, this.getTotalAmount()))
               );
         }

         this.setSelectedItem(var1);
         this.setConfirmTransactionItem();
         if (this.transactionType != Transaction.Type.SHOPSTAND_BUY_SCREEN && this.transactionType != Transaction.Type.SHOPSTAND_SELL_SCREEN) {
            OnyxShop.getInstance().navBar.addTransactionNavBar(this.inv, this.transactionMode, this.player, this.shopItem, this.getEcoTypeSimple());
         } else {
            OnyxShop.getInstance().navBar.addShopStandsNavBar(this.inv, this.transactionMode, this.player, this.shopItem, this.getEcoTypeSimple());
         }

         this.open(this.player);
      }
   }

   private void open(Player var1) {
      UserManager.getUser(var1).setOpenNewGUI(true);
      var1.openInventory(this.inv);
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

      public SelectedItem(ShopItem nullx) {
         TransactionItem var3 = TransactionMenus.getItemByType(TransactionMenu.this.transactionType, CreateItem.TransactionItemType.SELECTED_ITEM);
         this.item = nullx.getShopItem().clone();
         this.maxStackSize = this.item.getMaxStackSize();
         if (var3 != null) {
            this.placeholders = this.getPlaceholders(var3.getItem().getItemMeta());
         }
      }

      public ItemStack get() {
         this.item.setAmount(TransactionMenu.this.amount);
         this.parsePlaceholders();
         return this.item;
      }

      private void parsePlaceholders() {
         ItemMeta var1 = this.item.getItemMeta();
         if (this.displayname != null) {
            OnyxShop.getInstance()
               .getMetaUtils()
               .setDisplayName(
                  var1,
                  this.translatePapi(this.displayname, -1)
                     .replace("%amount%", String.valueOf(TransactionMenu.this.getTotalAmount()))
                     .replace("%price%", this.formatPrice(this.displayname, -1))
               );
         }

         if (this.lore != null) {
            ArrayList var2 = new ArrayList<>(this.lore);

            for (int var3 = 0; var3 < var2.size(); var3++) {
               var2.set(
                  var3,
                  this.translatePapi((Translatable)var2.get(var3), var3)
                     .replace("%amount%", String.valueOf(TransactionMenu.this.getTotalAmount()))
                     .replace("%price%", this.formatPrice((Translatable)var2.get(var3), var3))
               );
            }

            this.addStockLore(var2);
            OnyxShop.getInstance().getMetaUtils().setLore(var1, var2);
         }

         this.item.setItemMeta(var1);
      }

      private String formatPrice(Translatable var1, int var2) {
         if (this.pricingPlaceholder != var2) {
            return "0";
         } else if (TransactionMenu.this.transactionMode == Transaction.Mode.BUY) {
            return TransactionMenu.this.shopItem.isABuyPricing()
               ? TransactionMenu.this.shopItem
                  .getABuyPrice()
                  .formatBuyPrices(
                     var1.getLegacy().split("%price%")[0],
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
                     var1.getLegacy().split("%price%")[0],
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

      private Translatable translatePapi(Translatable var1, int var2) {
         return !this.placeholders.contains(var2) ? var1 : var1.translate(TransactionMenu.this.player);
      }

      private void addStockLore(List<Translatable> var1) {
         if (TransactionMenu.this.transactionMode == Transaction.Mode.BUY && TransactionMenu.this.shopItem.getLimitedStockMode() != 0) {
            var1.add(
               OnyxShop.getInstance()
                  .getLimitedStockProvider()
                  .getStockLore(TransactionMenu.this.shopItem, TransactionMenu.this.player, TransactionMenu.this.itemStock)
            );
         } else if (TransactionMenu.this.transactionMode == Transaction.Mode.SELL && TransactionMenu.this.shopItem.getLimitedSellMode() != 0) {
            var1.add(
               OnyxShop.getInstance()
                  .getLimitedSellProvider()
                  .getStockLore(TransactionMenu.this.shopItem, TransactionMenu.this.player, TransactionMenu.this.sellLimit)
            );
         }
      }

      private Set<Integer> getPlaceholders(ItemMeta var1) {
         if (var1.hasDisplayName()) {
            Translatable var2 = OnyxShop.getInstance().getMetaUtils().getDisplayName(var1);
            if (var2.toString().contains("%price%")) {
               this.pricingPlaceholder = -1;
            }

            this.displayname = var2;
         }

         if (var1.hasLore()) {
            List var4 = OnyxShop.getInstance().getMetaUtils().getLore(var1);

            for (int var3 = 0; var3 < var4.size(); var3++) {
               if (((Translatable)var4.get(var3)).toString().contains("%price%")) {
                  this.pricingPlaceholder = var3;
                  break;
               }
            }

            this.lore = var4;
         }

         return PlaceholderAPIHook.getPlaceholders(var1).keySet();
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
         } catch (IllegalArgumentException var1) {
            SendMessage.logDebugMessage(
               "Failed to load transaction visual limit for " + ConfigManager.getConfig().getString("visual-transaction-limits.action")
            );
            return REMOVE;
         }
      }
   }
}
