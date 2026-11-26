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

   public void setProvider(RotatingShopProvider var1) {
      this.provider = var1;
   }

   public RotatingShop(OnyxShop var1, String var2) {
      this.plugin = var1;
      this.section = var2;
      this.title = new ShopTitle(this.plugin.getSectionTitle(var2), this.plugin.getBedrockSectionTitle(var2));
      this.subSection = ConfigManager.getSection(this.section).getBoolean("sub-section");
      this.hidden = ConfigManager.getSection(this.section).getBoolean("hidden");
      this.dynamicLore = var1.dynamicPricing;
      this.navBar = this.plugin.navBar.isEnabled(this.section);
      this.updateInterval = this.loadUpdateInterval();
      this.itemLocs.addAll(this.plugin.getConfigManager().getRotatingItemsRaw(this.section));
      this.mappings = ConfigManager.getSection(this.section).contains("click-mappings") ? new ClickMappings(var2) : var1.getClickMappings();
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
      return this.provider.getPage(var1);
   }

   public Map<Integer, DisplayItem> getStaticItems(int var1) {
      return this.staticItems.get(var1);
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
      return this.displayItems.values().stream().filter(var0 -> ShopItem.class.equals(var0.getClass())).map(DisplayItem::itemLoc).collect(Collectors.toList());
   }

   @Override
   public ShopType getType() {
      return ShopType.ROTATING;
   }

   @Override
   public ClickAction getClickAction(ClickType var1) {
      return this.mappings.getAction(var1);
   }

   public Collection<ShopItem> getItemsInRotation() {
      if (this.provider.getCurrentItems() == null) {
         return null;
      } else {
         ArrayList var1 = new ArrayList();

         for (String var3 : this.provider.getCurrentItems()) {
            var1.add((ShopItem)this.displayItems.get(var3));
         }

         return var1;
      }
   }

   public void stopCurrent() {
      this.provider.stopCurrent();
   }

   public void forceRotate(boolean var1) {
      this.provider.loadNextRotation(var1);
   }

   public String getNextRotation() {
      return this.provider.getNextRotation();
   }

   @Override
   public int getPageForShopItem(String var1) {
      return this.provider.getPageForShopItem(var1);
   }

   @Override
   public List<ShopItem> getPageItems(int var1) {
      List var2 = this.provider
         .getPage(var1)
         .getItemOrder()
         .stream()
         .filter(Objects::nonNull)
         .map(var1x -> this.displayItems.get(this.section + "." + var1x))
         .filter(var0 -> var0 instanceof ShopItem)
         .map(var0 -> (ShopItem)var0)
         .collect(Collectors.toCollection(ArrayList::new));
      if (this.staticItems.containsKey(var1)) {
         this.staticItems.get(var1).values().stream().filter(var0 -> var0 instanceof ShopItem).map(var0 -> (ShopItem)var0).forEach(var2::add);
      }

      return var2;
   }

   private void loadShopItems() {
      try {
         ArrayList var1 = new ArrayList();

         for (String var3 : this.itemLocs) {
            Object var4 = ConfigManager.getShop(this.section).contains(var3 + ".action")
               ? new ActionItem(this.plugin, this, var3)
               : new ShopItem(this.plugin, this, var3);
            if (ConfigManager.getShop(this.section).contains(var3 + ".slot")) {
               var1.add(var3);

               for (Integer var6 : this.plugin.calculateAmount.getSlots(ConfigManager.getShop(this.section).getString(var3 + ".slot"))) {
                  Map var7 = this.staticItems.computeIfAbsent(var6 / 54 + 1, var0 -> new HashMap<>());
                  var7.put(var6 - var6 / 54 * 54, var4);
               }
            }

            this.displayItems.put(this.section + "." + var3, (DisplayItem)var4);
         }

         this.itemLocs.removeAll(var1);
      } catch (NullPointerException var8) {
         var8.printStackTrace();
      }

      if (ConfigManager.getSection(this.section).get("fill-item") != null) {
         this.loadFillItem();
      }
   }

   private long loadUpdateInterval() {
      String var1 = ConfigManager.getSection(this.section).getString("update-interval");
      if (var1 == null) {
         return 0L;
      } else {
         try {
            return TimeUtils.getTime(var1) / 1000L * 20L;
         } catch (Exception var3) {
            SendMessage.warnMessage("Invalid update-interval for shop '" + this.section + "', disabling periodic item updates...");
            return 0L;
         }
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
      ShopPageItems var5 = this.provider.getPage(var2);
      Map var6 = var5.getItems();
      if (this.staticItems.containsKey(var2)) {
         this.staticItems.get(var2).forEach((var1x, var2x) -> var6.put(var1x, var2x.getShopItem()));
      }

      User var7 = UserManager.getUser(var1);
      boolean var8 = PermissionsCache.hasPermission(var1, "OnyxShop.itemindexes");
      boolean var9 = this.plugin.discountsActive && this.plugin.hasDiscount(this.section);
      boolean var10 = this.plugin.multipliers && this.plugin.hasMultiplier(this.section);
      this.updateLore(
         var6, var1, var7.isPr(), var7.isBedrock(), var9, var10, this.plugin.itemStockLore, this.plugin.sellLimits, false, var8, this.plugin.seasonalLore, var2
      );
      if (this.plugin.bedrock && this.plugin.bedrockMenus && var7.isBedrock() && this.plugin.getBedrockInventorys().isEnabledFor(var1)) {
         this.plugin
            .getBedrockInventorys()
            .openShopSection(var1, this, var2, var3, var4, var6, var5.getItemOrder(), this.fillItem, this.getTitle(var1, var2), var5.getSize(), this.navBar);
      } else {
         int var11 = this.plugin.resizeGUI && var7.isBedrock() ? 54 : var5.getSize();
         InventoryHolder var12 = InventoryUtil.getTopHolder(var1);
         if (var12 instanceof ShopPage) {
            ShopPage var13 = (ShopPage)var12;
            if (this.section.equals(var13.getSection()) && var11 == var13.getSize()) {
               var13.updateContents(this.plugin, var1, var6, var5.getItemOrder(), this.fillItem, var2, this.navBar);
               this.plugin.versionHandler.updateInventoryTitle(var1, this.getTitle(var1, var2));
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
                  this.getTitle(var1, var2),
                  this.section,
                  var2,
                  this.provider.getPageSize(),
                  var3,
                  this.navBar,
                  var11,
                  var4
               )
               .getInventory()
         );
      }
   }

   public void reopen() {
      for (Player var2 : Bukkit.getOnlinePlayers()) {
         InventoryHolder var3 = InventoryUtil.getTopHolder(var2);
         if (var3 instanceof ShopPage || var3 instanceof TransactionMenu) {
            if (var3 instanceof ShopPage) {
               ShopPage var4 = (ShopPage)var3;
               if (this.section.equals(var4.getSection())) {
                  this.openShopSection(var2, var4.getPage(), var4.isDisabledBackButton(), var4.getRootSection());
               }
            } else {
               TransactionMenu var5 = (TransactionMenu)var3;
               if (this.section.equals(var5.getShopItem().section)) {
                  this.openShopSection(var2, 1, var5.isDisabledBackButton(), var5.getRootSection());
               }
            }
         }
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
      var1.replaceAll(
         (var13x, var14) -> {
            if (var14.getType().equals(Material.AIR)) {
               return (ItemStack)var14;
            } else {
               String var15 = this.provider.getPage(var12).getItem(var13x);
               if (var15 != null) {
                  DisplayItem var16 = this.displayItems.get(this.section + "." + var15);
                  return var16 instanceof ShopItem
                     ? this.updateLore(var13, (ShopItem)var16, var14, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12)
                     : this.updateLore((ActionItem)var16, var14, var2, var3 || var4, false, var10, var12);
               } else {
                  return this.updateLore(
                     var13, (ShopItem)this.staticItems.get(var12).get(var13x), var14, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12
                  );
               }
            }
         }
      );
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
         SendMessage.logDebugMessage("Error occurred while updating shop lore of item '" + var2.getItemPath() + "'");
         var16.printStackTrace();
         return var3;
      }
   }

   private ItemStack updateLore(ActionItem var1, ItemStack var2, Player var3, boolean var4, boolean var5, boolean var6, int var7) {
      try {
         return this.plugin
            .getLoreFormatter()
            .format(
               var1, var2, var3, this.plugin.getEcoHandler().getEcon(this.section).getType(), this.section, this.nms, var4, var5, var6, var7, this.getPages()
            );
      } catch (Exception var9) {
         SendMessage.logDebugMessage("Error occurred while updating shop lore of action item '" + var1.getItemPath() + "'");
         var9.printStackTrace();
         return var2;
      }
   }

   private Translatable getTitle(Player var1, int var2) {
      return this.pageTitles.get(var2).getTitle(var1, var2, this.provider.getPageSize());
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
         this.getPageForShopItem(var2.itemLoc)
      );
   }

   @Override
   public void reloadItem(String var1) {
      ShopItem var2 = new ShopItem(this.plugin, this, var1.split("\\.", 2)[1]);
      this.displayItems.put(var1, var2);
      this.provider.getPage(this.getPageForShopItem(var2.itemLoc)).setItem(var2.itemLoc, var2.getShopItem());
   }

   @Override
   public void addNewItem(String var1) {
   }
}
