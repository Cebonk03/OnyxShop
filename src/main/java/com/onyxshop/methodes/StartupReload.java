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

   public StartupReload(OnyxShop var1) {
      this.plugin = var1;
   }

   public void loadInventoryTitles() {
      this.plugin.getSectionTitles().clear();
      this.plugin.getBedrockSectionTitles().clear();
      HashMap var1 = new HashMap();
      HashMap var2 = new HashMap();

      for (String var4 : this.plugin.getShopSections()) {
         String var5 = ConfigManager.getSection(var4).getString("title");
         if (var5 == null || var5.isEmpty()) {
            var5 = ConfigManager.getSection(var4).getString("item.name");
         }

         if (var5 != null && !var5.isEmpty()) {
            var1.put(var4, SendMessage.fromConfig(this.plugin.getInvTitle(var5)));
         } else {
            var1.put(var4, Lang.DISPLAYNAME_NULL.get());
         }

         String var6 = ConfigManager.getSection(var4).getString("bedrock-title");
         if (var6 != null && !var6.isEmpty()) {
            var2.put(var4, SendMessage.fromConfig(this.plugin.getInvTitle(var6)));
         }
      }

      this.plugin.setSectionTitles(var1);
      this.plugin.setBedrockSectionTitles(var2);
      this.plugin.getTitleManager().load();
   }

   public void checkIfLanguageFilesExist() {
      String[] var1 = new String[]{
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

      for (String var5 : var1) {
         if (!new File(this.plugin.getDataFolder() + File.separator + "LanguageFiles", var5).exists()) {
            this.plugin.saveResource("LanguageFiles" + File.separator + var5, false);
         }
      }
   }

   public void loadItems(boolean var1) {
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
         .map(var0 -> NBTMaps.getTag(var0, null, "sold-items-ignored-NBTtags"))
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
      this.plugin.bannedGamemodes = ConfigManager.getConfig().getStringList("banned-gamemodes").stream().filter(var0 -> {
         try {
            GameMode.valueOf(var0);
            return true;
         } catch (IllegalArgumentException var2x) {
            SendMessage.warnMessage("Failed to load banned gamemode like '" + var0 + "'");
            return false;
         }
      }).<GameMode>map(GameMode::valueOf).collect(Collectors.toList());
      this.plugin.createItem.isLatestVersion(var1);
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
      String var2 = ConfigManager.getConfig().getString("shop-search.default-mode", "BY_NAME");

      try {
         this.plugin.defaultSearchMode = ShopSearch.QueryMethod.valueOf(var2);
      } catch (IllegalArgumentException var12) {
         SendMessage.warnMessage("Invalid default search mode for '" + var2 + "'");
         this.plugin.defaultSearchMode = ShopSearch.QueryMethod.BY_NAME;
      }

      HashMap<String, SectionItem> var3 = new HashMap<>();
      Map<Integer, String> var4 = this.plugin.getMainMenuItemSlots();

      for (Entry<Integer, String> var6 : var4.entrySet()) {
         SectionItem var7;
         if (!var3.containsKey(var6.getValue())) {
            var3.put((String)var6.getValue(), var7 = new SectionItem(this.plugin, (String)var6.getValue()));
         } else {
            var7 = (SectionItem)var3.get(var6.getValue());
         }

         var7.addSlot((Integer)var6.getKey());
      }

      this.plugin.setSectionItems(var3);
      this.plugin.stopRotatingShops();
      HashSet var13 = new HashSet();

      for (String var16 : this.plugin.getShopSections()) {
         ShopType var8 = ShopType.valueOf(ConfigManager.getSection(var16).getString("shop-type", "CATEGORY"));

         try {
            this.plugin
               .addSection(
                  var16, (ShopSection)(var8 == ShopType.ROTATING ? new RotatingShopProvider(this.plugin, var16) : new ShopCategory(this.plugin, var16))
               );
         } catch (NullPointerException var10) {
            var13.add(var16);
            SendMessage.errorMessage("Failed to create section " + var16 + ": ");
            var10.printStackTrace();
         } catch (ShopLoadException var11) {
            var13.add(var16);
            SendMessage.errorMessage("Failed to create section " + var16 + ": " + var11.getMessage());
         }
      }

      this.plugin.getShopSections().removeAll(var13);
      if (ConfigManager.getConfig().getBoolean("main-menu.fill-item.enable", true)) {
         LocalizedItemStack var15 = this.plugin.createItem.createFillItem(ConfigManager.getConfig().getConfigurationSection("main-menu.fill-item"), null);
         if (var15 != null) {
            this.plugin.createItem.fillItem = var15.getItem();
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

   private double validatedPercentage(double var1, String var2, double var3, double var4, String var5) {
      if (var1 > var3 && var1 <= var4) {
         return var1;
      }

      if (var1 != 0.0) {
         SendMessage.warnMessage(
            "Ignoring out-of-range " + var5 + " value " + var1 + " for '" + var2 + "' (expected " + var3 + " to " + var4 + ")"
         );
      }

      return 0.0;
   }

   private void setupDiscounts() {
      HashMap<String, HashMap<String, Double>> var1 = new HashMap<>();

      for (String var3 : ConfigManager.getConfig().getConfigurationSection("discounts").getKeys(false)) {
         if (var3.equals("*") || var3.equals("all")) {
            for (String var5 : this.plugin.getShopSections()) {
               HashMap<String, Double> var6 = var1.getOrDefault(var5, new HashMap<>());

               for (String var8 : ConfigManager.getConfig().getConfigurationSection("discounts." + var3).getKeys(false)) {
                  double var9 = this.validatedPercentage(ConfigManager.getConfig().getDouble("discounts." + var3 + "." + var8), var8, 0.0, 100.0, "discount");
                  if (var9 != 0.0) {
                     var6.put(var8, var9);
                  }
               }

               if (!var6.isEmpty()) {
                  var1.put(var5, var6);
               }
            }
         } else if (this.plugin.getShopSections().contains(var3)) {
            HashMap<String, Double> var11 = var1.getOrDefault(var3, new HashMap<>());

            for (String var13 : ConfigManager.getConfig().getConfigurationSection("discounts." + var3).getKeys(false)) {
               double var14 = this.validatedPercentage(ConfigManager.getConfig().getDouble("discounts." + var3 + "." + var13), var13, 0.0, 100.0, "discount");
               if (var14 != 0.0) {
                  var11.put(var13, var14);
               }
            }

            if (!var11.isEmpty()) {
               var1.put(var3, var11);
            }
         } else {
            SendMessage.warnMessage(Lang.CANNOT_FIND_DISCOUNTED_SECTION.get().replace("%section%", var3));
         }
      }

      if (!var1.isEmpty()) {
         this.plugin.setDiscounts(var1);
      }
   }

   private void loadSellMultipliers() {
      HashMap<String, HashMap<String, Double>> var1 = new HashMap<>();

      for (String var3 : ConfigManager.getConfig().getConfigurationSection("sell-multipliers").getKeys(false)) {
         if (var3.equals("*") || var3.equals("all")) {
            for (String var5 : this.plugin.getShopSections()) {
               HashMap<String, Double> var6 = var1.getOrDefault(var5, new HashMap<>());

               for (String var8 : ConfigManager.getConfig().getConfigurationSection("sell-multipliers." + var3).getKeys(false)) {
                  double var9 = this.validatedPercentage(ConfigManager.getConfig().getDouble("sell-multipliers." + var3 + "." + var8), var8, -100.0, 1000.0, "sell multiplier");
                  if (var9 != 0.0) {
                     var6.put(var8, var9);
                  }
               }

               if (!var6.isEmpty()) {
                  var1.put(var5, var6);
               }
            }
         } else if (this.plugin.getShopSections().contains(var3)) {
            HashMap<String, Double> var11 = var1.getOrDefault(var3, new HashMap<>());

            for (String var13 : ConfigManager.getConfig().getConfigurationSection("sell-multipliers." + var3).getKeys(false)) {
               double var14 = this.validatedPercentage(ConfigManager.getConfig().getDouble("sell-multipliers." + var3 + "." + var13), var13, -100.0, 1000.0, "sell multiplier");
               if (var14 != 0.0) {
                  var11.put(var13, var14);
               }
            }

            if (!var11.isEmpty()) {
               var1.put(var3, var11);
            }
         } else {
            SendMessage.warnMessage(
               "Cannot add sell multipliers for section '%section%', either the shop section is disabled or not found.".replace("%section%", var3)
            );
         }
      }

      if (!var1.isEmpty()) {
         this.plugin.setSellMultipliers(var1);
      }
   }

   private boolean usesEnchantmentVariations() {
      if (this.plugin.allowEnchanted) {
         return true;
      } else {
         for (String var2 : ConfigManager.getShops()) {
            if (ConfigManager.getSection(var2).getBoolean("enchanted-variants")) {
               return true;
            }

            for (String var4 : this.plugin.getConfigManager().getItemsRaw(var2)) {
               String var5 = (this.plugin.getShopType(var2) == ShopType.CATEGORY ? "pages." : "") + var4;
               if (ConfigManager.getShop(var2).getBoolean(var5 + ".enchanted-variants")) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public void getShopItemsByMaterialName() {
      this.plugin.getItemManager().clearShopItemIds();

      for (String var2 : this.plugin.getShopSections()) {
         ShopSection var3 = this.plugin.getSection(var2);
         if (var3.getType() != ShopType.ROTATING) {
            for (ShopItem var5 : var3.getShopItems()) {
               if (!var5.hasItemError() && !var5.isDisplayItem()) {
                  ItemStack var6x = var5.getItemToGive();
                  if (var6x == null) {
                     continue;
                  }

                  String var6 = var6x.getType().name();
                  List var7 = this.plugin.shopItemsByMaterialName.getOrDefault(var6, new ArrayList<>());
                  var7.add(var5);
                  this.plugin.shopItemsByMaterialName.put(var6, var7);
                  String var8 = this.plugin.getItemManager().getItem(var5.getShopItem());
                  if (var8 != null) {
                     String[] var9 = var8.split(":", 2);
                     ItemProviderType var10 = ItemProviderType.fromPrefix(var9[0]);
                     if (var10 != null && var10 != ItemProviderType.HEAD_DATABASE) {
                        this.plugin.getItemManager().registerItem(var9[1], new ItemType(var10));
                     }
                  }
               }
            }
         }
      }
   }

   public void putShopItemsByOrder() {
      SortOrder var1 = SortOrder.fromConfig();
      if (var1 != SortOrder.SHOP_LOAD_ORDER) {
         for (String var3 : this.plugin.shopItemsByMaterialName.keySet()) {
            List<ShopItem> var4 = this.plugin.shopItemsByMaterialName.get(var3);
            if (var4.size() >= 2) {
               switch (var1) {
                  case DESCENDING_SELL_PRICE:
                     var4.sort((var0, var1x) -> Double.compare(var1x.getSellPrice(), var0.getSellPrice()));
                     break;
                  case ASCENDING_SELL_PRICE:
                     var4.sort((var0, var1x) -> Double.compare(var0.getSellPrice(), var1x.getSellPrice()));
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
      ServerInfo.Version var1 = ServerInfo.getVersion();
      if (var1 == ServerInfo.Version.UNKOWN_VERSION) {
         SendMessage.errorMessage("===================================================");
         SendMessage.errorMessage(" ");
         SendMessage.errorMessage(Lang.COULD_NOT_FIND_VALID_VERSION.get());
         SendMessage.errorMessage("Version: " + Bukkit.getBukkitVersion());
         SendMessage.errorMessage(" ");
         SendMessage.errorMessage("===================================================");
         Bukkit.getPluginManager().disablePlugin(OnyxShop.getInstance());
      } else if (var1.olderOrEqualAs(ServerInfo.Version.v1_16_R2)) {
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
            String var2 = OnyxShop.class.getPackage().getName() + ".versions";
            this.plugin.versionHandler = (VersionHandler)Class.forName(
                  var2
                     + "."
                     + (
                        ServerInfo.supportsPaper()
                              && (var1 == ServerInfo.Version.v1_21_R6 || var1 == ServerInfo.Version.v1_21_R7)
                              && ServerInfo.usesMojangMappings()
                           ? "paper_"
                           : ""
                     )
                     + var1.name()
                     + "."
                     + var1.name()
               )
               .newInstance();
            AnvilGUI.WRAPPER = (VersionHandler.AnvilGUI)Class.forName(
                  var2
                     + "."
                     + (
                        ServerInfo.supportsPaper()
                              && (var1 == ServerInfo.Version.v1_21_R6 || var1 == ServerInfo.Version.v1_21_R7)
                              && ServerInfo.usesMojangMappings()
                           ? "paper_"
                           : ""
                     )
                     + var1.name()
                     + ".GUIAnvil"
               )
               .newInstance();
            SendMessage.infoMessage(Lang.MINECRAFT_VERSION_USING.get().replace("%version%", Bukkit.getBukkitVersion().split("-")[0].split("\\.build")[0]));
         } catch (InstantiationException | IllegalAccessException | ClassCastException | ClassNotFoundException var3) {
            SendMessage.errorMessage("===================================================");
            SendMessage.errorMessage(" ");
            SendMessage.errorMessage("You are running a unsupported server version, please use a supported server version or update the plugin");
            SendMessage.errorMessage("Version: " + Bukkit.getBukkitVersion());
            SendMessage.errorMessage(" ");
            SendMessage.errorMessage("===================================================");
            var3.printStackTrace();
         }
      }
   }

   public void checkDebugMode() {
      if (ConfigManager.getConfig().getBoolean("debug", true)) {
         SendMessage.infoMessage(Lang.DEBUG_MODE_ENABLED.get());
      }
   }
}
