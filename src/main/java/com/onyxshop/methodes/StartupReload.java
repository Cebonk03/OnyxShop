package com.onyxshop.methodes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.VersionHandler;
import com.onyxshop.api.events.ShopItemsLoadEvent;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.objects.LoreFormatter;
import com.onyxshop.objects.SectionItem;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.ShopSearch;
import com.onyxshop.objects.TransactionMenus;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.objects.shops.types.ShopCategory;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.RotatingShopProvider;
import com.onyxshop.providers.StandProvider;
import com.onyxshop.util.AnvilGUI;
import com.onyxshop.util.BackButton;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import com.onyxshop.util.LocalizedItemStack;
import com.onyxshop.util.NBTMaps;
import com.onyxshop.util.ServerInfo;
import com.onyxshop.util.SortOrder;
import com.onyxshop.util.exceptions.ShopLoadException;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;

public class StartupReload {
   private OnyxShop plugin;

   public StartupReload(OnyxShop plugin) {
      this.plugin = plugin;
   }

   public void loadInventoryTitles() {
      this.plugin.getSectionTitles().clear();
      this.plugin.getBedrockSectionTitles().clear();
      HashMap map = new HashMap();
      HashMap map2 = new HashMap();

      for (String shopId : this.plugin.getShopSections()) {
         String text = ConfigManager.getSection(shopId).getString("title");
         if (text == null || text.isEmpty()) {
            text = ConfigManager.getSection(shopId).getString("item.name");
         }

         if (text != null && !text.isEmpty()) {
            map.put(shopId, SendMessage.fromConfig(this.plugin.getInvTitle(text)));
         } else {
            map.put(shopId, Lang.DISPLAYNAME_NULL.get());
         }

         String text2 = ConfigManager.getSection(shopId).getString("bedrock-title");
         if (text2 != null && !text2.isEmpty()) {
            map2.put(shopId, SendMessage.fromConfig(this.plugin.getInvTitle(text2)));
         }
      }

      this.plugin.setSectionTitles(map);
      this.plugin.setBedrockSectionTitles(map2);
      this.plugin.getTitleManager().load();
   }

   public void checkIfLanguageFilesExist() {
      String[] values = new String[]{
         "lang-cs.yml",
         "lang-de.yml",
         "lang-en.yml",
         "lang-es.yml",
         "lang-fr.yml",
         "lang-hu.yml",
         "lang-it.yml",
         "lang-ja.yml",
         "lang-ko.yml",
         "lang-nl.yml",
         "lang-pl.yml",
         "lang-pt.yml",
         "lang-ru.yml",
         "lang-sk.yml",
         "lang-sv.yml",
         "lang-tr.yml",
         "lang-uk.yml",
         "lang-vi.yml",
         "lang-zh_cn.yml",
         "lang-zh_hk.yml",
         "lang-zh_tw.yml"
      };

      for (String text : values) {
         if (!new File(this.plugin.getDataFolder() + File.separator + "LanguageFiles", text).exists()) {
            this.plugin.saveResource("LanguageFiles" + File.separator + text, false);
         }
      }
   }

   public void loadItems(boolean flag) {
      this.plugin.allowEnchanted = ConfigManager.getConfig().getBoolean("enchanted-variants", true);
      if (this.usesEnchantmentVariations()) {
         this.plugin.getEnchantCostManager().reload();
      }

      SendMessage.infoMessage(Lang.LOADING_ITEMS.get());
      if (!this.plugin.shopItemsByMaterialName.isEmpty()) {
         this.plugin.shopItemsByMaterialName.clear();
      }

      if (ConfigManager.getConfig().getBoolean("shop-stands.enable")) {
         if (this.plugin.getStandProvider() == null) {
            this.plugin.setStandProvider(new StandProvider(this.plugin));
            this.plugin.shopStands = true;
         } else {
            this.plugin.getStandProvider().reload();
         }
      }

      this.plugin.createItem.reloadPrices();
      this.plugin.boughtItemsLore = ConfigManager.getConfig().getBoolean("bought-items-lore");
      this.plugin.allowUnsafeEnchants = ConfigManager.getConfig().getBoolean("allow-unsafe-enchants");
      this.plugin.shopSearch = ConfigManager.getConfig().getBoolean("shop-search.enabled", true);
      this.plugin.discountsActive = ConfigManager.getConfig().getBoolean("enable-discounts");
      this.plugin.multipliers = ConfigManager.getConfig().getBoolean("enable-sell-multipliers");
      this.plugin.ignoredNBTData = ConfigManager.getConfig()
         .getStringList("sold-items-ignored-NBTtags")
         .stream()
         .map(element -> NBTMaps.getTag(element, null, "sold-items-ignored-NBTtags"))
         .collect(Collectors.toList());
      if (ConfigManager.getConfig().getBoolean("allow-renamed-items")) {
         this.plugin.ignoredNBTData.add(NBTMaps.DISPLAYNAME.getTag());
      }

      if (ConfigManager.getConfig().getBoolean("allow-lore-items")) {
         this.plugin.ignoredNBTData.add(NBTMaps.LORE.getTag());
      }

      this.plugin.dropItemsOnGround = ConfigManager.getConfig().getBoolean("drop-remaining-items-on-ground");
      this.plugin.MMB = ConfigManager.getConfig().getBoolean("shift-right-click-sell-all", true);
      this.plugin.maxShopSize = ConfigManager.getConfig().getInt("max-shop-pages", 50);
      this.plugin.allowIllegalStacks = ConfigManager.getConfig().getBoolean("allow-illegal-stacks");
      this.plugin.useItemName = ConfigManager.getConfig().getBoolean("use-item-name", true);
      this.plugin.permissionLore = ConfigManager.getConfig().getBoolean("item-permission-lore", true);
      this.plugin.permissionRankName = ConfigManager.getConfig().getBoolean("show-group-displayname", true);
      this.plugin.matchMeta = ConfigManager.getConfig().getBoolean("match-item-meta", true);
      this.plugin.navBar.loadNavBars();
      this.plugin.navBar.createCustomPlayerProfile();
      this.plugin.mainMenuSize = CalculateAmount.getInvSlots(ConfigManager.getConfig().getInt("main-menu.gui-rows", 6));
      this.plugin.setMainMenuItemSlots(this.plugin.calculateAmount.getMainMenuItemSlots());
      this.plugin.itemStockLore = ConfigManager.getConfig().getBoolean("item-stock-lore");
      this.plugin.seasonalLore = ConfigManager.getConfig().getBoolean("seasonal-lore.enabled");
      this.plugin.getTransactionLog().saveUnsaved();
      this.plugin.bedrock = this.plugin.getServer().getPluginManager().getPlugin("floodgate") != null;
      this.plugin.resizeGUI = ConfigManager.getConfig().getBoolean("resize-gui-for-bedrock", true);
      this.plugin.bedrockMenus = ConfigManager.getConfig().getBoolean("bedrock-forms.enable", false);
      this.plugin.perPlayerLanguages = ConfigManager.getConfig().getBoolean("per-player-languages", true);
      this.plugin.bannedGamemodes = ConfigManager.getConfig().getStringList("banned-gamemodes").stream().filter(element -> {
         try {
            GameMode.valueOf(element);
            return true;
         } catch (IllegalArgumentException exception) {
            SendMessage.warnMessage("Failed to load banned gamemode like '" + element + "'");
            return false;
         }
      }).<GameMode>map(GameMode::valueOf).collect(Collectors.toList());
      this.plugin.createItem.isLatestVersion(flag);
      if (this.plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
         this.plugin.loadChat();
      }

      if (!ConfigManager.getConfig().getBoolean("use-vault-prefix-suffix", true)
         || this.plugin.getServer().getPluginManager().isPluginEnabled("Essentials")
            && this.plugin.getServer().getPluginManager().getPlugin("Essentials").getConfig().getBoolean("change-displayname")
            && this.plugin
               .getServer()
               .getPluginManager()
               .getPlugin("Essentials")
               .getConfig()
               .getBoolean("add-prefix-suffix", this.plugin.getServer().getPluginManager().isPluginEnabled("EssentialsChat"))) {
         this.plugin.prefixSuffix = false;
      }

      TransactionMenus.load(this.plugin);
      BackButton.init();
      this.plugin.sellShulkers = ConfigManager.getConfig().getBoolean("sell-shulker-boxes", true);
      this.plugin.setLoreFormatter(new LoreFormatter(this.plugin));
      String text = ConfigManager.getConfig().getString("shop-search.default-mode", "BY_NAME");

      try {
         this.plugin.defaultSearchMode = ShopSearch.QueryMethod.valueOf(text);
      } catch (IllegalArgumentException exception) {
         SendMessage.warnMessage("Invalid default search mode for '" + text + "'");
         this.plugin.defaultSearchMode = ShopSearch.QueryMethod.BY_NAME;
      }

      HashMap<String, SectionItem> map = new HashMap<>();
      Map<Integer, String> mainMenuItemSlots = this.plugin.getMainMenuItemSlots();

      for (Entry<Integer, String> entry : mainMenuItemSlots.entrySet()) {
         SectionItem sectionItem;
         if (!map.containsKey(entry.getValue())) {
            map.put((String)entry.getValue(), sectionItem = new SectionItem(this.plugin, (String)entry.getValue()));
         } else {
            sectionItem = (SectionItem)map.get(entry.getValue());
         }

         sectionItem.addSlot((Integer)entry.getKey());
      }

      this.plugin.setSectionItems(map);
      this.plugin.stopRotatingShops();
      HashSet values = new HashSet();

      for (String shopId : this.plugin.getShopSections()) {
         ShopType shopType = ShopType.valueOf(ConfigManager.getSection(shopId).getString("shop-type", "CATEGORY"));

         try {
            this.plugin
               .addSection(
                  shopId, (ShopSection)(shopType == ShopType.ROTATING ? new RotatingShopProvider(this.plugin, shopId) : new ShopCategory(this.plugin, shopId))
               );
         } catch (NullPointerException exception) {
            values.add(shopId);
            SendMessage.errorMessage("Failed to create section " + shopId + ": ");
            exception.printStackTrace();
         } catch (ShopLoadException exception) {
            values.add(shopId);
            SendMessage.errorMessage("Failed to create section " + shopId + ": " + exception.getMessage());
         }
      }

      this.plugin.getShopSections().removeAll(values);
      if (ConfigManager.getConfig().getBoolean("main-menu.fill-item.enable", true)) {
         LocalizedItemStack localizedItemStack = this.plugin.createItem.createFillItem(ConfigManager.getConfig().getConfigurationSection("main-menu.fill-item"), null);
         if (localizedItemStack != null) {
            this.plugin.createItem.fillItem = localizedItemStack.getItem();
         } else {
            SendMessage.logDebugMessage(Lang.ITEMS_PATH_IN_CONFIG.get().replace("%location%", "main-menu.fill-item"));
         }
      } else {
         this.plugin.createItem.fillItem = null;
      }

      if (PlaceholderAPIHook.enabled) {
         this.plugin.getEcoHandler().translatePlaceholdersDelayed();
      }

      if (this.plugin.discountsActive) {
         this.setupDiscounts();
      }

      if (this.plugin.multipliers) {
         this.loadSellMultipliers();
      }

      this.plugin.getQueryBuilder().reload();
      this.getShopItemsByMaterialName();
      this.putShopItemsByOrder();
      Bukkit.getPluginManager().callEvent(new ShopItemsLoadEvent());
   }

   private double validatedPercentage(double amount, String text, double amount2, double amount3, String text2) {
      if (amount > amount2 && amount <= amount3) {
         return amount;
      }

      if (amount != 0.0) {
         SendMessage.warnMessage(
            "Ignoring out-of-range " + text2 + " value " + amount + " for '" + text + "' (expected " + amount2 + " to " + amount3 + ")"
         );
      }

      return 0.0;
   }

   private void setupDiscounts() {
      HashMap<String, HashMap<String, Double>> map = new HashMap<>();

      for (String sectionId : ConfigManager.getConfig().getConfigurationSection("discounts").getKeys(false)) {
         if (sectionId.equals("*") || sectionId.equals("all")) {
            for (String shopId : this.plugin.getShopSections()) {
               HashMap<String, Double> orDefault = map.getOrDefault(shopId, new HashMap<>());

               for (String text : ConfigManager.getConfig().getConfigurationSection("discounts." + sectionId).getKeys(false)) {
                  double amount = this.validatedPercentage(ConfigManager.getConfig().getDouble("discounts." + sectionId + "." + text), text, 0.0, 100.0, "discount");
                  if (amount != 0.0) {
                     orDefault.put(text, amount);
                  }
               }

               if (!orDefault.isEmpty()) {
                  map.put(shopId, orDefault);
               }
            }
         } else if (this.plugin.getShopSections().contains(sectionId)) {
            HashMap<String, Double> orDefault = map.getOrDefault(sectionId, new HashMap<>());

            for (String text : ConfigManager.getConfig().getConfigurationSection("discounts." + sectionId).getKeys(false)) {
               double amount = this.validatedPercentage(ConfigManager.getConfig().getDouble("discounts." + sectionId + "." + text), text, 0.0, 100.0, "discount");
               if (amount != 0.0) {
                  orDefault.put(text, amount);
               }
            }

            if (!orDefault.isEmpty()) {
               map.put(sectionId, orDefault);
            }
         } else {
            SendMessage.warnMessage(Lang.CANNOT_FIND_DISCOUNTED_SECTION.get().replace("%section%", sectionId));
         }
      }

      if (!map.isEmpty()) {
         this.plugin.setDiscounts(map);
      }
   }

   private void loadSellMultipliers() {
      HashMap<String, HashMap<String, Double>> map = new HashMap<>();

      for (String sectionId : ConfigManager.getConfig().getConfigurationSection("sell-multipliers").getKeys(false)) {
         if (sectionId.equals("*") || sectionId.equals("all")) {
            for (String shopId : this.plugin.getShopSections()) {
               HashMap<String, Double> orDefault = map.getOrDefault(shopId, new HashMap<>());

               for (String text : ConfigManager.getConfig().getConfigurationSection("sell-multipliers." + sectionId).getKeys(false)) {
                  double amount = this.validatedPercentage(ConfigManager.getConfig().getDouble("sell-multipliers." + sectionId + "." + text), text, -100.0, 1000.0, "sell multiplier");
                  if (amount != 0.0) {
                     orDefault.put(text, amount);
                  }
               }

               if (!orDefault.isEmpty()) {
                  map.put(shopId, orDefault);
               }
            }
         } else if (this.plugin.getShopSections().contains(sectionId)) {
            HashMap<String, Double> orDefault = map.getOrDefault(sectionId, new HashMap<>());

            for (String text : ConfigManager.getConfig().getConfigurationSection("sell-multipliers." + sectionId).getKeys(false)) {
               double amount = this.validatedPercentage(ConfigManager.getConfig().getDouble("sell-multipliers." + sectionId + "." + text), text, -100.0, 1000.0, "sell multiplier");
               if (amount != 0.0) {
                  orDefault.put(text, amount);
               }
            }

            if (!orDefault.isEmpty()) {
               map.put(sectionId, orDefault);
            }
         } else {
            SendMessage.warnMessage(
               "Cannot add sell multipliers for section '%section%', either the shop section is disabled or not found.".replace("%section%", sectionId)
            );
         }
      }

      if (!map.isEmpty()) {
         this.plugin.setSellMultipliers(map);
      }
   }

   private boolean usesEnchantmentVariations() {
      if (this.plugin.allowEnchanted) {
         return true;
      } else {
         for (String text : ConfigManager.getShops()) {
            if (ConfigManager.getSection(text).getBoolean("enchanted-variants")) {
               return true;
            }

            for (String text2 : this.plugin.getConfigManager().getItemsRaw(text)) {
               String shopId = (this.plugin.getShopType(text) == ShopType.CATEGORY ? "pages." : "") + text2;
               if (ConfigManager.getShop(text).getBoolean(shopId + ".enchanted-variants")) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public void getShopItemsByMaterialName() {
      this.plugin.getItemManager().clearShopItemIds();

      for (String shopId : this.plugin.getShopSections()) {
         ShopSection section = this.plugin.getSection(shopId);
         if (section.getType() != ShopType.ROTATING) {
            for (ShopItem shopItem : section.getShopItems()) {
               if (!shopItem.hasItemError() && !shopItem.isDisplayItem()) {
                  ItemStack itemStack = shopItem.getItemToGive();
                  if (itemStack == null) {
                     continue;
                  }

                  String materialName = itemStack.getType().name();
                  List orDefault = this.plugin.shopItemsByMaterialName.getOrDefault(materialName, new ArrayList<>());
                  orDefault.add(shopItem);
                  this.plugin.shopItemsByMaterialName.put(materialName, orDefault);
                  String item = this.plugin.getItemManager().getItem(shopItem.getShopItem());
                  if (item != null) {
                     String[] names = item.split(":", 2);
                     ItemProviderType itemProviderType = ItemProviderType.fromPrefix(names[0]);
                     if (itemProviderType != null && itemProviderType != ItemProviderType.HEAD_DATABASE) {
                        this.plugin.getItemManager().registerItem(names[1], new ItemType(itemProviderType));
                     }
                  }
               }
            }
         }
      }
   }

   public void putShopItemsByOrder() {
      SortOrder sortOrder = SortOrder.fromConfig();
      if (sortOrder != SortOrder.SHOP_LOAD_ORDER) {
         for (String shopId : this.plugin.shopItemsByMaterialName.keySet()) {
            List<ShopItem> values = this.plugin.shopItemsByMaterialName.get(shopId);
            if (values.size() >= 2) {
               switch (sortOrder) {
                  case DESCENDING_SELL_PRICE:
                     values.sort((element, arg2) -> Double.compare(arg2.getSellPrice(), element.getSellPrice()));
                     break;
                  case ASCENDING_SELL_PRICE:
                     values.sort((element, arg2) -> Double.compare(element.getSellPrice(), arg2.getSellPrice()));
               }
            }
         }
      }
   }

   public void updateAvailable() {
      if (this.plugin.updateChecker != null) {
         this.plugin.updateChecker.checkForUpdates();
      }
   }

   public void setupPluginVersion() {
      ServerInfo.Version version = ServerInfo.getVersion();
      if (version == ServerInfo.Version.UNKOWN_VERSION) {
         SendMessage.errorMessage("===================================================");
         SendMessage.errorMessage(" ");
         SendMessage.errorMessage(Lang.COULD_NOT_FIND_VALID_VERSION.get());
         SendMessage.errorMessage("Version: " + Bukkit.getBukkitVersion());
         SendMessage.errorMessage(" ");
         SendMessage.errorMessage("===================================================");
         Bukkit.getPluginManager().disablePlugin(OnyxShop.getInstance());
      } else if (version.olderOrEqualAs(ServerInfo.Version.v1_16_R2)) {
         SendMessage.errorMessage("===================================================");
         SendMessage.errorMessage(" ");
         SendMessage.errorMessage(
            "This server version is not supported anymore on this plugin version, please downgrade to v5.30.1 or lower to keep using the plugin on this server version"
         );
         SendMessage.errorMessage("Version: " + Bukkit.getBukkitVersion());
         SendMessage.errorMessage(" ");
         SendMessage.errorMessage("===================================================");
         Bukkit.getPluginManager().disablePlugin(OnyxShop.getInstance());
      } else {
         try {
            String shopId = OnyxShop.class.getPackage().getName() + ".versions";
            this.plugin.versionHandler = (VersionHandler)Class.forName(
                  shopId
                     + "."
                     + (
                        ServerInfo.supportsPaper()
                              && (version == ServerInfo.Version.v1_21_R6 || version == ServerInfo.Version.v1_21_R7)
                              && ServerInfo.usesMojangMappings()
                           ? "paper_"
                           : ""
                     )
                     + version.name()
                     + "."
                     + version.name()
               )
               .newInstance();
            AnvilGUI.WRAPPER = (VersionHandler.AnvilGUI)Class.forName(
                  shopId
                     + "."
                     + (
                        ServerInfo.supportsPaper()
                              && (version == ServerInfo.Version.v1_21_R6 || version == ServerInfo.Version.v1_21_R7)
                              && ServerInfo.usesMojangMappings()
                           ? "paper_"
                           : ""
                     )
                     + version.name()
                     + ".GUIAnvil"
               )
               .newInstance();
            SendMessage.infoMessage(Lang.MINECRAFT_VERSION_USING.get().replace("%version%", Bukkit.getBukkitVersion().split("-")[0].split("\\.build")[0]));
         } catch (InstantiationException | IllegalAccessException | ClassCastException | ClassNotFoundException exception) {
            SendMessage.errorMessage("===================================================");
            SendMessage.errorMessage(" ");
            SendMessage.errorMessage("You are running a unsupported server version, please use a supported server version or update the plugin");
            SendMessage.errorMessage("Version: " + Bukkit.getBukkitVersion());
            SendMessage.errorMessage(" ");
            SendMessage.errorMessage("===================================================");
            exception.printStackTrace();
         }
      }
   }

   public void checkDebugMode() {
      if (ConfigManager.getConfig().getBoolean("debug", true)) {
         SendMessage.infoMessage(Lang.DEBUG_MODE_ENABLED.get());
      }
   }
}
