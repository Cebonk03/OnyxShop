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

   public AdvancedSellPrice(ShopItem var1) {
      this.itemLoc = var1.itemLoc;
      this.section = var1.section;
      this.itemPath = var1.getItemPath();
      this.stackSize = var1.getStackSize();
      this.dynamicPricing = var1.isDynamicPricing();
      this.all = ConfigManager.getShop(this.section).getBoolean(this.getItemLoc() + ".sell-prices.give-all");
      this.allowDamaged = var1.allowDamaged();
      this.damagedPriceDifference = var1.getDamagedPriceDifference();
      this.enchantedVariants = var1.isEnchantedVariants();
   }

   public void init(ShopItem var1) throws ItemLoadException {
      this.stackSize = var1.getStackSize();
      this.dynamicPricing = var1.isDynamicPricing();
      this.baseItem = var1.getItemToGive();
      this.initSellPrices(var1);
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

   private void initSellPrices(ShopItem var1) throws ItemLoadException {
      List var2 = ConfigManager.getShop(this.section).getStringList(this.getItemLoc() + ".sell-prices.prices");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         String var4 = (String)var2.get(var3);

         try {
            String var5 = var1.section + "." + (var1.rotates() ? "" : "pages.") + var1.itemLoc;
            EcoType var6 = OnyxShop.getInstance().getEcoHandler().getEcoType(var5 + ".sell-prices.prices", var4.split("::")[0], var3);
            if (var6 != null) {
               double var7 = Double.parseDouble(var4.split("::")[1]);
               if (!(var7 < 0.0)) {
                  this.sellPrices.put(var6, var7 / this.stackSize);
                  if (ChatUtil.hasRGB(OnyxShop.getInstance().getEcoHandler().getEcon(var6).getPlural())
                     || ChatUtil.hasRGB(OnyxShop.getInstance().getEcoHandler().getEcon(var6).getSingular())) {
                     var1.rgb = true;
                  }
               }
            }
         } catch (ArrayIndexOutOfBoundsException | NumberFormatException var9) {
            if (var2.size() == 1 && var3 == var2.size()) {
               throw new ItemLoadException(Lang.SELL_VALUE_WRONG.get());
            }

            SendMessage.warnMessage(Lang.INVALID_SELL_PRICE.get().replace("%price%", var4));
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

   public double getFirstPrice(Player var1, int var2, Modifiers var3, boolean var4) {
      return this.getSellPrice(this.getTypes().get(0), var1, var2, var3, var4);
   }

   public Map<EcoType, Double> getSellPrices() {
      return this.sellPrices
         .entrySet()
         .stream()
         .collect(Collectors.toMap(Entry::getKey, var1 -> (Double)var1.getValue() * this.stackSize, (var0, var1) -> var0, LinkedHashMap::new));
   }

   public Map<EcoType, Double> getSellPrices(EcoType var1, int var2) {
      return this.getSellPrices(var1, var2, false);
   }

   public Map<EcoType, Double> getSellPrices(EcoType var1, int var2, boolean var3) {
      return this.sellPrices
         .entrySet()
         .stream()
         .filter(var1x -> var1 == null || var1.equals(var1x.getKey()))
         .collect(Collectors.toMap(Entry::getKey, var3x -> this.getSellPrice((EcoType)var3x.getKey(), var2, var3), (var0, var1x) -> var0, LinkedHashMap::new));
   }

   private double getSellPrice(EcoType var1, int var2, boolean var3) {
      double var4 = this.sellPrices.get(var1);
      return this.dynamicPricing && var4 > 0.0
         ? OnyxShop.getInstance().round(var1, OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, var4, var2, 0, var3))
         : var4 * var2;
   }

   public Map<EcoType, Double> getSellPrices(EcoType var1, ItemStack var2) {
      return this.getSellPrices(var1, var2, false);
   }

   public Map<EcoType, Double> getSellPrices(EcoType var1, ItemStack var2, boolean var3) {
      return this.sellPrices
         .entrySet()
         .stream()
         .filter(var1x -> var1 == null || var1.equals(var1x.getKey()))
         .collect(Collectors.toMap(Entry::getKey, var3x -> this.getSellPrice((EcoType)var3x.getKey(), var2, var3), (var0, var1x) -> var0, LinkedHashMap::new));
   }

   private double getSellPrice(EcoType var1, ItemStack var2, boolean var3) {
      double var4 = this.sellPrices.get(var1);
      var4 = var2.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(var2, var4)
         : var4;
      if (this.dynamicPricing && var4 > 0.0) {
         double var6 = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, var4, var2.getAmount(), 0, var3);
         return OnyxShop.getInstance().round(var1, this.getItemSellPrice(var6, var2));
      } else {
         return OnyxShop.getInstance().round(var1, this.getItemSellPrice(var4 * var2.getAmount(), var2));
      }
   }

   public Map<EcoType, Double> getSellPrices(EcoType var1, ItemStack var2, int var3, int var4) {
      return this.getSellPrices(var1, var2, var3, var4, false);
   }

   public Map<EcoType, Double> getSellPrices(EcoType var1, ItemStack var2, int var3, int var4, boolean var5) {
      return this.sellPrices
         .entrySet()
         .stream()
         .filter(var1x -> var1 == null || var1.equals(var1x.getKey()))
         .collect(
            Collectors.toMap(
               Entry::getKey, var5x -> this.getSellPrice((EcoType)var5x.getKey(), var2, var3, var4, var5), (var0, var1x) -> var0, LinkedHashMap::new
            )
         );
   }

   private double getSellPrice(EcoType var1, ItemStack var2, int var3, int var4, boolean var5) {
      double var6 = this.sellPrices.get(var1);
      var6 = var2.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(var2, var6)
         : var6;
      if (this.dynamicPricing && var6 > 0.0) {
         double var8 = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, var6, var3, var4, var5);
         return OnyxShop.getInstance().round(var1, this.getItemSellPrice(var8, var2));
      } else {
         return OnyxShop.getInstance().round(var1, this.getItemSellPrice(var6 * var3, var2));
      }
   }

   public Map<EcoType, Double> getSellPrices(EcoType var1, Player var2, int var3, Modifiers var4) {
      return this.getSellPrices(var1, var2, var3, var4, false);
   }

   public Map<EcoType, Double> getSellPrices(EcoType var1, Player var2, int var3, Modifiers var4, boolean var5) {
      return this.sellPrices
         .entrySet()
         .stream()
         .filter(var1x -> var1 == null || var1.equals(var1x.getKey()))
         .collect(
            Collectors.toMap(
               Entry::getKey, var5x -> this.getSellPrice((EcoType)var5x.getKey(), var2, var3, var4, var5), (var0, var1x) -> var0, LinkedHashMap::new
            )
         );
   }

   private double getSellPrice(EcoType var1, Player var2, int var3, Modifiers var4, boolean var5) {
      double var6 = this.sellPrices.get(var1);
      if (this.dynamicPricing && var6 > 0.0) {
         double var8 = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, var6, var3, 0, var5);
         return this.getPlayerSellPrice(var2, var1, var8, var4);
      } else {
         return this.getPlayerSellPrice(var2, var1, var6 * var3, var4);
      }
   }

   public Map<EcoType, Double> getSellPrices(EcoType var1, Player var2, ItemStack var3, Modifiers var4) {
      return this.getSellPrices(var1, var2, var3, var4, false);
   }

   public Map<EcoType, Double> getSellPrices(EcoType var1, Player var2, ItemStack var3, Modifiers var4, boolean var5) {
      return this.sellPrices
         .entrySet()
         .stream()
         .filter(var1x -> var1 == null || var1.equals(var1x.getKey()))
         .collect(
            Collectors.toMap(
               Entry::getKey, var5x -> this.getSellPrice((EcoType)var5x.getKey(), var2, var3, var4, var5), (var0, var1x) -> var0, LinkedHashMap::new
            )
         );
   }

   private double getSellPrice(EcoType var1, Player var2, ItemStack var3, Modifiers var4, boolean var5) {
      double var6 = this.sellPrices.get(var1);
      var6 = var3.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(var3, var6)
         : var6;
      if (this.dynamicPricing && var6 > 0.0) {
         double var8 = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, var6, var3.getAmount(), 0, var5);
         return this.getPlayerSellPrice(var2, var1, this.getItemSellPrice(var8, var3), var4);
      } else {
         return this.getPlayerSellPrice(var2, var1, this.getItemSellPrice(var6 * var3.getAmount(), var3), var4);
      }
   }

   public Map<EcoType, Double> getSellPrices(EcoType var1, Player var2, ItemStack var3, int var4, int var5, Modifiers var6) {
      return this.getSellPrices(var1, var2, var3, var4, var5, var6, false);
   }

   public Map<EcoType, Double> getSellPrices(EcoType var1, Player var2, ItemStack var3, int var4, int var5, Modifiers var6, boolean var7) {
      return this.sellPrices
         .entrySet()
         .stream()
         .filter(var1x -> var1 == null || var1.equals(var1x.getKey()))
         .collect(
            Collectors.toMap(
               Entry::getKey,
               var7x -> this.getSellPrice((EcoType)var7x.getKey(), var2, var3, var4, var5, var6, var7),
               (var0, var1x) -> var0,
               LinkedHashMap::new
            )
         );
   }

   private double getSellPrice(EcoType var1, Player var2, ItemStack var3, int var4, int var5, Modifiers var6, boolean var7) {
      double var8 = this.sellPrices.get(var1);
      var8 = var3.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(var3, var8)
         : var8;
      if (this.dynamicPricing && var8 > 0.0) {
         double var10 = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, var8, var4, var5, var7);
         return this.getPlayerSellPrice(var2, var1, this.getItemSellPrice(var10, var3), var6);
      } else {
         return this.getPlayerSellPrice(var2, var1, this.getItemSellPrice(var8 * var4, var3), var6);
      }
   }

   public Map<EcoType, Double> getSellPricesRaw(int var1) {
      return this.sellPrices
         .entrySet()
         .stream()
         .collect(Collectors.toMap(Entry::getKey, var1x -> (Double)var1x.getValue() * var1, (var0, var1x) -> var0, LinkedHashMap::new));
   }

   public String formatSellPrices(String var1) {
      String var2 = ChatColor.RESET.toString();
      if (var1 != null) {
         var2 = ChatUtil.getLastColor(var1.split("%sellPrice%")[0]);
      }

      Map<EcoType, Double> var3 = this.getSellPrices();
      StringBuilder var4 = new StringBuilder();
      int var5 = var3.size();

      for (Entry var7 : var3.entrySet()) {
         if (var5 == 1) {
            if (var3.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)var7.getKey(), (Double)var7.getValue());
            }

            var4.append(var2).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get().getLegacy());
         } else {
            if (var5 != var3.size()) {
               var4.append(var2).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get().getLegacy());
            }

            var5--;
         }

         var4.append(OnyxShop.getInstance().formatPrice((EcoType)var7.getKey(), (Double)var7.getValue()));
      }

      return var4.toString();
   }

   public String formatSellPrices(String var1, EcoType var2, int var3) {
      return this.formatSellPrices(var1, var2, var3, false);
   }

   public String formatSellPrices(String var1, EcoType var2, int var3, boolean var4) {
      String var5 = ChatColor.RESET.toString();
      if (var1 != null) {
         var5 = ChatUtil.getLastColor(var1);
      }

      Map<EcoType, Double> var6 = this.getSellPrices(var2, var3, var4);
      StringBuilder var7 = new StringBuilder();
      int var8 = var6.size();

      for (Entry var10 : var6.entrySet()) {
         if (var8 == 1) {
            if (var6.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)var10.getKey(), (Double)var10.getValue());
            }

            var7.append(var5).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get().getLegacy());
         } else {
            if (var8 != var6.size()) {
               var7.append(var5).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get().getLegacy());
            }

            var8--;
         }

         var7.append(OnyxShop.getInstance().formatPrice((EcoType)var10.getKey(), (Double)var10.getValue()));
      }

      return var7.toString();
   }

   public String formatSellPrices(String var1, EcoType var2, ItemStack var3) {
      return this.formatSellPrices(var1, var2, var3, false);
   }

   public String formatSellPrices(String var1, EcoType var2, ItemStack var3, boolean var4) {
      String var5 = ChatColor.RESET.toString();
      if (var1 != null) {
         var5 = ChatUtil.getLastColor(var1);
      }

      Map<EcoType, Double> var6 = this.getSellPrices(var2, var3, var4);
      StringBuilder var7 = new StringBuilder();
      int var8 = var6.size();

      for (Entry var10 : var6.entrySet()) {
         if (var8 == 1) {
            if (var6.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)var10.getKey(), (Double)var10.getValue());
            }

            var7.append(var5).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get().getLegacy());
         } else {
            if (var8 != var6.size()) {
               var7.append(var5).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get().getLegacy());
            }

            var8--;
         }

         var7.append(OnyxShop.getInstance().formatPrice((EcoType)var10.getKey(), (Double)var10.getValue()));
      }

      return var7.toString();
   }

   public String formatSellPrices(String var1, EcoType var2, Player var3, int var4, Modifiers var5) {
      return this.formatSellPrices(var1, var2, var3, var4, var5, false);
   }

   public String formatSellPrices(String var1, EcoType var2, Player var3, int var4, Modifiers var5, boolean var6) {
      String var7 = ChatColor.RESET.toString();
      if (var1 != null) {
         var7 = ChatUtil.getLastColor(var1);
      }

      Map<EcoType, Double> var8 = this.getSellPrices(var2, var3, var4, var5, var6);
      StringBuilder var9 = new StringBuilder();
      int var10 = var8.size();

      for (Entry var12 : var8.entrySet()) {
         if (var10 == 1) {
            if (var8.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)var12.getKey(), (Double)var12.getValue());
            }

            var9.append(var7).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get(var3).getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get(var3).getLegacy());
         } else {
            if (var10 != var8.size()) {
               var9.append(var7).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get(var3).getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get(var3).getLegacy());
            }

            var10--;
         }

         var9.append(OnyxShop.getInstance().formatPrice((EcoType)var12.getKey(), (Double)var12.getValue()));
      }

      return var9.toString();
   }

   public String formatSellPrices(String var1, EcoType var2, Player var3, ItemStack var4, Modifiers var5) {
      return this.formatSellPrices(var1, var2, var3, var4, var5, false);
   }

   public String formatSellPrices(String var1, EcoType var2, Player var3, ItemStack var4, Modifiers var5, boolean var6) {
      String var7 = ChatColor.RESET.toString();
      if (var1 != null) {
         var7 = ChatUtil.getLastColor(var1);
      }

      Map<EcoType, Double> var8 = this.getSellPrices(var2, var3, var4, var5, var6);
      StringBuilder var9 = new StringBuilder();
      int var10 = var8.size();

      for (Entry var12 : var8.entrySet()) {
         if (var10 == 1) {
            if (var8.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)var12.getKey(), (Double)var12.getValue());
            }

            var9.append(var7).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get(var3).getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get(var3).getLegacy());
         } else {
            if (var10 != var8.size()) {
               var9.append(var7).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get(var3).getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get(var3).getLegacy());
            }

            var10--;
         }

         var9.append(OnyxShop.getInstance().formatPrice((EcoType)var12.getKey(), (Double)var12.getValue()));
      }

      return var9.toString();
   }

   public String formatSellPrices(String var1, EcoType var2, Player var3, ItemStack var4, int var5, int var6, Modifiers var7) {
      return this.formatSellPrices(var1, var2, var3, var4, var5, var6, var7, false);
   }

   public String formatSellPrices(String var1, EcoType var2, Player var3, ItemStack var4, int var5, int var6, Modifiers var7, boolean var8) {
      String var9 = ChatColor.RESET.toString();
      if (var1 != null) {
         var9 = ChatUtil.getLastColor(var1);
      }

      Map<EcoType, Double> var10 = this.getSellPrices(var2, var3, var4, var5, var6, var7, var8);
      StringBuilder var11 = new StringBuilder();
      int var12 = var10.size();

      for (Entry var14 : var10.entrySet()) {
         if (var12 == 1) {
            if (var10.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)var14.getKey(), (Double)var14.getValue());
            }

            var11.append(var9).append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get(var3).getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get(var3).getLegacy());
         } else {
            if (var12 != var10.size()) {
               var11.append(var9).append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get(var3).getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get(var3).getLegacy());
            }

            var12--;
         }

         var11.append(OnyxShop.getInstance().formatPrice((EcoType)var14.getKey(), (Double)var14.getValue()));
      }

      return var11.toString();
   }

   public String formatSellPricesRaw(int var1) {
      Map<EcoType, Double> var2 = this.getSellPrices();
      StringBuilder var3 = new StringBuilder();
      int var4 = var2.size();

      for (Entry var6 : var2.entrySet()) {
         if (var4 == 1) {
            if (var2.size() == 1) {
               return OnyxShop.getInstance().formatPrice((EcoType)var6.getKey(), (Double)var6.getValue());
            }

            var3.append(this.all ? Lang.AND_PRICE_SEPARATOR_LONG.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_LONG.get().getLegacy());
         } else {
            if (var4 != var2.size()) {
               var3.append(this.all ? Lang.AND_PRICE_SEPARATOR_SHORT.get().getLegacy() : Lang.OR_PRICE_SEPARATOR_SHORT.get().getLegacy());
            }

            var4--;
         }

         var3.append(OnyxShop.getInstance().formatPrice((EcoType)var6.getKey(), (Double)var6.getValue()));
      }

      return var3.toString();
   }

   private double getPlayerSellPrice(Player var1, EcoType var2, double var3, Modifiers var5) {
      if (var5 != null) {
         for (PriceModifier var7 : var5) {
            var3 = var7.modify(var1, var3);
         }
      }

      if (OnyxShop.getInstance().multipliers && OnyxShop.getInstance().hasMultiplier(this.section)) {
         var3 = CalculateAmount.calculateMultiplier(var1, this.section, var3);
      }

      return OnyxShop.getInstance().getEcoHandler().getEcon(var2).round(var3);
   }

   private double getItemSellPrice(double var1, ItemStack var3) {
      if (this.allowDamaged && var3.getType().getMaxDurability() > 0) {
         Integer var4 = OnyxShop.getInstance().versionHandler.getNBTInt(var3, NBTMaps.DAMAGE.getTag());
         if (var4 != null && var4 > 0) {
            short var5 = var3.getType().getMaxDurability();
            double var6 = (double)(var5 - var4) / var5;
            var1 = var1 * var6 * this.damagedPriceDifference;
         }
      }

      if (this.enchantedVariants) {
         int var8 = Math.max(1, var3.getAmount());
         var1 += OnyxShop.getInstance().getEnchantCostManager().getEnchantAddition(var3, this.baseItem, var1 / var8) * var8;
      }

      return var1;
   }

   private String getItemLoc() {
      String[] var1 = this.itemPath.split("\\.", 2);
      return (OnyxShop.getInstance().getShopType(var1[0]) == ShopType.CATEGORY ? "pages." : "") + var1[1];
   }
}
