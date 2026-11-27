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

   public LoreFormatter(OnyxShop plugin) {
      this.plugin = plugin;
      this.components = plugin.paperMeta;
      if (this.plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
         this.plugin.runTaskLaterAsync(this::init, 5L);
      } else {
         this.init();
      }
   }

   private void init() {
      List<String> stringList = ConfigManager.getConfig().getStringList("lore-arrangement");
      int index = stringList.indexOf("item-lore");
      if (index == -1) {
         SendMessage.warnMessage("Failed to load lore arrangement, cannot find 'item-lore' which is required!");
         stringList = ConfigManager.getConfig().getDef().getStringList("lore-arrangement");
         index = stringList.indexOf("item-lore");
      }

      String lore = "display-lore-layout.";
      ArrayList<LoreFormatter.DisplayLore> values = new ArrayList<>();

      for (String text : stringList) {
         int value = stringList.indexOf(text);
         ConfigurationSection section = null;
         if (!text.equals("item-lore")) {
            section = ConfigManager.getConfig().getConfigurationSection(lore + text);
            if (section == null) {
               SendMessage.warnMessage("Cannot find lore format for %part% inside config.yml, using default...".replace("%part%", text));
               section = ConfigManager.getConfig().getDef().getConfigurationSection(lore + text);
            }
         }

         switch (text) {
            case "special-lore":
               values.add(new LoreFormatter.SpecialLore(section, value > index));
               break;
            case "item-requirements":
               values.add(new LoreFormatter.RequirementLore());
               break;
            case "buy-prices":
               values.add(new LoreFormatter.BuyLore(section, value > index));
               break;
            case "sell-prices":
               values.add(new LoreFormatter.SellLore(section, value > index));
               break;
            case "decoration-lore":
               values.add(new LoreFormatter.DecorationLore(section));
               break;
            case "item-lore":
               this.item = value;
               values.add(new LoreFormatter.ItemLore());
               break;
            case "item-stock":
               values.add(new LoreFormatter.StockLore(section, value > index));
               break;
            case "sell-limit":
               values.add(new LoreFormatter.LimitLore(section, value > index));
               break;
            case "admin-lore":
               values.add(new LoreFormatter.AdminLore(section, value > index));
         }
      }

      this.displayLore = values;
      ArrayList values2 = new ArrayList();

      for (String text : stringList) {
         ConfigurationSection section = ConfigManager.getConfig().getConfigurationSection(lore + text);
         if (section == null && !text.equals("item-lore")) {
            SendMessage.warnMessage("Cannot find lore format for %part% inside config.yml, using default...".replace("%part%", text));
            section = ConfigManager.getConfig().getDef().getConfigurationSection(lore + text);
         }

         switch (text) {
            case "buy-prices":
               values2.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.BUY_LORE, section.getStringList("normal")));
               break;
            case "sell-prices":
               values2.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.SELL_LORE, section.getStringList("normal")));
               if (this.plugin.MMB && !Lang.SHIFT_RIGHT_CLICK_SELL_ALL.get().getLegacy().isEmpty()) {
                  values2.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.SELL_ALL_LORE, section.getStringList("sell-all")));
               }
               break;
            case "decoration-lore":
               for (String sectionId : ConfigManager.getConfig().getConfigurationSection(lore + text).getKeys(false)) {
                  List stringList2 = ConfigManager.getConfig().getStringList(lore + text + "." + sectionId);
                  if (!stringList2.isEmpty()) {
                     switch (sectionId) {
                        case "extra-lore":
                           values2.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.DECORATION_LORE, stringList2));
                           break;
                        case "extra-buy-lore":
                           values2.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.DECORATION_LORE_BUY, stringList2));
                           break;
                        case "extra-sell-all-lore":
                           if (this.plugin.MMB && !Lang.SHIFT_RIGHT_CLICK_SELL_ALL.get().getLegacy().isEmpty()) {
                              values2.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.DECORATION_LORE_SELL_ALL, stringList2));
                           }
                           break;
                        case "extra-sell-lore":
                           values2.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.DECORATION_LORE_SELL, stringList2));
                     }
                  }
               }
               break;
            case "item-lore":
               values2.add(new LoreFormatter.StaticLore(LoreFormatter.LoreType.ITEM_LORE, (List<String>)null));
         }
      }

      this.itemLore = values2;
      this.actionAdminLore = new LoreFormatter.ActionAdminLore(ConfigManager.getConfig().getConfigurationSection(lore + ".admin-lore"), false);
   }

   public ItemStack format(
      ActionItem actionItem, ItemStack itemStack, Player player, EcoType ecoType, String itemId, boolean flag, boolean flag2, boolean flag3, boolean flag4, int value, int value2
   ) {
      if (actionItem.hasPlaceholders() && actionItem.getPlaceholders().containsKey(-1)) {
         itemStack = this.setDisplayname(itemStack, actionItem.getPlaceholders().get(-1), player, actionItem, ecoType, flag, flag2, value, value2);
      }

      Object value3 = this.components
         ? (flag ? new LoreFormatter.ComponentLore(itemStack) : new LoreFormatter.RawComponentLore(itemStack))
         : (flag && !flag2 ? new LoreFormatter.FastLore(itemStack) : new LoreFormatter.ShopLore(itemStack));
      if (!actionItem.isHideIfUnmet() && actionItem.getRequirements() != null) {
         for (ItemRequirement itemRequirement : actionItem.getRequirements()) {
            if (!itemRequirement.isMet(player)) {
               ((LoreFormatter.Lore)value3).addAll(itemRequirement.getLore(player, flag, flag2));
            }
         }
      }

      ((LoreFormatter.Lore)value3).addAll(this.parsePlaceholders(actionItem, player, ecoType, flag, flag2, value, value2));
      this.actionAdminLore.apply(actionItem, player, (LoreFormatter.Lore)value3, itemId, flag, flag2, flag3, flag4);
      return ((LoreFormatter.Lore)value3).form();
   }

   public ItemStack format(
      LoreFormatter.PageIndex pageIndex,
      ShopItem shopItem,
      ItemStack itemStack,
      Player player,
      String text,
      boolean flag,
      boolean flag2,
      boolean flag3,
      boolean flag4,
      boolean flag5,
      boolean flag6,
      boolean flag7,
      boolean flag8,
      boolean flag9,
      boolean flag10,
      boolean flag11,
      boolean flag12,
      int value,
      int value2
   ) {
      if (shopItem.hasPlaceholders() && shopItem.getPlaceholders().containsKey(-1)) {
         itemStack = this.setDisplayname(itemStack, shopItem.getPlaceholders().get(-1), player, shopItem, flag4, flag5, flag12, flag, flag2, value, value2);
      }

      Object value3 = this.components
         ? (flag ? new LoreFormatter.ComponentLore(itemStack) : new LoreFormatter.RawComponentLore(itemStack))
         : (flag && !flag2 && !flag3 ? new LoreFormatter.FastLore(itemStack) : new LoreFormatter.ShopLore(itemStack));

      for (LoreFormatter.DisplayLore displayLore : shopItem.isPriorLore() ? this.priorLore() : this.displayLore) {
         if (displayLore instanceof LoreFormatter.ItemLore) {
            ((LoreFormatter.Lore)value3).addAll(this.parsePlaceholders(shopItem, player, flag, flag2 || flag3, flag4, flag5, flag12, value, value2));
         } else if (displayLore instanceof LoreFormatter.RequirementLore) {
            if (!shopItem.isHideIfUnmet() && shopItem.getRequirements() != null) {
               for (ItemRequirement itemRequirement : shopItem.getRequirements()) {
                  if (!itemRequirement.isMet(player)) {
                     ((LoreFormatter.Lore)value3).addAll(itemRequirement.getLore(player, flag, flag2 || flag3));
                  }
               }
            }
         } else if (!(displayLore instanceof LoreFormatter.BuyLore) && !(displayLore instanceof LoreFormatter.SellLore)) {
            if (displayLore instanceof LoreFormatter.AdminLore) {
               displayLore.apply(shopItem, player, (LoreFormatter.Lore)value3, text, flag, flag2 || flag3, flag8, flag9);
            } else if (displayLore instanceof LoreFormatter.SpecialLore) {
               displayLore.apply(shopItem, player, (LoreFormatter.Lore)value3, text, flag, flag2 || flag3, flag12, flag10);
            } else if (displayLore instanceof LoreFormatter.StockLore) {
               if (shopItem.getLimitedStockMode() != 0 && !shopItem.isDisplayItem() && !shopItem.isHideUtilLore() && flag6) {
                  displayLore.apply(pageIndex, shopItem, player, (LoreFormatter.Lore)value3, text, flag, flag2 || flag3);
               }
            } else if (displayLore instanceof LoreFormatter.LimitLore) {
               if (shopItem.getLimitedSellMode() != 0 && !shopItem.isDisplayItem() && !shopItem.isHideUtilLore() && flag7) {
                  displayLore.apply(pageIndex, shopItem, player, (LoreFormatter.Lore)value3, text, flag, flag2 || flag3);
               }
            } else if (displayLore instanceof LoreFormatter.DecorationLore) {
               displayLore.apply(shopItem, player, (LoreFormatter.Lore)value3, text, flag, flag2, flag3);
            }
         } else if (!shopItem.isHidePricingLore() && !shopItem.hasItemError() && !shopItem.isDisplayItem()) {
            displayLore.apply(pageIndex, shopItem, player, (LoreFormatter.Lore)value3, text, flag, flag2 || flag3, flag4, flag5, flag11, flag12);
         }
      }

      return ((LoreFormatter.Lore)value3).form();
   }

   private List<LoreFormatter.DisplayLore> priorLore() {
      ArrayList values = new ArrayList<>(this.displayLore);
      values.add(0, values.remove(this.item));
      return values;
   }

   public List<LoreFormatter.StaticLore> getItemLore() {
      return this.itemLore;
   }

   private void addLore(ShopItem shopItem, LoreFormatter.FastLore fastLore, String lore, int value, boolean flag) {
      if (flag) {
         this.plugin.versionHandler.addItemLore(fastLore, lore);
      } else {
         this.plugin.versionHandler.addItemLore(fastLore, lore, value);
      }
   }

   private ItemStack setDisplayname(
      ItemStack itemStack, Placeholder placeholder, Player player, ShopItem shopItem, boolean flag, boolean flag2, boolean flag3, boolean flag4, boolean flag5, int value, int value2
   ) {
      String text = placeholder.translate(
         player, shopItem.isPriceLore() ? this.pricePlaceholders(placeholder.getRaw(), shopItem, player, flag, flag2, flag3, value, value2) : new Replacement[0]
      );
      if (flag4 && !this.components && !flag5) {
         return this.plugin.versionHandler.setDisplayname(itemStack, this.plugin.versionHandler.toNBT(text));
      } else {
         ItemMeta itemMeta = itemStack.getItemMeta();
         this.plugin.getMetaUtils().setRawDisplayName(itemMeta, text);
         itemStack.setItemMeta(itemMeta);
         return itemStack;
      }
   }

   private ItemStack setDisplayname(
      ItemStack itemStack, Placeholder placeholder, Player player, ActionItem actionItem, EcoType ecoType, boolean flag, boolean flag2, int value, int value2
   ) {
      String text = placeholder.translate(player, this.actionPlaceholders(actionItem, player, ecoType, value, value2));
      if (!this.components && flag && !flag2) {
         return this.plugin.versionHandler.setDisplayname(itemStack, this.plugin.versionHandler.toNBT(text));
      } else {
         ItemMeta itemMeta = itemStack.getItemMeta();
         this.plugin.getMetaUtils().setRawDisplayName(itemMeta, text);
         itemStack.setItemMeta(itemMeta);
         return itemStack;
      }
   }

   private LoreFormatter.DynamicLoreList getDynamicLoreList(List<String> lores, String text, String... values) {
      return (LoreFormatter.DynamicLoreList)(this.components
         ? new LoreFormatter.DynamicCompLoreList(lores, text, values)
         : new LoreFormatter.DynamicBukkitLoreList(lores, text, values));
   }

   private static LoreFormatter.LocalizedLore getLocalized(Map<Locale, LoreFormatter.LocalizedLore> map, Player player) {
      LoreFormatter.LocalizedLore localizedLore = (LoreFormatter.LocalizedLore)map.get(UserManager.getUser(player).getLocale());
      return localizedLore != null ? localizedLore : (LoreFormatter.LocalizedLore)map.get(Lang.getDefaultLocale());
   }

   private List<String> getStringList(ConfigurationSection section, String sectionId) {
      return (List<String>)(section.get(sectionId) instanceof List ? section.getStringList(sectionId) : new ArrayList<>(Collections.singletonList(section.getString(sectionId))));
   }

   private boolean isEmpty(List<String> values) {
      return values.stream().allMatch(String::isEmpty);
   }

   private String translate(String text) {
      return this.translate(text, Lang.getDefaultLocale());
   }

   private String translate(String text, Locale locale) {
      if (text == null) {
         return "";
      } else {
         while (text.contains("%translations-")) {
            try {
               Lang lang = Lang.valueOf(text.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
               text = text.replace("%translations-" + lang.getKey() + "%", lang.getRaw(locale));
            } catch (IllegalArgumentException exception) {
            }
         }

         return this.components ? ChatUtil.getAdventureUtils().formatLegacyToMini(text) : ChatUtil.formatColors(text);
      }
   }

   private List<String> translate(List<String> values) {
      return this.translate(values, Lang.getDefaultLocale());
   }

   private List<String> translate(List<String> values, Locale locale) {
      if (values.isEmpty()) {
         return values;
      } else {
         ArrayList values2 = new ArrayList();

         for (String text : values) {
            while (text.contains("%translations-")) {
               try {
                  Lang lang = Lang.valueOf(text.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
                  text = text.replace("%translations-" + lang.getKey() + "%", lang.getRaw(locale).replace("\\n", "\n"));
               } catch (IllegalArgumentException exception) {
               }
            }

            for (String text2 : text.split("\n")) {
               values2.add(this.components ? ChatUtil.getAdventureUtils().formatLegacyToMini(text2) : ChatUtil.formatColors(text2));
            }
         }

         return values2;
      }
   }

   private String getBuyPrice(String price, ShopItem shopItem, Player player, boolean flag, boolean flag2) {
      if (shopItem.isBuyAble()) {
         if (!flag2 && !flag) {
            return shopItem.isABuyPricing()
               ? shopItem.getABuyPrice().formatBuyPrices(price.split("%buyPrice%")[0], null, shopItem.getStackSize(), true)
               : this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getBuyPrice(shopItem.getStackSize(), true));
         } else {
            return shopItem.isABuyPricing()
               ? shopItem.getABuyPrice().formatBuyPrices(price.split("%buyPrice%")[0], null, player, shopItem.getStackSize(), shopItem.getBuyModifiers(), true)
               : this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getBuyPrice(player, shopItem.getStackSize(), true));
         }
      } else {
         return Lang.ITEM_CANNOT_BE_BOUGHT.get(player).getLegacy();
      }
   }

   private String getSellPrice(String price, ShopItem shopItem, Player player, boolean flag, boolean flag2) {
      if (shopItem.isSellAble()) {
         if (!flag2 && !flag) {
            return shopItem.isASellPricing()
               ? shopItem.getASellPrice().formatSellPrices(price.split("%sellPrice%")[0], null, shopItem.getStackSize(), true)
               : this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getSellPrice(shopItem.getStackSize(), true));
         } else {
            return shopItem.isASellPricing()
               ? shopItem.getASellPrice().formatSellPrices(price.split("%sellPrice%")[0], null, player, shopItem.getStackSize(), shopItem.getSellModifiers(), true)
               : this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getSellPrice(player, shopItem.getStackSize(), true));
         }
      } else {
         return Lang.ITEM_CANNOT_BE_SOLD.get(player).getLegacy();
      }
   }

   private String getBuyPriceRaw(ShopItem shopItem, Player player, boolean flag, boolean flag2) {
      if (shopItem.isBuyAble()) {
         if (!flag2 && !flag) {
            return shopItem.isABuyPricing()
               ? shopItem.getABuyPrice()
                  .getBuyPrices(null, shopItem.getStackSize(), true)
                  .entrySet()
                  .stream()
                  .map(entry -> entry.getKey() + ": " + entry.getValue())
                  .collect(Collectors.joining(", "))
               : String.valueOf(shopItem.getBuyPrice(shopItem.getStackSize(), true));
         } else {
            return shopItem.isABuyPricing()
               ? shopItem.getABuyPrice()
                  .getBuyPrices(null, player, shopItem.getStackSize(), shopItem.getBuyModifiers(), true)
                  .entrySet()
                  .stream()
                  .map(entry -> entry.getKey() + ": " + entry.getValue())
                  .collect(Collectors.joining(", "))
               : String.valueOf(shopItem.getBuyPrice(player, shopItem.getStackSize(), true));
         }
      } else {
         return "-1";
      }
   }

   private String getSellPriceRaw(ShopItem shopItem, Player player, boolean flag, boolean flag2) {
      if (shopItem.isSellAble()) {
         if (!flag2 && !flag) {
            return shopItem.isASellPricing()
               ? shopItem.getASellPrice()
                  .getSellPrices(null, shopItem.getStackSize(), true)
                  .entrySet()
                  .stream()
                  .map(entry -> entry.getKey() + ": " + entry.getValue())
                  .collect(Collectors.joining(", "))
               : String.valueOf(shopItem.getSellPrice(shopItem.getStackSize(), true));
         } else {
            return shopItem.isASellPricing()
               ? shopItem.getASellPrice()
                  .getSellPrices(null, player, shopItem.getStackSize(), shopItem.getSellModifiers(), true)
                  .entrySet()
                  .stream()
                  .map(entry -> entry.getKey() + ": " + entry.getValue())
                  .collect(Collectors.joining(", "))
               : String.valueOf(shopItem.getSellPrice(player, shopItem.getStackSize(), true));
         }
      } else {
         return "-1";
      }
   }

   private Replacement[] pricePlaceholders(String price, ShopItem shopItem, Player player, boolean flag, boolean flag2, boolean flag3, int value, int value2) {
      return new Replacement[]{
         new Replacement("%buyPrice%", () -> this.getBuyPrice(price, shopItem, player, flag, flag3)),
         new Replacement("%sellPrice%", () -> this.getSellPrice(price, shopItem, player, flag2, flag3)),
         new Replacement("%buyPriceRaw%", () -> this.getBuyPriceRaw(shopItem, player, flag, flag3)),
         new Replacement("%sellPriceRaw%", () -> this.getSellPriceRaw(shopItem, player, flag2, flag3)),
         new Replacement("%player_displayname%", () -> this.plugin.getDisplayName(player)),
         new Replacement(
            "%player_balance%", () -> this.plugin.formatPrice(shopItem.getEcoType(), this.plugin.getEcoHandler().getEcon(shopItem.getEcoType()).getBalance(player))
         ),
         new Replacement("%page%", () -> String.valueOf(value)),
         new Replacement("%pages%", () -> String.valueOf(value2))
      };
   }

   private String[] parsePlaceholders(ShopItem shopItem, Player player, boolean flag, boolean flag2, boolean flag3, boolean flag4, boolean flag5, int value, int value2) {
      String[] lores = shopItem.getLore().get(flag, flag2);

      for (Entry<Integer, Placeholder> entry : shopItem.getPlaceholders().entrySet()) {
         if ((Integer)entry.getKey() != -1) {
            lores[entry.getKey()] = ((Placeholder)entry.getValue())
               .translate(
                  lores[entry.getKey()],
                  player,
                  shopItem.isPriceLore()
                     ? this.pricePlaceholders(((Placeholder)entry.getValue()).getRaw(), shopItem, player, flag3, flag4, flag5, value, value2)
                     : new Replacement[0]
               );
         }
      }

      return lores;
   }

   private Replacement[] actionPlaceholders(ActionItem actionItem, Player player, EcoType ecoType, int value, int value2) {
      return new Replacement[]{
         new Replacement("%player_displayname%", () -> this.plugin.getDisplayName(player)),
         new Replacement("%player_balance%", () -> this.plugin.formatPrice(ecoType, this.plugin.getEcoHandler().getEcon(ecoType).getBalance(player))),
         new Replacement("%page%", () -> String.valueOf(value)),
         new Replacement("%pages%", () -> String.valueOf(value2))
      };
   }

   private String[] parsePlaceholders(ActionItem actionItem, Player player, EcoType ecoType, boolean flag, boolean flag2, int value, int value2) {
      String[] lores = actionItem.getLore().get(flag, flag2);

      for (Entry<Integer, Placeholder> entry : actionItem.getPlaceholders().entrySet()) {
         if ((Integer)entry.getKey() != -1) {
            lores[entry.getKey()] = ((Placeholder)entry.getValue()).translate(lores[entry.getKey()], player, this.actionPlaceholders(actionItem, player, ecoType, value, value2));
         }
      }

      return lores;
   }

   private final class ActionAdminLore {
      private final LoreFormatter.DynamicLoreList itemEditor;
      private final LoreFormatter.DynamicLoreList itemIndex;
      private final boolean after;

      public ActionAdminLore(ConfigurationSection section, boolean flag) {
         this.after = flag;
         this.itemEditor = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(section, "item-editor"), Lang.SHIFT_CLICK_ITEM_EDITOR.get().toString()
         );
         this.itemIndex = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(section, "item-index"), Lang.ITEM_LOCATION_IN_SHOP.get().toString(), "%shopsection%", "%itemLoc%"
         );
      }

      public void apply(ActionItem actionItem, Player player, LoreFormatter.Lore lore, String itemId, boolean flag, boolean flag2, boolean... playerNames) {
         if (playerNames[0]) {
            lore.addAll(this.itemEditor.get(flag, flag2, player));
         }

         if (playerNames[1]) {
            lore.addAll(this.itemIndex.get(flag, flag2, player, "%shopsection%", itemId, "%itemLoc%", actionItem.itemLoc));
         }
      }
   }

   private final class AdminLore implements LoreFormatter.DisplayLore {
      private final LoreFormatter.DynamicLoreList itemEditor;
      private final LoreFormatter.DynamicLoreList itemIndex;
      private final boolean after;

      public AdminLore(ConfigurationSection section, boolean flag) {
         this.after = flag;
         this.itemEditor = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(section, "item-editor"), Lang.SHIFT_CLICK_ITEM_EDITOR.get().toString()
         );
         this.itemIndex = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(section, "item-index"), Lang.ITEM_LOCATION_IN_SHOP.get().toString(), "%shopsection%", "%itemLoc%"
         );
      }

      @Override
      public void apply(ShopItem shopItem, Player player, LoreFormatter.Lore lore, String shopId, boolean flag, boolean flag2, boolean... itemIds) {
         if (itemIds[0]) {
            lore.addAll(this.itemEditor.get(flag, flag2, player));
         }

         if (itemIds[1]) {
            lore.addAll(this.itemIndex.get(flag, flag2, player, "%shopsection%", shopId, "%itemLoc%", shopItem.itemLoc));
         }
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex pageIndex, ShopItem shopItem, Player player, LoreFormatter.Lore lore, String lore2, boolean flag, boolean flag2, boolean... shopIds
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

      public BuyLore(ConfigurationSection section, boolean flag) {
         this.after = flag;
         this.disabled = LoreFormatter.this.getDynamicLoreList(section.getStringList("disabled"), Lang.ITEM_CANNOT_BE_BOUGHT.get().toString());
         this.undefined = LoreFormatter.this.getDynamicLoreList(section.getStringList("undefined"), "");
         this.buyPrices = LoreFormatter.this.getDynamicLoreList(
            section.getStringList("normal"), Lang.LEFT_CLICK_BUY.get().toString(), "%buyPrice%", "%buyPriceRaw%"
         );
         this.dynamicBuyPrices = LoreFormatter.this.getDynamicLoreList(
            section.getStringList("dynamic-price"), Lang.DYNAMIC_BUY_PRICE.get().toString(), "%dpp%", "%buyPrice%", "%buyPriceRaw%", "%dynamicBuyPrice%"
         );
         this.seasonalBuyPrices = LoreFormatter.this.getDynamicLoreList(
            section.getStringList("seasonal"), Lang.SEASONAL_BUY_PRICE.get().toString(), "%dpp%", "%buyPrice%", "%buyPriceRaw%", "%seasonalBuyPrice%"
         );
         this.discountedBuyPrices = LoreFormatter.this.getDynamicLoreList(
            section.getStringList("discounted"), Lang.DISCOUNTED_BUY_PRICE.get().toString(), "%dpp%", "%buyPrice%", "%buyPriceRaw%", "%discountedPrice%"
         );
      }

      @Override
      public void apply(ShopItem shopItem, Player player, LoreFormatter.Lore lore, String shopId, boolean flag, boolean flag2, boolean... itemIds) {
         this.apply(null, shopItem, player, lore, shopId, flag, flag2, itemIds);
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex pageIndex, ShopItem shopItem, Player player, LoreFormatter.Lore lore, String lore2, boolean flag, boolean flag2, boolean... shopIds
      ) {
         if (shopItem.isBuyLore()) {
            if (shopItem.isBuyAble()) {
               if (shopIds[3]) {
                  lore.addAll(
                     this.seasonalBuyPrices
                        .get(
                           flag,
                           flag2,
                           player,
                           "%dpp%",
                           shopItem.isDynamicPricing() && shopIds[2] ? LoreFormatter.this.plugin.getDynamicPricingProvider().getStatus(shopItem, player, true) : "",
                           "%buyPrice%",
                           ChatColor.stripColor(
                              shopItem.isABuyPricing()
                                 ? shopItem.getABuyPrice().formatBuyPrices(null)
                                 : LoreFormatter.this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getBuyPrice())
                           ),
                           "%buyPriceRaw%",
                           shopItem.isABuyPricing()
                              ? shopItem.getABuyPrice()
                                 .getBuyPrices(null, player, shopItem.getStackSize(), shopItem.getBuyModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(entry -> entry.getKey() + ": " + entry.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(shopItem.getBuyPrice(player, shopItem.getStackSize(), true, pageIndex)),
                           "%seasonalBuyPrice%",
                           shopItem.isABuyPricing()
                              ? shopItem.getABuyPrice().formatBuyPrices(null, null, player, shopItem.getStackSize(), shopItem.getBuyModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getBuyPrice(player, shopItem.getStackSize(), true, pageIndex))
                        )
                  );
               } else if (shopIds[0] && CalculateAmount.calculateDiscount(player, lore2, shopItem.getBuyPrice()) != shopItem.getBuyPrice()) {
                  lore.addAll(
                     this.discountedBuyPrices
                        .get(
                           flag,
                           flag2,
                           player,
                           "%dpp%",
                           shopItem.isDynamicPricing() && shopIds[2] ? LoreFormatter.this.plugin.getDynamicPricingProvider().getStatus(shopItem, player, true) : "",
                           "%buyPrice%",
                           ChatColor.stripColor(
                              shopItem.isABuyPricing()
                                 ? shopItem.getABuyPrice().formatBuyPrices(null)
                                 : LoreFormatter.this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getBuyPrice())
                           ),
                           "%buyPriceRaw%",
                           shopItem.isABuyPricing()
                              ? shopItem.getABuyPrice()
                                 .getBuyPrices(null, player, shopItem.getStackSize(), shopItem.getBuyModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(entry -> entry.getKey() + ": " + entry.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(shopItem.getBuyPrice(player, shopItem.getStackSize(), true, pageIndex)),
                           "%discountedPrice%",
                           shopItem.isABuyPricing()
                              ? shopItem.getABuyPrice().formatBuyPrices(null, null, player, shopItem.getStackSize(), shopItem.getBuyModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getBuyPrice(player, shopItem.getStackSize(), true, pageIndex))
                        )
                  );
               } else if (shopItem.isDynamicPricing() && shopIds[2] && shopItem.getBuyPrice(1, true, pageIndex) != shopItem.getBuyPriceRaw(1)) {
                  lore.addAll(
                     this.dynamicBuyPrices
                        .get(
                           flag,
                           flag2,
                           player,
                           "%dpp%",
                           LoreFormatter.this.plugin.getDynamicPricingProvider().getStatus(shopItem, player, true),
                           "%buyPrice%",
                           ChatColor.stripColor(
                              shopItem.isABuyPricing()
                                 ? shopItem.getABuyPrice().formatBuyPrices(null)
                                 : LoreFormatter.this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getBuyPrice())
                           ),
                           "%buyPriceRaw%",
                           shopItem.isABuyPricing()
                              ? shopItem.getABuyPrice()
                                 .getBuyPrices(null, player, shopItem.getStackSize(), shopItem.getBuyModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(entry -> entry.getKey() + ": " + entry.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(shopItem.getBuyPrice(player, shopItem.getStackSize(), true, pageIndex)),
                           "%dynamicBuyPrice%",
                           shopItem.isABuyPricing()
                              ? shopItem.getABuyPrice().formatBuyPrices(null, null, player, shopItem.getStackSize(), shopItem.getBuyModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getBuyPrice(player, shopItem.getStackSize(), true, pageIndex))
                        )
                  );
               } else {
                  lore.addAll(
                     this.buyPrices
                        .get(
                           flag,
                           flag2,
                           player,
                           "%buyPrice%",
                           shopItem.isABuyPricing()
                              ? shopItem.getABuyPrice().formatBuyPrices(null, null, player, shopItem.getStackSize(), shopItem.getBuyModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getBuyPrice(player, shopItem.getStackSize(), true, pageIndex)),
                           "%buyPriceRaw%",
                           shopItem.isABuyPricing()
                              ? shopItem.getABuyPrice()
                                 .getBuyPrices(null, player, shopItem.getStackSize(), shopItem.getBuyModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(entry -> entry.getKey() + ": " + entry.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(shopItem.getBuyPrice(player, shopItem.getStackSize(), true, pageIndex))
                        )
                  );
               }
            } else {
               lore.addAll(this.disabled.get(flag, flag2, player));
            }
         } else {
            lore.addAll(this.undefined.get(flag, flag2, player));
         }
      }
   }

   private final class ComponentLore implements LoreFormatter.Lore {
      private final List<String> lore = new ArrayList<>();
      private final ItemStack item;

      public ComponentLore(ItemStack itemStack) {
         this.item = itemStack;
      }

      @Override
      public void addAll(String[] values) {
         this.lore.addAll(Arrays.asList(values));
      }

      @Override
      public void add(String text) {
         this.lore.add(text);
      }

      @Override
      public ItemStack form() {
         try {
            return (ItemStack)ReflectUtil.invokeStaticLegacyMethod(
               "setLore", LoreFormatter.this.plugin.getMetaUtils().getClass(), new Class[]{ItemStack.class, ArrayList.class}, this.item, this.lore
            );
         } catch (Exception exception) {
            SendMessage.warnMessage("Failed to display component lore on shop item");
            exception.printStackTrace();
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

      public DecorationLore(ConfigurationSection section) {
         this.lore = LoreFormatter.this.getDynamicLoreList(section.getStringList("extra-lore"), "");
         this.buyLore = LoreFormatter.this.getDynamicLoreList(section.getStringList("extra-buy-lore"), "");
         this.sellLore = LoreFormatter.this.getDynamicLoreList(section.getStringList("extra-sell-lore"), "");
         this.sellAllLore = LoreFormatter.this.getDynamicLoreList(section.getStringList("extra-sell-all-lore"), "");
         this.manageLore = LoreFormatter.this.getDynamicLoreList(section.getStringList("extra-view-lore"), "");
      }

      @Override
      public void apply(ShopItem shopItem, Player player, LoreFormatter.Lore lore, String shopId, boolean flag, boolean flag2, boolean... itemIds) {
         if (shopItem.isDecorationLore()) {
            lore.addAll(this.lore.get(flag, flag2 || itemIds[0], player));
            if (!itemIds[0]) {
               if (shopItem.isBuyAble() && shopItem.isBuyLore()) {
                  lore.addAll(this.buyLore.get(flag, flag2 || itemIds[0], player));
               }

               if (shopItem.isSellAble() && shopItem.isSellLore()) {
                  if (LoreFormatter.this.plugin.MMB) {
                     lore.addAll(this.sellAllLore.get(flag, flag2 || itemIds[0], player));
                  }

                  lore.addAll(this.sellLore.get(flag, flag2 || itemIds[0], player));
               }
            } else {
               lore.addAll(this.manageLore.get(flag, itemIds[0], player));
            }
         }
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex pageIndex, ShopItem shopItem, Player player, LoreFormatter.Lore lore, String lore2, boolean flag, boolean flag2, boolean... shopIds
      ) {
      }
   }

   private interface DisplayLore {
      void apply(ShopItem shopItem, Player player, LoreFormatter.Lore lore, String shopId, boolean flag, boolean flag2, boolean... itemIds);

      void apply(LoreFormatter.PageIndex pageIndex, ShopItem shopItem, Player player, LoreFormatter.Lore lore, String lore2, boolean flag, boolean flag2, boolean... shopIds);
   }

   private final class DynamicBukkitLoreList implements LoreFormatter.DynamicLoreList {
      private final LoreFormatter.LocalizedLore lore;
      private final Map<Locale, LoreFormatter.LocalizedLore> localizedLore;

      public DynamicBukkitLoreList(List<String> lores, String text, String... values) {
         String[] values2 = lores != null
            ? (!LoreFormatter.this.isEmpty(lores) ? LoreFormatter.this.translate(lores).toArray(new String[lores.size()]) : new String[0])
            : LoreFormatter.this.translate(text).split("\n");
         String[] values3 = Arrays.stream(values2).map(element -> LoreFormatter.this.plugin.versionHandler.toNBT(element)).toArray(String[]::new);
         this.lore = !LoreFormatter.this.plugin.perPlayerLanguages ? new LoreFormatter.LocalizedLore(values2, values3, values) : null;
         this.localizedLore = LoreFormatter.this.plugin.perPlayerLanguages ? this.getlocalizedLore(lores, text, values) : null;
      }

      private Map<Locale, LoreFormatter.LocalizedLore> getlocalizedLore(List<String> lores, String text, String... values) {
         HashMap map = new HashMap();

         for (Locale locale : Lang.getLocales()) {
            String[] values2 = lores != null
               ? (!LoreFormatter.this.isEmpty(lores) ? LoreFormatter.this.translate(lores, locale).toArray(new String[0]) : new String[0])
               : LoreFormatter.this.translate(text, locale).split("\n");
            String[] values3 = Arrays.stream(values2).map(element -> LoreFormatter.this.plugin.versionHandler.toNBT(element)).toArray(String[]::new);
            map.put(locale, new LoreFormatter.LocalizedLore(values2, values3, values));
         }

         return map;
      }

      @Override
      public String[] get(boolean flag, boolean flag2, Player player, String... playerNames) {
         LoreFormatter.LocalizedLore localizedLore = this.localizedLore != null ? LoreFormatter.getLocalized(this.localizedLore, player) : this.lore;
         String[] text = (String[])(flag && !flag2 ? localizedLore.fastLore : localizedLore.lore).clone();

         for (Entry<Integer, Placeholder> entry : localizedLore.placeholders.entrySet()) {
            text[entry.getKey()] = ((Placeholder)entry.getValue()).translate(text[entry.getKey()], player, playerNames);
         }

         return text;
      }
   }

   private final class DynamicCompLoreList implements LoreFormatter.DynamicLoreList {
      private final LoreFormatter.LocalizedLore lore;
      private final Map<Locale, LoreFormatter.LocalizedLore> localizedLore;

      public DynamicCompLoreList(List<String> lores, String text, String... values) {
         String[] values2 = lores != null
            ? (!LoreFormatter.this.isEmpty(lores) ? LoreFormatter.this.translate(lores).toArray(new String[lores.size()]) : new String[0])
            : LoreFormatter.this.translate(text).split("\n");
         String[] values3 = Arrays.stream(values2).map(ChatUtil::getGsonComponent).toArray(String[]::new);
         this.lore = !LoreFormatter.this.plugin.perPlayerLanguages ? new LoreFormatter.LocalizedLore(values2, values3, values) : null;
         this.localizedLore = LoreFormatter.this.plugin.perPlayerLanguages ? this.getlocalizedLore(lores, text, values) : null;
      }

      private Map<Locale, LoreFormatter.LocalizedLore> getlocalizedLore(List<String> lores, String text, String... values) {
         HashMap map = new HashMap();

         for (Locale locale : Lang.getLocales()) {
            String[] values2 = lores != null
               ? (!LoreFormatter.this.isEmpty(lores) ? LoreFormatter.this.translate(lores, locale).toArray(new String[0]) : new String[0])
               : LoreFormatter.this.translate(text, locale).split("\n");
            String[] values3 = Arrays.stream(values2).map(ChatUtil::getGsonComponent).toArray(String[]::new);
            map.put(locale, new LoreFormatter.LocalizedLore(values2, values3, values));
         }

         return map;
      }

      @Override
      public String[] get(boolean flag, boolean flag2, Player player, String... playerNames) {
         LoreFormatter.LocalizedLore localizedLore = this.localizedLore != null ? LoreFormatter.getLocalized(this.localizedLore, player) : this.lore;
         String[] text = (String[])(flag ? localizedLore.fastLore : localizedLore.lore).clone();

         for (Entry<Integer, Placeholder> entry : localizedLore.placeholders.entrySet()) {
            text[entry.getKey()] = ((Placeholder)entry.getValue()).translate(text[entry.getKey()], player, playerNames);
         }

         return text;
      }
   }

   private interface DynamicLoreList {
      String[] get(boolean flag, boolean flag2, Player player, String... playerNames);
   }

   private final class FastLore implements LoreFormatter.Lore {
      private final Object lore = LoreFormatter.this.plugin.versionHandler.emptyList();
      private final ItemStack stack;

      public FastLore(ItemStack itemStack) {
         this.stack = itemStack;
      }

      @Override
      public void add(String text) {
         LoreFormatter.this.plugin.versionHandler.addItemLore(this.lore, text);
      }

      public void add(String text, int value) {
         LoreFormatter.this.plugin.versionHandler.addItemLore(this.lore, text, value);
      }

      @Override
      public void addAll(String[] values) {
         for (String text : values) {
            this.add(text);
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
      public void apply(ShopItem shopItem, Player player, LoreFormatter.Lore lore, String shopId, boolean flag, boolean flag2, boolean... itemIds) {
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex pageIndex, ShopItem shopItem, Player player, LoreFormatter.Lore lore, String lore2, boolean flag, boolean flag2, boolean... shopIds
      ) {
      }
   }

   private final class LimitLore implements LoreFormatter.DisplayLore {
      private final LoreFormatter.DynamicLoreList currentLimit;
      private final LoreFormatter.DynamicLoreList reachedLimit;
      private final LoreFormatter.DynamicLoreList restocking;
      private final boolean after;

      public LimitLore(ConfigurationSection section, boolean flag) {
         this.after = flag;
         this.currentLimit = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(section, "current-limit"), Lang.SELL_LIMIT.get().toString(), "%limit%"
         );
         this.reachedLimit = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(section, "limit-reached"), Lang.SELL_LIMIT_REACHED.get().toString()
         );
         this.restocking = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(section, "restocking"), Lang.SELL_LIMIT_RESTOCKING.get().toString(), "%time%"
         );
      }

      @Override
      public void apply(ShopItem shopItem, Player player, LoreFormatter.Lore lore, String shopId, boolean flag, boolean flag2, boolean... itemIds) {
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex pageIndex, ShopItem shopItem, Player player, LoreFormatter.Lore lore, String lore2, boolean flag, boolean flag2, boolean... shopIds
      ) {
         int itemLimit = LoreFormatter.this.plugin.getLimitedSellProvider().getItemLimit(shopItem, player.getUniqueId(), true, true, pageIndex);
         if (itemLimit >= 0) {
            lore.addAll(this.currentLimit.get(flag, flag2, player, "%limit%", String.valueOf(itemLimit)));
         } else if (itemLimit == -1) {
            lore.addAll(
               this.restocking.get(flag, flag2, player, "%time%", LoreFormatter.this.plugin.getLimitedSellProvider().getTimeLeftToRestock(shopItem.getItemPath()))
            );
         } else if (itemLimit == -2) {
            lore.addAll(
               this.restocking
                  .get(
                     flag,
                     flag2,
                     player,
                     "%time%",
                     LoreFormatter.this.plugin.getLimitedSellProvider().getTimeLeftToRestock(player.getUniqueId(), shopItem.getItemPath())
                  )
            );
         } else if (itemLimit != -3 && itemLimit != -4) {
            lore.addAll(this.currentLimit.get(flag, flag2, player, "%limit%", String.valueOf(itemLimit)));
         } else {
            lore.addAll(this.reachedLimit.get(flag, flag2, player));
         }
      }
   }

   private static final class LocalizedLore {
      private final Map<Integer, Placeholder> placeholders = new HashMap<>();
      private final String[] lore;
      private final String[] fastLore;

      private LocalizedLore(String[] lores, String[] values, String... values2) {
         this.lore = lores;
         this.fastLore = values;
         int value = 0;

         for (String text : lores) {
            if (Placeholder.isPlaceholder(text, values2)) {
               this.placeholders.put(value, new Placeholder(text, values2));
            }

            value++;
         }
      }
   }

   private interface Lore {
      void addAll(String[] values);

      void add(String text);

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

      public PageIndex(String text, int page) {
         this.section = text;
         this.page = page;
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

      public RawComponentLore(ItemStack itemStack) {
         this.item = itemStack;
      }

      @Override
      public void addAll(String[] values) {
         this.lore.addAll(Arrays.asList(values));
      }

      @Override
      public void add(String text) {
         this.lore.add(text);
      }

      @Override
      public ItemStack form() {
         try {
            return (ItemStack)ReflectUtil.invokeStaticLegacyMethod(
               "setRawLore", LoreFormatter.this.plugin.getMetaUtils().getClass(), new Class[]{ItemStack.class, ArrayList.class}, this.item, this.lore
            );
         } catch (Exception exception) {
            SendMessage.warnMessage("Failed to display component lore on shop item");
            exception.printStackTrace();
            return null;
         }
      }
   }

   private final class RequirementLore implements LoreFormatter.DisplayLore {
      public RequirementLore() {
      }

      @Override
      public void apply(ShopItem shopItem, Player player, LoreFormatter.Lore lore, String shopId, boolean flag, boolean flag2, boolean... itemIds) {
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex pageIndex, ShopItem shopItem, Player player, LoreFormatter.Lore lore, String lore2, boolean flag, boolean flag2, boolean... shopIds
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

      public SellLore(ConfigurationSection section, boolean flag) {
         this.after = flag;
         this.disabled = LoreFormatter.this.getDynamicLoreList(section.getStringList("disabled"), Lang.ITEM_CANNOT_BE_SOLD.get().toString());
         this.undefined = LoreFormatter.this.getDynamicLoreList(section.getStringList("undefined"), "");
         this.sellPrices = LoreFormatter.this.getDynamicLoreList(
            section.getStringList("normal"), Lang.RIGHT_CLICK_SELL.get().toString(), "%sellPrice%", "%sellPriceRaw%"
         );
         this.dynamicSellPrices = LoreFormatter.this.getDynamicLoreList(
            section.getStringList("dynamic-price"), Lang.DYNAMIC_SELL_PRICE.get().toString(), "%dpp%", "%sellPrice%", "%sellPriceRaw%", "%dynamicSellPrice%"
         );
         this.seasonalSellPrices = LoreFormatter.this.getDynamicLoreList(
            section.getStringList("seasonal"), Lang.SEASONAL_SELL_PRICE.get().toString(), "%dpp%", "%sellPrice%", "%sellPriceRaw%", "%seasonalSellPrice%"
         );
         this.multipliedSellPrices = LoreFormatter.this.getDynamicLoreList(
            section.getStringList("multiplied"), Lang.MULTIPLIED_SELL_PRICE.get().toString(), "%dpp%", "%sellPrice%", "%sellPriceRaw%", "%multipliedPrice%"
         );
         this.sellAllLore = LoreFormatter.this.getDynamicLoreList(section.getStringList("sell-all"), Lang.SHIFT_RIGHT_CLICK_SELL_ALL.get().toString());
      }

      @Override
      public void apply(ShopItem shopItem, Player player, LoreFormatter.Lore lore, String shopId, boolean flag, boolean flag2, boolean... itemIds) {
         this.apply(null, shopItem, player, lore, shopId, flag, flag2, itemIds);
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex pageIndex, ShopItem shopItem, Player player, LoreFormatter.Lore lore, String lore2, boolean flag, boolean flag2, boolean... shopIds
      ) {
         if (shopItem.isSellLore()) {
            if (shopItem.isSellAble()) {
               if (shopIds[3]) {
                  lore.addAll(
                     this.seasonalSellPrices
                        .get(
                           flag,
                           flag2,
                           player,
                           "%dpp%",
                           shopItem.isDynamicPricing() && shopIds[2] ? LoreFormatter.this.plugin.getDynamicPricingProvider().getStatus(shopItem, player, false) : "",
                           "%sellPrice%",
                           ChatColor.stripColor(
                              shopItem.isASellPricing()
                                 ? shopItem.getASellPrice().formatSellPrices(null)
                                 : LoreFormatter.this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getSellPrice())
                           ),
                           "%sellPriceRaw%",
                           shopItem.isASellPricing()
                              ? shopItem.getASellPrice()
                                 .getSellPrices(null, player, shopItem.getStackSize(), shopItem.getSellModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(entry -> entry.getKey() + ": " + entry.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(shopItem.getSellPrice(player, shopItem.getStackSize(), true, pageIndex)),
                           "%seasonalSellPrice%",
                           shopItem.isASellPricing()
                              ? shopItem.getASellPrice().formatSellPrices(null, null, player, shopItem.getStackSize(), shopItem.getSellModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getSellPrice(player, shopItem.getStackSize(), true, pageIndex))
                        )
                  );
               } else if (shopIds[1] && CalculateAmount.calculateMultiplier(player, lore2, shopItem.getSellPrice()) != shopItem.getSellPrice()) {
                  lore.addAll(
                     this.multipliedSellPrices
                        .get(
                           flag,
                           flag2,
                           player,
                           "%dpp%",
                           shopItem.isDynamicPricing() && shopIds[2] ? LoreFormatter.this.plugin.getDynamicPricingProvider().getStatus(shopItem, player, false) : "",
                           "%sellPrice%",
                           ChatColor.stripColor(
                              shopItem.isASellPricing()
                                 ? shopItem.getASellPrice().formatSellPrices(null)
                                 : LoreFormatter.this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getSellPrice())
                           ),
                           "%sellPriceRaw%",
                           shopItem.isASellPricing()
                              ? shopItem.getASellPrice()
                                 .getSellPrices(null, player, shopItem.getStackSize(), shopItem.getSellModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(entry -> entry.getKey() + ": " + entry.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(shopItem.getSellPrice(player, shopItem.getStackSize(), true, pageIndex)),
                           "%multipliedPrice%",
                           shopItem.isASellPricing()
                              ? shopItem.getASellPrice().formatSellPrices(null, null, player, shopItem.getStackSize(), shopItem.getSellModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getSellPrice(player, shopItem.getStackSize(), true, pageIndex))
                        )
                  );
               } else if (shopItem.isDynamicPricing() && shopIds[2] && shopItem.getSellPrice(1, true, pageIndex) != shopItem.getSellPriceRaw(1)) {
                  lore.addAll(
                     this.dynamicSellPrices
                        .get(
                           flag,
                           flag2,
                           player,
                           "%dpp%",
                           LoreFormatter.this.plugin.getDynamicPricingProvider().getStatus(shopItem, player, false),
                           "%sellPrice%",
                           ChatColor.stripColor(
                              shopItem.isASellPricing()
                                 ? shopItem.getASellPrice().formatSellPrices(null)
                                 : LoreFormatter.this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getSellPrice())
                           ),
                           "%sellPriceRaw%",
                           shopItem.isASellPricing()
                              ? shopItem.getASellPrice()
                                 .getSellPrices(null, player, shopItem.getStackSize(), shopItem.getSellModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(entry -> entry.getKey() + ": " + entry.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(shopItem.getSellPrice(player, shopItem.getStackSize(), true, pageIndex)),
                           "%dynamicSellPrice%",
                           shopItem.isASellPricing()
                              ? shopItem.getASellPrice().formatSellPrices(null, null, player, shopItem.getStackSize(), shopItem.getSellModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getSellPrice(player, shopItem.getStackSize(), true, pageIndex))
                        )
                  );
               } else {
                  lore.addAll(
                     this.sellPrices
                        .get(
                           flag,
                           flag2,
                           player,
                           "%sellPrice%",
                           shopItem.isASellPricing()
                              ? shopItem.getASellPrice().formatSellPrices(null, null, player, shopItem.getStackSize(), shopItem.getSellModifiers(), true)
                              : LoreFormatter.this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getSellPrice(player, shopItem.getStackSize(), true, pageIndex)),
                           "%sellPriceRaw%",
                           shopItem.isASellPricing()
                              ? shopItem.getASellPrice()
                                 .getSellPrices(null, player, shopItem.getStackSize(), shopItem.getSellModifiers(), true)
                                 .entrySet()
                                 .stream()
                                 .map(entry -> entry.getKey() + ": " + entry.getValue())
                                 .collect(Collectors.joining(", "))
                              : String.valueOf(shopItem.getSellPrice(player, shopItem.getStackSize(), true, pageIndex))
                        )
                  );
               }

               if (LoreFormatter.this.plugin.MMB) {
                  lore.addAll(this.sellAllLore.get(flag, flag2, player));
               }
            } else {
               lore.addAll(this.disabled.get(flag, flag2, player));
            }
         } else {
            lore.addAll(this.undefined.get(flag, flag2, player));
         }
      }
   }

   private final class ShopLore implements LoreFormatter.Lore {
      private final List<String> lore = new ArrayList<>();
      private final ItemStack stack;

      public ShopLore(ItemStack itemStack) {
         this.stack = itemStack;
      }

      @Override
      public void add(String text) {
         this.lore.add(text);
      }

      public void add(String text, int value) {
         this.lore.add(value, text);
      }

      @Override
      public void addAll(String[] values) {
         for (String text : values) {
            this.add(text);
         }
      }

      @Override
      public ItemStack form() {
         ItemMeta itemMeta = this.stack.getItemMeta();
         itemMeta.setLore(this.lore);
         this.stack.setItemMeta(itemMeta);
         return this.stack;
      }
   }

   private final class SpecialLore implements LoreFormatter.DisplayLore {
      private final LoreFormatter.DynamicLoreList itemPerms;
      private final LoreFormatter.DynamicLoreList itemPermsRank;
      private final LoreFormatter.DynamicLoreList alreadyPurchased;
      private final LoreFormatter.DynamicLoreList seasonalPrice;
      private final boolean after;

      public SpecialLore(ConfigurationSection section, boolean flag) {
         this.after = flag;
         this.itemPerms = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(section, "missing-item-permission.without-rank"), Lang.MISSING_REQUIRED_ITEM_PERMISSIONS.get().toString()
         );
         this.itemPermsRank = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(section, "missing-item-permission.with-rank"), Lang.MISSING_REQUIRED_ITEM_PERMISSIONS_RANK.get().toString()
         );
         this.alreadyPurchased = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(section, "already-purchased"), Lang.ALREADY_PURCHASED_ITEM.get().toString()
         );
         this.seasonalPrice = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(section, "seasonal-pricing"), Lang.ITEM_SEASONAL_PRICE.get().toString(), "%season-icon%", "%season-name%"
         );
      }

      @Override
      public void apply(ShopItem shopItem, Player player, LoreFormatter.Lore lore, String shopId, boolean flag, boolean flag2, boolean... itemIds) {
         if (itemIds[0]) {
            lore.addAll(
               this.seasonalPrice
                  .get(
                     flag,
                     flag2,
                     player,
                     "%season-icon%",
                     SeasonProvider.getSeason(player.getWorld().getName()).getIcon(),
                     "%season-name%",
                     SeasonProvider.getSeason(player.getWorld().getName()).getName()
                  )
            );
         }

         if (!shopItem.isPermRequirement()) {
            if (itemIds[1]) {
               lore.addAll(this.alreadyPurchased.get(flag, flag2, player));
            }
         } else if (!itemIds[1]) {
            lore.addAll(this.getMissingRequiredPerms(player, shopItem.getPermission(), shopId, flag, flag2));
         }
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex pageIndex, ShopItem shopItem, Player player, LoreFormatter.Lore lore, String lore2, boolean flag, boolean flag2, boolean... shopIds
      ) {
      }

      private String[] getMissingRequiredPerms(Player player, String playerName, String text, boolean flag, boolean flag2) {
         if (LoreFormatter.this.plugin.getPermissionManager().getProvider() != null && LoreFormatter.this.plugin.permissionRankName) {
            String groupName = LoreFormatter.this.plugin.getPermissionManager().getProvider().getGroupName("OnyxShop.shop." + text + ".item." + playerName);
            if (groupName != null) {
               return this.itemPermsRank.get(flag, flag2, player, "%rank_name%", groupName);
            }
         }

         return this.itemPerms.get(flag, flag2, player);
      }
   }

   public final class StaticLore {
      public final LoreFormatter.LoreType type;
      public final List<String> lore;

      public StaticLore(LoreFormatter.LoreType loreType, String[] lores) {
         this.type = loreType;
         this.lore = lores == null && loreType == LoreFormatter.LoreType.ITEM_LORE
            ? null
            : LoreFormatter.this.translate(
               loreType != LoreFormatter.LoreType.BUY_LORE && loreType != LoreFormatter.LoreType.SELL_LORE
                  ? Arrays.asList(lores)
                  : Arrays.stream(lores).map(element -> element.replace("%dpp%", "").replace("%dps%", "")).collect(Collectors.toList())
            );
      }

      public StaticLore(LoreFormatter.LoreType loreType, List<String> lores) {
         this.type = loreType;
         this.lore = lores == null && loreType == LoreFormatter.LoreType.ITEM_LORE
            ? null
            : (
               loreType != LoreFormatter.LoreType.BUY_LORE && loreType != LoreFormatter.LoreType.SELL_LORE
                  ? LoreFormatter.this.translate(lores)
                  : LoreFormatter.this.translate(lores).stream().map(element -> element.replace("%dpp%", "").replace("%dps%", "")).collect(Collectors.toList())
            );
      }
   }

   private final class StockLore implements LoreFormatter.DisplayLore {
      private final LoreFormatter.DynamicLoreList currentStock;
      private final LoreFormatter.DynamicLoreList outOfStock;
      private final LoreFormatter.DynamicLoreList restocking;
      private final boolean after;

      public StockLore(ConfigurationSection section, boolean flag) {
         this.after = flag;
         this.currentStock = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(section, "current-stock"), Lang.CURRENT_ITEM_STOCK.get().toString(), "%itemStock%"
         );
         this.outOfStock = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(section, "out-of-stock"), Lang.ITEM_OUT_OF_STOCK.get().toString()
         );
         this.restocking = LoreFormatter.this.getDynamicLoreList(
            LoreFormatter.this.getStringList(section, "restocking"), Lang.RESTOCKING_ITEM.get().toString(), "%time%"
         );
      }

      @Override
      public void apply(ShopItem shopItem, Player player, LoreFormatter.Lore lore, String shopId, boolean flag, boolean flag2, boolean... itemIds) {
      }

      @Override
      public void apply(
         LoreFormatter.PageIndex pageIndex, ShopItem shopItem, Player player, LoreFormatter.Lore lore, String lore2, boolean flag, boolean flag2, boolean... shopIds
      ) {
         int itemStock = LoreFormatter.this.plugin.getLimitedStockProvider().getItemStock(shopItem, player.getUniqueId(), true, true, pageIndex);
         if (itemStock >= 0) {
            lore.addAll(this.currentStock.get(flag, flag2, player, "%itemStock%", String.valueOf(itemStock)));
         } else if (itemStock == -1) {
            lore.addAll(
               this.restocking.get(flag, flag2, player, "%time%", LoreFormatter.this.plugin.getLimitedStockProvider().getTimeLeftToRestock(shopItem.getItemPath()))
            );
         } else if (itemStock == -2) {
            lore.addAll(
               this.restocking
                  .get(
                     flag,
                     flag2,
                     player,
                     "%time%",
                     LoreFormatter.this.plugin.getLimitedStockProvider().getTimeLeftToRestock(player.getUniqueId(), shopItem.getItemPath())
                  )
            );
         } else if (itemStock != -3 && itemStock != -4) {
            lore.addAll(this.currentStock.get(flag, flag2, player, "%itemStock%", String.valueOf(itemStock)));
         } else {
            lore.addAll(this.outOfStock.get(flag, flag2, player));
         }
      }
   }
}
