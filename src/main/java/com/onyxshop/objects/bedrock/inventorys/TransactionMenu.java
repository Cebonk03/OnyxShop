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

   public TransactionMenu(boolean flag, com.onyxshop.objects.TransactionMenu transactionMenu) {
      super(flag ? TransactionMenus.getSize(transactionMenu.getTransactionType()) : 0);
      this.menu = transactionMenu;
      this.shopItem = transactionMenu.getShopItem();
      this.buy = transactionMenu.getTransactionMode() == Transaction.Mode.BUY;
      this.type = transactionMenu.getTransactionType();
      this.currencyTypes = transactionMenu.isAPricing() ? (this.buy ? this.shopItem.getABuyPrice().getTypes() : this.shopItem.getASellPrice().getTypes()) : null;
      this.showCurrencyDropdown = this.currencyTypes != null && !transactionMenu.isAllPrices() && this.currencyTypes.size() > 1;
   }

   @Override
   public void openForm(OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer) {
      this.p = player;
      int size = plugin.allowIllegalStacks ? 64 : this.shopItem.getItemToGive().getMaxStackSize();
      int value = this.buy
         ? (this.shopItem.getMaxBuy() > 0 ? this.shopItem.getMaxBuy() : size)
         : (this.shopItem.getMaxSell() > 0 ? this.shopItem.getMaxSell() : size);
      int value2 = this.buy ? Math.max(1, this.shopItem.getMinBuy()) : Math.max(1, this.shopItem.getMinSell());
      if (value2 > value) {
         value2 = value;
      }

      int value3 = 1;
      if (this.shopItem.isFixedStackSize()) {
         value2 = this.shopItem.getSmallestStackSize(value2);
         value = this.shopItem.getLargestStackSize(value);
         if (value2 > value) {
            value2 = value;
         }

         value3 = this.shopItem.getFixedStackSize();
      }

      int value4 = value2;
      int value5 = value;
      int value6 = Math.max(
         value2, Math.min(value, this.shopItem.isFixedStackSize() ? this.shopItem.getClosestStackSize(this.menu.getAmount()) : this.menu.getAmount())
      );
      Builder builder = (Builder)CustomForm.builder().title(this.menu.getInvTitle().getBedrockLegacy());
      builder.label(this.getFormattedPrice(player, value6));
      builder.slider(Lang.SELECTED_AMOUNT.get(player).getBedrockLegacy(), value2, value, value3, value6);
      byte value7 = -1;
      if (this.showCurrencyDropdown) {
         ArrayList values = new ArrayList();

         for (EcoType ecoType : this.currencyTypes) {
            values.add(plugin.getEcoHandler().getEcon(ecoType).getFriendly());
         }

         int index = Math.max(0, this.currencyTypes.indexOf(this.menu.getCurrentEcoType()));
         builder.dropdown(Lang.CHANGE_CURRENCY.get(player).getBedrockLegacy(), values, index);
         value7 = 2;
      }

      byte value8 = value7;
      builder.validResultHandler(element -> {
         if (value8 != -1) {
            this.menu.setEcoType(this.currencyTypes.get(element.asDropdown(value8)));
         }

         int value9 = Math.max(value4, Math.min(value5, Math.round(element.asSlider(1))));
         this.menu.setAmount(value9);
         this.complete(plugin, player);
      });
      floodgatePlayer.sendForm(builder);
   }

   private String getFormattedPrice(Player player, int price) {
      Map map = this.buy
         ? (
            this.menu.isAPricing()
               ? this.shopItem.getABuyPrice().getBuyPrices(this.menu.getCurrentEcoType(), player, price, this.shopItem.getBuyModifiers())
               : Collections.singletonMap(this.menu.getCurrentEcoType(), this.shopItem.getBuyPrice(player, price))
         )
         : (
            this.menu.isAPricing()
               ? this.shopItem.getASellPrice().getSellPrices(this.menu.getCurrentEcoType(), player, price, this.shopItem.getSellModifiers())
               : Collections.singletonMap(this.menu.getCurrentEcoType(), this.shopItem.getSellPrice(player, price))
         );
      return OnyxShop.getInstance().formatPrices(null, map, this.menu.isAllPrices());
   }

   @Override
   public void openGridForm(OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, boolean flag) {
      this.p = player;
      this.buildGridForm(plugin, player, floodgatePlayer, flag);
   }

   private void buildGridForm(OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, boolean flag) {
      HashMap map = new HashMap();
      HashMap map2 = new HashMap();
      HashSet<Integer> values = new HashSet<>();
      HashSet values2 = new HashSet();

      for (TransactionItem transactionItem : TransactionMenus.getItems(this.type)) {
         ItemStack itemStack = transactionItem.getItem(this.shopItem, player, this.type, this.menu.getCurrentEcoType(), this.menu.getTotalAmount());
         boolean flag2 = transactionItem.getType() == CreateItem.TransactionItemType.FILLITEM;
         boolean flag3 = transactionItem.getType() == CreateItem.TransactionItemType.SELECTED_ITEM;

         for (int value : transactionItem.getSlots()) {
            if (value >= 0 && value < this.items.length) {
               map.put(value, itemStack);
               map2.put(value, transactionItem.getBedrockTexture());
               if (flag2) {
                  values.add(value);
               }

               if (flag3) {
                  values2.add(value);
               }
            }
         }
      }

      this.currencySlot = null;
      if (this.showCurrencyDropdown && !values.isEmpty()) {
         this.currencySlot = Collections.min(values);
         values.remove(this.currencySlot);
         map.put(this.currencySlot, this.getCurrencyToggleItem(plugin));
      }

      org.geysermc.cumulus.form.SimpleForm.Builder builder = (org.geysermc.cumulus.form.SimpleForm.Builder)SimpleForm.builder()
         .title(this.getTitle(this.menu.getInvTitle(), this.items.length, 99, flag));

      for (int index = 0; index < this.items.length; index++) {
         ItemStack itemStack = (ItemStack)map.get(index);
         if (itemStack == null) {
            builder.button("null");
         } else {
            this.addGridItem(
               builder, itemStack, values.contains(index), index, false, values2.contains(index) ? this.shopItem.getBedrockTexture() : (String)map2.get(index)
            );
         }
      }

      builder.validResultHandler(element -> {
         int id = element.clickedButtonId();
         if (this.items[id] != null) {
            this.handleClick(builder, plugin, player, floodgatePlayer, id, -1, flag);
         }
      });
      floodgatePlayer.sendForm(builder);
   }

   private void reopenGridForm(
      org.geysermc.cumulus.form.SimpleForm.Builder builder, OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, boolean flag, int value, int value2
   ) {
      if (flag && value != value2) {
         builder = (org.geysermc.cumulus.form.SimpleForm.Builder)SimpleForm.builder().title(this.getTitle(this.menu.getInvTitle(), this.items.length, value, flag));

         for (int index = 0; index < this.items.length; index++) {
            if (this.items[index] != null) {
               this.readdGridItem(builder, index, value == index);
            } else {
               builder.button("null");
            }
         }

         value2 = value;
      }

      int value3 = value2;
      org.geysermc.cumulus.form.SimpleForm.Builder builder2 = builder;
      builder.validResultHandler(element -> {
         int id = element.clickedButtonId();
         if (this.items[id] != null) {
            this.handleClick(builder2, plugin, player, floodgatePlayer, id, value3, flag);
         }
      });
      floodgatePlayer.sendForm(builder);
   }

   private void handleClick(
      org.geysermc.cumulus.form.SimpleForm.Builder builder, OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, int value, int value2, boolean flag
   ) {
      if (flag && value2 != value) {
         this.reopenGridForm(builder, plugin, player, floodgatePlayer, flag, value, value2);
      } else if (this.items[value].isFillItem()) {
         this.reopenGridForm(builder, plugin, player, floodgatePlayer, flag, 99, value2);
      } else if (this.currencySlot != null && value == this.currencySlot) {
         this.toggleCurrency();
         this.buildGridForm(plugin, player, floodgatePlayer, flag);
      } else {
         TransactionItem transactionItem = TransactionMenus.getItemFromSlot(this.type, value);
         if (!this.handleAction(plugin, player, transactionItem)) {
            this.buildGridForm(plugin, player, floodgatePlayer, flag);
         }
      }
   }

   private void toggleCurrency() {
      int index = this.currencyTypes.indexOf(this.menu.getCurrentEcoType());
      this.menu.setEcoType(this.currencyTypes.get((index + 1) % this.currencyTypes.size()));
   }

   private ItemStack getCurrencyToggleItem(OnyxShop plugin) {
      ItemStack itemStack = new ItemStack(Material.COMPASS);
      ItemMeta itemMeta = itemStack.getItemMeta();
      plugin.getMetaUtils().setDisplayName(itemMeta, Lang.CHANGE_CURRENCY.get(this.p));
      plugin.getMetaUtils()
         .setLore(
            itemMeta,
            Collections.singletonList(
               Lang.CURRENT_CURRENCY.get(this.p).replace("%currency%", plugin.getEcoHandler().getEcon(this.menu.getCurrentEcoType()).getFriendly())
            )
         );
      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }

   private boolean handleAction(OnyxShop plugin, Player player, TransactionItem transactionItem) {
      if (transactionItem == null) {
         return false;
      } else {
         switch (transactionItem.getAction().type) {
            case BACK:
               if (this.type != Transaction.Type.BUY_STACKS_SCREEN) {
                  com.onyxshop.objects.shops.ShopSection section = plugin.getSection(this.shopItem.section);
                  section.openShopSection(player, section.getPageForShopItem(this.shopItem.itemLoc), this.menu.isDisabledBackButton(), this.menu.getRootSection());
                  plugin.getSoundEffects().useBackButton(player);
               } else {
                  new com.onyxshop.objects.TransactionMenu(
                        player,
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
               List<String> clickCommands = transactionItem.getClickCommands();
               if (!clickCommands.isEmpty()) {
                  clickCommands.forEach(entry -> CommandHelper.executeCommand(null, player, entry));
               }

               return false;
            case INSTA_BUY:
            case INSTA_SELL:
               this.menu.setAmount(transactionItem.getItem().getAmount());
            case CONFIRM_TRANSACTION:
               this.complete(plugin, player);
               return true;
            case SELL_ALL:
               plugin.menuHandler.completeSellAll(this.menu, player);
               return true;
            case OPEN_BUY_STACKS:
               if (!this.shopItem.isMaxBuy(this.menu.getMaxStackSize()) && this.shopItem.isAllowQuantity()) {
                  new com.onyxshop.objects.TransactionMenu(
                        player,
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
               this.menu.updateAmount(transactionItem.getAction());
               return false;
         }
      }
   }

   private void complete(OnyxShop plugin, Player player) {
      if (this.menu.isAPricing()) {
         plugin.menuHandler.completeTransactionA(this.menu, player);
      } else {
         plugin.menuHandler.completeTransaction(this.menu, player);
      }
   }
}
