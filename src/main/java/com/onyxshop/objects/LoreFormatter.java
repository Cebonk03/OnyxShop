package com.onyxshop.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.UserManager;
import com.onyxshop.providers.priceModifiers.seasons.SeasonProvider;
import com.onyxshop.providers.requirements.ItemRequirement;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.Placeholder;
import com.onyxshop.util.ReflectUtil;
import com.onyxshop.util.Replacement;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class LoreFormatter {
   private final OnyxShop plugin;
   private final boolean components;
   private List<LoreFormatter.DisplayLore> displayLore;
   private LoreFormatter.ActionAdminLore actionAdminLore;
   private int item;
   private List<LoreFormatter.StaticLore> itemLore;

   public LoreFormatter(OnyxShop var1) {
      this.plugin = var1;
      this.components = var1.paperMeta;
      if (this.plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
         this.plugin.runTaskLaterAsync(this::init, 5L);
      } else {
         this.init();
      }
   }

   private void init() {
      List<String> var1 = ConfigManager.getConfig().getStringList("lore-arrangement");
      int var2 = var1.indexOf("item-lore");
      if (var2 == -1) {
         SendMessage.warnMessage("Failed to load lore arrangement, cannot find 'item-lore' which is required!");
         var1 = ConfigManager.getConfig().getDef().getStringList("lore-arrangement");
         var2 = var1.indexOf("item-lore");
      }

      String var3 = "display-lore-layout.";
      ArrayList<LoreFormatter.DisplayLore> var4 = new ArrayList<>();

      for (String var6 : var1) {
         int var7 = var1.indexOf(var6);
         ConfigurationSection var8 = null;
         if (!var6.equals("item-lore")) {
            var8 = ConfigManager.getConfig().getConfigurationSection(var3 + var6);
            if (var8 == null) {
               SendMessage.warnMessage("Cannot find lore format for %part% inside config.yml, using default...".replace("%part%", var6));
               var8 = ConfigManager.getConfig().getDef().getConfigurationSection(var3 + var6);
            }
         }

         switch (var6) {
            case "special-lore":
               var4.add(new LoreFormatter.SpecialLore(var8, var7 > var2));
               break;
            case "item-requirements":
               var4.add(new LoreFormatter.RequirementLore());
               break;
            case "buy-prices":
               var4.add(new LoreFormatter.BuyLore(var8, var7 > var2));
               break;
            case "sell-prices":
               var4.add(new LoreFormatter.SellLore(var8, var7 > var2));
               break;
            case "decoration-lore":
               var4.add(new LoreFormatter.DecorationLore(var8));
               break;
            case "item-lore":
               this.item = var7;
               var4.add(new LoreFormatter.ItemLore());
               break;
            case "item-stock":
               var4.add(new LoreFormatter.StockLore(var8, var7 > var2));
               break;
            case "sell-limit":
               var4.add(new LoreFormatter.LimitLore(var8, var7 > var2));
               break;
            case "admin-lore":
               var4.add(new LoreFormatter.AdminLore(var8, var7 > var2));
         }
      }

      this.displayLore = var4;
      ArrayList var16 = new ArrayList();

      for (String var18 : var1) {
         ConfigurationSection var19 = ConfigManager.getConfig().getConfigurationSection(var3 + var18);
         if (var19 == null && !var18.equals("item-lore")) {
            SendMessage.warnMessage("Cannot find lore format for %part% inside config.yml, using default...".replace("%part%", var18));
            var19 = ConfigManager.getConfig().getDef().getConfigurationSection(var3 + var18);
         }

         switch (var18) {
            case "buy-prices":
               var16.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.BUY_LORE, var19.getStringList("normal")));
               break;
            case "sell-prices":
               var16.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.SELL_LORE, var19.getStringList("normal")));
               if (this.plugin.MMB && !Lang.SHIFT_RIGHT_CLICK_SELL_ALL.get().getLegacy().isEmpty()) {
                  var16.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.SELL_ALL_LORE, var19.getStringList("sell-all")));
               }
               break;
            case "decoration-lore":
               for (String var12 : ConfigManager.getConfig().getConfigurationSection(var3 + var18).getKeys(false)) {
                  List var13 = ConfigManager.getConfig().getStringList(var3 + var18 + "." + var12);
                  if (!var13.isEmpty()) {
                     switch (var12) {
                        case "extra-lore":
                           var16.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.DECORATION_LORE, var13));
                           break;
                        case "extra-buy-lore":
                           var16.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.DECORATION_LORE_BUY, var13));
                           break;
                        case "extra-sell-all-lore":
                           if (this.plugin.MMB && !Lang.SHIFT_RIGHT_CLICK_SELL_ALL.get().getLegacy().isEmpty()) {
                              var16.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.DECORATION_LORE_SELL_ALL, var13));
                           }
                           break;
                        case "extra-sell-lore":
                           var16.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.DECORATION_LORE_SELL, var13));
                     }
                  }
               }
               break;
            case "item-lore":
               var16.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.ITEM_LORE, (List<String>)null));
         }
      }

      this.itemLore = var16;
      this.actionAdminLore = new LoreFormatter.ActionAdminLore(ConfigManager.getConfig().getConfigurationSection(var3 + ".admin-lore"), false);
   }

   public ItemStack format(
      ActionItem var1, ItemStack var2, Player var3, EcoType var4, String var5, boolean var6, boolean var7, boolean var8, boolean var9, int var10, int var11
   ) {
      if (var1.hasPlaceholders() && var1.getPlaceholders().containsKey(-1)) {
         var2 = this.setDisplayname(var2, var1.getPlaceholders().get(-1), var3, var1, var4, var6, var7, var10, var11);
      }

      Object var12 = this.components
         ? (var6 ? new LoreFormatter.ComponentLore(var2) : new LoreFormatter.RawComponentLore(var2))
         : (var6 && !var7 ? new LoreFormatter.FastLore(var2) : new LoreFormatter.ShopLore(var2));
      if (!var1.isHideIfUnmet() && var1.getRequirements() != null) {
         for (ItemRequirement var14 : var1.getRequirements()) {
            if (!var14.isMet(var3)) {
               ((LoreFormatter.Lore)var12).addAll(var14.getLore(var3, var6, var7));
            }
         }
      }

      ((LoreFormatter.Lore)var12).addAll(this.parsePlaceholders(var1, var3, var4, var6, var7, var10, var11));
      this.actionAdminLore.apply(var1, var3, (LoreFormatter.Lore)var12, var5, var6, var7, var8, var9);
      return ((LoreFormatter.Lore)var12).form();
   }

   public ItemStack format(
      LoreFormatter.PageIndex var1,
      ShopItem var2,
      ItemStack var3,
      Player var4,
      String var5,
      boolean var6,
      boolean var7,
      boolean var8,
      boolean var9,
      boolean var10,
      boolean var11,
      boolean var12,
      boolean var13,
      boolean var14,
      boolean var15,
      boolean var16,
      boolean var17,
      int var18,
      int var19
   ) {
      if (var2.hasPlaceholders() && var2.getPlaceholders().containsKey(-1)) {
         var3 = this.setDisplayname(var3, var2.getPlaceholders().get(-1), var4, var2, var9, var10, var17, var6, var7, var18, var19);
      }

      Object var20 = this.components
         ? (var6 ? new LoreFormatter.ComponentLore(var3) : new LoreFormatter.RawComponentLore(var3))
         : (var6 && !var7 && !var8 ? new LoreFormatter.FastLore(var3) : new LoreFormatter.ShopLore(var3));

      for (LoreFormatter.DisplayLore var22 : var2.isPriorLore() ? this.priorLore() : this.displayLore) {
         if (var22 instanceof LoreFormatter.ItemLore) {
            ((LoreFormatter.Lore)var20).addAll(this.parsePlaceholders(var2, var4, var6, var7 || var8, var9, var10, var17, var18, var19));
         } else if (var22 instanceof LoreFormatter.RequirementLore) {
            if (!var2.isHideIfUnmet() && var2.getRequirements() != null) {
               for (ItemRequirement var24 : var2.getRequirements()) {
                  if (!var24.isMet(var4)) {
                     ((LoreFormatter.Lore)var20).addAll(var24.getLore(var4, var6, var7 || var8));
                  }
               }
            }
         } else if (!(var22 instanceof LoreFormatter.BuyLore) && !(var22 instanceof LoreFormatter.SellLore)) {
            if (var22 instanceof LoreFormatter.AdminLore) {
               var22.apply(var2, var4, (LoreFormatter.Lore)var20, var5, var6, var7 || var8, var13, var14);
            } else if (var22 instanceof LoreFormatter.SpecialLore) {
               var22.apply(var2, var4, (LoreFormatter.Lore)var20, var5, var6, var7 || var8, var17, var15);
            } else if (var22 instanceof LoreFormatter.StockLore) {
               if (var2.getLimitedStockMode() != 0 && !var2.isDisplayItem() && !var2.isHideUtilLore() && var11) {
                  var22.apply(var1, var2, var4, (LoreFormatter.Lore)var20, var5, var6, var7 || var8);
               }
            } else if (var22 instanceof LoreFormatter.LimitLore) {
               if (var2.getLimitedSellMode() != 0 && !var2.isDisplayItem() && !var2.isHideUtilLore() && var12) {
                  var22.apply(var1, var2, var4, (LoreFormatter.Lore)var20, var5, var6, var7 || var8);
               }
            } else if (var22 instanceof LoreFormatter.DecorationLore) {
               var22.apply(var2, var4, (LoreFormatter.Lore)var20, var5, var6, var7, var8);
            }
         } else if (!var2.isHidePricingLore() && !var2.hasItemError() && !var2.isDisplayItem()) {
            var22.apply(var1, var2, var4, (LoreFormatter.Lore)var20, var5, var6, var7 || var8, var9, var10, var16, var17);
         }
      }

      return ((LoreFormatter.Lore)var20).form();
   }

   private List<LoreFormatter.DisplayLore> priorLore() {
      ArrayList var1 = new ArrayList<>(this.displayLore);
      var1.add(0, var1.remove(this.item));
      return var1;
   }

   public List<LoreFormatter.StaticLore> getItemLore() {
      return this.itemLore;
   }

   private void addLore(ShopItem var1, LoreFormatter.FastLore var2, String var3, int var4, boolean var5) {
      if (var5) {
         this.plugin.versionHandler.addItemLore(var2, var3);
      } else {
         this.plugin.versionHandler.addItemLore(var2, var3, var4);
      }
   }

   private ItemStack setDisplayname(
      ItemStack var1, Placeholder var2, Player var3, ShopItem var4, boolean var5, boolean var6, boolean var7, boolean var8, boolean var9, int var10, int var11
   ) {
      String var12 = var2.translate(
         var3, var4.isPriceLore() ? this.pricePlaceholders(var2.getRaw(), var4, var3, var5, var6, var7, var10, var11) : new Replacement[0]
      );
      if (var8 && !this.components && !var9) {
         return this.plugin.versionHandler.setDisplayname(var1, this.plugin.versionHandler.toNBT(var12));
      } else {
         ItemMeta var13 = var1.getItemMeta();
         this.plugin.getMetaUtils().setRawDisplayName(var13, var12);
         var1.setItemMeta(var13);
         return var1;
      }
   }

   private ItemStack setDisplayname(
      ItemStack var1, Placeholder var2, Player var3, ActionItem var4, EcoType var5, boolean var6, boolean var7, int var8, int var9
   ) {
      String var10 = var2.translate(var3, this.actionPlaceholders(var4, var3, var5, var8, var9));
      if (!this.components && var6 && !var7) {
         return this.plugin.versionHandler.setDisplayname(var1, this.plugin.versionHandler.toNBT(var10));
      } else {
         ItemMeta var11 = var1.getItemMeta();
         this.plugin.getMetaUtils().setRawDisplayName(var11, var10);
         var1.setItemMeta(var11);
         return var1;
      }
   }

   private LoreFormatter.DynamicLoreList getDynamicLoreList(List<String> var1, String var2, String... var3) {
      return (LoreFormatter.DynamicLoreList)(this.components
         ? new LoreFormatter.DynamicCompLoreList(var1, var2, var3)
         : new LoreFormatter.DynamicBukkitLoreList(var1, var2, var3));
   }

   private static LoreFormatter.LocalizedLore getLocalized(Map<Locale, LoreFormatter.LocalizedLore> var0, Player var1) {
      LoreFormatter.LocalizedLore var2 = (LoreFormatter.LocalizedLore)var0.get(UserManager.getUser(var1).getLocale());
      return var2 != null ? var2 : (LoreFormatter.LocalizedLore)var0.get(Lang.getDefaultLocale());
   }

   private List<String> getStringList(ConfigurationSection var1, String var2) {
      return (List<String>)(var1.get(var2) instanceof List ? var1.getStringList(var2) : new ArrayList<>(Collections.singletonList(var1.getString(var2))));
   }

   private boolean isEmpty(List<String> var1) {
      return var1.stream().allMatch(String::isEmpty);
   }

   private String translate(String var1) {
      return this.translate(var1, Lang.getDefaultLocale());
   }

   private String translate(String var1, Locale var2) {
      if (var1 == null) {
         return "";
      } else {
         while (var1.contains("%translations-")) {
            try {
               Lang var3 = Lang.valueOf(var1.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
               var1 = var1.replace("%translations-" + var3.getKey() + "%", var3.getRaw(var2));
            } catch (IllegalArgumentException var4) {
            }
         }

         return this.components ? ChatUtil.getAdventureUtils().formatLegacyToMini(var1) : ChatUtil.formatColors(var1);
      }
   }

   private List<String> translate(List<String> var1) {
      return this.translate(var1, Lang.getDefaultLocale());
   }

   private List<String> translate(List<String> var1, Locale var2) {
      if (var1.isEmpty()) {
         return var1;
      } else {
         ArrayList var3 = new ArrayList();

         for (String var5 : var1) {
            while (var5.contains("%translations-")) {
               try {
                  Lang var6 = Lang.valueOf(var5.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
                  var5 = var5.replace("%translations-" + var6.getKey() + "%", var6.getRaw(var2).replace("\\n", "\n"));
               } catch (IllegalArgumentException var10) {
               }
            }

            for (String var9 : var5.split("\n")) {
               var3.add(this.components ? ChatUtil.getAdventureUtils().formatLegacyToMini(var9) : ChatUtil.formatColors(var9));
            }
         }

         return var3;
      }
   }

   private String getBuyPrice(String var1, ShopItem var2, Player var3, boolean var4, boolean var5) {
      if (var2.isBuyAble()) {
         if (!var5 && !var4) {
            return var2.isABuyPricing()
               ? var2.getABuyPrice().formatBuyPrices(var1.split("%buyPrice%")[0], null, var2.getStackSize(), true)
               : this.plugin.formatPrice(var2.getEcoType(), var2.getBuyPrice(var2.getStackSize(), true));
         } else {
            return var2.isABuyPricing()
               ? var2.getABuyPrice().formatBuyPrices(var1.split("%buyPrice%")[0], null, var3, var2.getStackSize(), var2.getBuyModifiers(), true)
               : this.plugin.formatPrice(var2.getEcoType(), var2.getBuyPrice(var3, var2.getStackSize(), true));
         }
      } else {
         return Lang.ITEM_CANNOT_BE_BOUGHT.get(var3).getLegacy();
      }
   }

   private String getSellPrice(String var1, ShopItem var2, Player var3, boolean var4, boolean var5) {
      if (var2.isSellAble()) {
         if (!var5 && !var4) {
            return var2.isASellPricing()
               ? var2.getASellPrice().formatSellPrices(var1.split("%sellPrice%")[0], null, var2.getStackSize(), true)
               : this.plugin.formatPrice(var2.getEcoType(), var2.getSellPrice(var2.getStackSize(), true));
         } else {
            return var2.isASellPricing()
               ? var2.getASellPrice().formatSellPrices(var1.split("%sellPrice%")[0], null, var3, var2.getStackSize(), var2.getSellModifiers(), true)
               : this.plugin.formatPrice(var2.getEcoType(), var2.getSellPrice(var3, var2.getStackSize(), true));
         }
      } else {
         return Lang.ITEM_CANNOT_BE_SOLD.get(var3).getLegacy();
      }
   }

   private String getBuyPriceRaw(ShopItem var1, Player var2, boolean var3, boolean var4) {
      if (var1.isBuyAble()) {
         if (!var4 && !var3) {
            return var1.isABuyPricing()
               ? var1.getABuyPrice()
                  .getBuyPrices(null, var1.getStackSize(), true)
                  .entrySet()
                  .stream()
                  .map(var0 -> var0.getKey() + ": " + var0.getValue())
                  .collect(Collectors.joining(", "))
               : String.valueOf(var1.getBuyPrice(var1.getStackSize(), true));
         } else {
            return var1.isABuyPricing()
               ? var1.getABuyPrice()
                  .getBuyPrices(null, var2, var1.getStackSize(), var1.getBuyModifiers(), true)
                  .entrySet()
                  .stream()
                  .map(var0 -> var0.getKey() + ": " + var0.getValue())
                  .collect(Collectors.joining(", "))
               : String.valueOf(var1.getBuyPrice(var2, var1.getStackSize(), true));
         }
      } else {
         return "-1";
      }
   }

   private String getSellPriceRaw(ShopItem var1, Player var2, boolean var3, boolean var4) {
      if (var1.isSellAble()) {
         if (!var4 && !var3) {
            return var1.isASellPricing()
               ? var1.getASellPrice()
                  .getSellPrices(null, var1.getStackSize(), true)
                  .entrySet()
                  .stream()
                  .map(var0 -> var0.getKey() + ": " + var0.getValue())
                  .collect(Collectors.joining(", "))
               : String.valueOf(var1.getSellPrice(var1.getStackSize(), true));
         } else {
            return var1.isASellPricing()
               ? var1.getASellPrice()
                  .getSellPrices(null, var2, var1.getStackSize(), var1.getSellModifiers(), true)
                  .entrySet()
                  .stream()
                  .map(var0 -> var0.getKey() + ": " + var0.getValue())
                  .collect(Collectors.joining(", "))
               : String.valueOf(var1.getSellPrice(var2, var1.getStackSize(), true));
         }
      } else {
         return "-1";
      }
   }

   private Replacement[] pricePlaceholders(String var1, ShopItem var2, Player var3, boolean var4, boolean var5, boolean var6, int var7, int var8) {
      return new Replacement[]{
         new Replacement("%buyPrice%", () -> this.getBuyPrice(var1, var2, var3, var4, var6)),
         new Replacement("%sellPrice%", () -> this.getSellPrice(var1, var2, var3, var5, var6)),
         new Replacement("%buyPriceRaw%", () -> this.getBuyPriceRaw(var2, var3, var4, var6)),
         new Replacement("%sellPriceRaw%", () -> this.getSellPriceRaw(var2, var3, var5, var6)),
         new Replacement("%player_displayname%", () -> this.plugin.getDisplayName(var3)),
         new Replacement(
            "%player_balance%", () -> this.plugin.formatPrice(var2.getEcoType(), this.plugin.getEcoHandler().getEcon(var2.getEcoType()).getBalance(var3))
         ),
         new Replacement("%page%", () -> String.valueOf(var7)),
         new Replacement("%pages%", () -> String.valueOf(var8))
      };
   }

   private String[] parsePlaceholders(ShopItem var1, Player var2, boolean var3, boolean var4, boolean var5, boolean var6, boolean var7, int var8, int var9) {
      String[] var10 = var1.getLore().get(var3, var4);

      for (Entry<Integer, Placeholder> var12 : var1.getPlaceholders().entrySet()) {
         if ((Integer)var12.getKey() != -1) {
            var10[var12.getKey()] = ((Placeholder)var12.getValue())
               .translate(
                  var10[var12.getKey()],
                  var2,
                  var1.isPriceLore()
                     ? this.pricePlaceholders(((Placeholder)var12.getValue()).getRaw(), var1, var2, var5, var6, var7, var8, var9)
                     : new Replacement[0]
               );
         }
      }

      return var10;
   }

   private Replacement[] actionPlaceholders(ActionItem var1, Player var2, EcoType var3, int var4, int var5) {
      return new Replacement[]{
         new Replacement("%player_displayname%", () -> this.plugin.getDisplayName(var2)),
         new Replacement("%player_balance%", () -> this.plugin.formatPrice(var3, this.plugin.getEcoHandler().getEcon(var3).getBalance(var2))),
         new Replacement("%page%", () -> String.valueOf(var4)),
         new Replacement("%pages%", () -> String.valueOf(var5))
      };
   }

   private String[] parsePlaceholders(ActionItem var1, Player var2, EcoType var3, boolean var4, boolean var5, int var6, int var7) {
      String[] var8 = var1.getLore().get(var4, var5);

      for (Entry<Integer, Placeholder> var10 : var1.getPlaceholders().entrySet()) {
         if ((Integer)var10.getKey() != -1) {
            var8[var10.getKey()] = ((Placeholder)var10.getValue()).translate(var8[var10.getKey()], var2, this.actionPlaceholders(var1, var2, var3, var6, var7));
         }
      }

      return var8;
   }

   private final class ActionAdminLore {
      private final LoreFormatter.DynamicLoreList itemEditor;
      private final LoreFormatter.DynamicLoreList itemIndex;
      private final boolean after;

      public ActionAdminLore(ConfigurationSection nullx, boolean nullxx) {
         this.after = nullxx;
         this.itemEditor = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(nullx, "item-editor"), Lang.SHIFT_CLICK_ITEM_EDITOR.get().toString()
         );
         this.itemIndex = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(nullx, "item-index"), Lang.ITEM_LOCATION_IN_SHOP.get().toString(), "%shopsection%", "%itemLoc%"
         );
      }

      public void apply(ActionItem var1, Player var2, LoreFormatter.Lore var3, String var4, boolean var5, boolean var6, boolean... var7) {
         if (var7[0]) {
            var3.addAll(this.itemEditor.get(var5, var6, var2));
         }

         if (var7[1]) {
            var3.addAll(this.itemIndex.get(var5, var6, var2, "%shopsection%", var4, "%itemLoc%", var1.itemLoc));
         }
      }
   }

   private final class AdminLore implements LoreFormatter.DisplayLore {
      private final LoreFormatter.DynamicLoreList itemEditor;
      private final LoreFormatter.DynamicLoreList itemIndex;
      private final boolean after;

      public AdminLore(ConfigurationSection nullx, boolean nullxx) {
         this.after = nullxx;
         this.itemEditor = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(nullx, "item-editor"), Lang.SHIFT_CLICK_ITEM_EDITOR.get().toString()
         );
         this.itemIndex = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(nullx, "item-index"), Lang.ITEM_LOCATION_IN_SHOP.get().toString(), "%shopsection%", "%itemLoc%"
         );
      }

      @Override
      public void apply(ShopItem var1, Player var2, LoreFormatter.Lore var3, String var4, boolean var5, boolean var6, boolean... var7) {
         if (var7[0]) {
            var3.addAll(this.itemEditor.get(var5, var6, var2));
         }

         if (var7[1]) {
            var3.addAll(this.itemIndex.get(var5, var6, var2, "%shopsection%", var4, "%itemLoc%", var1.itemLoc));
         }
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex var1, ShopItem var2, Player var3, LoreFormatter.Lore var4, String var5, boolean var6, boolean var7, boolean... var8
      ) {
      }
   }

   private final class BuyLore implements LoreFormatter.DisplayLore {
      private final LoreFormatter.DynamicLoreList disabled;
      private final LoreFormatter.DynamicLoreList undefined;
      private final LoreFormatter.DynamicLoreList buyPrices;
      private final LoreFormatter.DynamicLoreList dynamicBuyPrices;
      private final LoreFormatter.DynamicLoreList seasonalBuyPrices;
      private final LoreFormatter.DynamicLoreList discountedBuyPrices;
      private final boolean after;

      public BuyLore(ConfigurationSection nullx, boolean nullxx) {
         this.after = nullxx;
         this.disabled = LoreFormatter.this.getDynamicLoreList(nullx.getStringList("disabled"), Lang.ITEM_CANNOT_BE_BOUGHT.get().toString());
         this.undefined = LoreFormatter.this.getDynamicLoreList(nullx.getStringList("undefined"), "");
         this.buyPrices = LoreFormatter.this.getDynamicLoreList(
            nullx.getStringList("normal"), Lang.LEFT_CLICK_BUY.get().toString(), "%buyPrice%", "%buyPriceRaw%"
         );
         this.dynamicBuyPrices = LoreFormatter.this.getDynamicLoreList(
            nullx.getStringList("dynamic-price"), Lang.DYNAMIC_BUY_PRICE.get().toString(), "%dpp%", "%buyPrice%", "%buyPriceRaw%", "%dynamicBuyPrice%"
         );
         this.seasonalBuyPrices = LoreFormatter.this.getDynamicLoreList(
            nullx.getStringList("seasonal"), Lang.SEASONAL_BUY_PRICE.get().toString(), "%dpp%", "%buyPrice%", "%buyPriceRaw%", "%seasonalBuyPrice%"
         );
         this.discountedBuyPrices = LoreFormatter.this.getDynamicLoreList(
            nullx.getStringList("discounted"), Lang.DISCOUNTED_BUY_PRICE.get().toString(), "%dpp%", "%buyPrice%", "%buyPriceRaw%", "%discountedPrice%"
         );
      }

      @Override
      public void apply(ShopItem var1, Player var2, LoreFormatter.Lore var3, String var4, boolean var5, boolean var6, boolean... var7) {
         this.apply(null, var1, var2, var3, var4, var5, var6, var7);
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex var1, ShopItem var2, Player var3, LoreFormatter.Lore var4, String var5, boolean var6, boolean var7, boolean... var8
      ) {
         if (var2.isBuyLore()) {
            if (var2.isBuyAble()) {
               if (var8[3]) {
                  var4.addAll(
                     this.seasonalBuyPrices
                        .get(
                           var6,
                           var7,
                           var3,
                           "%dpp%",
                           var2.isDynamicPricing() && var8[2] ? LoreFormatter.this.plugin.getDynamicPricingProvider().getStatus(var2, var3, true) : "",
                           "%buyPrice%",
                           ChatColor.stripColor(
                              var2.isABuyPricing()
                                 ? var2.getABuyPrice().formatBuyPrices(null)
                                 : LoreFormatter.this.plugin.formatPrice(var2.getEcoType(), var2.getBuyPrice())
                           ),
                           "%buyPriceRaw%",
                           var2.isABuyPricing()
                              ? var2.getABuyPrice()
                                 .getBuyPrices(null, var3, var2.getStackSize(), var2.getBuyModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(var0 -> var0.getKey() + ": " + var0.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(var2.getBuyPrice(var3, var2.getStackSize(), true, var1)),
                           "%seasonalBuyPrice%",
                           var2.isABuyPricing()
                              ? var2.getABuyPrice().formatBuyPrices(null, null, var3, var2.getStackSize(), var2.getBuyModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(var2.getEcoType(), var2.getBuyPrice(var3, var2.getStackSize(), true, var1))
                        )
                  );
               } else if (var8[0] && CalculateAmount.calculateDiscount(var3, var5, var2.getBuyPrice()) != var2.getBuyPrice()) {
                  var4.addAll(
                     this.discountedBuyPrices
                        .get(
                           var6,
                           var7,
                           var3,
                           "%dpp%",
                           var2.isDynamicPricing() && var8[2] ? LoreFormatter.this.plugin.getDynamicPricingProvider().getStatus(var2, var3, true) : "",
                           "%buyPrice%",
                           ChatColor.stripColor(
                              var2.isABuyPricing()
                                 ? var2.getABuyPrice().formatBuyPrices(null)
                                 : LoreFormatter.this.plugin.formatPrice(var2.getEcoType(), var2.getBuyPrice())
                           ),
                           "%buyPriceRaw%",
                           var2.isABuyPricing()
                              ? var2.getABuyPrice()
                                 .getBuyPrices(null, var3, var2.getStackSize(), var2.getBuyModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(var0 -> var0.getKey() + ": " + var0.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(var2.getBuyPrice(var3, var2.getStackSize(), true, var1)),
                           "%discountedPrice%",
                           var2.isABuyPricing()
                              ? var2.getABuyPrice().formatBuyPrices(null, null, var3, var2.getStackSize(), var2.getBuyModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(var2.getEcoType(), var2.getBuyPrice(var3, var2.getStackSize(), true, var1))
                        )
                  );
               } else if (var2.isDynamicPricing() && var8[2] && var2.getBuyPrice(1, true, var1) != var2.getBuyPriceRaw(1)) {
                  var4.addAll(
                     this.dynamicBuyPrices
                        .get(
                           var6,
                           var7,
                           var3,
                           "%dpp%",
                           LoreFormatter.this.plugin.getDynamicPricingProvider().getStatus(var2, var3, true),
                           "%buyPrice%",
                           ChatColor.stripColor(
                              var2.isABuyPricing()
                                 ? var2.getABuyPrice().formatBuyPrices(null)
                                 : LoreFormatter.this.plugin.formatPrice(var2.getEcoType(), var2.getBuyPrice())
                           ),
                           "%buyPriceRaw%",
                           var2.isABuyPricing()
                              ? var2.getABuyPrice()
                                 .getBuyPrices(null, var3, var2.getStackSize(), var2.getBuyModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(var0 -> var0.getKey() + ": " + var0.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(var2.getBuyPrice(var3, var2.getStackSize(), true, var1)),
                           "%dynamicBuyPrice%",
                           var2.isABuyPricing()
                              ? var2.getABuyPrice().formatBuyPrices(null, null, var3, var2.getStackSize(), var2.getBuyModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(var2.getEcoType(), var2.getBuyPrice(var3, var2.getStackSize(), true, var1))
                        )
                  );
               } else {
                  var4.addAll(
                     this.buyPrices
                        .get(
                           var6,
                           var7,
                           var3,
                           "%buyPrice%",
                           var2.isABuyPricing()
                              ? var2.getABuyPrice().formatBuyPrices(null, null, var3, var2.getStackSize(), var2.getBuyModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(var2.getEcoType(), var2.getBuyPrice(var3, var2.getStackSize(), true, var1)),
                           "%buyPriceRaw%",
                           var2.isABuyPricing()
                              ? var2.getABuyPrice()
                                 .getBuyPrices(null, var3, var2.getStackSize(), var2.getBuyModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(var0 -> var0.getKey() + ": " + var0.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(var2.getBuyPrice(var3, var2.getStackSize(), true, var1))
                        )
                  );
               }
            } else {
               var4.addAll(this.disabled.get(var6, var7, var3));
            }
         } else {
            var4.addAll(this.undefined.get(var6, var7, var3));
         }
      }
   }

   private final class ComponentLore implements LoreFormatter.Lore {
      private final List<String> lore = new ArrayList<>();
      private final ItemStack item;

      public ComponentLore(ItemStack nullx) {
         this.item = nullx;
      }

      @Override
      public void addAll(String[] var1) {
         this.lore.addAll(Arrays.asList(var1));
      }

      @Override
      public void add(String var1) {
         this.lore.add(var1);
      }

      @Override
      public ItemStack form() {
         try {
            return (ItemStack)ReflectUtil.invokeStaticLegacyMethod(
               "setLore", LoreFormatter.this.plugin.getMetaUtils().getClass(), new Class[]{ItemStack.class, ArrayList.class}, this.item, this.lore
            );
         } catch (Exception var2) {
            SendMessage.warnMessage("Failed to display component lore on shop item");
            var2.printStackTrace();
            return null;
         }
      }
   }

   private final class DecorationLore implements LoreFormatter.DisplayLore {
      private final LoreFormatter.DynamicLoreList lore;
      private final LoreFormatter.DynamicLoreList buyLore;
      private final LoreFormatter.DynamicLoreList sellLore;
      private final LoreFormatter.DynamicLoreList sellAllLore;
      private final LoreFormatter.DynamicLoreList manageLore;

      public DecorationLore(ConfigurationSection nullx) {
         this.lore = LoreFormatter.this.getDynamicLoreList(nullx.getStringList("extra-lore"), "");
         this.buyLore = LoreFormatter.this.getDynamicLoreList(nullx.getStringList("extra-buy-lore"), "");
         this.sellLore = LoreFormatter.this.getDynamicLoreList(nullx.getStringList("extra-sell-lore"), "");
         this.sellAllLore = LoreFormatter.this.getDynamicLoreList(nullx.getStringList("extra-sell-all-lore"), "");
         this.manageLore = LoreFormatter.this.getDynamicLoreList(nullx.getStringList("extra-view-lore"), "");
      }

      @Override
      public void apply(ShopItem var1, Player var2, LoreFormatter.Lore var3, String var4, boolean var5, boolean var6, boolean... var7) {
         if (var1.isDecorationLore()) {
            var3.addAll(this.lore.get(var5, var6 || var7[0], var2));
            if (!var7[0]) {
               if (var1.isBuyAble() && var1.isBuyLore()) {
                  var3.addAll(this.buyLore.get(var5, var6 || var7[0], var2));
               }

               if (var1.isSellAble() && var1.isSellLore()) {
                  if (LoreFormatter.this.plugin.MMB) {
                     var3.addAll(this.sellAllLore.get(var5, var6 || var7[0], var2));
                  }

                  var3.addAll(this.sellLore.get(var5, var6 || var7[0], var2));
               }
            } else {
               var3.addAll(this.manageLore.get(var5, var7[0], var2));
            }
         }
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex var1, ShopItem var2, Player var3, LoreFormatter.Lore var4, String var5, boolean var6, boolean var7, boolean... var8
      ) {
      }
   }

   private interface DisplayLore {
      void apply(ShopItem var1, Player var2, LoreFormatter.Lore var3, String var4, boolean var5, boolean var6, boolean... var7);

      void apply(LoreFormatter.PageIndex var1, ShopItem var2, Player var3, LoreFormatter.Lore var4, String var5, boolean var6, boolean var7, boolean... var8);
   }

   private final class DynamicBukkitLoreList implements LoreFormatter.DynamicLoreList {
      private final LoreFormatter.LocalizedLore lore;
      private final Map<Locale, LoreFormatter.LocalizedLore> localizedLore;

      public DynamicBukkitLoreList(List<String> nullx, String nullxx, String... nullxxx) {
         String[] var5 = nullx != null
            ? (!LoreFormatter.this.isEmpty(nullx) ? LoreFormatter.this.translate(nullx).toArray(new String[nullx.size()]) : new String[0])
            : LoreFormatter.this.translate(nullxx).split("\n");
         String[] var6 = Arrays.stream(var5).map(var1 -> LoreFormatter.this.plugin.versionHandler.toNBT(var1)).toArray(String[]::new);
         this.lore = !LoreFormatter.this.plugin.perPlayerLanguages ? new LoreFormatter.LocalizedLore(var5, var6, nullxxx) : null;
         this.localizedLore = LoreFormatter.this.plugin.perPlayerLanguages ? this.getlocalizedLore(nullx, nullxx, nullxxx) : null;
      }

      private Map<Locale, LoreFormatter.LocalizedLore> getlocalizedLore(List<String> var1, String var2, String... var3) {
         HashMap var4 = new HashMap();

         for (Locale var6 : Lang.getLocales()) {
            String[] var7 = var1 != null
               ? (!LoreFormatter.this.isEmpty(var1) ? LoreFormatter.this.translate(var1, var6).toArray(new String[0]) : new String[0])
               : LoreFormatter.this.translate(var2, var6).split("\n");
            String[] var8 = Arrays.stream(var7).map(var1x -> LoreFormatter.this.plugin.versionHandler.toNBT(var1x)).toArray(String[]::new);
            var4.put(var6, new LoreFormatter.LocalizedLore(var7, var8, var3));
         }

         return var4;
      }

      @Override
      public String[] get(boolean var1, boolean var2, Player var3, String... var4) {
         LoreFormatter.LocalizedLore var5 = this.localizedLore != null ? LoreFormatter.getLocalized(this.localizedLore, var3) : this.lore;
         String[] var6 = (String[])(var1 && !var2 ? var5.fastLore : var5.lore).clone();

         for (Entry<Integer, Placeholder> var8 : var5.placeholders.entrySet()) {
            var6[var8.getKey()] = ((Placeholder)var8.getValue()).translate(var6[var8.getKey()], var3, var4);
         }

         return var6;
      }
   }

   private final class DynamicCompLoreList implements LoreFormatter.DynamicLoreList {
      private final LoreFormatter.LocalizedLore lore;
      private final Map<Locale, LoreFormatter.LocalizedLore> localizedLore;

      public DynamicCompLoreList(List<String> nullx, String nullxx, String... nullxxx) {
         String[] var5 = nullx != null
            ? (!LoreFormatter.this.isEmpty(nullx) ? LoreFormatter.this.translate(nullx).toArray(new String[nullx.size()]) : new String[0])
            : LoreFormatter.this.translate(nullxx).split("\n");
         String[] var6 = Arrays.stream(var5).map(ChatUtil::getGsonComponent).toArray(String[]::new);
         this.lore = !LoreFormatter.this.plugin.perPlayerLanguages ? new LoreFormatter.LocalizedLore(var5, var6, nullxxx) : null;
         this.localizedLore = LoreFormatter.this.plugin.perPlayerLanguages ? this.getlocalizedLore(nullx, nullxx, nullxxx) : null;
      }

      private Map<Locale, LoreFormatter.LocalizedLore> getlocalizedLore(List<String> var1, String var2, String... var3) {
         HashMap var4 = new HashMap();

         for (Locale var6 : Lang.getLocales()) {
            String[] var7 = var1 != null
               ? (!LoreFormatter.this.isEmpty(var1) ? LoreFormatter.this.translate(var1, var6).toArray(new String[0]) : new String[0])
               : LoreFormatter.this.translate(var2, var6).split("\n");
            String[] var8 = Arrays.stream(var7).map(ChatUtil::getGsonComponent).toArray(String[]::new);
            var4.put(var6, new LoreFormatter.LocalizedLore(var7, var8, var3));
         }

         return var4;
      }

      @Override
      public String[] get(boolean var1, boolean var2, Player var3, String... var4) {
         LoreFormatter.LocalizedLore var5 = this.localizedLore != null ? LoreFormatter.getLocalized(this.localizedLore, var3) : this.lore;
         String[] var6 = (String[])(var1 ? var5.fastLore : var5.lore).clone();

         for (Entry<Integer, Placeholder> var8 : var5.placeholders.entrySet()) {
            var6[var8.getKey()] = ((Placeholder)var8.getValue()).translate(var6[var8.getKey()], var3, var4);
         }

         return var6;
      }
   }

   private interface DynamicLoreList {
      String[] get(boolean var1, boolean var2, Player var3, String... var4);
   }

   private final class FastLore implements LoreFormatter.Lore {
      private final Object lore = LoreFormatter.this.plugin.versionHandler.emptyList();
      private final ItemStack stack;

      public FastLore(ItemStack nullx) {
         this.stack = nullx;
      }

      @Override
      public void add(String var1) {
         LoreFormatter.this.plugin.versionHandler.addItemLore(this.lore, var1);
      }

      public void add(String var1, int var2) {
         LoreFormatter.this.plugin.versionHandler.addItemLore(this.lore, var1, var2);
      }

      @Override
      public void addAll(String[] var1) {
         for (String var5 : var1) {
            this.add(var5);
         }
      }

      @Override
      public ItemStack form() {
         return LoreFormatter.this.plugin.versionHandler.applyNMSLore(this.stack, this.lore);
      }
   }

   private final class ItemLore implements LoreFormatter.DisplayLore {
      public ItemLore() {
      }

      @Override
      public void apply(ShopItem var1, Player var2, LoreFormatter.Lore var3, String var4, boolean var5, boolean var6, boolean... var7) {
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex var1, ShopItem var2, Player var3, LoreFormatter.Lore var4, String var5, boolean var6, boolean var7, boolean... var8
      ) {
      }
   }

   private final class LimitLore implements LoreFormatter.DisplayLore {
      private final LoreFormatter.DynamicLoreList currentLimit;
      private final LoreFormatter.DynamicLoreList reachedLimit;
      private final LoreFormatter.DynamicLoreList restocking;
      private final boolean after;

      public LimitLore(ConfigurationSection nullx, boolean nullxx) {
         this.after = nullxx;
         this.currentLimit = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(nullx, "current-limit"), Lang.SELL_LIMIT.get().toString(), "%limit%"
         );
         this.reachedLimit = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(nullx, "limit-reached"), Lang.SELL_LIMIT_REACHED.get().toString()
         );
         this.restocking = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(nullx, "restocking"), Lang.SELL_LIMIT_RESTOCKING.get().toString(), "%time%"
         );
      }

      @Override
      public void apply(ShopItem var1, Player var2, LoreFormatter.Lore var3, String var4, boolean var5, boolean var6, boolean... var7) {
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex var1, ShopItem var2, Player var3, LoreFormatter.Lore var4, String var5, boolean var6, boolean var7, boolean... var8
      ) {
         int var9 = LoreFormatter.this.plugin.getLimitedSellProvider().getItemLimit(var2, var3.getUniqueId(), true, true, var1);
         if (var9 >= 0) {
            var4.addAll(this.currentLimit.get(var6, var7, var3, "%limit%", String.valueOf(var9)));
         } else if (var9 == -1) {
            var4.addAll(
               this.restocking.get(var6, var7, var3, "%time%", LoreFormatter.this.plugin.getLimitedSellProvider().getTimeLeftToRestock(var2.getItemPath()))
            );
         } else if (var9 == -2) {
            var4.addAll(
               this.restocking
                  .get(
                     var6,
                     var7,
                     var3,
                     "%time%",
                     LoreFormatter.this.plugin.getLimitedSellProvider().getTimeLeftToRestock(var3.getUniqueId(), var2.getItemPath())
                  )
            );
         } else if (var9 != -3 && var9 != -4) {
            var4.addAll(this.currentLimit.get(var6, var7, var3, "%limit%", String.valueOf(var9)));
         } else {
            var4.addAll(this.reachedLimit.get(var6, var7, var3));
         }
      }
   }

   private static final class LocalizedLore {
      private final Map<Integer, Placeholder> placeholders = new HashMap<>();
      private final String[] lore;
      private final String[] fastLore;

      private LocalizedLore(String[] var1, String[] var2, String... var3) {
         this.lore = var1;
         this.fastLore = var2;
         int var4 = 0;

         for (String var8 : var1) {
            if (Placeholder.isPlaceholder(var8, var3)) {
               this.placeholders.put(var4, new Placeholder(var8, var3));
            }

            var4++;
         }
      }
   }

   private interface Lore {
      void addAll(String[] var1);

      void add(String var1);

      ItemStack form();
   }

   public static enum LoreType {
      BUY_LORE,
      SELL_LORE,
      SELL_ALL_LORE,
      DECORATION_LORE,
      DECORATION_LORE_BUY,
      DECORATION_LORE_SELL,
      DECORATION_LORE_SELL_ALL,
      ITEM_LORE;

      private LoreType() {
      }
   }

   public static final class PageIndex {
      public final String section;
      public final int page;
      private List<ShopItem> items;

      public PageIndex(String var1, int var2) {
         this.section = var1;
         this.page = var2;
      }

      public List<ShopItem> getItems() {
         if (this.items == null) {
            this.items = OnyxShop.getInstance().getSection(this.section).getPageItems(this.page);
         }

         return this.items;
      }
   }

   private final class RawComponentLore implements LoreFormatter.Lore {
      private final ArrayList<String> lore = new ArrayList<>();
      private final ItemStack item;

      public RawComponentLore(ItemStack nullx) {
         this.item = nullx;
      }

      @Override
      public void addAll(String[] var1) {
         this.lore.addAll(Arrays.asList(var1));
      }

      @Override
      public void add(String var1) {
         this.lore.add(var1);
      }

      @Override
      public ItemStack form() {
         try {
            return (ItemStack)ReflectUtil.invokeStaticLegacyMethod(
               "setRawLore", LoreFormatter.this.plugin.getMetaUtils().getClass(), new Class[]{ItemStack.class, ArrayList.class}, this.item, this.lore
            );
         } catch (Exception var2) {
            SendMessage.warnMessage("Failed to display component lore on shop item");
            var2.printStackTrace();
            return null;
         }
      }
   }

   private final class RequirementLore implements LoreFormatter.DisplayLore {
      public RequirementLore() {
      }

      @Override
      public void apply(ShopItem var1, Player var2, LoreFormatter.Lore var3, String var4, boolean var5, boolean var6, boolean... var7) {
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex var1, ShopItem var2, Player var3, LoreFormatter.Lore var4, String var5, boolean var6, boolean var7, boolean... var8
      ) {
      }
   }

   private final class SellLore implements LoreFormatter.DisplayLore {
      private final LoreFormatter.DynamicLoreList disabled;
      private final LoreFormatter.DynamicLoreList undefined;
      private final LoreFormatter.DynamicLoreList sellPrices;
      private final LoreFormatter.DynamicLoreList dynamicSellPrices;
      private final LoreFormatter.DynamicLoreList seasonalSellPrices;
      private final LoreFormatter.DynamicLoreList multipliedSellPrices;
      private final LoreFormatter.DynamicLoreList sellAllLore;
      private final boolean after;

      public SellLore(ConfigurationSection nullx, boolean nullxx) {
         this.after = nullxx;
         this.disabled = LoreFormatter.this.getDynamicLoreList(nullx.getStringList("disabled"), Lang.ITEM_CANNOT_BE_SOLD.get().toString());
         this.undefined = LoreFormatter.this.getDynamicLoreList(nullx.getStringList("undefined"), "");
         this.sellPrices = LoreFormatter.this.getDynamicLoreList(
            nullx.getStringList("normal"), Lang.RIGHT_CLICK_SELL.get().toString(), "%sellPrice%", "%sellPriceRaw%"
         );
         this.dynamicSellPrices = LoreFormatter.this.getDynamicLoreList(
            nullx.getStringList("dynamic-price"), Lang.DYNAMIC_SELL_PRICE.get().toString(), "%dpp%", "%sellPrice%", "%sellPriceRaw%", "%dynamicSellPrice%"
         );
         this.seasonalSellPrices = LoreFormatter.this.getDynamicLoreList(
            nullx.getStringList("seasonal"), Lang.SEASONAL_SELL_PRICE.get().toString(), "%dpp%", "%sellPrice%", "%sellPriceRaw%", "%seasonalSellPrice%"
         );
         this.multipliedSellPrices = LoreFormatter.this.getDynamicLoreList(
            nullx.getStringList("multiplied"), Lang.MULTIPLIED_SELL_PRICE.get().toString(), "%dpp%", "%sellPrice%", "%sellPriceRaw%", "%multipliedPrice%"
         );
         this.sellAllLore = LoreFormatter.this.getDynamicLoreList(nullx.getStringList("sell-all"), Lang.SHIFT_RIGHT_CLICK_SELL_ALL.get().toString());
      }

      @Override
      public void apply(ShopItem var1, Player var2, LoreFormatter.Lore var3, String var4, boolean var5, boolean var6, boolean... var7) {
         this.apply(null, var1, var2, var3, var4, var5, var6, var7);
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex var1, ShopItem var2, Player var3, LoreFormatter.Lore var4, String var5, boolean var6, boolean var7, boolean... var8
      ) {
         if (var2.isSellLore()) {
            if (var2.isSellAble()) {
               if (var8[3]) {
                  var4.addAll(
                     this.seasonalSellPrices
                        .get(
                           var6,
                           var7,
                           var3,
                           "%dpp%",
                           var2.isDynamicPricing() && var8[2] ? LoreFormatter.this.plugin.getDynamicPricingProvider().getStatus(var2, var3, false) : "",
                           "%sellPrice%",
                           ChatColor.stripColor(
                              var2.isASellPricing()
                                 ? var2.getASellPrice().formatSellPrices(null)
                                 : LoreFormatter.this.plugin.formatPrice(var2.getEcoType(), var2.getSellPrice())
                           ),
                           "%sellPriceRaw%",
                           var2.isASellPricing()
                              ? var2.getASellPrice()
                                 .getSellPrices(null, var3, var2.getStackSize(), var2.getSellModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(var0 -> var0.getKey() + ": " + var0.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(var2.getSellPrice(var3, var2.getStackSize(), true, var1)),
                           "%seasonalSellPrice%",
                           var2.isASellPricing()
                              ? var2.getASellPrice().formatSellPrices(null, null, var3, var2.getStackSize(), var2.getSellModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(var2.getEcoType(), var2.getSellPrice(var3, var2.getStackSize(), true, var1))
                        )
                  );
               } else if (var8[1] && CalculateAmount.calculateMultiplier(var3, var5, var2.getSellPrice()) != var2.getSellPrice()) {
                  var4.addAll(
                     this.multipliedSellPrices
                        .get(
                           var6,
                           var7,
                           var3,
                           "%dpp%",
                           var2.isDynamicPricing() && var8[2] ? LoreFormatter.this.plugin.getDynamicPricingProvider().getStatus(var2, var3, false) : "",
                           "%sellPrice%",
                           ChatColor.stripColor(
                              var2.isASellPricing()
                                 ? var2.getASellPrice().formatSellPrices(null)
                                 : LoreFormatter.this.plugin.formatPrice(var2.getEcoType(), var2.getSellPrice())
                           ),
                           "%sellPriceRaw%",
                           var2.isASellPricing()
                              ? var2.getASellPrice()
                                 .getSellPrices(null, var3, var2.getStackSize(), var2.getSellModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(var0 -> var0.getKey() + ": " + var0.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(var2.getSellPrice(var3, var2.getStackSize(), true, var1)),
                           "%multipliedPrice%",
                           var2.isASellPricing()
                              ? var2.getASellPrice().formatSellPrices(null, null, var3, var2.getStackSize(), var2.getSellModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(var2.getEcoType(), var2.getSellPrice(var3, var2.getStackSize(), true, var1))
                        )
                  );
               } else if (var2.isDynamicPricing() && var8[2] && var2.getSellPrice(1, true, var1) != var2.getSellPriceRaw(1)) {
                  var4.addAll(
                     this.dynamicSellPrices
                        .get(
                           var6,
                           var7,
                           var3,
                           "%dpp%",
                           LoreFormatter.this.plugin.getDynamicPricingProvider().getStatus(var2, var3, false),
                           "%sellPrice%",
                           ChatColor.stripColor(
                              var2.isASellPricing()
                                 ? var2.getASellPrice().formatSellPrices(null)
                                 : LoreFormatter.this.plugin.formatPrice(var2.getEcoType(), var2.getSellPrice())
                           ),
                           "%sellPriceRaw%",
                           var2.isASellPricing()
                              ? var2.getASellPrice()
                                 .getSellPrices(null, var3, var2.getStackSize(), var2.getSellModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(var0 -> var0.getKey() + ": " + var0.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(var2.getSellPrice(var3, var2.getStackSize(), true, var1)),
                           "%dynamicSellPrice%",
                           var2.isASellPricing()
                              ? var2.getASellPrice().formatSellPrices(null, null, var3, var2.getStackSize(), var2.getSellModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(var2.getEcoType(), var2.getSellPrice(var3, var2.getStackSize(), true, var1))
                        )
                  );
               } else {
                  var4.addAll(
                     this.sellPrices
                        .get(
                           var6,
                           var7,
                           var3,
                           "%sellPrice%",
                           var2.isASellPricing()
                              ? var2.getASellPrice().formatSellPrices(null, null, var3, var2.getStackSize(), var2.getSellModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(var2.getEcoType(), var2.getSellPrice(var3, var2.getStackSize(), true, var1)),
                           "%sellPriceRaw%",
                           var2.isASellPricing()
                              ? var2.getASellPrice()
                                 .getSellPrices(null, var3, var2.getStackSize(), var2.getSellModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(var0 -> var0.getKey() + ": " + var0.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(var2.getSellPrice(var3, var2.getStackSize(), true, var1))
                        )
                  );
               }

               if (LoreFormatter.this.plugin.MMB) {
                  var4.addAll(this.sellAllLore.get(var6, var7, var3));
               }
            } else {
               var4.addAll(this.disabled.get(var6, var7, var3));
            }
         } else {
            var4.addAll(this.undefined.get(var6, var7, var3));
         }
      }
   }

   private final class ShopLore implements LoreFormatter.Lore {
      private final List<String> lore = new ArrayList<>();
      private final ItemStack stack;

      public ShopLore(ItemStack nullx) {
         this.stack = nullx;
      }

      @Override
      public void add(String var1) {
         this.lore.add(var1);
      }

      public void add(String var1, int var2) {
         this.lore.add(var2, var1);
      }

      @Override
      public void addAll(String[] var1) {
         for (String var5 : var1) {
            this.add(var5);
         }
      }

      @Override
      public ItemStack form() {
         ItemMeta var1 = this.stack.getItemMeta();
         var1.setLore(this.lore);
         this.stack.setItemMeta(var1);
         return this.stack;
      }
   }

   private final class SpecialLore implements LoreFormatter.DisplayLore {
      private final LoreFormatter.DynamicLoreList itemPerms;
      private final LoreFormatter.DynamicLoreList itemPermsRank;
      private final LoreFormatter.DynamicLoreList alreadyPurchased;
      private final LoreFormatter.DynamicLoreList seasonalPrice;
      private final boolean after;

      public SpecialLore(ConfigurationSection nullx, boolean nullxx) {
         this.after = nullxx;
         this.itemPerms = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(nullx, "missing-item-permission.without-rank"), Lang.MISSING_REQUIRED_ITEM_PERMISSIONS.get().toString()
         );
         this.itemPermsRank = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(nullx, "missing-item-permission.with-rank"), Lang.MISSING_REQUIRED_ITEM_PERMISSIONS_RANK.get().toString()
         );
         this.alreadyPurchased = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(nullx, "already-purchased"), Lang.ALREADY_PURCHASED_ITEM.get().toString()
         );
         this.seasonalPrice = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(nullx, "seasonal-pricing"), Lang.ITEM_SEASONAL_PRICE.get().toString(), "%season-icon%", "%season-name%"
         );
      }

      @Override
      public void apply(ShopItem var1, Player var2, LoreFormatter.Lore var3, String var4, boolean var5, boolean var6, boolean... var7) {
         if (var7[0]) {
            var3.addAll(
               this.seasonalPrice
                  .get(
                     var5,
                     var6,
                     var2,
                     "%season-icon%",
                     SeasonProvider.getSeason(var2.getWorld().getName()).getIcon(),
                     "%season-name%",
                     SeasonProvider.getSeason(var2.getWorld().getName()).getName()
                  )
            );
         }

         if (!var1.isPermRequirement()) {
            if (var7[1]) {
               var3.addAll(this.alreadyPurchased.get(var5, var6, var2));
            }
         } else if (!var7[1]) {
            var3.addAll(this.getMissingRequiredPerms(var2, var1.getPermission(), var4, var5, var6));
         }
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex var1, ShopItem var2, Player var3, LoreFormatter.Lore var4, String var5, boolean var6, boolean var7, boolean... var8
      ) {
      }

      private String[] getMissingRequiredPerms(Player var1, String var2, String var3, boolean var4, boolean var5) {
         if (LoreFormatter.this.plugin.getPermissionManager().getProvider() != null && LoreFormatter.this.plugin.permissionRankName) {
            String var6 = LoreFormatter.this.plugin.getPermissionManager().getProvider().getGroupName("OnyxShop.shop." + var3 + ".item." + var2);
            if (var6 != null) {
               return this.itemPermsRank.get(var4, var5, var1, "%rank_name%", var6);
            }
         }

         return this.itemPerms.get(var4, var5, var1);
      }
   }

   public final class StaticLore {
      public final LoreFormatter.LoreType type;
      public final List<String> lore;

      public StaticLore(LoreFormatter.LoreType nullx, String[] nullxx) {
         this.type = nullx;
         this.lore = nullxx == null && nullx == LoreFormatter.LoreType.ITEM_LORE
            ? null
            : LoreFormatter.this.translate(
               nullx != LoreFormatter.LoreType.BUY_LORE && nullx != LoreFormatter.LoreType.SELL_LORE
                  ? Arrays.asList(nullxx)
                  : Arrays.stream(nullxx).map(var0 -> var0.replace("%dpp%", "").replace("%dps%", "")).collect(Collectors.toList())
            );
      }

      public StaticLore(LoreFormatter.LoreType nullx, List<String> nullxx) {
         this.type = nullx;
         this.lore = nullxx == null && nullx == LoreFormatter.LoreType.ITEM_LORE
            ? null
            : (
               nullx != LoreFormatter.LoreType.BUY_LORE && nullx != LoreFormatter.LoreType.SELL_LORE
                  ? LoreFormatter.this.translate(nullxx)
                  : LoreFormatter.this.translate(nullxx).stream().map(var0 -> var0.replace("%dpp%", "").replace("%dps%", "")).collect(Collectors.toList())
            );
      }
   }

   private final class StockLore implements LoreFormatter.DisplayLore {
      private final LoreFormatter.DynamicLoreList currentStock;
      private final LoreFormatter.DynamicLoreList outOfStock;
      private final LoreFormatter.DynamicLoreList restocking;
      private final boolean after;

      public StockLore(ConfigurationSection nullx, boolean nullxx) {
         this.after = nullxx;
         this.currentStock = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(nullx, "current-stock"), Lang.CURRENT_ITEM_STOCK.get().toString(), "%itemStock%"
         );
         this.outOfStock = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(nullx, "out-of-stock"), Lang.ITEM_OUT_OF_STOCK.get().toString()
         );
         this.restocking = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(nullx, "restocking"), Lang.RESTOCKING_ITEM.get().toString(), "%time%"
         );
      }

      @Override
      public void apply(ShopItem var1, Player var2, LoreFormatter.Lore var3, String var4, boolean var5, boolean var6, boolean... var7) {
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex var1, ShopItem var2, Player var3, LoreFormatter.Lore var4, String var5, boolean var6, boolean var7, boolean... var8
      ) {
         int var9 = LoreFormatter.this.plugin.getLimitedStockProvider().getItemStock(var2, var3.getUniqueId(), true, true, var1);
         if (var9 >= 0) {
            var4.addAll(this.currentStock.get(var6, var7, var3, "%itemStock%", String.valueOf(var9)));
         } else if (var9 == -1) {
            var4.addAll(
               this.restocking.get(var6, var7, var3, "%time%", LoreFormatter.this.plugin.getLimitedStockProvider().getTimeLeftToRestock(var2.getItemPath()))
            );
         } else if (var9 == -2) {
            var4.addAll(
               this.restocking
                  .get(
                     var6,
                     var7,
                     var3,
                     "%time%",
                     LoreFormatter.this.plugin.getLimitedStockProvider().getTimeLeftToRestock(var3.getUniqueId(), var2.getItemPath())
                  )
            );
         } else if (var9 != -3 && var9 != -4) {
            var4.addAll(this.currentStock.get(var6, var7, var3, "%itemStock%", String.valueOf(var9)));
         } else {
            var4.addAll(this.outOfStock.get(var6, var7, var3));
         }
      }
   }
}
