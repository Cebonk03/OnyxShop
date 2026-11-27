package com.onyxshop.objects.prices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.providers.priceModifiers.Modifiers;
import com.onyxshop.providers.priceModifiers.PriceModifier;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class AdvancedBuyPrice {
   private final Map<EcoType, Double> buyPrices = new LinkedHashMap<>();
   private final String section;
   private final String itemLoc;
   private final String itemPath;
   private final boolean all;
   private boolean dynamicPricing;
   private int stackSize;

   public AdvancedBuyPrice(ShopItem shopItem) {
      this.itemLoc = shopItem.itemLoc;
      this.section = shopItem.section;
      this.itemPath = shopItem.getItemPath();
      this.all = ConfigManager.getShop(this.section).getBoolean(this.getItemLoc() + ".buy-prices.require-all");
   }

   public void init(ShopItem shopItem) throws ItemLoadException {
      this.stackSize = shopItem.getStackSize();
      this.dynamicPricing = shopItem.isDynamicPricing();
      this.initBuyPrices(shopItem);
   }

   public List<EcoType> has(Player player, double amount) {
      if (this.all) {
         return (List<EcoType>)(this.buyPrices
               .keySet()
               .stream()
               .allMatch(element -> OnyxShop.getInstance().getEcoHandler().getEcon(element).getBalance(player) >= amount)
            ? new ArrayList<>(this.buyPrices.keySet())
            : Collections.emptyList());
      } else {
         return this.buyPrices
            .keySet()
            .stream()
            .filter(element -> OnyxShop.getInstance().getEcoHandler().getEcon(element).getBalance(player) >= amount)
            .collect(Collectors.toList());
      }
   }

   public void withdraw(List<EcoType> values, Player player, double amount) {
      if (this.all) {
         values.forEach(entry -> OnyxShop.getInstance().getEcoHandler().getEcon(entry).withdrawBalance(player, amount));
      } else {
         OnyxShop.getInstance().getEcoHandler().getEcon((EcoType)values.get(0)).withdrawBalance(player, amount);
      }
   }

   public List<EcoType> getTypes() {
      return new ArrayList<>(this.buyPrices.keySet());
   }

   public boolean isAll() {
      return this.all;
   }

   public String getSection() {
      return this.section;
   }

   private void initBuyPrices(ShopItem shopItem) throws ItemLoadException {
      List stringList = ConfigManager.getShop(this.section).getStringList(this.getItemLoc() + ".buy-prices.prices");

      for (int size = 0; size < stringList.size(); size++) {
         String text = (String)stringList.get(size);

         try {
            String sectionId = shopItem.section + "." + (shopItem.rotates() ? "" : "pages.") + shopItem.itemLoc;
            EcoType ecoType = OnyxShop.getInstance().getEcoHandler().getEcoType(sectionId + ".buy-prices.prices", text.split("::")[0], size);
            if (ecoType != null) {
               double amount = Double.parseDouble(text.split("::")[1]);
               if (!(amount < 0.0)) {
                  this.buyPrices.put(ecoType, amount / this.stackSize);
                  if (ChatUtil.hasRGB(OnyxShop.getInstance().getEcoHandler().getEcon(ecoType).getPlural())
                     || ChatUtil.hasRGB(OnyxShop.getInstance().getEcoHandler().getEcon(ecoType).getSingular())) {
                     shopItem.rgb = true;
                  }
               }
            }
         } catch (ArrayIndexOutOfBoundsException | NumberFormatException exception) {
            if (stringList.size() == 1 && size == stringList.size()) {
               throw new ItemLoadException(Lang.BUY_VALUE_WRONG.get());
            }

            SendMessage.warnMessage(Lang.INVALID_BUY_PRICE.get().replace("%price%", text));
            SendMessage.errorShops(this.section, this.itemLoc);
         }
      }
   }

   public boolean isBuyAble() {
      return !this.buyPrices.isEmpty();
   }

   public double getFirstPrice() {
      return this.buyPrices.get(this.getTypes().get(0));
   }

   public double getFirstPrice(Player player, int price, Modifiers modifiers, boolean flag) {
      return this.getBuyPrice(this.getTypes().get(0), player, price, modifiers, flag);
   }

   public Map<EcoType, Double> getBuyPrices() {
      return this.buyPrices
         .entrySet()
         .stream()
         .collect(Collectors.toMap(Entry::getKey, entry -> (Double)entry.getValue() * this.stackSize, (element, value) -> element, LinkedHashMap::new));
   }

   public Map<EcoType, Double> getBuyPrices(EcoType ecoType, int value) {
      return this.getBuyPrices(ecoType, value, false);
   }

   public Map<EcoType, Double> getBuyPrices(EcoType ecoType, int value, boolean flag) {
      return this.buyPrices
         .entrySet()
         .stream()
         .filter(entry -> ecoType == null || ecoType.equals(entry.getKey()))
         .collect(Collectors.toMap(Entry::getKey, entry -> this.getBuyPrice((EcoType)entry.getKey(), value, flag), (element, value2) -> element, LinkedHashMap::new));
   }

   private double getBuyPrice(EcoType ecoType, int price, boolean flag) {
      double amount = this.buyPrices.get(ecoType);
      return this.dynamicPricing && amount > 0.0
         ? OnyxShop.getInstance().round(ecoType, OnyxShop.getInstance().getDynamicPricingProvider().getBuyPrice(this.itemPath, amount, price, flag))
         : amount * price;
   }

   public Map<EcoType, Double> getBuyPrices(EcoType ecoType, Player player, int value, Modifiers modifiers) {
      return this.getBuyPrices(ecoType, player, value, modifiers, false);
   }

   public Map<EcoType, Double> getBuyPrices(EcoType ecoType, Player player, int value, Modifiers modifiers, boolean flag) {
      return this.buyPrices
         .entrySet()
         .stream()
         .filter(entry -> ecoType == null || ecoType.equals(entry.getKey()))
         .collect(
            Collectors.toMap(
               Entry::getKey, entry -> this.getBuyPrice((EcoType)entry.getKey(), player, value, modifiers, flag), (element, value2) -> element, LinkedHashMap::new
            )
         );
   }

   private double getBuyPrice(EcoType ecoType, Player player, int price, Modifiers modifiers, boolean flag) {
      double amount = this.buyPrices.get(ecoType);
      if (this.dynamicPricing && amount > 0.0) {
         double buyPrice = OnyxShop.getInstance().getDynamicPricingProvider().getBuyPrice(this.itemPath, amount, price, flag);
         return this.getPlayerBuyPrice(player, ecoType, buyPrice, modifiers);
      } else {
         return this.getPlayerBuyPrice(player, ecoType, amount * price, modifiers);
      }
   }

   public Map<EcoType, Double> getBuyPricesRaw(int value) {
      return this.buyPrices
         .entrySet()
         .stream()
         .collect(Collectors.toMap(Entry::getKey, entry -> (Double)entry.getValue() * value, (element, value2) -> element, LinkedHashMap::new));
   }

   public String formatBuyPrices(String text) {
      String text2 = ChatColor.RESET.toString();
      if (text != null) {
         text2 = ChatUtil.getLastColor(text.split("%buyPrice%")[0]);
      }

      Map<EcoType, Double> buyPrices = this.getBuyPrices();
      StringBuilder builder = new StringBuilder();
      int size = buyPrices.size();

      for (Entry entry : buyPrices.entrySet()) {
         if (size == 1) {
            if (buyPrices.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue());
            }

            builder.append(text2).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get().getLegacy());
         } else {
            if (size != buyPrices.size()) {
               builder.append(text2).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get().getLegacy());
            }

            size--;
         }

         builder.append(OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue()));
      }

      return builder.toString();
   }

   public String formatBuyPrices(String type, EcoType ecoType, int value) {
      return this.formatBuyPrices(type, ecoType, value, false);
   }

   public String formatBuyPrices(String type, EcoType ecoType, int value, boolean flag) {
      String text = ChatColor.RESET.toString();
      if (type != null) {
         text = ChatUtil.getLastColor(type);
      }

      Map<EcoType, Double> buyPrices = this.getBuyPrices(ecoType, value, flag);
      StringBuilder builder = new StringBuilder();
      int size = buyPrices.size();

      for (Entry entry : buyPrices.entrySet()) {
         if (size == 1) {
            if (buyPrices.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue());
            }

            builder.append(text).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get().getLegacy());
         } else {
            if (size != buyPrices.size()) {
               builder.append(text).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get().getLegacy());
            }

            size--;
         }

         builder.append(OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue()));
      }

      return builder.toString();
   }

   public String formatBuyPrices(String type, EcoType ecoType, Player player, int value, Modifiers modifiers) {
      return this.formatBuyPrices(type, ecoType, player, value, modifiers, false);
   }

   public String formatBuyPrices(String type, EcoType ecoType, Player player, int value, Modifiers modifiers, boolean flag) {
      String text = ChatColor.RESET.toString();
      if (type != null) {
         text = ChatUtil.getLastColor(type);
      }

      Map<EcoType, Double> buyPrices = this.getBuyPrices(ecoType, player, value, modifiers, flag);
      StringBuilder builder = new StringBuilder();
      int size = buyPrices.size();

      for (Entry entry : buyPrices.entrySet()) {
         if (size == 1) {
            if (buyPrices.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue());
            }

            builder.append(text).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get(player).getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get(player).getLegacy());
         } else {
            if (size != buyPrices.size()) {
               builder.append(text).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get(player).getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get(player).getLegacy());
            }

            size--;
         }

         builder.append(OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue()));
      }

      return builder.toString();
   }

   public String formatBuyPricesRaw(int value) {
      Map<EcoType, Double> buyPricesRaw = this.getBuyPricesRaw(value);
      StringBuilder builder = new StringBuilder();
      int size = buyPricesRaw.size();

      for (Entry entry : buyPricesRaw.entrySet()) {
         if (size == 1) {
            if (buyPricesRaw.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue());
            }

            builder.append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get().getLegacy());
         } else {
            if (size != buyPricesRaw.size()) {
               builder.append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get().getLegacy());
            }

            size--;
         }

         builder.append(OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue()));
      }

      return builder.toString();
   }

   private double getPlayerBuyPrice(Player player, EcoType ecoType, double price, Modifiers modifiers) {
      if (modifiers != null) {
         for (PriceModifier priceModifier : modifiers) {
            price = priceModifier.modify(player, price);
         }
      }

      if (OnyxShop.getInstance().discountsActive && OnyxShop.getInstance().hasDiscount(this.section)) {
         price = CalculateAmount.calculateDiscount(player, this.section, price);
      }

      return OnyxShop.getInstance().round(ecoType, price);
   }

   private String getItemLoc() {
      String[] itemIds = this.itemPath.split("\\.", 2);
      return (OnyxShop.getInstance().getShopType(itemIds[0]) == ShopType.CATEGORY ? "pages." : "") + itemIds[1];
   }
}
