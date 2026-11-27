package com.onyxshop.objects.prices;

import java.util.ArrayList;
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
import com.onyxshop.util.NBTMaps;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AdvancedSellPrice {
   private final Map<EcoType, Double> sellPrices = new LinkedHashMap<>();
   private final String section;
   private final String itemLoc;
   private final String itemPath;
   private final boolean all;
   private boolean dynamicPricing;
   private int stackSize;
   private final boolean allowDamaged;
   private final double damagedPriceDifference;
   private final boolean enchantedVariants;
   private ItemStack baseItem;

   public AdvancedSellPrice(ShopItem shopItem) {
      this.itemLoc = shopItem.itemLoc;
      this.section = shopItem.section;
      this.itemPath = shopItem.getItemPath();
      this.stackSize = shopItem.getStackSize();
      this.dynamicPricing = shopItem.isDynamicPricing();
      this.all = ConfigManager.getShop(this.section).getBoolean(this.getItemLoc() + ".sell-prices.give-all");
      this.allowDamaged = shopItem.allowDamaged();
      this.damagedPriceDifference = shopItem.getDamagedPriceDifference();
      this.enchantedVariants = shopItem.isEnchantedVariants();
   }

   public void init(ShopItem shopItem) throws ItemLoadException {
      this.stackSize = shopItem.getStackSize();
      this.dynamicPricing = shopItem.isDynamicPricing();
      this.baseItem = shopItem.getItemToGive();
      this.initSellPrices(shopItem);
   }

   public List<EcoType> getTypes() {
      return new ArrayList<>(this.sellPrices.keySet());
   }

   public boolean isAll() {
      return this.all;
   }

   public String getItemPath() {
      return this.itemPath;
   }

   private void initSellPrices(ShopItem shopItem) throws ItemLoadException {
      List stringList = ConfigManager.getShop(this.section).getStringList(this.getItemLoc() + ".sell-prices.prices");

      for (int size = 0; size < stringList.size(); size++) {
         String text = (String)stringList.get(size);

         try {
            String sectionId = shopItem.section + "." + (shopItem.rotates() ? "" : "pages.") + shopItem.itemLoc;
            EcoType ecoType = OnyxShop.getInstance().getEcoHandler().getEcoType(sectionId + ".sell-prices.prices", text.split("::")[0], size);
            if (ecoType != null) {
               double amount = Double.parseDouble(text.split("::")[1]);
               if (!(amount < 0.0)) {
                  this.sellPrices.put(ecoType, amount / this.stackSize);
                  if (ChatUtil.hasRGB(OnyxShop.getInstance().getEcoHandler().getEcon(ecoType).getPlural())
                     || ChatUtil.hasRGB(OnyxShop.getInstance().getEcoHandler().getEcon(ecoType).getSingular())) {
                     shopItem.rgb = true;
                  }
               }
            }
         } catch (ArrayIndexOutOfBoundsException | NumberFormatException exception) {
            if (stringList.size() == 1 && size == stringList.size()) {
               throw new ItemLoadException(Lang.SELL_VALUE_WRONG.get());
            }

            SendMessage.warnMessage(Lang.INVALID_SELL_PRICE.get().replace("%price%", text));
            SendMessage.errorShops(this.section, this.itemLoc);
         }
      }
   }

   public boolean isSellAble() {
      return !this.sellPrices.isEmpty();
   }

   public double getFirstPrice() {
      return this.sellPrices.get(this.getTypes().get(0));
   }

   public double getFirstPrice(Player player, int price, Modifiers modifiers, boolean flag) {
      return this.getSellPrice(this.getTypes().get(0), player, price, modifiers, flag);
   }

   public Map<EcoType, Double> getSellPrices() {
      return this.sellPrices
         .entrySet()
         .stream()
         .collect(Collectors.toMap(Entry::getKey, entry -> (Double)entry.getValue() * this.stackSize, (element, value) -> element, LinkedHashMap::new));
   }

   public Map<EcoType, Double> getSellPrices(EcoType ecoType, int value) {
      return this.getSellPrices(ecoType, value, false);
   }

   public Map<EcoType, Double> getSellPrices(EcoType ecoType, int value, boolean flag) {
      return this.sellPrices
         .entrySet()
         .stream()
         .filter(entry -> ecoType == null || ecoType.equals(entry.getKey()))
         .collect(Collectors.toMap(Entry::getKey, entry -> this.getSellPrice((EcoType)entry.getKey(), value, flag), (element, value2) -> element, LinkedHashMap::new));
   }

   private double getSellPrice(EcoType ecoType, int price, boolean flag) {
      double amount = this.sellPrices.get(ecoType);
      return this.dynamicPricing && amount > 0.0
         ? OnyxShop.getInstance().round(ecoType, OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, amount, price, 0, flag))
         : amount * price;
   }

   public Map<EcoType, Double> getSellPrices(EcoType ecoType, ItemStack itemStack) {
      return this.getSellPrices(ecoType, itemStack, false);
   }

   public Map<EcoType, Double> getSellPrices(EcoType ecoType, ItemStack itemStack, boolean flag) {
      return this.sellPrices
         .entrySet()
         .stream()
         .filter(entry -> ecoType == null || ecoType.equals(entry.getKey()))
         .collect(Collectors.toMap(Entry::getKey, entry -> this.getSellPrice((EcoType)entry.getKey(), itemStack, flag), (element, value) -> element, LinkedHashMap::new));
   }

   private double getSellPrice(EcoType ecoType, ItemStack itemStack, boolean flag) {
      double price = this.sellPrices.get(ecoType);
      price = itemStack.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(itemStack, price)
         : price;
      if (this.dynamicPricing && price > 0.0) {
         double sellPrice = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, price, itemStack.getAmount(), 0, flag);
         return OnyxShop.getInstance().round(ecoType, this.getItemSellPrice(sellPrice, itemStack));
      } else {
         return OnyxShop.getInstance().round(ecoType, this.getItemSellPrice(price * itemStack.getAmount(), itemStack));
      }
   }

   public Map<EcoType, Double> getSellPrices(EcoType ecoType, ItemStack itemStack, int value, int value2) {
      return this.getSellPrices(ecoType, itemStack, value, value2, false);
   }

   public Map<EcoType, Double> getSellPrices(EcoType ecoType, ItemStack itemStack, int value, int value2, boolean flag) {
      return this.sellPrices
         .entrySet()
         .stream()
         .filter(entry -> ecoType == null || ecoType.equals(entry.getKey()))
         .collect(
            Collectors.toMap(
               Entry::getKey, entry -> this.getSellPrice((EcoType)entry.getKey(), itemStack, value, value2, flag), (element, value3) -> element, LinkedHashMap::new
            )
         );
   }

   private double getSellPrice(EcoType ecoType, ItemStack itemStack, int price, int value, boolean flag) {
      double amount = this.sellPrices.get(ecoType);
      amount = itemStack.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(itemStack, amount)
         : amount;
      if (this.dynamicPricing && amount > 0.0) {
         double sellPrice = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, amount, price, value, flag);
         return OnyxShop.getInstance().round(ecoType, this.getItemSellPrice(sellPrice, itemStack));
      } else {
         return OnyxShop.getInstance().round(ecoType, this.getItemSellPrice(amount * price, itemStack));
      }
   }

   public Map<EcoType, Double> getSellPrices(EcoType ecoType, Player player, int value, Modifiers modifiers) {
      return this.getSellPrices(ecoType, player, value, modifiers, false);
   }

   public Map<EcoType, Double> getSellPrices(EcoType ecoType, Player player, int value, Modifiers modifiers, boolean flag) {
      return this.sellPrices
         .entrySet()
         .stream()
         .filter(entry -> ecoType == null || ecoType.equals(entry.getKey()))
         .collect(
            Collectors.toMap(
               Entry::getKey, entry -> this.getSellPrice((EcoType)entry.getKey(), player, value, modifiers, flag), (element, value2) -> element, LinkedHashMap::new
            )
         );
   }

   private double getSellPrice(EcoType ecoType, Player player, int price, Modifiers modifiers, boolean flag) {
      double amount = this.sellPrices.get(ecoType);
      if (this.dynamicPricing && amount > 0.0) {
         double sellPrice = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, amount, price, 0, flag);
         return this.getPlayerSellPrice(player, ecoType, sellPrice, modifiers);
      } else {
         return this.getPlayerSellPrice(player, ecoType, amount * price, modifiers);
      }
   }

   public Map<EcoType, Double> getSellPrices(EcoType ecoType, Player player, ItemStack itemStack, Modifiers modifiers) {
      return this.getSellPrices(ecoType, player, itemStack, modifiers, false);
   }

   public Map<EcoType, Double> getSellPrices(EcoType ecoType, Player player, ItemStack itemStack, Modifiers modifiers, boolean flag) {
      return this.sellPrices
         .entrySet()
         .stream()
         .filter(entry -> ecoType == null || ecoType.equals(entry.getKey()))
         .collect(
            Collectors.toMap(
               Entry::getKey, entry -> this.getSellPrice((EcoType)entry.getKey(), player, itemStack, modifiers, flag), (element, value) -> element, LinkedHashMap::new
            )
         );
   }

   private double getSellPrice(EcoType ecoType, Player player, ItemStack itemStack, Modifiers modifiers, boolean flag) {
      double price = this.sellPrices.get(ecoType);
      price = itemStack.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(itemStack, price)
         : price;
      if (this.dynamicPricing && price > 0.0) {
         double sellPrice = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, price, itemStack.getAmount(), 0, flag);
         return this.getPlayerSellPrice(player, ecoType, this.getItemSellPrice(sellPrice, itemStack), modifiers);
      } else {
         return this.getPlayerSellPrice(player, ecoType, this.getItemSellPrice(price * itemStack.getAmount(), itemStack), modifiers);
      }
   }

   public Map<EcoType, Double> getSellPrices(EcoType ecoType, Player player, ItemStack itemStack, int value, int value2, Modifiers modifiers) {
      return this.getSellPrices(ecoType, player, itemStack, value, value2, modifiers, false);
   }

   public Map<EcoType, Double> getSellPrices(EcoType ecoType, Player player, ItemStack itemStack, int value, int value2, Modifiers modifiers, boolean flag) {
      return this.sellPrices
         .entrySet()
         .stream()
         .filter(entry -> ecoType == null || ecoType.equals(entry.getKey()))
         .collect(
            Collectors.toMap(
               Entry::getKey,
               entry -> this.getSellPrice((EcoType)entry.getKey(), player, itemStack, value, value2, modifiers, flag),
               (element, value3) -> element,
               LinkedHashMap::new
            )
         );
   }

   private double getSellPrice(EcoType ecoType, Player player, ItemStack itemStack, int price, int value, Modifiers modifiers, boolean flag) {
      double amount = this.sellPrices.get(ecoType);
      amount = itemStack.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(itemStack, amount)
         : amount;
      if (this.dynamicPricing && amount > 0.0) {
         double sellPrice = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, amount, price, value, flag);
         return this.getPlayerSellPrice(player, ecoType, this.getItemSellPrice(sellPrice, itemStack), modifiers);
      } else {
         return this.getPlayerSellPrice(player, ecoType, this.getItemSellPrice(amount * price, itemStack), modifiers);
      }
   }

   public Map<EcoType, Double> getSellPricesRaw(int value) {
      return this.sellPrices
         .entrySet()
         .stream()
         .collect(Collectors.toMap(Entry::getKey, entry -> (Double)entry.getValue() * value, (element, value2) -> element, LinkedHashMap::new));
   }

   public String formatSellPrices(String text) {
      String text2 = ChatColor.RESET.toString();
      if (text != null) {
         text2 = ChatUtil.getLastColor(text.split("%sellPrice%")[0]);
      }

      Map<EcoType, Double> sellPrices = this.getSellPrices();
      StringBuilder builder = new StringBuilder();
      int size = sellPrices.size();

      for (Entry entry : sellPrices.entrySet()) {
         if (size == 1) {
            if (sellPrices.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue());
            }

            builder.append(text2).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get().getLegacy());
         } else {
            if (size != sellPrices.size()) {
               builder.append(text2).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get().getLegacy());
            }

            size--;
         }

         builder.append(OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue()));
      }

      return builder.toString();
   }

   public String formatSellPrices(String type, EcoType ecoType, int value) {
      return this.formatSellPrices(type, ecoType, value, false);
   }

   public String formatSellPrices(String type, EcoType ecoType, int value, boolean flag) {
      String text = ChatColor.RESET.toString();
      if (type != null) {
         text = ChatUtil.getLastColor(type);
      }

      Map<EcoType, Double> sellPrices = this.getSellPrices(ecoType, value, flag);
      StringBuilder builder = new StringBuilder();
      int size = sellPrices.size();

      for (Entry entry : sellPrices.entrySet()) {
         if (size == 1) {
            if (sellPrices.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue());
            }

            builder.append(text).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get().getLegacy());
         } else {
            if (size != sellPrices.size()) {
               builder.append(text).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get().getLegacy());
            }

            size--;
         }

         builder.append(OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue()));
      }

      return builder.toString();
   }

   public String formatSellPrices(String type, EcoType ecoType, ItemStack itemStack) {
      return this.formatSellPrices(type, ecoType, itemStack, false);
   }

   public String formatSellPrices(String type, EcoType ecoType, ItemStack itemStack, boolean flag) {
      String text = ChatColor.RESET.toString();
      if (type != null) {
         text = ChatUtil.getLastColor(type);
      }

      Map<EcoType, Double> sellPrices = this.getSellPrices(ecoType, itemStack, flag);
      StringBuilder builder = new StringBuilder();
      int size = sellPrices.size();

      for (Entry entry : sellPrices.entrySet()) {
         if (size == 1) {
            if (sellPrices.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue());
            }

            builder.append(text).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get().getLegacy());
         } else {
            if (size != sellPrices.size()) {
               builder.append(text).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get().getLegacy());
            }

            size--;
         }

         builder.append(OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue()));
      }

      return builder.toString();
   }

   public String formatSellPrices(String type, EcoType ecoType, Player player, int value, Modifiers modifiers) {
      return this.formatSellPrices(type, ecoType, player, value, modifiers, false);
   }

   public String formatSellPrices(String type, EcoType ecoType, Player player, int value, Modifiers modifiers, boolean flag) {
      String text = ChatColor.RESET.toString();
      if (type != null) {
         text = ChatUtil.getLastColor(type);
      }

      Map<EcoType, Double> sellPrices = this.getSellPrices(ecoType, player, value, modifiers, flag);
      StringBuilder builder = new StringBuilder();
      int size = sellPrices.size();

      for (Entry entry : sellPrices.entrySet()) {
         if (size == 1) {
            if (sellPrices.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue());
            }

            builder.append(text).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get(player).getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get(player).getLegacy());
         } else {
            if (size != sellPrices.size()) {
               builder.append(text).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get(player).getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get(player).getLegacy());
            }

            size--;
         }

         builder.append(OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue()));
      }

      return builder.toString();
   }

   public String formatSellPrices(String type, EcoType ecoType, Player player, ItemStack itemStack, Modifiers modifiers) {
      return this.formatSellPrices(type, ecoType, player, itemStack, modifiers, false);
   }

   public String formatSellPrices(String type, EcoType ecoType, Player player, ItemStack itemStack, Modifiers modifiers, boolean flag) {
      String text = ChatColor.RESET.toString();
      if (type != null) {
         text = ChatUtil.getLastColor(type);
      }

      Map<EcoType, Double> sellPrices = this.getSellPrices(ecoType, player, itemStack, modifiers, flag);
      StringBuilder builder = new StringBuilder();
      int size = sellPrices.size();

      for (Entry entry : sellPrices.entrySet()) {
         if (size == 1) {
            if (sellPrices.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue());
            }

            builder.append(text).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get(player).getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get(player).getLegacy());
         } else {
            if (size != sellPrices.size()) {
               builder.append(text).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get(player).getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get(player).getLegacy());
            }

            size--;
         }

         builder.append(OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue()));
      }

      return builder.toString();
   }

   public String formatSellPrices(String type, EcoType ecoType, Player player, ItemStack itemStack, int value, int value2, Modifiers modifiers) {
      return this.formatSellPrices(type, ecoType, player, itemStack, value, value2, modifiers, false);
   }

   public String formatSellPrices(String type, EcoType ecoType, Player player, ItemStack itemStack, int value, int value2, Modifiers modifiers, boolean flag) {
      String text = ChatColor.RESET.toString();
      if (type != null) {
         text = ChatUtil.getLastColor(type);
      }

      Map<EcoType, Double> sellPrices = this.getSellPrices(ecoType, player, itemStack, value, value2, modifiers, flag);
      StringBuilder builder = new StringBuilder();
      int size = sellPrices.size();

      for (Entry entry : sellPrices.entrySet()) {
         if (size == 1) {
            if (sellPrices.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue());
            }

            builder.append(text).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get(player).getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get(player).getLegacy());
         } else {
            if (size != sellPrices.size()) {
               builder.append(text).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get(player).getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get(player).getLegacy());
            }

            size--;
         }

         builder.append(OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue()));
      }

      return builder.toString();
   }

   public String formatSellPricesRaw(int value) {
      Map<EcoType, Double> sellPrices = this.getSellPrices();
      StringBuilder builder = new StringBuilder();
      int size = sellPrices.size();

      for (Entry entry : sellPrices.entrySet()) {
         if (size == 1) {
            if (sellPrices.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue());
            }

            builder.append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get().getLegacy());
         } else {
            if (size != sellPrices.size()) {
               builder.append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get().getLegacy());
            }

            size--;
         }

         builder.append(OnyxShop.getInstance().formatPrice((EcoType)entry.getKey(), (Double)entry.getValue()));
      }

      return builder.toString();
   }

   private double getPlayerSellPrice(Player player, EcoType ecoType, double price, Modifiers modifiers) {
      if (modifiers != null) {
         for (PriceModifier priceModifier : modifiers) {
            price = priceModifier.modify(player, price);
         }
      }

      if (OnyxShop.getInstance().multipliers && OnyxShop.getInstance().hasMultiplier(this.section)) {
         price = CalculateAmount.calculateMultiplier(player, this.section, price);
      }

      return OnyxShop.getInstance().getEcoHandler().getEcon(ecoType).round(price);
   }

   private double getItemSellPrice(double price, ItemStack itemStack) {
      if (this.allowDamaged && itemStack.getType().getMaxDurability() > 0) {
         Integer nbtInt = OnyxShop.getInstance().versionHandler.getNBTInt(itemStack, NBTMaps.DAMAGE.getTag());
         if (nbtInt != null && nbtInt > 0) {
            short maxDurability = itemStack.getType().getMaxDurability();
            double amount = (double)(maxDurability - nbtInt) / maxDurability;
            price = price * amount * this.damagedPriceDifference;
         }
      }

      if (this.enchantedVariants) {
         int amount = Math.max(1, itemStack.getAmount());
         price += OnyxShop.getInstance().getEnchantCostManager().getEnchantAddition(itemStack, this.baseItem, price / amount) * amount;
      }

      return price;
   }

   private String getItemLoc() {
      String[] itemIds = this.itemPath.split("\\.", 2);
      return (OnyxShop.getInstance().getShopType(itemIds[0]) == ShopType.CATEGORY ? "pages." : "") + itemIds[1];
   }
}
