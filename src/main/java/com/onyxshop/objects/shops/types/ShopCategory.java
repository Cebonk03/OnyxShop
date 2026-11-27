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

   public ShopCategory(OnyxShop plugin, String shopId) {
      this.plugin = plugin;
      this.section = shopId;
      this.title = new ShopTitle(this.plugin.getSectionTitle(shopId), this.plugin.getBedrockSectionTitle(shopId));
      this.subSection = ConfigManager.getSection(this.section).getBoolean("sub-section");
      this.hidden = ConfigManager.getSection(this.section).getBoolean("hidden");
      this.dynamicLore = plugin.dynamicPricing;
      this.pages = this.plugin.getConfigManager().getPages(this.section, false);
      this.navBar = this.plugin.navBar.isEnabled(this.section);
      this.updateInterval = this.loadUpdateInterval();
      this.itemLocs.addAll(this.plugin.getConfigManager().getItemsRaw(this.section));
      this.mappings = ConfigManager.getSection(this.section).contains("click-mappings") ? new ClickMappings(shopId) : plugin.getClickMappings();
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
   public DisplayItem getDisplayItem(String itemId) {
      return this.displayItems.get(itemId);
   }

   @Override
   public boolean isActionItem(String itemId, String text) {
      return this.displayItems.get(itemId + "." + text) instanceof ActionItem;
   }

   @Override
   public Collection<ShopItem> getShopItems() {
      return this.displayItems
         .values()
         .stream()
         .filter(element -> ShopItem.class.equals(element.getClass()))
         .map(ShopItem.class::cast)
         .collect(Collectors.toCollection(ArrayList::new));
   }

   @Override
   public ShopItem getShopItem(String shopId) {
      DisplayItem displayItem = this.displayItems.get(shopId);
      return displayItem instanceof ShopItem ? (ShopItem)displayItem : null;
   }

   @Override
   public String getSection() {
      return this.section;
   }

   @Override
   public ShopPageItems getShopPageItems(int page) {
      return this.shopPages.get(page);
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
      return this.displayItems.values().stream().filter(element -> ShopItem.class.equals(element.getClass())).map(DisplayItem::itemLoc).collect(Collectors.toList());
   }

   @Override
   public ShopType getType() {
      return ShopType.CATEGORY;
   }

   @Override
   public ClickAction getClickAction(ClickType clickType) {
      return this.mappings.getAction(clickType);
   }

   @Override
   public int getPageForShopItem(String shopId) {
      return this.pages.indexOf(shopId.split("\\.")[0]) + 1;
   }

   @Override
   public List<ShopItem> getPageItems(int page) {
      return this.shopPages
         .get(page)
         .getItemOrder()
         .stream()
         .filter(Objects::nonNull)
         .map(element -> this.displayItems.get(this.section + "." + element))
         .filter(element -> element instanceof ShopItem)
         .map(element -> (ShopItem)element)
         .collect(Collectors.toList());
   }

   private void loadShopItems() {
      try {
         for (String shopId : this.itemLocs) {
            this.displayItems
               .put(
                  this.section + "." + shopId,
                  (DisplayItem)(ConfigManager.getShop(this.section).contains("pages." + shopId + ".action")
                     ? new ActionItem(this.plugin, this, shopId)
                     : new ShopItem(this.plugin, this, shopId))
               );
         }

         if (!this.pages.isEmpty()) {
            this.loadPages();
         } else {
            SendMessage.errorMessage("No pages found for shop " + this.section + ", adding empty page...");
            this.shopPages.put(1, new ShopPageItems(this.section));
            this.pageTitles.put(1, this.title);
         }
      } catch (NullPointerException exception) {
         exception.printStackTrace();
      }

      if (ConfigManager.getSection(this.section).get("fill-item") != null) {
         this.loadFillItem();
      }
   }

   private void loadFillItem() {
      try {
         this.fillItem = this.plugin.createItem.loadSectionItem(this.section, "fill-item");
      } catch (ItemLoadException exception) {
         SendMessage.errorMessage(Lang.EXCEPTION_LOADING_FILL_ITEM.get().replace("%section%", this.section));
         SendMessage.warnMessage(exception.getMessage());
      }
   }

   private void loadPages() {
      try {
         int value = 1;

         for (String text : this.pages) {
            Integer pageSize = this.getPageSize(text);
            int value2 = this.plugin.navBar.isEnabled(this.section) ? (pageSize == null ? 45 : pageSize) : (pageSize == null ? 54 : pageSize);
            ItemLayout itemLayout = this.getItemLayout(text, value2);
            List values = this.plugin
               .calculateAmount
               .loadItemsInOrder((ItemLayout)(itemLayout == null ? new PageLayout(this.section) : itemLayout), this.section, text, pageSize, value2);
            if (pageSize == null) {
               pageSize = values.size() <= 45 && !values.isEmpty() ? CalculateAmount.getSlots(values.size()) : 54;
            }

            ArrayList values2 = new ArrayList();
            LinkedHashMap map = new LinkedHashMap();

            for (int size = 0; size < values.size(); size++) {
               String text2 = (String)values.get(size);
               if (text2 != null) {
                  map.put(size, this.displayItems.get(this.section + "." + text2).getShopItem());
               }

               values2.add(text2);
            }

            this.shopPages.put(value, new ShopPageItems(map, values2, this.section, pageSize));
            this.pageTitles.put(value, this.getPageTitle(text));
            value++;
         }
      } catch (Exception exception) {
         SendMessage.warnMessage("Error occurred while loading shop items for section " + this.section);
         exception.printStackTrace();
      }
   }

   private ItemLayout getItemLayout(String itemId, int value) {
      List stringList = ConfigManager.getSection(this.section).getStringList("item-layout");
      if (stringList.isEmpty()) {
         return null;
      } else if (stringList.size() < value / 9) {
         SendMessage.errorMessage(
            "Invalid item layout for " + this.section + " page '" + itemId + "': Expected " + value / 9 + " rows of values, but got " + stringList.size() + " instead"
         );
         return null;
      } else {
         try {
            ArrayList values = new ArrayList();

            for (int index = 0; index < value / 9; index++) {
               String[] values2 = ((String)stringList.get(index)).split(" ");
               if (values2.length != 9) {
                  SendMessage.errorMessage(
                     "Invalid item layout for " + this.section + " page '" + itemId + "': Row " + (index + 1) + " contains less or more symbols, expected 9"
                  );
                  return null;
               }

               if (Arrays.stream(values2).anyMatch(element -> !element.equals("0") && !element.equals("1"))) {
                  SendMessage.errorMessage(
                     "Invalid item layout for "
                        + this.section
                        + " page '"
                        + itemId
                        + "': Items from row "
                        + (index + 1)
                        + " does not match expected values, expected 0 or 1"
                  );
                  return null;
               }

               Arrays.stream(values2).forEach(entry -> values.add(entry.equals("1")));
            }

            ReservedLayout reservedLayout = new ReservedLayout(this.section);

            for (int index = 0; index < value / 9; index++) {
               for (int index2 = 0; index2 < 9; index2++) {
                  int value4 = index * 9 + index2;
                  if ((Boolean)values.get(value4)) {
                     reservedLayout.reserve(value4);
                  }
               }
            }

            return reservedLayout;
         } catch (IndexOutOfBoundsException exception) {
            SendMessage.errorMessage("Unexpected error occurred while loading item layout for " + this.section + " page '" + itemId + "'");
            exception.printStackTrace();
            return null;
         }
      }
   }

   private long loadUpdateInterval() {
      String text = ConfigManager.getSection(this.section).getString("update-interval");
      if (text == null) {
         return 0L;
      } else {
         try {
            return TimeUtils.getTime(text) / 1000L * 20L;
         } catch (ParseException exception) {
            SendMessage.warnMessage("Invalid update-interval for shop '" + this.section + "', disabling periodic item updates...");
            return 0L;
         }
      }
   }

   private Integer getPageSize(String text) {
      if (!ConfigManager.getShop(this.section).contains("pages." + text + ".gui-rows")) {
         return null;
      } else {
         try {
            int page = Integer.parseInt(ConfigManager.getShop(this.section).getString("pages." + text + ".gui-rows"));
            if (page >= 1 && page <= 6) {
               return page * 9;
            }

            SendMessage.warnMessage("Inventory size for page '" + text + "' from shop '" + this.section + "' has to be in range from 1-6, using default...");
         } catch (NumberFormatException exception) {
            SendMessage.warnMessage("Invalid inventory size for page '" + text + "' from shop '" + this.section + "', using default...");
         }

         return null;
      }
   }

   private ShopTitle getPageTitle(String title) {
      Translatable text = null;
      if (ConfigManager.getShop(this.section).contains("pages." + title + ".bedrock-title")) {
         text = SendMessage.fromConfig(this.plugin.getInvTitle(ConfigManager.getShop(this.section).getString("pages." + title + ".bedrock-title")));
      }

      if (ConfigManager.getShop(this.section).contains("pages." + title + ".title")) {
         String text2 = ConfigManager.getShop(this.section).getString("pages." + title + ".title");
         return new ShopTitle(SendMessage.fromConfig(this.plugin.getInvTitle(text2)), text);
      } else {
         return text != null ? new ShopTitle(this.title.getTitle(), text) : this.title;
      }
   }

   private boolean isStaticTitle() {
      String raw = this.title.getRaw();

      for (ShopTitle shopTitle : this.pageTitles.values()) {
         if (!shopTitle.getRaw().equals(raw) || !shopTitle.isStatic()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public void openShopSection(Player player, boolean flag) {
      this.openShopSection(player, 1, flag);
   }

   @Override
   public void openShopSection(Player player, boolean flag, String shopId) {
      this.openShopSection(player, 1, flag, shopId);
   }

   @Override
   public void openShopSection(Player player, int value, boolean flag) {
      this.openShopSection(player, value, flag, null);
   }

   @Override
   public void openShopSection(Player player, int value, boolean flag, @Nullable String shopId) {
      ShopPageItems shopPageItems = this.shopPages.get(value);
      Map items = shopPageItems.getItems();
      User user = UserManager.getUser(player);
      boolean permission = PermissionsCache.hasPermission(player, "OnyxShop.itemindexes");
      boolean permission2 = PermissionsCache.hasPermission(player, "OnyxShop.editshop.editor");
      boolean flag2 = this.plugin.discountsActive && this.plugin.hasDiscount(this.section);
      boolean flag3 = this.plugin.multipliers && this.plugin.hasMultiplier(this.section);
      this.updateLore(
         items, player, user.isPr(), user.isBedrock(), flag2, flag3, this.plugin.itemStockLore, this.plugin.sellLimits, permission2, permission, this.plugin.seasonalLore, value
      );
      if (this.plugin.bedrock && this.plugin.bedrockMenus && user.isBedrock() && this.plugin.getBedrockInventorys().isEnabledFor(player)) {
         this.plugin
            .getBedrockInventorys()
            .openShopSection(
               player,
               this,
               value,
               flag,
               shopId,
               items,
               shopPageItems.getItemOrder(),
               this.fillItem,
               this.pageTitles.get(value).getTitle(player, value, this.pages.size()),
               shopPageItems.getSize(),
               this.navBar
            );
      } else {
         int size = this.plugin.resizeGUI && user.isBedrock() ? 54 : shopPageItems.getSize();
         InventoryHolder holder = InventoryUtil.getTopHolder(player);
         if (holder instanceof ShopPage) {
            ShopPage shopPage = (ShopPage)holder;
            if (this.section.equals(shopPage.getSection()) && size == shopPage.getSize()) {
               shopPage.updateContents(this.plugin, player, items, shopPageItems.getItemOrder(), this.fillItem, value, this.navBar);
               if (!this.staticTitle) {
                  this.plugin.versionHandler.updateInventoryTitle(player, this.pageTitles.get(value).getTitle(player, value, this.pages.size()));
               }

               return;
            }
         }

         user.setOpenNewGUI(true);
         player.openInventory(
            new ShopPage(
                  this.plugin,
                  items,
                  shopPageItems.getItemOrder(),
                  this.fillItem,
                  player,
                  this.pageTitles.get(value).getTitle(player, value, this.pages.size()),
                  this.section,
                  value,
                  this.pages.size(),
                  flag,
                  this.navBar,
                  size,
                  shopId
               )
               .getInventory()
         );
      }
   }

   private void updateLore(
      Map<Integer, ItemStack> map,
      Player player,
      boolean flag,
      boolean flag2,
      boolean flag3,
      boolean flag4,
      boolean flag5,
      boolean flag6,
      boolean flag7,
      boolean flag8,
      boolean flag9,
      int value
   ) {
      LoreFormatter.PageIndex pageIndex = new LoreFormatter.PageIndex(this.section, value);
      map.replaceAll((key, value2) -> {
         if (value2.getType().equals(Material.AIR)) {
            return (ItemStack)value2;
         } else {
            DisplayItem displayItem = this.displayItems.get(this.section + "." + this.shopPages.get(value).getItemOrder().get(key));
            if (displayItem instanceof ShopItem) {
               return this.updateLore(pageIndex, (ShopItem)displayItem, value2, player, flag, flag2, flag3, flag4, flag5, flag6, flag7, flag8, flag9, value);
            } else if (!(displayItem instanceof ActionItem)) {
               System.out.println(value2.getType() + " - " + key);
               return (ItemStack)value2;
            } else {
               return this.updateLore((ActionItem)displayItem, value2, player, flag || flag2, false, flag8, value);
            }
         }
      });
   }

   private ItemStack updateLore(
      LoreFormatter.PageIndex pageIndex,
      ShopItem shopItem,
      ItemStack itemStack,
      Player player,
      boolean flag,
      boolean flag2,
      boolean flag3,
      boolean flag4,
      boolean flag5,
      boolean flag6,
      boolean flag7,
      boolean flag8,
      boolean flag9,
      int value
   ) {
      if (shopItem.isHideIfUnmet() && shopItem.getRequirements().stream().anyMatch(element -> !element.isMet(player))) {
         return shopItem.getIfUnmet() != null ? shopItem.getIfUnmet().getItem(player) : this.fillItem;
      } else {
         try {
            return this.plugin
               .getLoreFormatter()
               .format(
                  pageIndex,
                  shopItem,
                  itemStack,
                  player,
                  this.section,
                  this.nms && !shopItem.rgb,
                  flag,
                  flag2,
                  flag3,
                  flag4,
                  flag5,
                  flag6,
                  flag7,
                  flag8,
                  shopItem.hasPermission(player, "shop", true),
                  this.dynamicLore,
                  flag9 && shopItem.hasSeasonModifier(player.getWorld().getName()),
                  value,
                  this.getPages()
               );
         } catch (Exception exception) {
            SendMessage.logDebugMessage("Error occurred while updating shop lore of shop item '" + shopItem.getItemPath() + "'");
            exception.printStackTrace();
            return itemStack;
         }
      }
   }

   private ItemStack updateLore(ActionItem actionItem, ItemStack itemStack, Player player, boolean flag, boolean flag2, boolean flag3, int value) {
      if (actionItem.isHideIfUnmet() && actionItem.getRequirements().stream().anyMatch(element -> !element.isMet(player))) {
         return actionItem.getIfUnmet() != null ? actionItem.getIfUnmet().getItem(player) : this.fillItem;
      } else {
         try {
            return this.plugin
               .getLoreFormatter()
               .format(
                  actionItem,
                  itemStack,
                  player,
                  this.plugin.getEcoHandler().getEcon(this.section).getType(),
                  this.section,
                  this.nms,
                  flag,
                  flag2,
                  flag3,
                  value,
                  this.getPages()
               );
         } catch (Exception exception) {
            SendMessage.logDebugMessage("Error occurred while updating shop lore of action item '" + actionItem.getItemPath() + "'");
            exception.printStackTrace();
            return itemStack;
         }
      }
   }

   @Override
   public ItemStack updateItem(Player player, ShopItem shopItem) {
      ItemStack itemStack = new ItemStack(shopItem.getShopItem());
      boolean permission = PermissionsCache.hasPermission(player, "OnyxShop.itemindexes");
      boolean permission2 = PermissionsCache.hasPermission(player, "OnyxShop.editshop.editor");
      boolean flag = this.plugin.discountsActive && this.plugin.hasDiscount(this.section);
      boolean flag2 = this.plugin.multipliers && this.plugin.hasMultiplier(this.section);
      return this.updateLore(
         (LoreFormatter.PageIndex)null,
         shopItem,
         itemStack,
         player,
         UserManager.getUser(player).isPr(),
         UserManager.getUser(player).isBedrock(),
         flag,
         flag2,
         this.plugin.itemStockLore,
         this.plugin.sellLimits,
         permission2,
         permission,
         this.plugin.seasonalLore,
         this.getPageForShopItem(shopItem.itemLoc())
      );
   }

   @Override
   public void reloadItem(String itemId) {
      ShopItem shopItem = new ShopItem(this.plugin, this, itemId.split("\\.", 2)[1]);
      this.displayItems.put(itemId, shopItem);
      this.shopPages.get(this.getPageForShopItem(shopItem.itemLoc)).setItem(shopItem.itemLoc, shopItem.getShopItem());
   }

   @Override
   public void addNewItem(String itemId) {
      ShopItem shopItem = new ShopItem(this.plugin, this, itemId.split("\\.", 2)[1]);
      this.displayItems.put(itemId, shopItem);
      this.itemLocs.add(shopItem.itemLoc);
      String text = this.pages.get(this.pages.size() - 1);
      int pageSize = this.getPageSize(text);
      ShopPageItems shopPageItems = this.shopPages.get(this.shopPages.size());
      int value = new TreeSet<>(shopPageItems.getItems().keySet()).last() + 1;
      if (value <= pageSize) {
         shopPageItems.addItem(shopItem.itemLoc);
         shopPageItems.setItem(shopItem.itemLoc, shopItem.getShopItem());
      } else {
         LinkedHashMap map = new LinkedHashMap();
         map.put(1, shopItem.getShopItem());
         new ShopPageItems(map, Collections.singletonList(shopItem.itemLoc), this.section, 18);
      }

      if (this.plugin.dynamicPricing) {
         this.plugin.getDynamicPricingProvider().reload();
      }
   }
}
