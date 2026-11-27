package com.onyxshop.objects.shops.types;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ActionItem;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.objects.LoreFormatter;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.ShopPage;
import com.onyxshop.objects.ShopPageItems;
import com.onyxshop.objects.TransactionMenu;
import com.onyxshop.objects.User;
import com.onyxshop.objects.mappings.ClickAction;
import com.onyxshop.objects.mappings.ClickMappings;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.objects.titles.ShopTitle;
import com.onyxshop.providers.RotatingShopProvider;
import com.onyxshop.providers.UserManager;
import com.onyxshop.util.InventoryUtil;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.TimeUtils;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class RotatingShop implements ShopSection {
   private final String section;
   public final ShopTitle title;
   private final boolean subSection;
   private final boolean hidden;
   private final boolean dynamicLore;
   private final boolean navBar;
   private final long updateInterval;
   private ItemStack fillItem;
   public final Map<Integer, ShopTitle> pageTitles = new HashMap<>();
   private final Map<String, DisplayItem> displayItems = new LinkedHashMap<>();
   private final Map<Integer, Map<Integer, DisplayItem>> staticItems = new HashMap<>();
   private final List<String> clickCommands;
   private final ClickMappings mappings;
   private boolean closeMenu = false;
   public final OnyxShop plugin;
   private RotatingShopProvider provider;
   private final boolean nms = OnyxShop.getInstance().versionHandler != null
      && ConfigManager.getConfig().getBoolean("use-nms", true)
      && Bukkit.getPluginManager().getPlugin("EcoEnchants") == null;
   public final List<String> itemLocs = new ArrayList<>();

   public void setProvider(RotatingShopProvider rotatingShopProvider) {
      this.provider = rotatingShopProvider;
   }

   public RotatingShop(OnyxShop plugin, String shopId) {
      this.plugin = plugin;
      this.section = shopId;
      this.title = new ShopTitle(this.plugin.getSectionTitle(shopId), this.plugin.getBedrockSectionTitle(shopId));
      this.subSection = ConfigManager.getSection(this.section).getBoolean("sub-section");
      this.hidden = ConfigManager.getSection(this.section).getBoolean("hidden");
      this.dynamicLore = plugin.dynamicPricing;
      this.navBar = this.plugin.navBar.isEnabled(this.section);
      this.updateInterval = this.loadUpdateInterval();
      this.itemLocs.addAll(this.plugin.getConfigManager().getRotatingItemsRaw(this.section));
      this.mappings = ConfigManager.getSection(this.section).contains("click-mappings") ? new ClickMappings(shopId) : plugin.getClickMappings();
      if (!(this.clickCommands = ConfigManager.getSection(this.section).getStringList("click-commands")).isEmpty()) {
         this.closeMenu = ConfigManager.getSection(this.section).getBoolean("close-menu");
      } else {
         this.loadShopItems();
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
      return this.provider.getPage(page);
   }

   public Map<Integer, DisplayItem> getStaticItems(int value) {
      return this.staticItems.get(value);
   }

   @Override
   public int getPages() {
      return this.provider.getPageSize();
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
      return ShopType.ROTATING;
   }

   @Override
   public ClickAction getClickAction(ClickType clickType) {
      return this.mappings.getAction(clickType);
   }

   public Collection<ShopItem> getItemsInRotation() {
      if (this.provider.getCurrentItems() == null) {
         return null;
      } else {
         ArrayList values = new ArrayList();

         for (String text : this.provider.getCurrentItems()) {
            values.add((ShopItem)this.displayItems.get(text));
         }

         return values;
      }
   }

   public void stopCurrent() {
      this.provider.stopCurrent();
   }

   public void forceRotate(boolean flag) {
      this.provider.loadNextRotation(flag);
   }

   public String getNextRotation() {
      return this.provider.getNextRotation();
   }

   @Override
   public int getPageForShopItem(String shopId) {
      return this.provider.getPageForShopItem(shopId);
   }

   @Override
   public List<ShopItem> getPageItems(int page) {
      List values = this.provider
         .getPage(page)
         .getItemOrder()
         .stream()
         .filter(Objects::nonNull)
         .map(element -> this.displayItems.get(this.section + "." + element))
         .filter(element -> element instanceof ShopItem)
         .map(element -> (ShopItem)element)
         .collect(Collectors.toCollection(ArrayList::new));
      if (this.staticItems.containsKey(page)) {
         this.staticItems.get(page).values().stream().filter(element -> element instanceof ShopItem).map(element -> (ShopItem)element).forEach(values::add);
      }

      return values;
   }

   private void loadShopItems() {
      try {
         ArrayList shopIds = new ArrayList();

         for (String itemId : this.itemLocs) {
            Object value = ConfigManager.getShop(this.section).contains(itemId + ".action")
               ? new ActionItem(this.plugin, this, itemId)
               : new ShopItem(this.plugin, this, itemId);
            if (ConfigManager.getShop(this.section).contains(itemId + ".slot")) {
               shopIds.add(itemId);

               for (Integer amount : this.plugin.calculateAmount.getSlots(ConfigManager.getShop(this.section).getString(itemId + ".slot"))) {
                  Map map = this.staticItems.computeIfAbsent(amount / 54 + 1, key -> new HashMap<>());
                  map.put(amount - amount / 54 * 54, value);
               }
            }

            this.displayItems.put(this.section + "." + itemId, (DisplayItem)value);
         }

         this.itemLocs.removeAll(shopIds);
      } catch (NullPointerException exception) {
         exception.printStackTrace();
      }

      if (ConfigManager.getSection(this.section).get("fill-item") != null) {
         this.loadFillItem();
      }
   }

   private long loadUpdateInterval() {
      String text = ConfigManager.getSection(this.section).getString("update-interval");
      if (text == null) {
         return 0L;
      } else {
         try {
            return TimeUtils.getTime(text) / 1000L * 20L;
         } catch (Exception exception) {
            SendMessage.warnMessage("Invalid update-interval for shop '" + this.section + "', disabling periodic item updates...");
            return 0L;
         }
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
      ShopPageItems shopPageItems = this.provider.getPage(value);
      Map items = shopPageItems.getItems();
      if (this.staticItems.containsKey(value)) {
         this.staticItems.get(value).forEach((key, value2) -> items.put(key, value2.getShopItem()));
      }

      User user = UserManager.getUser(player);
      boolean permission = PermissionsCache.hasPermission(player, "OnyxShop.itemindexes");
      boolean flag2 = this.plugin.discountsActive && this.plugin.hasDiscount(this.section);
      boolean flag3 = this.plugin.multipliers && this.plugin.hasMultiplier(this.section);
      this.updateLore(
         items, player, user.isPr(), user.isBedrock(), flag2, flag3, this.plugin.itemStockLore, this.plugin.sellLimits, false, permission, this.plugin.seasonalLore, value
      );
      if (this.plugin.bedrock && this.plugin.bedrockMenus && user.isBedrock() && this.plugin.getBedrockInventorys().isEnabledFor(player)) {
         this.plugin
            .getBedrockInventorys()
            .openShopSection(player, this, value, flag, shopId, items, shopPageItems.getItemOrder(), this.fillItem, this.getTitle(player, value), shopPageItems.getSize(), this.navBar);
      } else {
         int size = this.plugin.resizeGUI && user.isBedrock() ? 54 : shopPageItems.getSize();
         InventoryHolder holder = InventoryUtil.getTopHolder(player);
         if (holder instanceof ShopPage) {
            ShopPage shopPage = (ShopPage)holder;
            if (this.section.equals(shopPage.getSection()) && size == shopPage.getSize()) {
               shopPage.updateContents(this.plugin, player, items, shopPageItems.getItemOrder(), this.fillItem, value, this.navBar);
               this.plugin.versionHandler.updateInventoryTitle(player, this.getTitle(player, value));
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
                  this.getTitle(player, value),
                  this.section,
                  value,
                  this.provider.getPageSize(),
                  flag,
                  this.navBar,
                  size,
                  shopId
               )
               .getInventory()
         );
      }
   }

   public void reopen() {
      for (Player player : Bukkit.getOnlinePlayers()) {
         OnyxShop.getInstance().runTaskFor(() -> this.reopenFor(player), player);
      }
   }

   private void reopenFor(Player player) {
      InventoryHolder holder = InventoryUtil.getTopHolder(player);
      if (holder instanceof ShopPage || holder instanceof TransactionMenu) {
         if (holder instanceof ShopPage) {
            ShopPage shopPage = (ShopPage)holder;
            if (this.section.equals(shopPage.getSection())) {
               this.openShopSection(player, shopPage.getPage(), shopPage.isDisabledBackButton(), shopPage.getRootSection());
            }
         } else {
            TransactionMenu transactionMenu = (TransactionMenu)holder;
            if (this.section.equals(transactionMenu.getShopItem().section)) {
               this.openShopSection(player, 1, transactionMenu.isDisabledBackButton(), transactionMenu.getRootSection());
            }
         }
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
      map.replaceAll(
         (key, value2) -> {
            if (value2.getType().equals(Material.AIR)) {
               return (ItemStack)value2;
            } else {
               String item = this.provider.getPage(value).getItem(key);
               if (item != null) {
                  DisplayItem displayItem = this.displayItems.get(this.section + "." + item);
                  return displayItem instanceof ShopItem
                     ? this.updateLore(pageIndex, (ShopItem)displayItem, value2, player, flag, flag2, flag3, flag4, flag5, flag6, flag7, flag8, flag9, value)
                     : this.updateLore((ActionItem)displayItem, value2, player, flag || flag2, false, flag8, value);
               } else {
                  return this.updateLore(
                     pageIndex, (ShopItem)this.staticItems.get(value).get(key), value2, player, flag, flag2, flag3, flag4, flag5, flag6, flag7, flag8, flag9, value
                  );
               }
            }
         }
      );
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
         SendMessage.logDebugMessage("Error occurred while updating shop lore of item '" + shopItem.getItemPath() + "'");
         exception.printStackTrace();
         return itemStack;
      }
   }

   private ItemStack updateLore(ActionItem actionItem, ItemStack itemStack, Player player, boolean flag, boolean flag2, boolean flag3, int value) {
      try {
         return this.plugin
            .getLoreFormatter()
            .format(
               actionItem, itemStack, player, this.plugin.getEcoHandler().getEcon(this.section).getType(), this.section, this.nms, flag, flag2, flag3, value, this.getPages()
            );
      } catch (Exception exception) {
         SendMessage.logDebugMessage("Error occurred while updating shop lore of action item '" + actionItem.getItemPath() + "'");
         exception.printStackTrace();
         return itemStack;
      }
   }

   private Translatable getTitle(Player player, int value) {
      return this.pageTitles.get(value).getTitle(player, value, this.provider.getPageSize());
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
         this.getPageForShopItem(shopItem.itemLoc)
      );
   }

   @Override
   public void reloadItem(String itemId) {
      ShopItem shopItem = new ShopItem(this.plugin, this, itemId.split("\\.", 2)[1]);
      this.displayItems.put(itemId, shopItem);
      this.provider.getPage(this.getPageForShopItem(shopItem.itemLoc)).setItem(shopItem.itemLoc, shopItem.getShopItem());
   }

   @Override
   public void addNewItem(String itemId) {
   }
}
