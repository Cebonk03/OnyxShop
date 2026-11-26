package com.onyxshop.objects.bedrock.inventorys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.CreateItem;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.TransactionItem;
import com.onyxshop.objects.TransactionMenus;
import com.onyxshop.objects.bedrock.BedrockInventory;
import com.onyxshop.util.CommandHelper;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.Transaction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.CustomForm.Builder;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

public class TransactionMenu extends BedrockInventory {
   private final com.onyxshop.objects.TransactionMenu menu;
   private final ShopItem shopItem;
   private final boolean buy;
   private final Transaction.Type type;
   private final List<EcoType> currencyTypes;
   private final boolean showCurrencyDropdown;
   private Integer currencySlot;

   public TransactionMenu(boolean var1, com.onyxshop.objects.TransactionMenu var2) {
      super(var1 ? TransactionMenus.getSize(var2.getTransactionType()) : 0);
      this.menu = var2;
      this.shopItem = var2.getShopItem();
      this.buy = var2.getTransactionMode() == Transaction.Mode.BUY;
      this.type = var2.getTransactionType();
      this.currencyTypes = var2.isAPricing() ? (this.buy ? this.shopItem.getABuyPrice().getTypes() : this.shopItem.getASellPrice().getTypes()) : null;
      this.showCurrencyDropdown = this.currencyTypes != null && !var2.isAllPrices() && this.currencyTypes.size() > 1;
   }

   @Override
   public void openForm(OnyxShop var1, Player var2, FloodgatePlayer var3) {
      this.p = var2;
      int var4 = var1.allowIllegalStacks ? 64 : this.shopItem.getItemToGive().getMaxStackSize();
      int var5 = this.buy
         ? (this.shopItem.getMaxBuy() > 0 ? this.shopItem.getMaxBuy() : var4)
         : (this.shopItem.getMaxSell() > 0 ? this.shopItem.getMaxSell() : var4);
      int var6 = this.buy ? Math.max(1, this.shopItem.getMinBuy()) : Math.max(1, this.shopItem.getMinSell());
      if (var6 > var5) {
         var6 = var5;
      }

      int var7 = 1;
      if (this.shopItem.isFixedStackSize()) {
         var6 = this.shopItem.getSmallestStackSize(var6);
         var5 = this.shopItem.getLargestStackSize(var5);
         if (var6 > var5) {
            var6 = var5;
         }

         var7 = this.shopItem.getFixedStackSize();
      }

      int var8 = var6;
      int var9 = var5;
      int var10 = Math.max(
         var6, Math.min(var5, this.shopItem.isFixedStackSize() ? this.shopItem.getClosestStackSize(this.menu.getAmount()) : this.menu.getAmount())
      );
      Builder var11 = (Builder)CustomForm.builder().title(this.menu.getInvTitle().getBedrockLegacy());
      var11.label(this.getFormattedPrice(var2, var10));
      var11.slider(Lang.SELECTED_AMOUNT.get(var2).getBedrockLegacy(), var6, var5, var7, var10);
      byte var12 = -1;
      if (this.showCurrencyDropdown) {
         ArrayList var13 = new ArrayList();

         for (EcoType var15 : this.currencyTypes) {
            var13.add(var1.getEcoHandler().getEcon(var15).getFriendly());
         }

         int var17 = Math.max(0, this.currencyTypes.indexOf(this.menu.getCurrentEcoType()));
         var11.dropdown(Lang.CHANGE_CURRENCY.get(var2).getBedrockLegacy(), var13, var17);
         var12 = 2;
      }

      byte var16 = var12;
      var11.validResultHandler(var6x -> {
         if (var16 != -1) {
            this.menu.setEcoType(this.currencyTypes.get(var6x.asDropdown(var16)));
         }

         int var7x = Math.max(var8, Math.min(var9, Math.round(var6x.asSlider(1))));
         this.menu.setAmount(var7x);
         this.complete(var1, var2);
      });
      var3.sendForm(var11);
   }

   private String getFormattedPrice(Player var1, int var2) {
      Map var3 = this.buy
         ? (
            this.menu.isAPricing()
               ? this.shopItem.getABuyPrice().getBuyPrices(this.menu.getCurrentEcoType(), var1, var2, this.shopItem.getBuyModifiers())
               : Collections.singletonMap(this.menu.getCurrentEcoType(), this.shopItem.getBuyPrice(var1, var2))
         )
         : (
            this.menu.isAPricing()
               ? this.shopItem.getASellPrice().getSellPrices(this.menu.getCurrentEcoType(), var1, var2, this.shopItem.getSellModifiers())
               : Collections.singletonMap(this.menu.getCurrentEcoType(), this.shopItem.getSellPrice(var1, var2))
         );
      return OnyxShop.getInstance().formatPrices(null, var3, this.menu.isAllPrices());
   }

   @Override
   public void openGridForm(OnyxShop var1, Player var2, FloodgatePlayer var3, boolean var4) {
      this.p = var2;
      this.buildGridForm(var1, var2, var3, var4);
   }

   private void buildGridForm(OnyxShop var1, Player var2, FloodgatePlayer var3, boolean var4) {
      HashMap var5 = new HashMap();
      HashMap var6 = new HashMap();
      HashSet<Integer> var7 = new HashSet<>();
      HashSet var8 = new HashSet();

      for (TransactionItem var10 : TransactionMenus.getItems(this.type)) {
         ItemStack var11 = var10.getItem(this.shopItem, var2, this.type, this.menu.getCurrentEcoType(), this.menu.getTotalAmount());
         boolean var12 = var10.getType() == CreateItem.TransactionItemType.FILLITEM;
         boolean var13 = var10.getType() == CreateItem.TransactionItemType.SELECTED_ITEM;

         for (int var15 : var10.getSlots()) {
            if (var15 >= 0 && var15 < this.items.length) {
               var5.put(var15, var11);
               var6.put(var15, var10.getBedrockTexture());
               if (var12) {
                  var7.add(var15);
               }

               if (var13) {
                  var8.add(var15);
               }
            }
         }
      }

      this.currencySlot = null;
      if (this.showCurrencyDropdown && !var7.isEmpty()) {
         this.currencySlot = Collections.min(var7);
         var7.remove(this.currencySlot);
         var5.put(this.currencySlot, this.getCurrencyToggleItem(var1));
      }

      org.geysermc.cumulus.form.SimpleForm.Builder var16 = (org.geysermc.cumulus.form.SimpleForm.Builder)SimpleForm.builder()
         .title(this.getTitle(this.menu.getInvTitle(), this.items.length, 99, var4));

      for (int var17 = 0; var17 < this.items.length; var17++) {
         ItemStack var18 = (ItemStack)var5.get(var17);
         if (var18 == null) {
            var16.button("null");
         } else {
            this.addGridItem(
               var16, var18, var7.contains(var17), var17, false, var8.contains(var17) ? this.shopItem.getBedrockTexture() : (String)var6.get(var17)
            );
         }
      }

      var16.validResultHandler(var6x -> {
         int var7x = var6x.clickedButtonId();
         if (this.items[var7x] != null) {
            this.handleClick(var16, var1, var2, var3, var7x, -1, var4);
         }
      });
      var3.sendForm(var16);
   }

   private void reopenGridForm(
      org.geysermc.cumulus.form.SimpleForm.Builder var1, OnyxShop var2, Player var3, FloodgatePlayer var4, boolean var5, int var6, int var7
   ) {
      if (var5 && var6 != var7) {
         var1 = (org.geysermc.cumulus.form.SimpleForm.Builder)SimpleForm.builder().title(this.getTitle(this.menu.getInvTitle(), this.items.length, var6, var5));

         for (int var8 = 0; var8 < this.items.length; var8++) {
            if (this.items[var8] != null) {
               this.readdGridItem(var1, var8, var6 == var8);
            } else {
               var1.button("null");
            }
         }

         var7 = var6;
      }

      int var10 = var7;
      org.geysermc.cumulus.form.SimpleForm.Builder var9 = var1;
      var1.validResultHandler(var7x -> {
         int var8x = var7x.clickedButtonId();
         if (this.items[var8x] != null) {
            this.handleClick(var9, var2, var3, var4, var8x, var10, var5);
         }
      });
      var4.sendForm(var1);
   }

   private void handleClick(
      org.geysermc.cumulus.form.SimpleForm.Builder var1, OnyxShop var2, Player var3, FloodgatePlayer var4, int var5, int var6, boolean var7
   ) {
      if (var7 && var6 != var5) {
         this.reopenGridForm(var1, var2, var3, var4, var7, var5, var6);
      } else if (this.items[var5].isFillItem()) {
         this.reopenGridForm(var1, var2, var3, var4, var7, 99, var6);
      } else if (this.currencySlot != null && var5 == this.currencySlot) {
         this.toggleCurrency();
         this.buildGridForm(var2, var3, var4, var7);
      } else {
         TransactionItem var8 = TransactionMenus.getItemFromSlot(this.type, var5);
         if (!this.handleAction(var2, var3, var8)) {
            this.buildGridForm(var2, var3, var4, var7);
         }
      }
   }

   private void toggleCurrency() {
      int var1 = this.currencyTypes.indexOf(this.menu.getCurrentEcoType());
      this.menu.setEcoType(this.currencyTypes.get((var1 + 1) % this.currencyTypes.size()));
   }

   private ItemStack getCurrencyToggleItem(OnyxShop var1) {
      ItemStack var2 = new ItemStack(Material.COMPASS);
      ItemMeta var3 = var2.getItemMeta();
      var1.getMetaUtils().setDisplayName(var3, Lang.CHANGE_CURRENCY.get(this.p));
      var1.getMetaUtils()
         .setLore(
            var3,
            Collections.singletonList(
               Lang.CURRENT_CURRENCY.get(this.p).replace("%currency%", var1.getEcoHandler().getEcon(this.menu.getCurrentEcoType()).getFriendly())
            )
         );
      var2.setItemMeta(var3);
      return var2;
   }

   private boolean handleAction(OnyxShop var1, Player var2, TransactionItem var3) {
      if (var3 == null) {
         return false;
      } else {
         switch (var3.getAction().type) {
            case BACK:
               if (this.type != Transaction.Type.BUY_STACKS_SCREEN) {
                  com.onyxshop.objects.shops.ShopSection var5 = var1.getSection(this.shopItem.section);
                  var5.openShopSection(var2, var5.getPageForShopItem(this.shopItem.itemLoc), this.menu.isDisabledBackButton(), this.menu.getRootSection());
                  var1.getSoundEffects().useBackButton(var2);
               } else {
                  new com.onyxshop.objects.TransactionMenu(
                        var2,
                        this.shopItem,
                        this.menu.getRootSection(),
                        this.menu.isDisabledBackButton(),
                        this.menu.getTransactionMode(),
                        Transaction.Type.BUY_SCREEN,
                        this.menu.getAmount()
                     )
                     .open(false);
               }

               return true;
            case COMMAND:
               List<String> var4 = var3.getClickCommands();
               if (!var4.isEmpty()) {
                  var4.forEach(var1x -> CommandHelper.executeCommand(null, var2, var1x));
               }

               return false;
            case INSTA_BUY:
            case INSTA_SELL:
               this.menu.setAmount(var3.getItem().getAmount());
            case CONFIRM_TRANSACTION:
               this.complete(var1, var2);
               return true;
            case SELL_ALL:
               var1.menuHandler.completeSellAll(this.menu, var2);
               return true;
            case OPEN_BUY_STACKS:
               if (!this.shopItem.isMaxBuy(this.menu.getMaxStackSize()) && this.shopItem.isAllowQuantity()) {
                  new com.onyxshop.objects.TransactionMenu(
                        var2,
                        this.shopItem,
                        this.menu.getRootSection(),
                        this.menu.isDisabledBackButton(),
                        Transaction.Mode.BUY,
                        Transaction.Type.BUY_STACKS_SCREEN,
                        this.menu.getAmount()
                     )
                     .open(false);
                  return true;
               }

               return false;
            case NONE:
               return false;
            default:
               this.menu.updateAmount(var3.getAction());
               return false;
         }
      }
   }

   private void complete(OnyxShop var1, Player var2) {
      if (this.menu.isAPricing()) {
         var1.menuHandler.completeTransactionA(this.menu, var2);
      } else {
         var1.menuHandler.completeTransaction(this.menu, var2);
      }
   }
}
