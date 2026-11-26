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

   public ShopItem(OnyxShop var1, ShopSection var2, String var3) {
      this.rotates = var2 instanceof RotatingShop;
      this.section = var2.getSection();
      this.itemLoc = var3;
      this.itemPath = this.section + "." + this.itemLoc;
      ConfigurationSection var4 = ConfigManager.getShop(this.section).getConfigurationSection((this.rotates ? "" : "pages.") + this.itemLoc);
      this.hidden = var4.getBoolean("hidden");
      this.subSection = var4.getString("section");
      this.permission = var4.getString("permission", "");
      if (!this.permission.isEmpty()) {
         this.permRequirement = var4.getBoolean("permission-requirement", true);
      }

      if (this.subSection != null) {
         this.displayItem = true;
      }

      this.priorLore = var4.getBoolean("prior-lore", ConfigManager.getConfig().getBoolean("prioritize-item-lore", false));
      this.maxBuy = var4.getInt("max-buy");
      this.maxSell = var4.getInt("max-sell");
      this.minBuy = var4.getInt("min-buy");
      this.minSell = var4.getInt("min-sell");
      this.closeMenu = var4.getBoolean("close-menu");
      this.hideUtilLore = var4.getBoolean("hideUtilLore");
      this.transactionMessage = var4.getBoolean("transactionMessage", true);
      this.bedrockTexture = var4.getString("bedrock-texture");
      this.iTags = var4.getStringList("ignored-tags")
         .stream()
         .map(var1x -> NBTMaps.getTag(var1x, this.section, this.itemLoc + ".ignored-tags"))
         .collect(Collectors.toList());
      this.enchantedVariants = var4.getBoolean(
         "enchanted-variants", ConfigManager.getSection(this.section).getBoolean("enchanted-variants", var1.allowEnchanted)
      );
      if (this.enchantedVariants) {
         this.iTags.add(NBTMaps.ENCHANTMENTS.getTag());
         this.iTags.add(NBTMaps.STORED_ENCHANTMENTS.getTag());
      }

      this.disablePricingLore = var4.getBoolean("hidePricingLore", false);
      this.matchMeta = var4.getBoolean("matchMeta", true) && var1.matchMeta;
      this.allowDamaged = var4.getBoolean(
         "pages." + this.itemLoc + ".damaged-items.enabled",
         ConfigManager.getSection(this.section).getBoolean("damaged-items.enabled", ConfigManager.getConfig().getBoolean("damaged-items.enabled", true))
      );
      this.damagedPriceDifference = var4.getDouble(
         "pages." + this.itemLoc + ".damaged-items.price-difference",
         ConfigManager.getSection(this.section)
            .getDouble("damaged-items.price-difference", ConfigManager.getConfig().getDouble("damaged-items.price-difference", 0.85))
      );
      this.fixedStackSize = var4.getInt("fixed-stack-size", 0);
      boolean var5 = false;
      if (!var4.contains("stack-size")) {
         if (this.fixedStackSize > 0) {
            this.stackSize = this.fixedStackSize;
         } else if (this.minBuy > 0 && this.minBuy > this.stackSize) {
            this.stackSize = this.minBuy;
            var5 = true;
         } else if (this.minSell > 0 && this.minSell > this.stackSize) {
            this.stackSize = this.minSell;
            var5 = true;
         }
      }

      try {
         this.economy = var1.getEcoHandler().getEconomy(this);
         if (ChatUtil.hasRGB(this.economy.getPlural()) || ChatUtil.hasRGB(this.economy.getSingular())) {
            this.rgb = true;
         }

         this.requirements = var1.getRequirementManager().load(this.section, var3);
         this.hideIfUnmet = this.requirements != null && var4.getBoolean("if-not-met.hide");
         if (this.hideIfUnmet && var4.contains("if-not-met.placeholder") && var4.contains("if-not-met.placeholder.material")) {
            this.ifUnmet = var1.createItem.createFillItem(var4.getConfigurationSection("if-not-met.placeholder"), this.section);
         }

         Double var6 = null;
         Double var7 = null;
         if (!var4.getStringList("buy-prices.prices").isEmpty() && var1.aPrices) {
            this.aBuyPrice = new AdvancedBuyPrice(this);
         } else if (var4.contains("buy")) {
            var6 = var4.getDouble("buy", -1.0);
         }

         if (!var4.getStringList("sell-prices.prices").isEmpty() && var1.aPrices) {
            this.aSellPrice = new AdvancedSellPrice(this);
         } else if (var4.contains("sell")) {
            var7 = var4.getDouble("sell", -1.0);
         }

         if (var7 == null && this.aSellPrice == null
            || (this.isASellPricing() ? this.aSellPrice.isSellAble() : this.sellPrice >= 0.0) && Lang.RIGHT_CLICK_SELL.get().getLegacy().isEmpty()
            || (this.isASellPricing() ? this.aSellPrice.isSellAble() : this.sellPrice < 0.0) && Lang.ITEM_CANNOT_BE_SOLD.get().getLegacy().isEmpty()) {
            this.sellLore = false;
         }

         if (var6 == null && this.aBuyPrice == null
            || (this.isABuyPricing() ? this.aBuyPrice.isBuyAble() : this.buyPrice >= 0.0) && Lang.LEFT_CLICK_BUY.get().getLegacy().isEmpty()
            || (this.isABuyPricing() ? !this.aBuyPrice.isBuyAble() : this.buyPrice < 0.0) && Lang.ITEM_CANNOT_BE_BOUGHT.get().getLegacy().isEmpty()) {
            this.buyLore = false;
         }

         this.shopItem = var1.createItem.loadShopSectionItem(this);
         if (this.shopItem == null) {
            this.shopItem = var1.createItem.getInvalidShopItem(this.section, this.itemLoc);
            this.totalLL = 0;
            this.error = true;
         } else if (this.shopItem.getType() != Material.AIR) {
            this.itemToGive = var1.createItem.loadItemToGive(this);
            this.stackSize = this.itemToGive.getAmount();
            if (var6 == null && var7 == null) {
               if (!this.isABuyPricing() && !this.isASellPricing()) {
                  this.displayItem = true;
               }
            } else {
               this.buyPrice = var6 != null ? (var5 ? var6 : var6 / this.stackSize) : -1.0;
               this.sellPrice = var7 != null ? (var5 ? var7 : var7 / this.stackSize) : -1.0;
            }

            List var8 = var1.versionHandler.getNBTCommands(this.shopItem, CommandType.CLICK);
            if (var8.size() >= 1) {
               this.clickCommand = true;
            }

            List<String> var9 = var1.versionHandler.getNBTCommands(this.shopItem, CommandType.BUY);
            if (var9.size() >= 1) {
               this.buyCommand = true;
               this.giveItem = var4.getBoolean("giveItem", false);
            }

            List<String> var10 = var1.versionHandler.getNBTCommands(this.shopItem, CommandType.SELL);
            if (var10.size() >= 1) {
               this.sellCommand = true;
               this.requireItem = var4.getBoolean("requireItem", false);
            }

            this.allowQuantity = !this.buyCommand && !this.sellCommand;
            if (var9.stream().anyMatch(var0 -> var0.contains("%quantity%"))) {
               this.allowQuantity = true;
            }

            if (var10.stream().anyMatch(var0 -> var0.contains("%quantity%"))) {
               this.allowQuantity = true;
            }

            this.dynamicPricing = var4.getBoolean(
                  "dynamic-pricing.enabled",
                  !this.buyCommand && !this.sellCommand && ConfigManager.getSection(this.section).getBoolean("dynamic-pricing.enabled", var1.dynamicPricing)
               )
               && var1.dynamicPricing
               && (
                  (this.isASellPricing() ? this.aSellPrice.isSellAble() : this.sellPrice > 0.0)
                     || (this.isABuyPricing() ? this.aBuyPrice.isBuyAble() : this.buyPrice > 0.0)
               );
            this.limitedStockMode = var1.itemStock ? var1.getLimitedStockProvider().getItemStockMode(this.itemPath) : 0;
            this.refillStock = this.limitedStockMode != 0 && var4.getBoolean("refill-stock");
            this.limitedSellMode = var1.sellLimits ? var1.getLimitedSellProvider().getSellLimitMode(this.itemPath) : 0;
            this.name = var1.useItemName
               ? PlayerItems.getRawDisplayName(this.itemToGive.getType(), this.itemToGive.getItemMeta())
               : var1.getMaterialName(this.itemToGive.getType().name());
            this.displayname = var1.useItemName
               ? PlayerItems.getRawDisplayName(this.shopItem.getType(), this.shopItem.getItemMeta())
               : var1.getMaterialName(this.shopItem.getType().name());
            this.customName = var1.useItemName && PlayerItems.hasCustomName(this.shopItem.getItemMeta());
            if (this.isABuyPricing()) {
               this.aBuyPrice.init(this);
            }

            if (this.isASellPricing()) {
               this.aSellPrice.init(this);
            }

            List var11 = this.shopItem.hasItemMeta() ? var1.getMetaUtils().getRawLore(this.shopItem.getItemMeta()) : null;
            this.lore = this.getDynamicLore(var1, var11);
            if (!var4.getBoolean("hideDecorationLore") && !this.isLinked() && (this.isBuyAble() || this.isSellAble())) {
               this.decorationLore = true;
            }

            this.placeholders.putAll(PlaceholderAPIHook.getPlaceholders(this.getShopItem(), pricePlaceholders));
            this.itgPlaceholders.putAll(PlaceholderAPIHook.getPlaceholders(this.getItemToGive(), pricePlaceholders));
         } else {
            this.displayItem = true;
         }
      } catch (ItemLoadException var12) {
         this.setError(var1, var12);
         this.totalLL = CalculateAmount.splitLongString(var12.getMessage()).size() - 1;
         this.error = true;
         SendMessage.warnMessage(var12.getMessage());
         SendMessage.errorShops(this.section, this.itemLoc);
      } catch (Exception var13) {
         if (var13.getMessage() != null) {
            this.setError(var1, var13);
         }

         this.totalLL = 0;
         this.error = true;
         SendMessage.warnMessage("A unknown error occurred while loading item: " + this.itemPath);
         var13.printStackTrace();
      }
   }

   public void setError(OnyxShop var1, Exception var2) {
      List var3 = CalculateAmount.splitLongString(var2.getMessage()).stream().map(var0 -> ChatUtil.formatColors("&c" + var0)).collect(Collectors.toList());
      this.shopItem = var1.createItem.getInvalidShopItem(this.section, this.itemLoc, (String)var3.remove(0));
      this.lore = this.getDynamicLore(var1, var3.isEmpty() ? null : var3);
   }

   public ShopItem(ShopItem var1) {
      this.itemPath = var1.itemPath;
      this.subSection = var1.subSection;
      this.section = var1.section;
      this.itemLoc = var1.itemLoc;
      this.permission = var1.permission;
      this.economy = var1.economy;
      this.closeMenu = var1.closeMenu;
      this.transactionMessage = var1.transactionMessage;
      this.permRequirement = var1.permRequirement;
      this.matchMeta = var1.matchMeta;
      this.iTags = new java.util.ArrayList<>(var1.iTags);
      this.placeholders.putAll(var1.placeholders);
      this.itgPlaceholders.putAll(var1.itgPlaceholders);
      this.taggables.addAll(var1.taggables);
      this.aBuyPrice = var1.aBuyPrice;
      this.aSellPrice = var1.aSellPrice;
      this.maxBuy = var1.maxBuy;
      this.maxSell = var1.maxSell;
      this.minBuy = var1.minBuy;
      this.minSell = var1.minSell;
      this.shopItem = var1.shopItem;
      this.itemToGive = var1.itemToGive;
      this.buyPrice = var1.buyPrice;
      this.sellPrice = var1.sellPrice;
      this.name = var1.name;
      this.displayname = var1.displayname;
      this.customName = var1.customName;
      this.buyLL = var1.buyLL;
      this.sellLL = var1.sellLL;
      this.totalLL = var1.totalLL;
      this.buyLore = var1.buyLore;
      this.sellLore = var1.sellLore;
      this.priorLore = var1.priorLore;
      this.disablePricingLore = var1.disablePricingLore;
      this.hideUtilLore = var1.hideUtilLore;
      this.bedrockTexture = var1.bedrockTexture;
      this.error = var1.error;
      this.stackSize = var1.stackSize;
      this.fixedStackSize = var1.fixedStackSize;
      this.displayItem = var1.displayItem;
      this.limitedStockMode = var1.limitedStockMode;
      this.limitedSellMode = var1.limitedSellMode;
      this.giveItem = var1.giveItem;
      this.requireItem = var1.requireItem;
      this.buyCommand = var1.buyCommand;
      this.sellCommand = var1.sellCommand;
      this.clickCommand = var1.clickCommand;
      this.refillStock = var1.refillStock;
      this.allowQuantity = var1.allowQuantity;
      this.dynamicPricing = var1.dynamicPricing;
      this.enchantedVariants = var1.enchantedVariants;
      this.requirements = var1.requirements;
      this.hideIfUnmet = var1.hideIfUnmet;
      this.ifUnmet = var1.ifUnmet;
      this.rotates = var1.rotates;
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

   public void addPriceModifier(PriceModifier var1) {
      this.addBuyPriceModifier(var1);
      this.addSellPriceModifier(var1);
   }

   public void addPriceModifiers(Collection<? extends PriceModifier> var1) {
      this.addBuyPriceModifiers(var1);
      this.addSellPriceModifiers(var1);
   }

   public void addBuyPriceModifier(PriceModifier var1) {
      this.buyModifiers.add(var1);
   }

   public void addBuyPriceModifiers(Collection<? extends PriceModifier> var1) {
      this.buyModifiers.addAll(var1);
   }

   public void addSellPriceModifier(PriceModifier var1) {
      this.sellModifiers.add(var1);
   }

   public void addSellPriceModifiers(Collection<? extends PriceModifier> var1) {
      this.sellModifiers.addAll(var1);
   }

   public boolean hasBuyPriceModifier(ModifierType var1) {
      return this.buyModifiers.hasType(var1);
   }

   public boolean hasSellPriceModifier(ModifierType var1) {
      return this.sellModifiers.hasType(var1);
   }

   public boolean hasSeasonModifier(String var1) {
      return this.buyModifiers.hasSeasonModifier(var1);
   }

   public void removePriceModifier(SeasonModifier var1) {
      this.buyModifiers.remove(var1);
      this.sellModifiers.remove(var1);
   }

   public void removePriceModifiers(Collection<? extends PriceModifier> var1) {
      this.buyModifiers.removeAll(var1);
      this.sellModifiers.removeAll(var1);
   }

   public void addTaggable(TaggableItemType var1) {
      this.taggables.add(var1);
   }

   public Set<TaggableItemType> getTaggables() {
      return this.taggables;
   }

   @Override
   public boolean meetsRequirements(Player var1, boolean var2) {
      if (this.requirements == null) {
         return true;
      } else {
         for (ItemRequirement var4 : this.requirements) {
            if (!var4.isMet(var1)) {
               if (!var2) {
                  var4.sendNotMetMessage(var1);
               }

               return false;
            }
         }

         return true;
      }
   }

   @Override
   public void addItemRequirement(ItemRequirement var1) {
      this.requirements.add(var1);
   }

   @Override
   public void addItemRequirements(Collection<? extends ItemRequirement> var1) {
      this.requirements.addAll(var1);
   }

   @Override
   public Requirements getRequirements() {
      return this.requirements;
   }

   public LocalizedItemStack getIfUnmet() {
      return this.ifUnmet;
   }

   @Override
   public boolean hasItemRequirement(RequirementType var1) {
      return this.requirements.hasType(var1);
   }

   @Override
   public void removeItemRequirement(ItemRequirement var1) {
      this.requirements.remove(var1);
   }

   @Override
   public void removeItemRequirements(Collection<? extends ItemRequirement> var1) {
      this.requirements.removeAll(var1);
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

   public int getClosestStackSize(int var1) {
      return Math.max(this.fixedStackSize, Math.round((float)var1 / this.fixedStackSize) * this.fixedStackSize);
   }

   public int getLargestStackSize(int var1) {
      return Math.max(this.fixedStackSize, (int)Math.ceil((double)var1 / this.fixedStackSize) * this.fixedStackSize);
   }

   public int getSmallestStackSize(int var1) {
      return Math.max(this.fixedStackSize, (int)Math.floor((double)var1 / this.fixedStackSize) * this.fixedStackSize);
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

   public double getBuyPrice(int var1) {
      return this.getBuyPrice(var1, false);
   }

   public double getBuyPrice(int var1, boolean var2) {
      return this.dynamicPricing && this.buyPrice > 0.0
         ? this.economy.round(OnyxShop.getInstance().getDynamicPricingProvider().getBuyPrice(this.itemPath, this.buyPrice, var1, var2))
         : this.buyPrice * var1;
   }

   public double getBuyPrice(int var1, boolean var2, LoreFormatter.PageIndex var3) {
      return this.dynamicPricing && this.buyPrice > 0.0
         ? this.economy.round(OnyxShop.getInstance().getDynamicPricingProvider().getBuyPrice(this.itemPath, this.buyPrice, var1, var2, var3))
         : this.buyPrice * var1;
   }

   public double getBuyPrice(Player var1, int var2) {
      return this.getBuyPrice(var1, var2, false);
   }

   public double getBuyPrice(Player var1, int var2, boolean var3) {
      if (this.dynamicPricing && this.buyPrice > 0.0) {
         double var4 = OnyxShop.getInstance().getDynamicPricingProvider().getBuyPrice(this.itemPath, this.buyPrice, var2, var3);
         return this.getPlayerBuyPrice(var1, var4);
      } else {
         return this.getPlayerBuyPrice(var1, this.buyPrice * var2);
      }
   }

   public double getBuyPrice(Player var1, int var2, boolean var3, LoreFormatter.PageIndex var4) {
      if (this.dynamicPricing && this.buyPrice > 0.0) {
         double var5 = OnyxShop.getInstance().getDynamicPricingProvider().getBuyPrice(this.itemPath, this.buyPrice, var2, var3, var4);
         return this.getPlayerBuyPrice(var1, var5);
      } else {
         return this.getPlayerBuyPrice(var1, this.buyPrice * var2);
      }
   }

   public double getBuyPriceRaw(int var1) {
      return this.buyPrice * var1;
   }

   private double getPlayerBuyPrice(Player var1, double var2) {
      for (PriceModifier var5 : this.buyModifiers) {
         var2 = var5.modify(var1, var2);
      }

      if (OnyxShop.getInstance().discountsActive && OnyxShop.getInstance().hasDiscount(this.section)) {
         var2 = CalculateAmount.calculateDiscount(var1, this.section, var2);
      }

      return this.economy.round(var2);
   }

   public boolean isSellAble() {
      return this.isASellPricing() ? this.aSellPrice.isSellAble() : this.sellPrice >= 0.0;
   }

   public double getSellPrice() {
      return this.sellPrice * this.stackSize;
   }

   public double getSellPrice(int var1) {
      return this.getSellPrice(var1, false);
   }

   public double getSellPrice(int var1, boolean var2) {
      return this.dynamicPricing && this.sellPrice > 0.0
         ? this.economy.round(OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, this.sellPrice, var1, 0, var2))
         : this.sellPrice * var1;
   }

   public double getSellPrice(int var1, boolean var2, LoreFormatter.PageIndex var3) {
      return this.dynamicPricing && this.sellPrice > 0.0
         ? this.economy.round(OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, this.sellPrice, var1, 0, var2, var3))
         : this.sellPrice * var1;
   }

   public double getSellPrice(ItemStack var1) {
      return this.getSellPrice(var1, false);
   }

   public double getSellPrice(ItemStack var1, boolean var2) {
      double var3 = var1.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(var1, this.sellPrice)
         : this.sellPrice;
      if (this.dynamicPricing && var3 > 0.0) {
         double var5 = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, var3, var1.getAmount(), 0, var2);
         return this.economy.round(this.getItemSellPrice(var5, var1));
      } else {
         return this.economy.round(this.getItemSellPrice(var3 * var1.getAmount(), var1));
      }
   }

   public double getSellPrice(ItemStack var1, int var2, int var3) {
      return this.getSellPrice(var1, var2, var3, false);
   }

   public double getSellPrice(ItemStack var1, int var2, int var3, boolean var4) {
      double var5 = var1.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(var1, this.sellPrice)
         : this.sellPrice;
      if (this.dynamicPricing && this.sellPrice > 0.0) {
         double var7 = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, var5, var2, var3, var4);
         return this.economy.round(this.getItemSellPrice(var7, var1));
      } else {
         return this.economy.round(this.getItemSellPrice(var5 * var2, var1));
      }
   }

   public double getSellPrice(Player var1, int var2) {
      return this.getSellPrice(var1, var2, false);
   }

   public double getSellPrice(Player var1, int var2, boolean var3) {
      if (this.dynamicPricing && this.sellPrice > 0.0) {
         double var4 = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, this.sellPrice, var2, 0, var3);
         return this.getPlayerSellPrice(var1, var4);
      } else {
         return this.getPlayerSellPrice(var1, this.sellPrice * var2);
      }
   }

   public double getSellPrice(Player var1, int var2, boolean var3, LoreFormatter.PageIndex var4) {
      if (this.dynamicPricing && this.sellPrice > 0.0) {
         double var5 = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, this.sellPrice, var2, 0, var3, var4);
         return this.getPlayerSellPrice(var1, var5);
      } else {
         return this.getPlayerSellPrice(var1, this.sellPrice * var2);
      }
   }

   public double getSellPrice(Player var1, ItemStack var2) {
      return this.getSellPrice(var1, var2, false);
   }

   public double getSellPrice(Player var1, ItemStack var2, boolean var3) {
      double var4 = var2.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(var2, this.sellPrice)
         : this.sellPrice;
      if (this.dynamicPricing && this.sellPrice > 0.0) {
         double var6 = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, var4, var2.getAmount(), 0, var3);
         return this.getPlayerSellPrice(var1, this.getItemSellPrice(var6, var2));
      } else {
         return this.getPlayerSellPrice(var1, this.getItemSellPrice(var4 * var2.getAmount(), var2));
      }
   }

   public double getSellPrice(Player var1, ItemStack var2, int var3, int var4) {
      return this.getSellPrice(var1, var2, var3, var4, false);
   }

   public double getSellPrice(Player var1, ItemStack var2, int var3, int var4, boolean var5) {
      double var6 = var2.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnerSellPrice(var2, this.sellPrice)
         : this.sellPrice;
      if (this.dynamicPricing && this.sellPrice > 0.0) {
         double var8 = OnyxShop.getInstance().getDynamicPricingProvider().getSellPrice(this.itemPath, var6, var3, var4, var5);
         return this.getPlayerSellPrice(var1, this.getItemSellPrice(var8, var2));
      } else {
         return this.getPlayerSellPrice(var1, this.getItemSellPrice(var6 * var3, var2));
      }
   }

   public double getSellPriceRaw(int var1) {
      return this.sellPrice * var1;
   }

   private double getPlayerSellPrice(Player var1, double var2) {
      for (PriceModifier var5 : this.sellModifiers) {
         var2 = var5.modify(var1, var2);
      }

      if (OnyxShop.getInstance().multipliers && OnyxShop.getInstance().hasMultiplier(this.section)) {
         var2 = CalculateAmount.calculateMultiplier(var1, this.section, var2);
      }

      return this.economy.round(var2);
   }

   private double getItemSellPrice(double var1, ItemStack var3) {
      if (this.allowDamaged && this.itemToGive.getType().getMaxDurability() > 0) {
         Integer var4 = OnyxShop.getInstance().versionHandler.getNBTInt(var3, NBTMaps.DAMAGE.getTag());
         if (var4 != null && var4 > 0) {
            short var5 = var3.getType().getMaxDurability();
            double var6 = (double)(var5 - var4) / var5;
            var1 = var1 * var6 * this.damagedPriceDifference;
         }
      }

      if (this.enchantedVariants) {
         var1 += this.getEnchantAddition(var1, var3);
      }

      return var1;
   }

   private double getEnchantAddition(double var1, ItemStack var3) {
      int var4 = Math.max(1, var3.getAmount());
      return OnyxShop.getInstance().getEnchantCostManager().getEnchantAddition(var3, this.itemToGive, var1 / var4) * var4;
   }

   public boolean hasPermission(Player var1, String var2, boolean var3) {
      if (!this.permission.isEmpty() && !PermissionsCache.hasPermission(var1, "OnyxShop." + var2 + "." + this.section + ".item." + this.permission)) {
         if (!var3) {
            SendMessage.chatToPlayer(var1, Lang.NO_PERMISSIONS.get(var1));
         }

         return false;
      } else {
         return true;
      }
   }

   @Override
   public boolean canPurchase(Player var1, String var2, boolean var3) {
      if (this.permission.isEmpty()) {
         return true;
      } else {
         boolean var4 = var1.hasPermission("OnyxShop." + var2 + "." + this.section + ".item." + this.permission);
         if (!var3) {
            if (!this.permRequirement) {
               if (var4) {
                  SendMessage.chatToPlayer(var1, Lang.ALREADY_PURCHASED_ITEM.get(var1));
               }
            } else if (!var4) {
               SendMessage.chatToPlayer(var1, Lang.NO_PERMISSIONS.get(var1));
            }
         }

         return !this.permRequirement ? !var4 : var4;
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

   public void setDisplayItem(boolean var1) {
      this.displayItem = var1;
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

   public boolean isMaxBuy(int var1) {
      return this.maxBuy > 0 && this.maxBuy < var1;
   }

   public int getMaxSell() {
      return this.maxSell;
   }

   public boolean isMaxSell(int var1) {
      return this.maxSell > 0 && this.maxSell < var1;
   }

   public int getMinBuy() {
      return this.minBuy;
   }

   public boolean isMinBuy(int var1) {
      return this.minBuy > 0 && this.minBuy > var1;
   }

   public int getMinSell() {
      return this.minSell;
   }

   public boolean isMinSell(int var1) {
      return this.minSell > 0 && this.minSell > var1;
   }

   public void addLoreLine(int var1) {
      this.totalLL += var1;
      this.buyLL += var1;
      this.sellLL += var1;
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

   public void setSerializer(SerializerType var1) {
      this.serializerType = var1;
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
   public void updateSkullTexture(Object var1, boolean var2) {
      SkullMeta var3 = (SkullMeta)(var2 ? this.itemToGive : this.shopItem).getItemMeta();
      SkullUtil.applySkullProfile(var1, var3);
      (var2 ? this.itemToGive : this.shopItem).setItemMeta(var3);
   }

   public boolean matchMeta(ItemStack var1) {
      if (!this.matchMeta) {
         return true;
      } else if (var1.getType().equals(XMaterial.SPAWNER.parseMaterial())) {
         return OnyxShop.getInstance().getSpawnerManager().getProvider().isShopSpawner(var1, this.itemToGive, this.iTags);
      } else {
         return this.iTags.isEmpty()
            ? OnyxShop.getInstance().isSimilar(var1, this.itemToGive)
            : OnyxShop.getInstance().isSimilar(var1, this.itemToGive, this.iTags);
      }
   }

   public boolean match(ItemStack var1) {
      return !var1.getType().equals(this.itemToGive.getType()) ? false : this.matchMeta(var1);
   }

   private DisplayItem.DynamicLore getDynamicLore(OnyxShop var1, List<String> var2) {
      return (DisplayItem.DynamicLore)(var1.paperMeta ? new ShopItem.DynamicCompLore(var2) : new ShopItem.DynamicBukkitLore(var1, var2));
   }

   private final class DynamicBukkitLore implements DisplayItem.DynamicLore {
      private final String[] lore;
      private final String[] fastLore;

      public DynamicBukkitLore(OnyxShop nullx, List<String> nullxx) {
         this.lore = nullxx != null && !nullxx.isEmpty()
            ? nullxx.stream().map(var1 -> ChatUtil.formatColors(this.translateLocalPlaceholders(var1))).toArray(String[]::new)
            : new String[0];
         this.fastLore = Arrays.stream(this.lore).map(var2 -> nullx.versionHandler.toNBT(this.translateLocalPlaceholders(var2))).toArray(String[]::new);
      }

      private String translateLocalPlaceholders(String var1) {
         if (Arrays.stream(ShopItem.pricePlaceholders).anyMatch(var1::contains)) {
            ShopItem.this.priceLore = true;
         }

         return this.removeUnknownFormats(var1);
      }

      private String removeUnknownFormats(String var1) {
         return var1.replaceAll("&[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "").replaceAll("§[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "");
      }

      @Override
      public String[] get(boolean var1, boolean var2) {
         return (String[])(var1 && !var2 ? this.fastLore : this.lore).clone();
      }
   }

   public final class DynamicCompLore implements DisplayItem.DynamicLore {
      private final String[] lore;
      private final String[] compLore;

      public DynamicCompLore(List<String> nullx) {
         this.lore = nullx != null && !nullx.isEmpty() ? nullx.stream().map(this::translateLocalPlaceholders).toArray(String[]::new) : new String[0];
         this.compLore = Arrays.stream(this.lore).map(ChatUtil::getGsonComponent).toArray(String[]::new);
      }

      private String translateLocalPlaceholders(String var1) {
         if (Arrays.stream(ShopItem.pricePlaceholders).anyMatch(var1::contains)) {
            ShopItem.this.priceLore = true;
         }

         return this.removeUnknownFormats(var1);
      }

      private String removeUnknownFormats(String var1) {
         return var1.replaceAll("&[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "").replaceAll("§[gGhHiIjJpPqQsStTuUvVwWyYzZ心他自天生物稳]", "");
      }

      @Override
      public String[] get(boolean var1, boolean var2) {
         return (String[])(var1 ? this.compLore : this.lore).clone();
      }
   }

   public static class Shulker extends ShopItem {
      private final Map<ShopItem, Integer> contents;
      private final ItemStack box;

      public Shulker(ShopItem var1, ItemStack var2, Map<ShopItem, Integer> var3) {
         super(var1);
         this.box = var2;
         this.contents = var3;
      }

      public Map<ShopItem, Integer> getContents() {
         return this.contents;
      }

      public ItemStack getBox() {
         return this.box;
      }
   }
}
