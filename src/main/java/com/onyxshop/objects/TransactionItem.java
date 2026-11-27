package com.onyxshop.objects;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.CreateItem;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.Transaction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TransactionItem {
   private final CreateItem.TransactionItemAction action;
   private final CreateItem.TransactionItemType type;
   private final Set<Integer> placeholders;
   private final List<String> clickCommands;
   private final ItemStack item;
   private final List<Integer> slots;
   private final boolean dynamicItem;
   private boolean pricingPlaceholders;
   private final String bedrockTexture;

   public TransactionItem(
      CreateItem.TransactionItemAction transactionItemAction,
      CreateItem.TransactionItemType transactionItemType,
      List<String> clickCommands,
      ItemStack item,
      List<Integer> slots,
      boolean dynamicItem
   ) {
      this(transactionItemAction, transactionItemType, clickCommands, item, slots, dynamicItem, null);
   }

   public TransactionItem(
      CreateItem.TransactionItemAction transactionItemAction,
      CreateItem.TransactionItemType transactionItemType,
      List<String> clickCommands,
      ItemStack item,
      List<Integer> slots,
      boolean dynamicItem,
      String bedrockTexture
   ) {
      this.placeholders = this.getPlaceholders(item);
      this.dynamicItem = dynamicItem;
      this.clickCommands = clickCommands;
      this.action = transactionItemAction;
      this.type = transactionItemType;
      this.item = item;
      this.slots = slots;
      this.bedrockTexture = bedrockTexture;
   }

   public String getBedrockTexture() {
      return this.bedrockTexture;
   }

   public CreateItem.TransactionItemAction getAction() {
      return this.action;
   }

   public CreateItem.TransactionItemType getType() {
      return this.type;
   }

   public List<String> getClickCommands() {
      return this.clickCommands;
   }

   public ItemStack getItem() {
      return this.item.clone();
   }

   public ItemStack getItem(ShopItem shopItem, Player player, Transaction.Type type, EcoType ecoType, int value) {
      ItemStack itemStack = this.dynamicItem ? new ItemBuilder(shopItem.getShopItem()).withSimpleMeta(this.getItem()).build() : this.getItem();
      return !this.placeholders.isEmpty() && player != null ? this.parsePlaceholders(shopItem, player, itemStack, type, ecoType, value) : itemStack;
   }

   public List<Integer> getSlots() {
      return this.slots;
   }

   private ItemStack parsePlaceholders(ShopItem shopItem, Player player, ItemStack itemStack, Transaction.Type type, EcoType ecoType, int value) {
      ItemMeta itemMeta = itemStack.getItemMeta();
      if (this.action.type == CreateItem.TransactionItemActionType.INSTA_BUY || this.action.type == CreateItem.TransactionItemActionType.INSTA_SELL) {
         value = type == Transaction.Type.BUY_STACKS_SCREEN
            ? itemStack.getAmount() * (shopItem.isFixedStackSize() ? shopItem.getFixedStackSize() : itemStack.getMaxStackSize())
            : itemStack.getAmount();
      }

      if (this.placeholders.contains(-1)) {
         String rawDisplayName = OnyxShop.getInstance().getMetaUtils().getRawDisplayName(itemMeta);
         OnyxShop.getInstance()
            .getMetaUtils()
            .setRawDisplayName(
               itemMeta,
               PlaceholderAPIHook.translate(player, rawDisplayName)
                  .replace("%amount%", value + "")
                  .replace("%price%", this.getCalculatedPriceFormatted(rawDisplayName, shopItem, player, itemStack, type.getTransactionMode(), ecoType, value))
            );
      }

      if (itemMeta.hasLore()) {
         List rawLore = OnyxShop.getInstance().getMetaUtils().getRawLore(itemMeta);

         for (Integer value2 : this.placeholders) {
            if (value2 != -1) {
               String text = (String)rawLore.get(value2);
               rawLore.set(
                  value2,
                  PlaceholderAPIHook.translate(player, text)
                     .replace("%amount%", value + "")
                     .replace("%price%", this.getCalculatedPriceFormatted(text, shopItem, player, itemStack, type.getTransactionMode(), ecoType, value))
               );
            }
         }

         OnyxShop.getInstance().getMetaUtils().setRawLore(itemMeta, rawLore);
      }

      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }

   private double getCalculatedPrice(ShopItem shopItem, Player player, ItemStack itemStack, Transaction.Mode mode, EcoType ecoType, int price) {
      if (!this.pricingPlaceholders) {
         return 0.0;
      } else if (mode == Transaction.Mode.BUY) {
         return shopItem.isABuyPricing()
            ? shopItem.getABuyPrice().getBuyPrices(ecoType, player, price, shopItem.getBuyModifiers()).entrySet().stream().findFirst().orElse(null).getValue()
            : shopItem.getBuyPrice(player, price);
      } else if (mode == Transaction.Mode.SELL) {
         return shopItem.isASellPricing()
            ? shopItem.getASellPrice().getSellPrices(ecoType, player, price, shopItem.getSellModifiers()).entrySet().stream().findFirst().orElse(null).getValue()
            : shopItem.getSellPrice(player, itemStack, price, 0);
      } else {
         return 0.0;
      }
   }

   private String getCalculatedPriceFormatted(String price, ShopItem shopItem, Player player, ItemStack itemStack, Transaction.Mode mode, EcoType ecoType, int value) {
      if (!this.pricingPlaceholders) {
         return "0.0";
      } else if (mode == Transaction.Mode.BUY) {
         return shopItem.isABuyPricing()
            ? shopItem.getABuyPrice().formatBuyPrices(price.split("%price%")[0], ecoType, player, value, shopItem.getBuyModifiers())
            : OnyxShop.getInstance().formatPrice(ecoType, shopItem.getBuyPrice(player, value));
      } else if (mode == Transaction.Mode.SELL) {
         return shopItem.isASellPricing()
            ? shopItem.getASellPrice().formatSellPrices(price.split("%price%")[0], ecoType, player, value, shopItem.getSellModifiers())
            : OnyxShop.getInstance().formatPrice(ecoType, shopItem.getSellPrice(player, itemStack, value, 0));
      } else {
         return "0.0";
      }
   }

   private boolean hasPricingPlaceholders(ItemStack itemStack) {
      if (!itemStack.hasItemMeta()) {
         return false;
      } else {
         String displayName = itemStack.getItemMeta().getDisplayName();
         if (displayName.contains("%price%") || displayName.contains("%formattedPrice%") || displayName.contains("%prices%")) {
            return true;
         } else if (!itemStack.getItemMeta().hasLore()) {
            return false;
         } else {
            for (String itemId : itemStack.getItemMeta().getLore()) {
               if (itemId.contains("%price%") || itemId.contains("%formattedPrice%") || itemId.contains("%prices%")) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private Set<Integer> getPlaceholders(ItemStack itemStack) {
      HashSet values = new HashSet<>(PlaceholderAPIHook.getPlaceholders(itemStack).keySet());
      if (!itemStack.hasItemMeta()) {
         return values;
      } else {
         String displayName = itemStack.getItemMeta().getDisplayName();
         if (displayName != null && (displayName.contains("%price%") || displayName.contains("%amount%"))) {
            this.pricingPlaceholders = true;
            values.add(-1);
         }

         if (!itemStack.getItemMeta().hasLore()) {
            return values;
         } else {
            int value = 0;

            for (String itemId : itemStack.getItemMeta().getLore()) {
               if (itemId.contains("%price%") || itemId.contains("%amount%")) {
                  this.pricingPlaceholders = true;
                  values.add(value);
               }

               value++;
            }

            return values;
         }
      }
   }
}
