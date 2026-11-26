package com.onyxshop.objects.shops.types;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ActionItem;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.objects.LoreFormatter;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.ShopPage;
import com.onyxshop.objects.ShopPageItems;
import com.onyxshop.objects.User;
import com.onyxshop.objects.mappings.ClickAction;
import com.onyxshop.objects.mappings.ClickMappings;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.objects.titles.ShopTitle;
import com.onyxshop.providers.UserManager;
import com.onyxshop.util.InventoryUtil;
import com.onyxshop.util.ItemLayout;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.TimeUtils;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.util.layouts.PageLayout;
import com.onyxshop.util.layouts.ReservedLayout;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ShopCategory implements ShopSection {
   private final String section;
   private final ShopTitle title;
   private final boolean subSection;
   private final boolean hidden;
   private final boolean dynamicLore;
   private final boolean navBar;
   private final long updateInterval;
   private boolean staticTitle;
   private ItemStack fillItem;
   private final List<String> pages;
   private final Map<Integer, ShopTitle> pageTitles = new HashMap<>();
   private final Map<Integer, ShopPageItems> shopPages = new HashMap<>();
   private final Map<String, DisplayItem> displayItems = new LinkedHashMap<>();
   private final List<String> clickCommands;
   private final ClickMappings mappings;
   private boolean closeMenu = false;
   public final OnyxShop plugin;
   private final boolean nms = OnyxShop.getInstance().versionHandler != null
      && ConfigManager.getConfig().getBoolean("use-nms", true)
      && Bukkit.getPluginManager().getPlugin("EcoEnchants") == null;
   public final List<String> itemLocs = new ArrayList<>();

   public ShopCategory(OnyxShop var1, String var2) {
      this.plugin = var1;
      this.section = var2;
      this.title = new ShopTitle(this.plugin.getSectionTitle(var2), this.plugin.getBedrockSectionTitle(var2));
      this.subSection = ConfigManager.getSection(this.section).getBoolean("sub-section");
      this.hidden = ConfigManager.getSection(this.section).getBoolean("hidden");
      this.dynamicLore = var1.dynamicPricing;
      this.pages = this.plugin.getConfigManager().getPages(this.section, false);
      this.navBar = this.plugin.navBar.isEnabled(this.section);
      this.updateInterval = this.loadUpdateInterval();
      this.itemLocs.addAll(this.plugin.getConfigManager().getItemsRaw(this.section));
      this.mappings = ConfigManager.getSection(this.section).contains("click-mappings") ? new ClickMappings(var2) : var1.getClickMappings();
      if (!(this.clickCommands = ConfigManager.getSection(this.section).getStringList("click-commands")).isEmpty()) {
         this.closeMenu = ConfigManager.getSection(this.section).getBoolean("close-menu");
      } else {
         this.loadShopItems();
         this.staticTitle = this.isStaticTitle();
      }
   }

   @Override
   public Collection<DisplayItem> getDisplayItems() {
      return this.displayItems.values();
   }

   @Override
   public DisplayItem getDisplayItem(String var1) {
      return this.displayItems.get(var1);
   }

   @Override
   public boolean isActionItem(String var1, String var2) {
      return this.displayItems.get(var1 + "." + var2) instanceof ActionItem;
   }

   @Override
   public Collection<ShopItem> getShopItems() {
      return this.displayItems
         .values()
         .stream()
         .filter(var0 -> ShopItem.class.equals(var0.getClass()))
         .map(ShopItem.class::cast)
         .collect(Collectors.toCollection(ArrayList::new));
   }

   @Override
   public ShopItem getShopItem(String var1) {
      DisplayItem var2 = this.displayItems.get(var1);
      return var2 instanceof ShopItem ? (ShopItem)var2 : null;
   }

   @Override
   public String getSection() {
      return this.section;
   }

   @Override
   public ShopPageItems getShopPageItems(int var1) {
      return this.shopPages.get(var1);
   }

   @Override
   public int getPages() {
      return this.pages.size();
   }

   @Override
   public long getUpdateInterval() {
      return this.updateInterval;
   }

   @Override
   public boolean isSubSection() {
      return this.subSection;
   }

   @Override
   public boolean isHidden() {
      return this.hidden;
   }

   @Override
   public boolean isCommandItem() {
      return !this.clickCommands.isEmpty();
   }

   @Override
   public List<String> getClickCommands() {
      return this.clickCommands;
   }

   @Override
   public boolean isCloseMenu() {
      return this.closeMenu;
   }

   @Override
   public List<String> getItemLocs() {
      return this.itemLocs;
   }

   @Override
   public List<String> getShopItemLocs() {
      return this.displayItems.values().stream().filter(var0 -> ShopItem.class.equals(var0.getClass())).map(DisplayItem::itemLoc).collect(Collectors.toList());
   }

   @Override
   public ShopType getType() {
      return ShopType.CATEGORY;
   }

   @Override
   public ClickAction getClickAction(ClickType var1) {
      return this.mappings.getAction(var1);
   }

   @Override
   public int getPageForShopItem(String var1) {
      return this.pages.indexOf(var1.split("\\.")[0]) + 1;
   }

   @Override
   public List<ShopItem> getPageItems(int var1) {
      return this.shopPages
         .get(var1)
         .getItemOrder()
         .stream()
         .filter(Objects::nonNull)
         .map(var1x -> this.displayItems.get(this.section + "." + var1x))
         .filter(var0 -> var0 instanceof ShopItem)
         .map(var0 -> (ShopItem)var0)
         .collect(Collectors.toList());
   }

   private void loadShopItems() {
      try {
         for (String var2 : this.itemLocs) {
            this.displayItems
               .put(
                  this.section + "." + var2,
                  (DisplayItem)(ConfigManager.getShop(this.section).contains("pages." + var2 + ".action")
                     ? new ActionItem(this.plugin, this, var2)
                     : new ShopItem(this.plugin, this, var2))
               );
         }

         if (!this.pages.isEmpty()) {
            this.loadPages();
         } else {
            SendMessage.errorMessage("No pages found for shop " + this.section + ", adding empty page...");
            this.shopPages.put(1, new ShopPageItems(this.section));
            this.pageTitles.put(1, this.title);
         }
      } catch (NullPointerException var3) {
         var3.printStackTrace();
      }

      if (ConfigManager.getSection(this.section).get("fill-item") != null) {
         this.loadFillItem();
      }
   }

   private void loadFillItem() {
      try {
         this.fillItem = this.plugin.createItem.loadSectionItem(this.section, "fill-item");
      } catch (ItemLoadException var2) {
         SendMessage.errorMessage(Lang.EXCEPTION_LOADING_FILL_ITEM.get().replace("%section%", this.section));
         SendMessage.warnMessage(var2.getMessage());
      }
   }

   private void loadPages() {
      try {
         int var1 = 1;

         for (String var3 : this.pages) {
            Integer var4 = this.getPageSize(var3);
            int var5 = this.plugin.navBar.isEnabled(this.section) ? (var4 == null ? 45 : var4) : (var4 == null ? 54 : var4);
            ItemLayout var6 = this.getItemLayout(var3, var5);
            List var7 = this.plugin
               .calculateAmount
               .loadItemsInOrder((ItemLayout)(var6 == null ? new PageLayout(this.section) : var6), this.section, var3, var4, var5);
            if (var4 == null) {
               var4 = var7.size() <= 45 && !var7.isEmpty() ? CalculateAmount.getSlots(var7.size()) : 54;
            }

            ArrayList var8 = new ArrayList();
            LinkedHashMap var9 = new LinkedHashMap();

            for (int var10 = 0; var10 < var7.size(); var10++) {
               String var11 = (String)var7.get(var10);
               if (var11 != null) {
                  var9.put(var10, this.displayItems.get(this.section + "." + var11).getShopItem());
               }

               var8.add(var11);
            }

            this.shopPages.put(var1, new ShopPageItems(var9, var8, this.section, var4));
            this.pageTitles.put(var1, this.getPageTitle(var3));
            var1++;
         }
      } catch (Exception var12) {
         SendMessage.warnMessage("Error occurred while loading shop items for section " + this.section);
         var12.printStackTrace();
      }
   }

   private ItemLayout getItemLayout(String var1, int var2) {
      List var3 = ConfigManager.getSection(this.section).getStringList("item-layout");
      if (var3.isEmpty()) {
         return null;
      } else if (var3.size() < var2 / 9) {
         SendMessage.errorMessage(
            "Invalid item layout for " + this.section + " page '" + var1 + "': Expected " + var2 / 9 + " rows of values, but got " + var3.size() + " instead"
         );
         return null;
      } else {
         try {
            ArrayList var4 = new ArrayList();

            for (int var5 = 0; var5 < var2 / 9; var5++) {
               String[] var6 = ((String)var3.get(var5)).split(" ");
               if (var6.length != 9) {
                  SendMessage.errorMessage(
                     "Invalid item layout for " + this.section + " page '" + var1 + "': Row " + (var5 + 1) + " contains less or more symbols, expected 9"
                  );
                  return null;
               }

               if (Arrays.stream(var6).anyMatch(var0 -> !var0.equals("0") && !var0.equals("1"))) {
                  SendMessage.errorMessage(
                     "Invalid item layout for "
                        + this.section
                        + " page '"
                        + var1
                        + "': Items from row "
                        + (var5 + 1)
                        + " does not match expected values, expected 0 or 1"
                  );
                  return null;
               }

               Arrays.stream(var6).forEach(var1x -> var4.add(var1x.equals("1")));
            }

            ReservedLayout var10 = new ReservedLayout(this.section);

            for (int var11 = 0; var11 < var2 / 9; var11++) {
               for (int var7 = 0; var7 < 9; var7++) {
                  int var8 = var11 * 9 + var7;
                  if ((Boolean)var4.get(var8)) {
                     var10.reserve(var8);
                  }
               }
            }

            return var10;
         } catch (IndexOutOfBoundsException var9) {
            SendMessage.errorMessage("Unexpected error occurred while loading item layout for " + this.section + " page '" + var1 + "'");
            var9.printStackTrace();
            return null;
         }
      }
   }

   private long loadUpdateInterval() {
      String var1 = ConfigManager.getSection(this.section).getString("update-interval");
      if (var1 == null) {
         return 0L;
      } else {
         try {
            return TimeUtils.getTime(var1) / 1000L * 20L;
         } catch (ParseException var3) {
            SendMessage.warnMessage("Invalid update-interval for shop '" + this.section + "', disabling periodic item updates...");
            return 0L;
         }
      }
   }

   private Integer getPageSize(String var1) {
      if (!ConfigManager.getShop(this.section).contains("pages." + var1 + ".gui-rows")) {
         return null;
      } else {
         try {
            int var2 = Integer.parseInt(ConfigManager.getShop(this.section).getString("pages." + var1 + ".gui-rows"));
            if (var2 >= 1 && var2 <= 6) {
               return var2 * 9;
            }

            SendMessage.warnMessage("Inventory size for page '" + var1 + "' from shop '" + this.section + "' has to be in range from 1-6, using default...");
         } catch (NumberFormatException var3) {
            SendMessage.warnMessage("Invalid inventory size for page '" + var1 + "' from shop '" + this.section + "', using default...");
         }

         return null;
      }
   }

   private ShopTitle getPageTitle(String var1) {
      Translatable var2 = null;
      if (ConfigManager.getShop(this.section).contains("pages." + var1 + ".bedrock-title")) {
         var2 = SendMessage.fromConfig(this.plugin.getInvTitle(ConfigManager.getShop(this.section).getString("pages." + var1 + ".bedrock-title")));
      }

      if (ConfigManager.getShop(this.section).contains("pages." + var1 + ".title")) {
         String var3 = ConfigManager.getShop(this.section).getString("pages." + var1 + ".title");
         return new ShopTitle(SendMessage.fromConfig(this.plugin.getInvTitle(var3)), var2);
      } else {
         return var2 != null ? new ShopTitle(this.title.getTitle(), var2) : this.title;
      }
   }

   private boolean isStaticTitle() {
      String var1 = this.title.getRaw();

      for (ShopTitle var3 : this.pageTitles.values()) {
         if (!var3.getRaw().equals(var1) || !var3.isStatic()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public void openShopSection(Player var1, boolean var2) {
      this.openShopSection(var1, 1, var2);
   }

   @Override
   public void openShopSection(Player var1, boolean var2, String var3) {
      this.openShopSection(var1, 1, var2, var3);
   }

   @Override
   public void openShopSection(Player var1, int var2, boolean var3) {
      this.openShopSection(var1, var2, var3, null);
   }

   @Override
   public void openShopSection(Player var1, int var2, boolean var3, @Nullable String var4) {
      ShopPageItems var5 = this.shopPages.get(var2);
      Map var6 = var5.getItems();
      User var7 = UserManager.getUser(var1);
      boolean var8 = PermissionsCache.hasPermission(var1, "OnyxShop.itemindexes");
      boolean var9 = PermissionsCache.hasPermission(var1, "OnyxShop.editshop.editor");
      boolean var10 = this.plugin.discountsActive && this.plugin.hasDiscount(this.section);
      boolean var11 = this.plugin.multipliers && this.plugin.hasMultiplier(this.section);
      this.updateLore(
         var6, var1, var7.isPr(), var7.isBedrock(), var10, var11, this.plugin.itemStockLore, this.plugin.sellLimits, var9, var8, this.plugin.seasonalLore, var2
      );
      if (this.plugin.bedrock && this.plugin.bedrockMenus && var7.isBedrock() && this.plugin.getBedrockInventorys().isEnabledFor(var1)) {
         this.plugin
            .getBedrockInventorys()
            .openShopSection(
               var1,
               this,
               var2,
               var3,
               var4,
               var6,
               var5.getItemOrder(),
               this.fillItem,
               this.pageTitles.get(var2).getTitle(var1, var2, this.pages.size()),
               var5.getSize(),
               this.navBar
            );
      } else {
         int var12 = this.plugin.resizeGUI && var7.isBedrock() ? 54 : var5.getSize();
         InventoryHolder var13 = InventoryUtil.getTopHolder(var1);
         if (var13 instanceof ShopPage) {
            ShopPage var14 = (ShopPage)var13;
            if (this.section.equals(var14.getSection()) && var12 == var14.getSize()) {
               var14.updateContents(this.plugin, var1, var6, var5.getItemOrder(), this.fillItem, var2, this.navBar);
               if (!this.staticTitle) {
                  this.plugin.versionHandler.updateInventoryTitle(var1, this.pageTitles.get(var2).getTitle(var1, var2, this.pages.size()));
               }

               return;
            }
         }

         var7.setOpenNewGUI(true);
         var1.openInventory(
            new ShopPage(
                  this.plugin,
                  var6,
                  var5.getItemOrder(),
                  this.fillItem,
                  var1,
                  this.pageTitles.get(var2).getTitle(var1, var2, this.pages.size()),
                  this.section,
                  var2,
                  this.pages.size(),
                  var3,
                  this.navBar,
                  var12,
                  var4
               )
               .getInventory()
         );
      }
   }

   private void updateLore(
      Map<Integer, ItemStack> var1,
      Player var2,
      boolean var3,
      boolean var4,
      boolean var5,
      boolean var6,
      boolean var7,
      boolean var8,
      boolean var9,
      boolean var10,
      boolean var11,
      int var12
   ) {
      LoreFormatter.PageIndex var13 = new LoreFormatter.PageIndex(this.section, var12);
      var1.replaceAll((var13x, var14) -> {
         if (var14.getType().equals(Material.AIR)) {
            return (ItemStack)var14;
         } else {
            DisplayItem var15 = this.displayItems.get(this.section + "." + this.shopPages.get(var12).getItemOrder().get(var13x));
            if (var15 instanceof ShopItem) {
               return this.updateLore(var13, (ShopItem)var15, var14, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12);
            } else if (!(var15 instanceof ActionItem)) {
               System.out.println(var14.getType() + " - " + var13x);
               return (ItemStack)var14;
            } else {
               return this.updateLore((ActionItem)var15, var14, var2, var3 || var4, false, var10, var12);
            }
         }
      });
   }

   private ItemStack updateLore(
      LoreFormatter.PageIndex var1,
      ShopItem var2,
      ItemStack var3,
      Player var4,
      boolean var5,
      boolean var6,
      boolean var7,
      boolean var8,
      boolean var9,
      boolean var10,
      boolean var11,
      boolean var12,
      boolean var13,
      int var14
   ) {
      if (var2.isHideIfUnmet() && var2.getRequirements().stream().anyMatch(var1x -> !var1x.isMet(var4))) {
         return var2.getIfUnmet() != null ? var2.getIfUnmet().getItem(var4) : this.fillItem;
      } else {
         try {
            return this.plugin
               .getLoreFormatter()
               .format(
                  var1,
                  var2,
                  var3,
                  var4,
                  this.section,
                  this.nms && !var2.rgb,
                  var5,
                  var6,
                  var7,
                  var8,
                  var9,
                  var10,
                  var11,
                  var12,
                  var2.hasPermission(var4, "shop", true),
                  this.dynamicLore,
                  var13 && var2.hasSeasonModifier(var4.getWorld().getName()),
                  var14,
                  this.getPages()
               );
         } catch (Exception var16) {
            SendMessage.logDebugMessage("Error occurred while updating shop lore of shop item '" + var2.getItemPath() + "'");
            var16.printStackTrace();
            return var3;
         }
      }
   }

   private ItemStack updateLore(ActionItem var1, ItemStack var2, Player var3, boolean var4, boolean var5, boolean var6, int var7) {
      if (var1.isHideIfUnmet() && var1.getRequirements().stream().anyMatch(var1x -> !var1x.isMet(var3))) {
         return var1.getIfUnmet() != null ? var1.getIfUnmet().getItem(var3) : this.fillItem;
      } else {
         try {
            return this.plugin
               .getLoreFormatter()
               .format(
                  var1,
                  var2,
                  var3,
                  this.plugin.getEcoHandler().getEcon(this.section).getType(),
                  this.section,
                  this.nms,
                  var4,
                  var5,
                  var6,
                  var7,
                  this.getPages()
               );
         } catch (Exception var9) {
            SendMessage.logDebugMessage("Error occurred while updating shop lore of action item '" + var1.getItemPath() + "'");
            var9.printStackTrace();
            return var2;
         }
      }
   }

   @Override
   public ItemStack updateItem(Player var1, ShopItem var2) {
      ItemStack var3 = new ItemStack(var2.getShopItem());
      boolean var4 = PermissionsCache.hasPermission(var1, "OnyxShop.itemindexes");
      boolean var5 = PermissionsCache.hasPermission(var1, "OnyxShop.editshop.editor");
      boolean var6 = this.plugin.discountsActive && this.plugin.hasDiscount(this.section);
      boolean var7 = this.plugin.multipliers && this.plugin.hasMultiplier(this.section);
      return this.updateLore(
         (LoreFormatter.PageIndex)null,
         var2,
         var3,
         var1,
         UserManager.getUser(var1).isPr(),
         UserManager.getUser(var1).isBedrock(),
         var6,
         var7,
         this.plugin.itemStockLore,
         this.plugin.sellLimits,
         var5,
         var4,
         this.plugin.seasonalLore,
         this.getPageForShopItem(var2.itemLoc())
      );
   }

   @Override
   public void reloadItem(String var1) {
      ShopItem var2 = new ShopItem(this.plugin, this, var1.split("\\.", 2)[1]);
      this.displayItems.put(var1, var2);
      this.shopPages.get(this.getPageForShopItem(var2.itemLoc)).setItem(var2.itemLoc, var2.getShopItem());
   }

   @Override
   public void addNewItem(String var1) {
      ShopItem var2 = new ShopItem(this.plugin, this, var1.split("\\.", 2)[1]);
      this.displayItems.put(var1, var2);
      this.itemLocs.add(var2.itemLoc);
      String var3 = this.pages.get(this.pages.size() - 1);
      int var4 = this.getPageSize(var3);
      ShopPageItems var5 = this.shopPages.get(this.shopPages.size());
      int var6 = new TreeSet<>(var5.getItems().keySet()).last() + 1;
      if (var6 <= var4) {
         var5.addItem(var2.itemLoc);
         var5.setItem(var2.itemLoc, var2.getShopItem());
      } else {
         LinkedHashMap var7 = new LinkedHashMap();
         var7.put(1, var2.getShopItem());
         new ShopPageItems(var7, Collections.singletonList(var2.itemLoc), this.section, 18);
      }

      if (this.plugin.dynamicPricing) {
         this.plugin.getDynamicPricingProvider().reload();
      }
   }
}
