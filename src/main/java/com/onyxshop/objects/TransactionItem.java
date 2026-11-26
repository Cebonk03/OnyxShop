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
      CreateItem.TransactionItemAction var1, CreateItem.TransactionItemType var2, List<String> var3, ItemStack var4, List<Integer> var5, boolean var6
   ) {
      this(var1, var2, var3, var4, var5, var6, null);
   }

   public TransactionItem(
      CreateItem.TransactionItemAction var1,
      CreateItem.TransactionItemType var2,
      List<String> var3,
      ItemStack var4,
      List<Integer> var5,
      boolean var6,
      String var7
   ) {
      this.placeholders = this.getPlaceholders(var4);
      this.dynamicItem = var6;
      this.clickCommands = var3;
      this.action = var1;
      this.type = var2;
      this.item = var4;
      this.slots = var5;
      this.bedrockTexture = var7;
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

   public ItemStack getItem(ShopItem var1, Player var2, Transaction.Type var3, EcoType var4, int var5) {
      ItemStack var6 = this.dynamicItem ? new ItemBuilder(var1.getShopItem()).withSimpleMeta(this.getItem()).build() : this.getItem();
      return !this.placeholders.isEmpty() && var2 != null ? this.parsePlaceholders(var1, var2, var6, var3, var4, var5) : var6;
   }

   public List<Integer> getSlots() {
      return this.slots;
   }

   private ItemStack parsePlaceholders(ShopItem var1, Player var2, ItemStack var3, Transaction.Type var4, EcoType var5, int var6) {
      ItemMeta var7 = var3.getItemMeta();
      if (this.action.type == CreateItem.TransactionItemActionType.INSTA_BUY || this.action.type == CreateItem.TransactionItemActionType.INSTA_SELL) {
         var6 = var4 == Transaction.Type.BUY_STACKS_SCREEN
            ? var3.getAmount() * (var1.isFixedStackSize() ? var1.getFixedStackSize() : var3.getMaxStackSize())
            : var3.getAmount();
      }

      if (this.placeholders.contains(-1)) {
         String var8 = OnyxShop.getInstance().getMetaUtils().getRawDisplayName(var7);
         OnyxShop.getInstance()
            .getMetaUtils()
            .setRawDisplayName(
               var7,
               PlaceholderAPIHook.translate(var2, var8)
                  .replace("%amount%", var6 + "")
                  .replace("%price%", this.getCalculatedPriceFormatted(var8, var1, var2, var3, var4.getTransactionMode(), var5, var6))
            );
      }

      if (var7.hasLore()) {
         List var12 = OnyxShop.getInstance().getMetaUtils().getRawLore(var7);

         for (Integer var10 : this.placeholders) {
            if (var10 != -1) {
               String var11 = (String)var12.get(var10);
               var12.set(
                  var10,
                  PlaceholderAPIHook.translate(var2, var11)
                     .replace("%amount%", var6 + "")
                     .replace("%price%", this.getCalculatedPriceFormatted(var11, var1, var2, var3, var4.getTransactionMode(), var5, var6))
               );
            }
         }

         OnyxShop.getInstance().getMetaUtils().setRawLore(var7, var12);
      }

      var3.setItemMeta(var7);
      return var3;
   }

   private double getCalculatedPrice(ShopItem var1, Player var2, ItemStack var3, Transaction.Mode var4, EcoType var5, int var6) {
      if (!this.pricingPlaceholders) {
         return 0.0;
      } else if (var4 == Transaction.Mode.BUY) {
         return var1.isABuyPricing()
            ? var1.getABuyPrice().getBuyPrices(var5, var2, var6, var1.getBuyModifiers()).entrySet().stream().findFirst().orElse(null).getValue()
            : var1.getBuyPrice(var2, var6);
      } else if (var4 == Transaction.Mode.SELL) {
         return var1.isASellPricing()
            ? var1.getASellPrice().getSellPrices(var5, var2, var6, var1.getSellModifiers()).entrySet().stream().findFirst().orElse(null).getValue()
            : var1.getSellPrice(var2, var3, var6, 0);
      } else {
         return 0.0;
      }
   }

   private String getCalculatedPriceFormatted(String var1, ShopItem var2, Player var3, ItemStack var4, Transaction.Mode var5, EcoType var6, int var7) {
      if (!this.pricingPlaceholders) {
         return "0.0";
      } else if (var5 == Transaction.Mode.BUY) {
         return var2.isABuyPricing()
            ? var2.getABuyPrice().formatBuyPrices(var1.split("%price%")[0], var6, var3, var7, var2.getBuyModifiers())
            : OnyxShop.getInstance().formatPrice(var6, var2.getBuyPrice(var3, var7));
      } else if (var5 == Transaction.Mode.SELL) {
         return var2.isASellPricing()
            ? var2.getASellPrice().formatSellPrices(var1.split("%price%")[0], var6, var3, var7, var2.getSellModifiers())
            : OnyxShop.getInstance().formatPrice(var6, var2.getSellPrice(var3, var4, var7, 0));
      } else {
         return "0.0";
      }
   }

   private boolean hasPricingPlaceholders(ItemStack var1) {
      if (!var1.hasItemMeta()) {
         return false;
      } else {
         String var2 = var1.getItemMeta().getDisplayName();
         if (var2.contains("%price%") || var2.contains("%formattedPrice%") || var2.contains("%prices%")) {
            return true;
         } else if (!var1.getItemMeta().hasLore()) {
            return false;
         } else {
            for (String var4 : var1.getItemMeta().getLore()) {
               if (var4.contains("%price%") || var4.contains("%formattedPrice%") || var4.contains("%prices%")) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private Set<Integer> getPlaceholders(ItemStack var1) {
      HashSet var2 = new HashSet<>(PlaceholderAPIHook.getPlaceholders(var1).keySet());
      if (!var1.hasItemMeta()) {
         return var2;
      } else {
         String var3 = var1.getItemMeta().getDisplayName();
         if (var3 != null && (var3.contains("%price%") || var3.contains("%amount%"))) {
            this.pricingPlaceholders = true;
            var2.add(-1);
         }

         if (!var1.getItemMeta().hasLore()) {
            return var2;
         } else {
            int var4 = 0;

            for (String var6 : var1.getItemMeta().getLore()) {
               if (var6.contains("%price%") || var6.contains("%amount%")) {
                  this.pricingPlaceholders = true;
                  var2.add(var4);
               }

               var4++;
            }

            return var2;
         }
      }
   }
}
