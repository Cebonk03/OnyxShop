package com.onyxshop.objects;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.prices.AdvancedBuyPrice;
import com.onyxshop.objects.prices.AdvancedSellPrice;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.objects.shops.types.RotatingShop;
import com.onyxshop.providers.EconomyProvider;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.priceModifiers.ModifierType;
import com.onyxshop.providers.priceModifiers.Modifiers;
import com.onyxshop.providers.priceModifiers.PriceModifier;
import com.onyxshop.providers.priceModifiers.seasons.SeasonModifier;
import com.onyxshop.providers.requirements.ItemRequirement;
import com.onyxshop.providers.requirements.RequirementType;
import com.onyxshop.providers.requirements.Requirements;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.CommandType;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.LocalizedItemStack;
import com.onyxshop.util.NBTMaps;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.Placeholder;
import com.onyxshop.util.PlayerItems;
import com.onyxshop.util.SkullUtil;
import com.onyxshop.util.TaggableItemType;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.util.serializer.SerializerType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class ShopItem implements DisplayItem {
   private static final String[] pricePlaceholders = new String[]{
      "%buyPrice%", "%buyPriceRaw%", "%sellPrice%", "%sellPriceRaw%", "%player_displayname%", "%player_balance%", "%page%", "%pages%"
   };
   private ItemStack shopItem;
   private ItemStack itemToGive;
   private boolean error = false;
   private int limitedStockMode;
   private int limitedSellMode;
   private boolean hideIfUnmet;
   private LocalizedItemStack ifUnmet;
   private Requirements requirements;
   private boolean decorationLore;
   private boolean giveItem;
   private boolean requireItem;
   private boolean displayItem;
   private boolean hidden;
   private boolean buyCommand;
   private boolean sellCommand;
   private boolean clickCommand;
   private boolean disablePricingLore;
   private boolean buyLore = true;
   private boolean sellLore = true;
   private boolean priorLore;
   private boolean priceLore;
   private boolean refillStock;
   private boolean dynamicPricing;
   private boolean permRequirement = true;
   private final boolean rotates;
   private final boolean closeMenu;
   private final boolean hideUtilLore;
   private final boolean transactionMessage;
   private final Modifiers buyModifiers = new Modifiers();
   private final Modifiers sellModifiers = new Modifiers();
   private final boolean matchMeta;
   public boolean rgb;
   private boolean allowDamaged;
   private double damagedPriceDifference;
   private boolean enchantedVariants;
   private SerializerType serializerType = SerializerType.BUKKIT;
   private final Map<Integer, Placeholder> itgPlaceholders = new HashMap<>();
   private final Map<Integer, Placeholder> placeholders = new HashMap<>();
   private final Set<TaggableItemType> taggables = new HashSet<>();
   private final List<String> iTags;
   private int stackSize = 1;
   private final int fixedStackSize;
   private double buyPrice = -1.0;
   private double sellPrice = -1.0;
   private AdvancedBuyPrice aBuyPrice;
   private AdvancedSellPrice aSellPrice;
   private int sellLL = -1;
   private int buyLL = -1;
   private int totalLL = 0;
   private DisplayItem.DynamicLore lore;
   private String displayname;
   private String name;
   private boolean customName;
   private boolean allowQuantity = true;
   private final int maxBuy;
   private final int maxSell;
   private final int minBuy;
   private final int minSell;
   private final String subSection;
   private final String itemPath;
   public final String section;
   public final String itemLoc;
   private final String permission;
   private EconomyProvider economy;
   private String bedrockTexture;

   public ShopItem(OnyxShop plugin, ShopSection section, String shopId) {
      this.rotates = section instanceof RotatingShop;
      this.section = section.getSection();
      this.itemLoc = shopId;
      this.itemPath = this.section + "." + this.itemLoc;
      ConfigurationSection section2 = ConfigManager.getShop(this.section).getConfigurationSection((this.rotates ? "" : "pages.") + this.itemLoc);
      this.hidden = section2.getBoolean("hidden");
      this.subSection = section2.getString("section");
      this.permission = section2.getString("permission", "");
      if (!this.permission.isEmpty()) {
         this.permRequirement = section2.getBoolean("permission-requirement", true);
      }

      if (this.subSection != null) {
         this.displayItem = true;
      }

      this.priorLore = section2.getBoolean("prior-lore", ConfigManager.getConfig().getBoolean("prioritize-item-lore", false));
      this.maxBuy = section2.getInt("max-buy");
      this.maxSell = section2.getInt("max-sell");
      this.minBuy = section2.getInt("min-buy");
      this.minSell = section2.getInt("min-sell");
      this.closeMenu = section2.getBoolean("close-menu");
      this.hideUtilLore = section2.getBoolean("hideUtilLore");
      this.transactionMessage = section2.getBoolean("transactionMessage", true);
      this.bedrockTexture = section2.getString("bedrock-texture");
      this.iTags = section2.getStringList("ignored-tags")
         .stream()
         .map(element -> NBTMaps.getTag(element, this.section, this.itemLoc + ".ignored-tags"))
         .collect(Collectors.toList());
      this.enchantedVariants = section2.getBoolean(
         "enchanted-variants", ConfigManager.getSection(this.section).getBoolean("enchanted-variants", plugin.allowEnchanted)
      );
      if (this.enchantedVariants) {
         this.iTags.add(NBTMaps.ENCHANTMENTS.getTag());
         this.iTags.add(NBTMaps.STORED_ENCHANTMENTS.getTag());
      }

      this.disablePricingLore = section2.getBoolean("hidePricingLore", false);
      this.matchMeta = section2.getBoolean("matchMeta", true) && plugin.matchMeta;
      this.allowDamaged = section2.getBoolean(
         "pages." + this.itemLoc + ".damaged-items.enabled",
         ConfigManager.getSection(this.section).getBoolean("damaged-items.enabled", ConfigManager.getConfig().getBoolean("damaged-items.enabled", true))
      );
      this.damagedPriceDifference = section2.getDouble(
         "pages." + this.itemLoc + ".damaged-items.price-difference",
         ConfigManager.getSection(this.section)
            .getDouble("damaged-items.price-difference", ConfigManager.getConfig().getDouble("damaged-items.price-difference", 0.85))
      );
      this.fixedStackSize = section2.getInt("fixed-stack-size", 0);
      boolean flag = false;
      if (!section2.contains("stack-size")) {
         if (this.fixedStackSize > 0) {
            this.stackSize = this.fixedStackSize;
         } else if (this.minBuy > 0 && this.minBuy > this.stackSize) {
            this.stackSize = this.minBuy;
            flag = true;
         } else if (this.minSell > 0 && this.minSell > this.stackSize) {
            this.stackSize = this.minSell;
            flag = true;
         }
      }

      try {
         this.economy = plugin.getEcoHandler().getEconomy(this);
         if (ChatUtil.hasRGB(this.economy.getPlural()) || ChatUtil.hasRGB(this.economy.getSingular())) {
            this.rgb = true;
         }

         this.requirements = plugin.getRequirementManager().load(this.section, shopId);
         this.hideIfUnmet = this.requirements != null && section2.getBoolean("if-not-met.hide");
         if (this.hideIfUnmet && section2.contains("if-not-met.placeholder") && section2.contains("if-not-met.placeholder.material")) {
            this.ifUnmet = plugin.createItem.createFillItem(section2.getConfigurationSection("if-not-met.placeholder"), this.section);
         }

         Double amount = null;
         Double amount2 = null;
         if (!section2.getStringList("buy-prices.prices").isEmpty() && plugin.aPrices) {
            this.aBuyPrice = new AdvancedBuyPrice(this);
         } else if (section2.contains("buy")) {
            amount = section2.getDouble("buy", -1.0);
         }

         if (!section2.getStringList("sell-prices.prices").isEmpty() && plugin.aPrices) {
            this.aSellPrice = new AdvancedSellPrice(this);
         } else if (section2.contains("sell")) {
            amount2 = section2.getDouble("sell", -1.0);
         }

         if (amount2 == null && this.aSellPrice == null
            || (this.isASellPricing() ? this.aSellPrice.isSellAble() : this.sellPrice >= 0.0) && Lang.RIGHT_CLICK_SELL.get().getLegacy().isEmpty()
            || (this.isASellPricing() ? this.aSellPrice.isSellAble() : this.sellPrice < 0.0) && Lang.ITEM_CANNOT_BE_SOLD.get().getLegacy().isEmpty()) {
            this.sellLore = false;
         }

         if (amount == null && this.aBuyPrice == null
            || (this.isABuyPricing() ? this.aBuyPrice.isBuyAble() : this.buyPrice >= 0.0) && Lang.LEFT_CLICK_BUY.get().getLegacy().isEmpty()
            || (this.isABuyPricing() ? !this.aBuyPrice.isBuyAble() : this.buyPrice < 0.0) && Lang.ITEM_CANNOT_BE_BOUGHT.get().getLegacy().isEmpty()) {
            this.buyLore = false;
         }

         this.shopItem = plugin.createItem.loadShopSectionItem(this);
         if (this.shopItem == null) {
            this.shopItem = plugin.createItem.getInvalidShopItem(this.section, this.itemLoc);
            this.totalLL = 0;
            this.error = true;
         } else if (this.shopItem.getType() != Material.AIR) {
            this.itemToGive = plugin.createItem.loadItemToGive(this);
            this.stackSize = this.itemToGive.getAmount();
            if (amount == null && amount2 == null) {
               if (!this.isABuyPricing() && !this.isASellPricing()) {
                  this.displayItem = true;
               }
            } else {
               this.buyPrice = amount != null ? (flag ? amount : amount / this.stackSize) : -1.0;
               this.sellPrice = amount2 != null ? (flag ? amount2 : amount2 / this.stackSize) : -1.0;
            }

            List nbtCommands = plugin.versionHandler.getNBTCommands(this.shopItem, CommandType.CLICK);
            if (nbtCommands.size() >= 1) {
               this.clickCommand = true;
            }

            List<String> nbtCommands2 = plugin.versionHandler.getNBTCommands(this.shopItem, CommandType.BUY);
            if (nbtCommands2.size() >= 1) {
               this.buyCommand = true;
               this.giveItem = section2.getBoolean("giveItem", false);
            }

            List<String> nbtCommands3 = plugin.versionHandler.getNBTCommands(this.shopItem, CommandType.SELL);
            if (nbtCommands3.size() >= 1) {
               this.sellCommand = true;
               this.requireItem = section2.getBoolean("requireItem", false);
            }

            this.allowQuantity = !this.buyCommand && !this.sellCommand;
            if (nbtCommands2.stream().anyMatch(element -> element.contains("%quantity%"))) {
               this.allowQuantity = true;
            }

            if (nbtCommands3.stream().anyMatch(element -> element.contains("%quantity%"))) {
               this.allowQuantity = true;
            }

            this.dynamicPricing = section2.getBoolean(
                  "dynamic-pricing.enabled",
                  !this.buyCommand && !this.sellCommand && ConfigManager.getSection(this.section).getBoolean("dynamic-pricing.enabled", plugin.dynamicPricing)
               )
               && plugin.dynamicPricing
               && (
                  (this.isASellPricing() ? this.aSellPrice.isSellAble() : this.sellPrice > 0.0)
                     || (this.isABuyPricing() ? this.aBuyPrice.isBuyAble() : this.buyPrice > 0.0)
               );
            this.limitedStockMode = plugin.itemStock ? plugin.getLimitedStockProvider().getItemStockMode(this.itemPath) : 0;
            this.refillStock = this.limitedStockMode != 0 && section2.getBoolean("refill-stock");
            this.limitedSellMode = plugin.sellLimits ? plugin.getLimitedSellProvider().getSellLimitMode(this.itemPath) : 0;
            this.name = plugin.useItemName
               ? PlayerItems.getRawDisplayName(this.itemToGive.getType(), this.itemToGive.getItemMeta())
               : plugin.getMaterialName(this.itemToGive.getType().name());
            this.displayname = plugin.useItemName
               ? PlayerItems.getRawDisplayName(this.shopItem.getType(), this.shopItem.getItemMeta())
               : plugin.getMaterialName(this.shopItem.getType().name());
            this.customName = plugin.useItemName && PlayerItems.hasCustomName(this.shopItem.getItemMeta());
            if (this.isABuyPricing()) {
               this.aBuyPrice.init(this);
            }

            if (this.isASellPricing()) {
               this.aSellPrice.init(this);
            }

            List itemIds = this.shopItem.hasItemMeta() ? plugin.getMetaUtils().getRawLore(this.shopItem.getItemMeta()) : null;
            this.lore = this.getDynamicLore(plugin, itemIds);
            if (!section2.getBoolean("hideDecorationLore") && !this.isLinked() && (this.isBuyAble() || this.isSellAble())) {
               this.decorationLore = true;
            }

            this.placeholders.putAll(PlaceholderAPIHook.getPlaceholders(this.getShopItem(), pricePlaceholders));
            this.itgPlaceholders.putAll(PlaceholderAPIHook.getPlaceholders(this.getItemToGive(), pricePlaceholders));
         } else {
            this.displayItem = true;
         }
      } catch (ItemLoadException exception) {
         this.setError(plugin, exception);
         this.totalLL = CalculateAmount.splitLongString(exception.getMessage()).size() - 1;
         this.error = true;
         SendMessage.warnMessage(exception.getMessage());
         SendMessage.errorShops(this.section, this.itemLoc);
      } catch (Exception exception) {
         if (exception.getMessage() != null) {
            this.setError(plugin, exception);
         }

         this.totalLL = 0;
         this.error = true;
         SendMessage.warnMessage("A unknown error occurred while loading item: " + this.itemPath);
         exception.printStackTrace();
      }
   }

   public void setError(OnyxShop plugin, Exception exception) {
      List messages = CalculateAmount.splitLongString(exception.getMessage()).stream().map(element -> ChatUtil.formatColors("&c" + element)).collect(Collectors.toList());
      this.shopItem = plugin.createItem.getInvalidShopItem(this.section, this.itemLoc, (String)messages.remove(0));
      this.lore = this.getDynamicLore(plugin, messages.isEmpty() ? null : messages);
   }

   public ShopItem(ShopItem shopItem) {
      this.itemPath = shopItem.itemPath;
      this.subSection = shopItem.subSection;
      this.section = shopItem.section;
      this.itemLoc = shopItem.itemLoc;
      this.permission = shopItem.permission;
      this.economy = shopItem.economy;
      this.closeMenu = shopItem.closeMenu;
      this.transactionMessage = shopItem.transactionMessage;
      this.permRequirement = shopItem.permRequirement;
      this.matchMeta = shopItem.matchMeta;
      this.iTags = new java.util.ArrayList<>(shopItem.iTags);
      this.placeholders.putAll(shopItem.placeholders);
      this.itgPlaceholders.putAll(shopItem.itgPlaceholders);
      this.taggables.addAll(shopItem.taggables);
      this.aBuyPrice = shopItem.aBuyPrice;
      this.aSellPrice = shopItem.aSellPrice;
      this.maxBuy = shopItem.maxBuy;
      this.maxSell = shopItem.maxSell;
      this.minBuy = shopItem.minBuy;
      this.minSell = shopItem.minSell;
      this.shopItem = shopItem.shopItem;
      this.itemToGive = shopItem.itemToGive;
      this.buyPrice = shopItem.buyPrice;
      this.sellPrice = shopItem.sellPrice;
      this.name = shopItem.name;
      this.displayname = shopItem.displayname;
      this.customName = shopItem.customName;
      this.buyLL = shopItem.buyLL;
      this.sellLL = shopItem.sellLL;
      this.totalLL = shopItem.totalLL;
      this.buyLore = shopItem.buyLore;
      this.sellLore = shopItem.sellLore;
      this.priorLore = shopItem.priorLore;
      this.disablePricingLore = shopItem.disablePricingLore;
      this.hideUtilLore = shopItem.hideUtilLore;
      this.bedrockTexture = shopItem.bedrockTexture;
      this.error = shopItem.error;
      this.stackSize = shopItem.stackSize;
      this.fixedStackSize = shopItem.fixedStackSize;
      this.displayItem = shopItem.displayItem;
      this.limitedStockMode = shopItem.limitedStockMode;
      this.limitedSellMode = shopItem.limitedSellMode;
      this.giveItem = shopItem.giveItem;
      this.requireItem = shopItem.requireItem;
      this.buyCommand = shopItem.buyCommand;
      this.sellCommand = shopItem.sellCommand;
      this.clickCommand = shopItem.clickCommand;
      this.refillStock = shopItem.refillStock;
      this.allowQuantity = shopItem.allowQuantity;
      this.dynamicPricing = shopItem.dynamicPricing;
      this.enchantedVariants = shopItem.enchantedVariants;
      this.requirements = shopItem.requirements;
      this.hideIfUnmet = shopItem.hideIfUnmet;
      this.ifUnmet = shopItem.ifUnmet;
      this.rotates = shopItem.rotates;
   }

   @Override
   public DisplayItem.DynamicLore getLore() {
      return this.lore;
   }

   @Override
   public String section() {
      return this.section;
   }

   @Override
   public String itemLoc() {
      return this.itemLoc;
   }

   @Override
   public String getItemPath() {
      return this.itemPath;
   }

   @Override
   public ItemStack getShopItem() {
      return this.shopItem;
   }

   public ItemStack getItemToGive() {
      return this.itemToGive;
   }

   public Modifiers getBuyModifiers() {
      return this.buyModifiers;
   }

   public Modifiers getSellModifiers() {
      return this.sellModifiers;
   }

   public void addPriceModifier(PriceModifier priceModifier) {
      this.addBuyPriceModifier(priceModifier);
      this.addSellPriceModifier(priceModifier);
   }

   public void addPriceModifiers(Collection<? extends PriceModifier> values) {
      this.addBuyPriceModifiers(values);
      this.addSellPriceModifiers(values);
   }

   public void addBuyPriceModifier(PriceModifier priceModifier) {
      this.buyModifiers.add(priceModifier);
   }

   public void addBuyPriceModifiers(Collection<? extends PriceModifier> values) {
      this.buyModifiers.addAll(values);
   }

   public void addSellPriceModifier(PriceModifier priceModifier) {
      this.sellModifiers.add(priceModifier);
   }

   public void addSellPriceModifiers(Collection<? extends PriceModifier> values) {
      this.sellModifiers.addAll(values);
   }

   public boolean hasBuyPriceModifier(ModifierType modifierType) {
      return this.buyModifiers.hasType(modifierType);
   }

   public boolean hasSellPriceModifier(ModifierType modifierType) {
      return this.sellModifiers.hasType(modifierType);
   }

   public boolean hasSeasonModifier(String text) {
      return this.buyModifiers.hasSeasonModifier(text);
   }

   public void removePriceModifier(SeasonModifier seasonModifier) {
      this.buyModifiers.remove(seasonModifier);
      this.sellModifiers.remove(seasonModifier);
   }

   public void removePriceModifiers(Collection<? extends PriceModifier> values) {
      this.buyModifiers.removeAll(values);
      this.sellModifiers.removeAll(values);
   }

   public void addTaggable(TaggableItemType taggableItemType) {
      this.taggables.add(taggableItemType);
   }

   public Set<TaggableItemType> getTaggables() {
      return this.taggables;
   }

   @Override
   public boolean meetsRequirements(Player player, boolean flag) {
      if (this.requirements == null) {
         return true;
      } else {
         for (ItemRequirement itemRequirement : this.requirements) {
            if (!itemRequirement.isMet(player)) {
               if (!flag) {
                  itemRequirement.sendNotMetMessage(player);
               }

               return false;
            }
         }

         return true;
      }
   }

   @Override
   public void addItemRequirement(ItemRequirement itemRequirement) {
      this.requirements.add(itemRequirement);
   }

   @Override
   public void addItemRequirements(Collection<? extends ItemRequirement> values) {
      this.requirements.addAll(values);
   }

   @Override
   public Requirements getRequirements() {
      return this.requirements;
   }

   public LocalizedItemStack getIfUnmet() {
      return this.ifUnmet;
   }

   @Override
   public boolean hasItemRequirement(RequirementType requirementType) {
      return this.requirements.hasType(requirementType);
   }

   @Override
   public void removeItemRequirement(ItemRequirement itemRequirement) {
      this.requirements.remove(itemRequirement);
   }

   @Override
   public void removeItemRequirements(Collection<? extends ItemRequirement> values) {
      this.requirements.removeAll(values);
   }

   public boolean isLinked() {
      return this.subSection != null;
   }

   public String getSubSection() {
      return this.subSection;
   }

   public int getStackSize() {
      return this.stackSize;
   }

   public boolean isFixedStackSize() {
      return this.fixedStackSize > 0;
   }

   public int getFixedStackSize() {
      return this.fixedStackSize;
   }

   public int getClosestStackSize(int size) {
      return Math.max(this.fixedStackSize, Math.round((float)size / this.fixedStackSize) * this.fixedStackSize);
   }

   public int getLargestStackSize(int size) {
      return Math.max(this.fixedStackSize, (int)Math.ceil((double)size / this.fixedStackSize) * this.fixedStackSize);
   }

   public int getSmallestStackSize(int size) {
      return Math.max(this.fixedStackSize, (int)Math.floor((double)size / this.fixedStackSize) * this.fixedStackSize);
   }

   public boolean isABuyPricing() {
      return this.aBuyPrice != null;
   }

   public boolean isASellPricing() {
      return this.aSellPrice != null;
   }

   public AdvancedBuyPrice getABuyPrice() {
      return this.aBuyPrice;
   }

   public AdvancedSellPrice getASellPrice() {
      return this.aSellPrice;
   }

   public boolean isBuyAble() {
      return this.isABuyPricing() ? this.aBuyPrice.isBuyAble() : this.buyPrice >= 0.0;
   }

   public double getBuyPrice() {
      return this.buyPrice * this.stackSize;
   }

   public double getBuyPrice(int price) {
      return this.getBuyPrice(price, false);
   }

   public double getBuyPrice(int price, boolean flag) {
      return this.dynamicPricing && this.buyPrice > 0.0
         ? this.economy.round(OnyxShop.getInstance().getDynamicPricingProvider().getBuyPrice(this.itemPath, this.buyPrice, price, flag))
         : this.buyPrice * price;
   }

   public double getBuyPrice(int price, boolean flag, LoreFormatter.PageIndex pageIndex) {
      return this.dynamicPricing && this.buyPrice > 0.0
         ? this.economy.round(OnyxShop.getInstance().getDynamicPricingProvider().getBuyPrice(this.itemPath, this.buyPrice, price, flag, pageIndex))
         : this.buyPrice * price;
   }

   public double getBuyPrice(Player player, int price) {
      return this.getBuyPrice(player, price, false);
   }

   public double getBuyPrice(Player player, int price, boolean flag) {
      if (this.dynamicPricing && this.buyPrice > 0.0) {
         double buyPrice = OnyxShop.getInstance().getDynamicPricingProvider().getBuyPrice(this.itemPath, this.buyPrice, price, flag);
         return this.getPlayerBuyPrice(player, buyPrice);
      } else {
         return this.getPlayerBuyPrice(player, this.buyPrice * price);
      }
   }

   public double getBuyPrice(Player player, int price, boolean flag, LoreFormatter.PageIndex pageIndex) {
      if (this.dynamicPricing && this.buyPrice > 0.0) {
         double buyPrice = OnyxShop.getInstance().getDynamicPricingProvider().getBuyPrice(this.itemPath, this.buyPrice, price, flag, pageIndex);
         return this.getPlayerBuyPrice(player, buyPrice);
      } else {
         return this.getPlayerBuyPrice(player, this.buyPrice * price);
      }
   }

   public double getBuyPriceRaw(int price) {
      return this.buyPrice * price;
   }

   private double getPlayerBuyPrice(Player player, double price) {
      for (PriceModifier priceModifier : this.buyModifiers) {
         price = priceModifier.modify(player, price);
      }

      if (OnyxShop.getInstance().discountsActive && OnyxShop.getInstance().hasDiscount(this.section)) {
         price = CalculateAmount.calculateDiscount(player, this.section, price);
      }

      return this.economy.round(price);
   }

   public boolean isSellAble() {
      return this.isASellPricing() ? this.aSellPrice.isSellAble() : this.sellPrice >= 0.0;
   }

   public double getSellPrice() {
      return this.sellPrice * this.stackSize;
   }

   public double getSellPrice(int price) {
      return this.getSellPrice(price, false);
   }

   public double getSellPrice(int price, boolean flag) {
      return this.dynamicPricing && this.sellPrice > 0.0
         ? this.economy.round(OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, this.sellPrice, price, 0, flag))
         : this.sellPrice * price;
   }

   public double getSellPrice(int price, boolean flag, LoreFormatter.PageIndex pageIndex) {
      return this.dynamicPricing && this.sellPrice > 0.0
         ? this.economy.round(OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, this.sellPrice, price, 0, flag, pageIndex))
         : this.sellPrice * price;
   }

   public double getSellPrice(ItemStack itemStack) {
      return this.getSellPrice(itemStack, false);
   }

   public double getSellPrice(ItemStack itemStack, boolean flag) {
      double price = itemStack.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(itemStack, this.sellPrice)
         : this.sellPrice;
      if (this.dynamicPricing && price > 0.0) {
         double sellPrice = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, price, itemStack.getAmount(), 0, flag);
         return this.economy.round(this.getItemSellPrice(sellPrice, itemStack));
      } else {
         return this.economy.round(this.getItemSellPrice(price * itemStack.getAmount(), itemStack));
      }
   }

   public double getSellPrice(ItemStack itemStack, int price, int value) {
      return this.getSellPrice(itemStack, price, value, false);
   }

   public double getSellPrice(ItemStack itemStack, int price, int value, boolean flag) {
      double amount = itemStack.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(itemStack, this.sellPrice)
         : this.sellPrice;
      if (this.dynamicPricing && this.sellPrice > 0.0) {
         double sellPrice = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, amount, price, value, flag);
         return this.economy.round(this.getItemSellPrice(sellPrice, itemStack));
      } else {
         return this.economy.round(this.getItemSellPrice(amount * price, itemStack));
      }
   }

   public double getSellPrice(Player player, int price) {
      return this.getSellPrice(player, price, false);
   }

   public double getSellPrice(Player player, int price, boolean flag) {
      if (this.dynamicPricing && this.sellPrice > 0.0) {
         double sellPrice = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, this.sellPrice, price, 0, flag);
         return this.getPlayerSellPrice(player, sellPrice);
      } else {
         return this.getPlayerSellPrice(player, this.sellPrice * price);
      }
   }

   public double getSellPrice(Player player, int price, boolean flag, LoreFormatter.PageIndex pageIndex) {
      if (this.dynamicPricing && this.sellPrice > 0.0) {
         double sellPrice = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, this.sellPrice, price, 0, flag, pageIndex);
         return this.getPlayerSellPrice(player, sellPrice);
      } else {
         return this.getPlayerSellPrice(player, this.sellPrice * price);
      }
   }

   public double getSellPrice(Player player, ItemStack itemStack) {
      return this.getSellPrice(player, itemStack, false);
   }

   public double getSellPrice(Player player, ItemStack itemStack, boolean flag) {
      double price = itemStack.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(itemStack, this.sellPrice)
         : this.sellPrice;
      if (this.dynamicPricing && this.sellPrice > 0.0) {
         double sellPrice = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, price, itemStack.getAmount(), 0, flag);
         return this.getPlayerSellPrice(player, this.getItemSellPrice(sellPrice, itemStack));
      } else {
         return this.getPlayerSellPrice(player, this.getItemSellPrice(price * itemStack.getAmount(), itemStack));
      }
   }

   public double getSellPrice(Player player, ItemStack itemStack, int price, int value) {
      return this.getSellPrice(player, itemStack, price, value, false);
   }

   public double getSellPrice(Player player, ItemStack itemStack, int price, int value, boolean flag) {
      double amount = itemStack.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(itemStack, this.sellPrice)
         : this.sellPrice;
      if (this.dynamicPricing && this.sellPrice > 0.0) {
         double sellPrice = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, amount, price, value, flag);
         return this.getPlayerSellPrice(player, this.getItemSellPrice(sellPrice, itemStack));
      } else {
         return this.getPlayerSellPrice(player, this.getItemSellPrice(amount * price, itemStack));
      }
   }

   public double getSellPriceRaw(int price) {
      return this.sellPrice * price;
   }

   private double getPlayerSellPrice(Player player, double price) {
      for (PriceModifier priceModifier : this.sellModifiers) {
         price = priceModifier.modify(player, price);
      }

      if (OnyxShop.getInstance().multipliers && OnyxShop.getInstance().hasMultiplier(this.section)) {
         price = CalculateAmount.calculateMultiplier(player, this.section, price);
      }

      return this.economy.round(price);
   }

   private double getItemSellPrice(double price, ItemStack itemStack) {
      if (this.allowDamaged && this.itemToGive.getType().getMaxDurability() > 0) {
         Integer nbtInt = OnyxShop.getInstance().versionHandler.getNBTInt(itemStack, NBTMaps.DAMAGE.getTag());
         if (nbtInt != null && nbtInt > 0) {
            short maxDurability = itemStack.getType().getMaxDurability();
            double amount = (double)(maxDurability - nbtInt) / maxDurability;
            price = price * amount * this.damagedPriceDifference;
         }
      }

      if (this.enchantedVariants) {
         price += this.getEnchantAddition(price, itemStack);
      }

      return price;
   }

   private double getEnchantAddition(double amount, ItemStack itemStack) {
      int amount2 = Math.max(1, itemStack.getAmount());
      return OnyxShop.getInstance().getEnchantCostManager().getEnchantAddition(itemStack, this.itemToGive, amount / amount2) * amount2;
   }

   public boolean hasPermission(Player player, String permission, boolean flag) {
      if (!this.permission.isEmpty() && !PermissionsCache.hasPermission(player, "OnyxShop." + permission + "." + this.section + ".item." + this.permission)) {
         if (!flag) {
            SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get(player));
         }

         return false;
      } else {
         return true;
      }
   }

   @Override
   public boolean canPurchase(Player player, String playerName, boolean flag) {
      if (this.permission.isEmpty()) {
         return true;
      } else {
         boolean permission = player.hasPermission("OnyxShop." + playerName + "." + this.section + ".item." + this.permission);
         if (!flag) {
            if (!this.permRequirement) {
               if (permission) {
                  SendMessage.chatToPlayer(player, Lang.ALREADY_PURCHASED_ITEM.get(player));
               }
            } else if (!permission) {
               SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get(player));
            }
         }

         return !this.permRequirement ? !permission : permission;
      }
   }

   @Override
   public boolean rotates() {
      return this.rotates;
   }

   public String getPermission() {
      return this.permission;
   }

   public boolean isPermRequirement() {
      return this.permRequirement;
   }

   @Override
   public String getDisplayname() {
      return this.displayname;
   }

   public String getName() {
      return this.name;
   }

   public boolean hasCustomName() {
      return this.customName;
   }

   public String getTranslationKey() {
      return this.shopItem.getType().getTranslationKey();
   }

   public boolean isAllowQuantity() {
      return this.allowQuantity;
   }

   public boolean isBuyCommand() {
      return this.buyCommand;
   }

   public boolean isSellCommand() {
      return this.sellCommand;
   }

   @Override
   public boolean isClickCommand() {
      return this.clickCommand;
   }

   @Override
   public boolean isDisplayItem() {
      return this.displayItem;
   }

   public void setDisplayItem(boolean flag) {
      this.displayItem = flag;
   }

   public int getLimitedStockMode() {
      return this.limitedStockMode;
   }

   public boolean isRefillStock() {
      return this.refillStock;
   }

   public int getLimitedSellMode() {
      return this.limitedSellMode;
   }

   @Override
   public boolean hasItemError() {
      return this.error;
   }

   public int getMaxBuy() {
      return this.maxBuy;
   }

   public boolean isMaxBuy(int value) {
      return this.maxBuy > 0 && this.maxBuy < value;
   }

   public int getMaxSell() {
      return this.maxSell;
   }

   public boolean isMaxSell(int value) {
      return this.maxSell > 0 && this.maxSell < value;
   }

   public int getMinBuy() {
      return this.minBuy;
   }

   public boolean isMinBuy(int value) {
      return this.minBuy > 0 && this.minBuy > value;
   }

   public int getMinSell() {
      return this.minSell;
   }

   public boolean isMinSell(int value) {
      return this.minSell > 0 && this.minSell > value;
   }

   public void addLoreLine(int value) {
      this.totalLL += value;
      this.buyLL += value;
      this.sellLL += value;
   }

   public int getTotalLL() {
      return this.totalLL;
   }

   public int getBuyLL() {
      return this.buyLL;
   }

   public int getSellLL() {
      return this.sellLL;
   }

   public boolean isBuyLore() {
      return this.buyLore;
   }

   public boolean isSellLore() {
      return this.sellLore;
   }

   public boolean isPriorLore() {
      return this.priorLore;
   }

   public boolean isDecorationLore() {
      return this.decorationLore;
   }

   public boolean isHidden() {
      return this.hidden;
   }

   public boolean isDynamicPricing() {
      return this.dynamicPricing;
   }

   @Override
   public boolean isCloseMenu() {
      return this.closeMenu;
   }

   public boolean isHideUtilLore() {
      return this.hideUtilLore;
   }

   public boolean isHidePricingLore() {
      return this.disablePricingLore;
   }

   public boolean isGiveItem() {
      return this.giveItem;
   }

   public boolean isRequireItem() {
      return this.requireItem;
   }

   public boolean isHideIfUnmet() {
      return this.hideIfUnmet;
   }

   public EcoType getEcoType() {
      return this.economy.getType();
   }

   @Override
   public boolean hasPlaceholders() {
      return !this.placeholders.isEmpty();
   }

   public boolean needsUpdate() {
      return this.serializerType != SerializerType.BUKKIT;
   }

   public void setSerializer(SerializerType serializerType) {
      this.serializerType = serializerType;
   }

   public SerializerType getSerializer() {
      return this.serializerType;
   }

   @Override
   public Map<Integer, Placeholder> getPlaceholders() {
      return this.placeholders;
   }

   public Map<Integer, Placeholder> getItgPlaceholders() {
      return this.itgPlaceholders;
   }

   public boolean transactionMessage() {
      return this.transactionMessage;
   }

   public boolean isPriceLore() {
      return this.priceLore;
   }

   public boolean allowDamaged() {
      return this.allowDamaged;
   }

   public double getDamagedPriceDifference() {
      return this.damagedPriceDifference;
   }

   public boolean isEnchantedVariants() {
      return this.enchantedVariants;
   }

   public String getBedrockTexture() {
      return this.bedrockTexture;
   }

   @Override
   public void updateSkullTexture(Object value, boolean flag) {
      SkullMeta skullMeta = (SkullMeta)(flag ? this.itemToGive : this.shopItem).getItemMeta();
      SkullUtil.applySkullProfile(value, skullMeta);
      (flag ? this.itemToGive : this.shopItem).setItemMeta(skullMeta);
   }

   public boolean matchMeta(ItemStack itemStack) {
      if (!this.matchMeta) {
         return true;
      } else if (itemStack.getType().equals(XMaterial.SPAWNER.parseMaterial())) {
         return OnyxShop.getInstance().getSpawnerManager().getProvider().isShopSpawner(itemStack, this.itemToGive, this.iTags);
      } else {
         return this.iTags.isEmpty()
            ? OnyxShop.getInstance().isSimilar(itemStack, this.itemToGive)
            : OnyxShop.getInstance().isSimilar(itemStack, this.itemToGive, this.iTags);
      }
   }

   public boolean match(ItemStack itemStack) {
      return !itemStack.getType().equals(this.itemToGive.getType()) ? false : this.matchMeta(itemStack);
   }

   private DisplayItem.DynamicLore getDynamicLore(OnyxShop plugin, List<String> lores) {
      return (DisplayItem.DynamicLore)(plugin.paperMeta ? new ShopItem.DynamicCompLore(lores) : new ShopItem.DynamicBukkitLore(plugin, lores));
   }

   private final class DynamicBukkitLore implements DisplayItem.DynamicLore {
      private final String[] lore;
      private final String[] fastLore;

      public DynamicBukkitLore(OnyxShop plugin, List<String> lores) {
         this.lore = lores != null && !lores.isEmpty()
            ? lores.stream().map(element -> ChatUtil.formatColors(this.translateLocalPlaceholders(element))).toArray(String[]::new)
            : new String[0];
         this.fastLore = Arrays.stream(this.lore).map(element -> plugin.versionHandler.toNBT(this.translateLocalPlaceholders(element))).toArray(String[]::new);
      }

      private String translateLocalPlaceholders(String text) {
         if (Arrays.stream(ShopItem.pricePlaceholders).anyMatch(text::contains)) {
            ShopItem.this.priceLore = true;
         }

         return this.removeUnknownFormats(text);
      }

      private String removeUnknownFormats(String text) {
         return text.replaceAll("&[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "").replaceAll("§[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "");
      }

      @Override
      public String[] get(boolean flag, boolean flag2) {
         return (String[])(flag && !flag2 ? this.fastLore : this.lore).clone();
      }
   }

   public final class DynamicCompLore implements DisplayItem.DynamicLore {
      private final String[] lore;
      private final String[] compLore;

      public DynamicCompLore(List<String> lores) {
         this.lore = lores != null && !lores.isEmpty() ? lores.stream().map(this::translateLocalPlaceholders).toArray(String[]::new) : new String[0];
         this.compLore = Arrays.stream(this.lore).map(ChatUtil::getGsonComponent).toArray(String[]::new);
      }

      private String translateLocalPlaceholders(String text) {
         if (Arrays.stream(ShopItem.pricePlaceholders).anyMatch(text::contains)) {
            ShopItem.this.priceLore = true;
         }

         return this.removeUnknownFormats(text);
      }

      private String removeUnknownFormats(String text) {
         return text.replaceAll("&[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "").replaceAll("§[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "");
      }

      @Override
      public String[] get(boolean flag, boolean flag2) {
         return (String[])(flag ? this.compLore : this.lore).clone();
      }
   }

   public static class Shulker extends ShopItem {
      private final Map<ShopItem, Integer> contents;
      private final ItemStack box;

      public Shulker(ShopItem shopItem, ItemStack itemStack, Map<ShopItem, Integer> map) {
         super(shopItem);
         this.box = itemStack;
         this.contents = map;
      }

      public Map<ShopItem, Integer> getContents() {
         return this.contents;
      }

      public ItemStack getBox() {
         return this.box;
      }
   }
}
