package com.onyxshop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import com.onyxshop.api.OnyxShopHook;
import com.onyxshop.commands.SellGUI;
import com.onyxshop.commands.Sellall;
import com.onyxshop.commands.Shop;
import com.onyxshop.commands.ShopGive;
import com.onyxshop.commands.Sreload;
import com.onyxshop.commands.editshop.CommandManager;
import com.onyxshop.events.JoinEvent;
import com.onyxshop.events.LevelEvent;
import com.onyxshop.events.LocaleChangeEvent;
import com.onyxshop.events.MenuHandler;
import com.onyxshop.events.delayed.DelayedLoadListener;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.ScheduleCache;
import com.onyxshop.files.TransactionLog;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.methodes.CreateItem;
import com.onyxshop.methodes.ItemEditor;
import com.onyxshop.methodes.ProviderManager;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.methodes.StartupReload;
import com.onyxshop.methodes.UpdateChecker;
import com.onyxshop.metrics.Metrics;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.objects.LoreFormatter;
import com.onyxshop.objects.SectionItem;
import com.onyxshop.objects.ShopInventory;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.ShopSearch;
import com.onyxshop.objects.bedrock.BedrockInventorys;
import com.onyxshop.objects.mappings.ClickMappings;
import com.onyxshop.objects.navbar.NavBar;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.objects.shops.types.RotatingShop;
import com.onyxshop.providers.DelayedCommandProvider;
import com.onyxshop.providers.DynamicPricingProvider;
import com.onyxshop.providers.EconomyProvider;
import com.onyxshop.providers.EnchantCostProvider;
import com.onyxshop.providers.EnchantsProvider;
import com.onyxshop.providers.GeyserModule;
import com.onyxshop.providers.ItemManager;
import com.onyxshop.providers.JobPoolProvider;
import com.onyxshop.providers.LimitedSellProvider;
import com.onyxshop.providers.LimitedStockProvider;
import com.onyxshop.providers.ModifierManager;
import com.onyxshop.providers.PermissionManager;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.RequirementManager;
import com.onyxshop.providers.ScheduleManager;
import com.onyxshop.providers.SpawnerManager;
import com.onyxshop.providers.StandProvider;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.providers.UserManager;
import com.onyxshop.providers.placeholders.handler.PlaceholderProvider;
import com.onyxshop.util.BukkitMeta;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.CommandHelper;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.EconomyHandler;
import com.onyxshop.util.EconomyType;
import com.onyxshop.util.InventoryUtil;
import com.onyxshop.util.MetaUtils;
import com.onyxshop.util.PotionTypes;
import com.onyxshop.util.QueryBuilder;
import com.onyxshop.util.ReflectUtil;
import com.onyxshop.util.ServerInfo;
import com.onyxshop.util.SkullUtil;
import com.onyxshop.util.SoundEffects;
import com.onyxshop.util.XEnchantment;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.ConfigLoadException;
import com.onyxshop.util.scheduler.ScheduledTask;
import com.onyxshop.util.scheduler.ServerScheduler;
import com.onyxshop.util.scheduler.schedulers.BukkitScheduler;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class OnyxShop extends JavaPlugin {
   public final int version = ServerInfo.getVersion().getVersion();
   public final boolean spigot = this.isSpigotServer();
   private Map<String, SectionItem> sectionItems = new HashMap<>();
   private Map<Integer, String> mainMenuItemSlots = new HashMap<>();
   private Map<String, ShopSection> sections = new HashMap<>();
   private List<String> shopSections = new ArrayList<>();
   private Map<String, Translatable> sectionTitles = new HashMap<>();
   private Map<String, Translatable> bedrockSectionTitles = new HashMap<>();
   public final List<String> supportedMatNames = this.setSupportedMatNames();
   public final List<ItemStack> supportedItems = this.setSupportedItems();
   public final List<Enchantment> supportedEnchants = this.setSupportedEnchantments();
   public final List<PotionTypes> supportedPots = PotionTypes.getSupportedPotions();
   public final List<ItemStack> supportedEntities = this.setSupportedEntities();
   public final Map<String, List<ShopItem>> shopItemsByMaterialName = new HashMap<>();
   public List<String> ignoredNBTData;
   public List<GameMode> bannedGamemodes = new ArrayList<>();
   private final Map<String, HashMap<String, Double>> sellMultipliers = new HashMap<>();
   private final Map<String, HashMap<String, Double>> discounts = new HashMap<>();
   public Map<EntityType, Translatable> spawnerNames = new HashMap<>();
   private final EconomyHandler ecoHandler = new EconomyHandler(this);
   public Permission permissions;
   private Locale locale;
   public boolean itemStock;
   public boolean itemStockLore;
   public boolean boughtItemsLore;
   public boolean commandItems = true;
   public boolean dropItemsOnGround;
   public boolean allowIllegalStacks;
   public boolean useItemEditor = true;
   public boolean shopSearch;
   public boolean nbtItems = true;
   public boolean rotatingShops = true;
   public int maxShopSize;
   public int mainMenuSize;
   public boolean dynamicPricing;
   public boolean bedrock;
   public boolean resizeGUI;
   public boolean bedrockMenus;
   public boolean sellLimits;
   public boolean sellShulkers;
   public boolean placeholders = true;
   public boolean MMB;
   public boolean permissionLore;
   public boolean permissionRankName;
   public boolean requirements = true;
   public boolean aPrices = true;
   public boolean seasonalLore;
   public boolean displayCache;
   public boolean matchMeta;
   public boolean paperMeta;
   public boolean perPlayerLanguages;
   public ShopSearch.QueryMethod defaultSearchMode;
   public boolean allowUnsafeEnchants;
   public boolean allowEnchanted;
   public boolean discountsActive;
   public boolean multipliers;
   public boolean useItemName;
   public boolean prefixSuffix = true;
   public boolean shopStands;
   public boolean shopSigns;
   private static OnyxShop instance;
   public VersionHandler versionHandler;
   public StartupReload startupReload = new StartupReload(this);
   private SendMessage sendMessage = new SendMessage(this);
   public CalculateAmount calculateAmount = new CalculateAmount(this);
   public CreateItem createItem = new CreateItem(this);
   public ItemEditor itemEditor = new ItemEditor(this);
   public EnchantsProvider enchantsProvider = new EnchantsProvider();
   private final EnchantCostProvider enchantCostManager = new EnchantCostProvider(this);
   public PermissionManager permissionManager;
   public NavBar navBar = new NavBar(this);
   public MenuHandler menuHandler;
   private JoinEvent joinEvent;
   private final OnyxShopHook api = new OnyxShopHook(this);
   public UpdateChecker updateChecker;
   private final TitleManager titleManager = new TitleManager();
   private final TransactionLog transactionLog = new TransactionLog(this);
   private final SoundEffects soundEffects = new SoundEffects(this);
   private final UserManager userManager = new UserManager(this);
   private final ConfigManager configManager = new ConfigManager(this);
   private final ScheduleManager scheduleManager = new ScheduleManager(this);
   private final RequirementManager requirementManager = new RequirementManager(this);
   private final DelayedCommandProvider delayedCommandProvider = new DelayedCommandProvider(this);
   private final ProviderManager providerManager = new ProviderManager(this);
   private final CommandHelper cmdHelper = new CommandHelper(this);
   private final JobPoolProvider jobPoolProvider = new JobPoolProvider(this);
   private BedrockInventorys bedrockInventorys;
   private GeyserModule geyserModule;
   private PlaceholderProvider placeholderProvider;
   private QueryBuilder queryBuilder = new QueryBuilder(this);
   private LoreFormatter loreFormatter;
   private ModifierManager modifierManager;
   private PlaceholderAPIHook papiHook;
   private ScheduleCache scheduleCache;
   private StandProvider standProvider;
   private final ItemManager itemManager = new ItemManager(this);
   private final SkullUtil skullUtil = new SkullUtil(this);
   private final ServerScheduler scheduler = this.getScheduler();
   private MetaUtils metaUtils;
   private ClickMappings mappings;
   private Chat chat;
   private final SpawnerManager spawnerManager = new SpawnerManager(this);

   public OnyxShop() {
   }

   public static OnyxShop getInstance() {
      return instance;
   }

   public void onEnable() {
      long startTime = System.currentTimeMillis();
      instance = this;
      this.loadMetaUtils();
      this.getConfigManager().loadConfig();
      Lang.CONFIGS_RELOADED.reload();
      this.metaUtils.update();
      this.getConfigManager().init();
      this.getConfigManager().validate();
      this.enableGeyserExtension();
      this.enableBedrockMenus();
      if (ConfigManager.badYaml == null) {
         this.startupReload.setupPluginVersion();
         SendMessage.init();
         this.itemManager.registerHooks();
         if (!this.ecoHandler.registerPaymentSolutions()) {
            this.getServer().getPluginManager().disablePlugin(this);
            return;
         }

         this.mappings = new ClickMappings(null);
         this.spawnerManager.init();
         this.setPapiHook(new PlaceholderAPIHook());
         if (!this.itemManager.doWait() && !this.spawnerManager.doWait() && !this.ecoHandler.doWait() && !this.papiHook.doWait()) {
            this.setPermManager(new PermissionManager(this));
            this.setModifierManager(new ModifierManager(this));
            this.setScheduleCache(new ScheduleCache(this));
            this.delayedCommandProvider.loadDelayedCommands();
            this.userManager.init();
            this.enchantsProvider.init();
            this.spawnerManager.initDelayed();
            this.setupPlaceholderStore();
            this.jobPoolProvider.init();
            this.startupReload.checkDebugMode();
            this.startupReload.loadInventoryTitles();
            this.providerManager.reloadProviders();
            this.startupReload.loadItems(false);
            this.soundEffects.reload();
            this.registerPermissions();
            SendMessage.infoMessage(Lang.DONE.get().replace("%millis%", String.valueOf(System.currentTimeMillis() - startTime)));
         } else {
            DelayedLoadListener delayedLoadListener = new DelayedLoadListener(
               this, this::loadDelayed, this.itemManager.doWait(), this.spawnerManager.doWait(), this.ecoHandler.doWait(), this.papiHook.doWait()
            );
            this.getServer().getPluginManager().registerEvents(delayedLoadListener, this);
         }

         this.updateChecker = new UpdateChecker();
      } else {
         SendMessage.errorMessage("Skipping all settings and item loading because the configuration files could not be loaded...");
      }

      if (this.checkForGson()) {
         int id = ConfigManager.getConfig().getInt("metrics.bstats-id", 0);
         if (id > 0) {
            new Metrics(this, (short)id);
         }
      }

      this.registerEvents();
      this.registerCommands();
      this.reloadPlayerData();
   }

   private void loadDelayed() {
      this.setPermManager(new PermissionManager(this));
      this.setModifierManager(new ModifierManager(this));
      this.setScheduleCache(new ScheduleCache(this));
      this.delayedCommandProvider.loadDelayedCommands();
      this.userManager.init();
      this.enchantsProvider.init();
      this.spawnerManager.initDelayed();
      this.setupPlaceholderStore();
      this.startupReload.checkDebugMode();
      this.startupReload.loadInventoryTitles();
      this.providerManager.reloadProviders();
      this.startupReload.loadItems(false);
      this.soundEffects.reload();
      this.registerPermissions();
      SendMessage.infoMessage("Completed delayed loading.");
   }

   public void onDisable() {
      this.closeOpenGUIS();
      this.transactionLog.saveUnsaved();
      if (this.skullUtil != null) {
         this.skullUtil.saveCache();
      }

      this.supportedMatNames.clear();
      this.supportedEnchants.clear();
      this.supportedEntities.clear();
      this.supportedItems.clear();
      this.supportedPots.clear();
      this.discounts.clear();
      this.spawnerNames.clear();
      this.stopProviders();
      this.sections.clear();
      Lang.DISABLING_PLUGIN.clearMessages();
      this.scheduler.cancelTasks(this);
      Bukkit.getPluginManager().disablePlugin(instance);
      instance = null;
      SendMessage.logDebugMessage("Completed disable tasks, shutting down...");
   }

   public ShopItem getShopItem(String shopId) {
      ShopSection section = this.sections.get(shopId.split("\\.")[0]);
      return section == null ? null : section.getShopItem(shopId);
   }

   public ShopItem getShopItem(String shopId, String itemId) {
      ShopSection section = this.sections.get(shopId);
      return section == null ? null : section.getShopItem(shopId + "." + itemId);
   }

   public DisplayItem getDisplayItem(String itemId) {
      ShopSection section = this.sections.get(itemId.split("\\.")[0]);
      return section == null ? null : section.getDisplayItem(itemId);
   }

   public DisplayItem getDisplayItem(String itemId, String text) {
      ShopSection section = this.sections.get(itemId);
      return section == null ? null : section.getDisplayItem(itemId + "." + text);
   }

   public void resetDynamicPricing(Object value) {
      this.providerManager.resetDynamicPricing(value);
   }

   public TransactionLog getTransactionLog() {
      return this.transactionLog;
   }

   public DynamicPricingProvider getDynamicPricingProvider() {
      return this.providerManager.getDynamicPricingProvider();
   }

   public ProviderManager getProviderManager() {
      return this.providerManager;
   }

   public LimitedStockProvider getLimitedStockProvider() {
      return this.providerManager.getLimitedStockProvider();
   }

   public LimitedSellProvider getLimitedSellProvider() {
      return this.providerManager.getLimitedSellProvider();
   }

   public Map<Integer, String> getMainMenuItemSlots() {
      return this.mainMenuItemSlots;
   }

   public String getMainMenuSectionForSlot(int slot) {
      return this.mainMenuItemSlots.get(slot);
   }

   public void setMainMenuItemSlots(Map<Integer, String> itemIds) {
      this.mainMenuItemSlots = itemIds;
   }

   public Map<String, SectionItem> getSectionItems() {
      return this.sectionItems;
   }

   public void setSectionItems(Map<String, SectionItem> map) {
      this.sectionItems = map;
   }

   public SectionItem getSectionItem(String sectionId) {
      return this.sectionItems.get(sectionId);
   }

   public void addSection(String sectionId, ShopSection section) {
      this.sections.put(sectionId, section);
   }

   public Map<String, ShopSection> getSections() {
      return this.sections;
   }

   public ShopSection getSection(String sectionId) {
      return this.sections.get(sectionId);
   }

   public ShopType getShopType(String shopId) {
      ShopSection section = this.sections.get(shopId);
      return section != null ? section.getType() : this.configManager.getShopType(shopId);
   }

   public void setShopSections(List<String> shopIds) {
      this.shopSections = shopIds;
   }

   public void removeShopSection(String shopId) {
      this.shopSections.remove(shopId);
   }

   public List<String> getShopSections() {
      return this.shopSections;
   }

   public List<String> getSupportedMatNames() {
      return this.supportedMatNames;
   }

   public List<ItemStack> getSupportedItems() {
      return this.supportedItems;
   }

   public void addSpawnerName(EntityType entityType, Translatable text) {
      this.spawnerNames.put(entityType, text);
   }

   public Translatable getSpawnerName(EntityType entityType) {
      return this.spawnerNames.get(entityType);
   }

   public boolean isSeasonalPricing() {
      return this.modifierManager.hasSeasonalPrices();
   }

   private void setModifierManager(ModifierManager modifierManager) {
      this.modifierManager = modifierManager;
   }

   public void reloadModifiers() {
      this.modifierManager.reloadModifiers();
   }

   public void setPapiHook(PlaceholderAPIHook placeholderAPIHook) {
      this.papiHook = placeholderAPIHook;
   }

   public PlaceholderAPIHook getPapiHook() {
      return this.papiHook;
   }

   public ItemManager getItemManager() {
      return this.itemManager;
   }

   public ScheduleCache getScheduleCache() {
      return this.scheduleCache;
   }

   private void setScheduleCache(ScheduleCache scheduleCache) {
      this.scheduleCache = scheduleCache;
   }

   public void setStandProvider(StandProvider standProvider) {
      this.standProvider = standProvider;
   }

   public StandProvider getStandProvider() {
      return this.standProvider;
   }

   public DelayedCommandProvider getDelayedCommandProvider() {
      return this.delayedCommandProvider;
   }

   public JobPoolProvider getJobPoolProvider() {
      return this.jobPoolProvider;
   }

   public ModifierManager getModifierManager() {
      return this.modifierManager;
   }

   public BedrockInventorys getBedrockInventorys() {
      return this.bedrockInventorys;
   }

   private void enableGeyserExtension() {
      if (ConfigManager.getConfig().getBoolean("pre-register-skulls", false)) {
         if (this.getServer().getPluginManager().getPlugin("Geyser-Spigot") == null) {
            SendMessage.errorMessage("Cannot enable Geyser hook: Failed to find Geyser-Spigot");
         } else {
            this.geyserModule = new GeyserModule(this);
         }
      }
   }

   private void enableBedrockMenus() {
      if (ConfigManager.getConfig().getBoolean("bedrock-forms.enable", false)) {
         if (this.getServer().getPluginManager().getPlugin("Floodgate") != null) {
            this.bedrockInventorys = new BedrockInventorys(this);
         }
      }
   }

   public HashMap<String, Double> getDiscounts(String text) {
      return this.discounts.get(text);
   }

   public void setDiscounts(HashMap<String, HashMap<String, Double>> map) {
      if (!this.discounts.isEmpty()) {
         this.discounts.clear();
      }

      this.discounts.putAll(map);
   }

   public boolean hasDiscount(String text) {
      return this.discounts.containsKey(text);
   }

   public HashMap<String, Double> getMultipliers(String text) {
      return this.sellMultipliers.get(text);
   }

   public boolean hasMultiplier(String text) {
      return this.sellMultipliers.containsKey(text);
   }

   public void setSellMultipliers(HashMap<String, HashMap<String, Double>> map) {
      if (!this.sellMultipliers.isEmpty()) {
         this.sellMultipliers.clear();
      }

      this.sellMultipliers.putAll(map);
   }

   public Map<String, Translatable> getSectionTitles() {
      return this.sectionTitles;
   }

   public void setSectionTitles(HashMap<String, Translatable> map) {
      this.sectionTitles = map;
   }

   public Translatable getSectionTitle(String sectionId) {
      return this.sectionTitles.get(sectionId);
   }

   public Map<String, Translatable> getBedrockSectionTitles() {
      return this.bedrockSectionTitles;
   }

   public void setBedrockSectionTitles(HashMap<String, Translatable> map) {
      this.bedrockSectionTitles = map;
   }

   public Translatable getBedrockSectionTitle(String sectionId) {
      return this.bedrockSectionTitles.get(sectionId);
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public void setLoreFormatter(LoreFormatter loreFormatter) {
      this.loreFormatter = loreFormatter;
   }

   public LoreFormatter getLoreFormatter() {
      return this.loreFormatter;
   }

   public String getDisplayName(Player player, String name) {
      try {
         return this.chat != null && this.prefixSuffix
            ? this.getPlayerPrefix(player) + ChatColor.getLastColors(name.split("%player_displayname%")[0]) + player.getDisplayName() + this.getPlayerSuffix(player)
            : player.getDisplayName();
      } catch (ArrayIndexOutOfBoundsException exception) {
         return this.getPlayerPrefix(player) + player.getDisplayName() + this.getPlayerSuffix(player);
      }
   }

   public String getDisplayName(Player player) {
      return this.chat != null && this.prefixSuffix ? this.getPlayerPrefix(player) + player.getDisplayName() + this.getPlayerSuffix(player) : player.getDisplayName();
   }

   private String getPlayerPrefix(Player player) {
      return !this.chat.getPlayerPrefix(player).isEmpty() ? ChatUtil.formatColors(this.chat.getPlayerPrefix(player) + "&r ").replace("&", "") : "";
   }

   private String getPlayerSuffix(Player player) {
      return !this.chat.getPlayerSuffix(player).isEmpty() ? ChatUtil.formatColors("&r " + this.chat.getPlayerSuffix(player)).replace("&", "") : "";
   }

   public RequirementManager getRequirementManager() {
      return this.requirementManager;
   }

   public SoundEffects getSoundEffects() {
      return this.soundEffects;
   }

   public SpawnerManager getSpawnerManager() {
      return this.spawnerManager;
   }

   public EnchantCostProvider getEnchantCostManager() {
      return this.enchantCostManager;
   }

   public EconomyHandler getEcoHandler() {
      return this.ecoHandler;
   }

   private void setPermManager(PermissionManager permissionManager) {
      this.permissionManager = permissionManager;
   }

   public PermissionManager getPermissionManager() {
      return this.permissionManager;
   }

   public QueryBuilder getQueryBuilder() {
      return this.queryBuilder;
   }

   public ScheduleManager getScheduleManager() {
      return this.scheduleManager;
   }

   public TitleManager getTitleManager() {
      return this.titleManager;
   }

   public void reloadPlayerData() {
      this.userManager.reloadPlayerData();
   }

   public void updateClickMappings() {
      this.mappings = new ClickMappings(null);
   }

   public MetaUtils getMetaUtils() {
      return this.metaUtils;
   }

   public ClickMappings getClickMappings() {
      return this.mappings;
   }

   private void registerPermissions() {
      try {
         this.registerPermission("shop", "Gives access to all shop category's");
         this.registerPermission("sellall", "Allows players to sell items to every shop section with the /sellall command");
         this.registerPermission("sellallitem", "Allows players to sell items to every shop section with the /sellall <item> command");
         this.registerPermission("sellallhand", "Allows players to sell items to every shop section with the /sellall hand command");
         this.registerPermission("sellgui", "Allows players to sell items to every shop section with the SellGUI screen");

         for (ShopSection section : this.sections.values()) {
            this.registerItemPermissions(section);
         }
      } catch (IllegalArgumentException exception) {
         SendMessage.logDebugMessage("Failed to register permissions, ignore this if you reloaded the plugin.");
         SendMessage.logDebugMessage(exception.getMessage());
      }
   }

   private void registerPermission(String permission, String text) {
      org.bukkit.permissions.Permission permission2 = new org.bukkit.permissions.Permission(
         "OnyxShop." + permission + ".all", text, PermissionDefault.TRUE, this.getChildren(permission)
      );
      this.getServer().getPluginManager().addPermission(permission2);
   }

   private Map<String, Boolean> getChildren(String text) {
      HashMap map = new HashMap();

      for (ShopSection section : this.sections.values()) {
         map.put("OnyxShop." + text + "." + section.getSection().toLowerCase(Locale.ENGLISH), true);
      }

      return map;
   }

   private void registerItemPermissions(ShopSection section) {
      for (ShopItem shopItem : section.getShopItems()) {
         try {
            if (shopItem.getPermission() != null && !shopItem.getPermission().isEmpty()) {
               HashMap itemIds = new HashMap();

               for (String text : new String[]{"sellall", "sellallitem", "sellallhand", "sellgui"}) {
                  itemIds.put("OnyxShop." + text + "." + section.getSection().toLowerCase(Locale.ENGLISH) + ".item." + shopItem.getPermission(), true);
               }

               this.getServer()
                  .getPluginManager()
                  .addPermission(
                     new org.bukkit.permissions.Permission(
                        "OnyxShop.shop." + section.getSection().toLowerCase(Locale.ENGLISH) + ".item." + shopItem.getPermission(), PermissionDefault.FALSE, itemIds
                     )
                  );
            }
         } catch (IllegalArgumentException exception) {
         }
      }
   }

   private void registerEvents() {
      this.menuHandler = new MenuHandler(this);
      this.getServer().getPluginManager().registerEvents(this.menuHandler, this);
      this.getServer().getPluginManager().registerEvents(new LocaleChangeEvent(), this);
      if (ConfigManager.badYaml == null) {
         if (ConfigManager.getConfig().getBoolean("enable-levelevent")) {
            this.getServer().getPluginManager().registerEvents(new LevelEvent(), this);
         }

         this.getServer()
            .getPluginManager()
            .registerEvents(this.joinEvent = new JoinEvent(this, ConfigManager.getConfig().getBoolean("enable-joinmessage")), this);
      }
   }

   private void registerCommands() {
      try {
         Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
         field.setAccessible(true);
         CommandMap commandMap = (CommandMap)field.get(Bukkit.getServer());
         if (ConfigManager.badYaml != null || ConfigManager.getConfig().getBoolean("commands.shopgive", true)) {
            commandMap.register("onyxshop", new ShopGive(this, this.getDisabledWorlds("shopgive")));
         }

         if (ConfigManager.badYaml != null || ConfigManager.getConfig().getBoolean("commands.editshop", true)) {
            commandMap.register("onyxshop", new CommandManager(this, this.getDisabledWorlds("editshop")));
         }

         if (ConfigManager.badYaml != null || ConfigManager.getConfig().getBoolean("commands.sreload", true)) {
            commandMap.register("onyxshop", new Sreload(this, this.getDisabledWorlds("sreload")));
         }

         if (ConfigManager.badYaml != null || ConfigManager.getConfig().getBoolean("commands.shop", true)) {
            List<String> shopIds = ConfigManager.badYaml != null ? new ArrayList<>() : ConfigManager.getConfig().getStringList("shop-commands");
            if (shopIds.isEmpty()) {
               shopIds.add("shop");
            }

            commandMap.register("onyxshop", new Shop(this, (List<String>)shopIds, this.getDisabledWorlds("shop")));
         }

         if (ConfigManager.badYaml != null || ConfigManager.getConfig().getBoolean("commands.sellall", true)) {
            List<String> values = ConfigManager.badYaml != null ? new ArrayList<>() : ConfigManager.getConfig().getStringList("sellall-commands");
            if (values.isEmpty()) {
               values.add("sellall");
            }

            if (ConfigManager.badYaml == null && Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
               this.overrideSellCommand(ConfigManager.getConfig().getBoolean("override-sell-command", true));
            }

            commandMap.register("onyxshop", new Sellall(this, (List<String>)values, this.getDisabledWorlds("sellall")));
         }

         if (ConfigManager.badYaml != null || ConfigManager.getConfig().getBoolean("commands.sellgui", true)) {
            List<String> values = ConfigManager.badYaml != null ? new ArrayList<>() : ConfigManager.getConfig().getStringList("sellgui-commands");
            if (values.isEmpty()) {
               values.add("sellgui");
            }

            commandMap.register("onyxshop", new SellGUI(this, (List<String>)values, this.getDisabledWorlds("sellgui")));
         }
      } catch (IllegalAccessException | NoSuchFieldException exception) {
         SendMessage.errorMessage("Error registering commands.");
         exception.printStackTrace();
      }
   }

   public void stopRotatingShops() {
      for (ShopSection section : this.sections.values()) {
         if (section.getType() == ShopType.ROTATING) {
            ((RotatingShop)section).stopCurrent();
         }
      }
   }

   private void setupPlaceholderStore() {
      if (this.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
         this.placeholderProvider = new PlaceholderProvider(this);
      }
   }

   private List<String> getDisabledWorlds(String text) {
      return (List<String>)(ConfigManager.badYaml == null && ConfigManager.getConfig().getBoolean("enable-disabled-worlds", false)
         ? ConfigManager.getConfig().getStringList("disabled-worlds-per-command." + text)
         : new ArrayList<>());
   }

   public void loadChat() {
      RegisteredServiceProvider registeredServiceProvider = this.getServer().getServicesManager().getRegistration(Chat.class);
      if (registeredServiceProvider != null) {
         this.chat = (Chat)registeredServiceProvider.getProvider();
      }
   }

   public void stopProviders() {
      if (this.modifierManager != null) {
         this.modifierManager.stop();
      }

      this.providerManager.stopProviders();
      if (this.standProvider != null) {
         this.standProvider.stop();
      }

      this.scheduleManager.end();
      this.stopRotatingShops();
      if (this.scheduleCache != null) {
         this.delayedCommandProvider.cacheDelayedCommands();
         this.scheduleCache.save();
      }
   }

   public String initLore(String lore) {
      ItemStack itemStack = new ItemStack(Material.BARRIER);
      ItemMeta itemMeta = itemStack.getItemMeta();
      itemMeta.setLore(Collections.singletonList(lore));
      itemStack.setItemMeta(itemMeta);
      return this.versionHandler.getLoreAsNBT(itemStack);
   }

   public YamlConfiguration loadConfiguration(File file) throws ConfigLoadException {
      Validate.notNull(file, "Cannot load " + file.getName() + " config file.");
      YamlConfiguration config = new YamlConfiguration();

      try {
         config.load(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
         if (ConfigManager.badYaml != null && ConfigManager.badYaml.equals(file.getName())) {
            ConfigManager.badYaml = null;
         }

         return config;
      } catch (FileNotFoundException exception) {
      } catch (IOException exception) {
         exception.printStackTrace();
      } catch (InvalidConfigurationException exception) {
         StringWriter stringWriter = new StringWriter();
         PrintWriter printWriter = new PrintWriter(stringWriter);
         exception.printStackTrace(printWriter);
         throw new ConfigLoadException(
            "Cannot read "
               + file.getName()
               + " config because it is mis-configured, use a online Yaml parser with the error underneath here to find out the cause of the problem and to solve it.\n"
               + stringWriter.toString()
         );
      }

      ConfigManager.badYaml = file.getName();
      return null;
   }

   public YamlConfiguration loadConfiguration(File file, String text) {
      return this.loadConfiguration(file, text, false);
   }

   public YamlConfiguration loadConfiguration(File file, String text, boolean flag) {
      Validate.notNull(file, "Cannot load " + text + " config file.");
      YamlConfiguration config = new YamlConfiguration();

      try {
         config.load(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
         if (!flag && ConfigManager.badYaml != null && ConfigManager.badYaml.equals(text)) {
            ConfigManager.badYaml = null;
         }

         return config;
      } catch (FileNotFoundException exception) {
      } catch (IOException exception) {
         exception.printStackTrace();
      } catch (InvalidConfigurationException exception) {
         if (!flag) {
            SendMessage.errorMessage(
               "Cannot read "
                  + text
                  + " config because it is mis-configured, use a online Yaml parser with the error underneath here to find out the cause of the problem and to solve it."
            );
            exception.printStackTrace();
         }
      }

      if (!flag) {
         ConfigManager.badYaml = text;
      }

      return null;
   }

   public YamlConfiguration loadConfiguration(BufferedReader bufferedReader, String text) {
      return this.loadConfiguration(bufferedReader, text, false);
   }

   public YamlConfiguration loadConfiguration(BufferedReader bufferedReader, String text, boolean flag) {
      Validate.notNull(bufferedReader, "Cannot load " + text + " config file.");
      YamlConfiguration config = new YamlConfiguration();

      try {
         config.load(bufferedReader);
         if (!flag && ConfigManager.badYaml != null && ConfigManager.badYaml.equals(text)) {
            ConfigManager.badYaml = null;
         }

         return config;
      } catch (FileNotFoundException exception) {
      } catch (IOException exception) {
         exception.printStackTrace();
      } catch (InvalidConfigurationException exception) {
         if (!flag) {
            SendMessage.errorMessage(
               "Cannot read "
                  + text
                  + " config because it is mis-configured, use a online Yaml parser with the error underneath here to find out the cause of the problem and to solve it."
            );
            exception.printStackTrace();
         }
      }

      if (!flag) {
         ConfigManager.badYaml = text;
      }

      return null;
   }

   public void createConfigBackup(String text) {
      SendMessage.infoMessage(Lang.CREATING_BACKUP.get().replace("%fileName%", text));
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
      LocalDateTime localDateTime = LocalDateTime.now();
      Path path = Paths.get(getInstance().getDataFolder() + "/backups/");
      if (!Files.exists(path) || !Files.isDirectory(path)) {
         try {
            Files.createDirectory(path);
         } catch (FileAlreadyExistsException exception) {
         } catch (IOException exception) {
            exception.printStackTrace();
         }
      }

      File file = new File(getInstance().getDataFolder(), text);

      try {
         try {
            Files.copy(
               file.toPath(), Paths.get(getInstance().getDataFolder() + "/backups/", text.substring(0, text.indexOf(".")) + " " + dateTimeFormatter.format(localDateTime) + ".yml")
            );
         } catch (FileAlreadyExistsException exception) {
         }
      } catch (Exception exception) {
         exception.printStackTrace();
      }
   }

   public void createFileBackup(String text) {
      SendMessage.logDebugMessage(Lang.CREATING_BACKUP.get().replace("%fileName%", text));
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
      LocalDateTime localDateTime = LocalDateTime.now();
      Path path = Paths.get(getInstance().getDataFolder() + "/backups/");
      if (!Files.exists(path) || !Files.isDirectory(path)) {
         try {
            Files.createDirectory(path);
         } catch (FileAlreadyExistsException exception) {
         } catch (IOException exception) {
            exception.printStackTrace();
         }
      }

      File file = new File(getInstance().getDataFolder(), text);

      try {
         try {
            Files.copy(
               file.toPath(),
               Paths.get(getInstance().getDataFolder() + "/backups/", text.split("\\.")[0] + " " + dateTimeFormatter.format(localDateTime) + "." + text.split("\\.")[1])
            );
         } catch (FileAlreadyExistsException exception) {
         }
      } catch (Exception exception) {
         exception.printStackTrace();
      }
   }

   public void disablePlugin() {
      SendMessage.errorMessage(Lang.DISABLING_PLUGIN.get());
      this.sections.clear();
      this.supportedMatNames.clear();
      this.supportedEnchants.clear();
      this.supportedEntities.clear();
      this.supportedItems.clear();
      this.supportedPots.clear();
      this.ignoredNBTData.clear();
      this.discounts.clear();
      this.spawnerNames.clear();
      Lang.DISABLING_PLUGIN.clearMessages();
      Bukkit.getPluginManager().disablePlugin(instance);
      instance = null;
   }

   public ScheduledTask runTask(Runnable task) {
      return this.scheduler.runTask(this, task);
   }

   public ScheduledTask runTaskFor(Runnable task, Player player) {
      return this.scheduler.runTaskFor(this, task, player);
   }

   public ScheduledTask runTaskLater(Runnable task, long value) {
      return this.scheduler.runTaskLater(this, task, value);
   }

   public ScheduledTask runTaskLaterFor(Runnable task, Player player, long value) {
      return this.scheduler.runTaskLaterFor(this, task, player, value);
   }

   public ScheduledTask runTaskTimer(Runnable task, long time, long value) {
      return this.scheduler.runTaskTimer(this, task, time, value);
   }

   public ScheduledTask runTaskAsyncTimer(Runnable task, long time, long value) {
      return this.scheduler.runTaskAsyncTimer(this, task, time, value);
   }

   public ScheduledTask runTaskLaterAsync(Runnable task, long value) {
      return this.scheduler.runTaskLaterAsync(this, task, value);
   }

   public ScheduledTask runTaskAsync(Runnable task) {
      return this.scheduler.runTaskAsync(this, task);
   }

   public void runKillableTask(Runnable task) {
      this.scheduler.runKillableTask(task);
   }

   public ScheduledTask runKillableTaskLater(Runnable task, long value) {
      return this.scheduler.runKillableTaskLater(task, value);
   }

   public ScheduledTask runKillableTaskTimer(Runnable task, long time, long value) {
      return this.scheduler.runKillableTaskTimer(task, time, value);
   }

   public String formatPrices(String type, Set<EcoType> values, boolean flag) {
      String text = "";
      if (type != null) {
         text = ChatUtil.getLastColor(type);
      }

      StringBuilder builder = new StringBuilder();
      int size = values.size();

      for (EcoType ecoType : values) {
         if (size == 1) {
            if (values.size() == 1) {
               return this.ecoHandler.getEcon(ecoType).getFriendly();
            }

            builder.append(text).append(flag ? Lang.AND_PRICE_SEPARATOR_LONG.get() : Lang.OR_PRICE_SEPARATOR_LONG.get());
         } else {
            if (size != values.size()) {
               builder.append(text).append(flag ? Lang.AND_PRICE_SEPARATOR_SHORT.get() : Lang.OR_PRICE_SEPARATOR_SHORT.get());
            }

            size--;
         }

         builder.append(this.ecoHandler.getEcon(ecoType).getFriendly());
      }

      return builder.toString();
   }

   public String formatPrices(String type, Map<EcoType, Double> map, boolean flag) {
      String text = "";
      if (type != null) {
         text = ChatUtil.getLastColor(type);
      }

      StringBuilder builder = new StringBuilder();
      int size = map.size();

      for (Entry entry : map.entrySet()) {
         if (size == 1) {
            if (map.size() == 1) {
               return this.formatPrice((EcoType)entry.getKey(), (Double)entry.getValue());
            }

            builder.append(text).append(flag ? Lang.AND_PRICE_SEPARATOR_LONG.get() : Lang.OR_PRICE_SEPARATOR_LONG.get());
         } else {
            if (size != map.size()) {
               builder.append(text).append(flag ? Lang.AND_PRICE_SEPARATOR_SHORT.get() : Lang.OR_PRICE_SEPARATOR_SHORT.get());
            }

            size--;
         }

         builder.append(text).append(this.formatPrice((EcoType)entry.getKey(), (Double)entry.getValue()));
      }

      return builder.toString();
   }

   public String formatNullablePrice(String price, double amount) {
      EcoType ecoType = EconomyType.getFromString(price);
      if (ecoType == null) {
         return null;
      } else {
         EconomyProvider economy = this.ecoHandler.getEcon(ecoType);
         return economy == null ? null : economy.formatPrice(amount);
      }
   }

   public String formatPrice(EcoType ecoType, double price) {
      return this.ecoHandler.getEcon(ecoType).formatPrice(price);
   }

   public String formatPrice(EconomyProvider economy, double price) {
      return economy.formatPrice(price);
   }

   public String formatPrice(String price, double amount) {
      return this.ecoHandler.getEcon(price).formatPrice(amount);
   }

   public double round(EcoType ecoType, double amount) {
      return this.ecoHandler.getEcon(ecoType).round(amount);
   }

   private NumberFormat getCurrencyFormat() {
      String text = ConfigManager.getConfig().getString("locale", "en-US");
      this.locale = Locale.forLanguageTag(text);
      String text2 = ConfigManager.getConfig().getString("currency-format", "#,##0.00");

      DecimalFormat decimalFormat;
      try {
         decimalFormat = new DecimalFormat(text2, DecimalFormatSymbols.getInstance(this.locale));
      } catch (IllegalArgumentException exception) {
         SendMessage.warnMessage("Could not load currency format of '" + text2 + "', with reason: " + exception.getMessage());
         decimalFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(this.locale));
      }

      decimalFormat.setRoundingMode(RoundingMode.FLOOR);
      return decimalFormat;
   }

   private boolean checkForGson() {
      return ServerInfo.getVersion() != ServerInfo.Version.v1_8_R1 && ServerInfo.getVersion() != ServerInfo.Version.v1_8_R2;
   }

   private boolean isSpigotServer() {
      try {
         Class.forName("org.spigotmc.SpigotConfig");
         return true;
      } catch (ClassNotFoundException exception) {
         return false;
      }
   }

   private void overrideSellCommand(boolean flag) {
      boolean flag2 = false;
      String absolutePath = new File("commands.yml").getAbsolutePath();
      YamlConfiguration config = this.loadConfiguration(new File(absolutePath), absolutePath);
      List stringList = config.getStringList("aliases.sell");
      if (flag) {
         if (stringList.isEmpty()) {
            config.set("aliases.sell", Collections.singletonList("onyxshop:sellall $1-"));
            flag2 = true;
         } else {
            if (!stringList.contains("onyxshop:sellall $1-")) {
               stringList.add("onyxshop:sellall $1-");
               flag2 = true;
            }

            if (stringList.remove("economyshopgui:sellall $1-")) {
               flag2 = true;
            }

            if (flag2) {
               config.set("aliases.sell", stringList);
            }
         }
      } else if (stringList.contains("onyxshop:sellall $1-")) {
         if (stringList.size() == 1) {
            config.set("aliases.sell", null);
         } else {
            stringList.remove("onyxshop:sellall $1-");
            config.set("aliases.sell", stringList);
            flag2 = true;
         }
      }

      if (flag2) {
         try {
            config.save(absolutePath);
         } catch (IOException exception) {
            SendMessage.errorMessage("Failed to save " + absolutePath);
         }
      }
   }

   public String getPost(String text) {
      try {
         HttpURLConnection httpURLConnection = (HttpURLConnection)new URL(text).openConnection();
         httpURLConnection.setConnectTimeout(5000);
         httpURLConnection.connect();
         return new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).readLine();
      } catch (Exception exception) {
         return null;
      }
   }

   public String getInvTitle(String title) {
      if (this.version == 18) {
         return title.length() > 32 ? title.substring(0, 32) : title;
      } else {
         return this.version == 113 ? this.getTitle(title) : title;
      }
   }

   private String getTitle(String title) {
      return this.versionHandler.getTitle(Bukkit.createInventory(null, 9, title));
   }

   private ServerScheduler getScheduler() {
      return (ServerScheduler)(ServerInfo.isFolia()
         ? (ServerScheduler)ReflectUtil.getPaperFeature("util.scheduler.schedulers.FoliaScheduler", this)
         : new BukkitScheduler(this));
   }

   private void loadMetaUtils() {
      if (ServerInfo.supportsPaper() && ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_19_R1)) {
         this.metaUtils = (MetaUtils)ReflectUtil.getPaperFeature("util.meta.PaperMeta", this);
      } else if (ChatUtil.getAdventureUtils() != null && ReflectUtil.supportsMiniMessageBukkit()) {
         this.metaUtils = (MetaUtils)ReflectUtil.getPaperFeature("util.meta.AdventureMeta", this);
      } else {
         this.metaUtils = new BukkitMeta(this);
      }

      SendMessage.infoMessage(
         "Initializing "
            + (
               this.metaUtils.getClass().getSimpleName().equals("PaperMeta")
                  ? "paper"
                  : (this.metaUtils.getClass().getSimpleName().equals("AdventureMeta") ? "adventure" : "bukkit")
            )
            + " meta..."
      );
   }

   public boolean isSimilar(ItemStack itemStack, ItemStack itemStack2) {
      return itemStack == itemStack2 ? true : this.versionHandler.isSimilar(itemStack, itemStack2, this.ignoredNBTData);
   }

   public boolean isSimilar(ItemStack itemStack, ItemStack itemStack2, List<String> itemIds) {
      if (itemStack == itemStack2) {
         return true;
      } else {
         List<String> values = new ArrayList<>(itemIds);
         values.addAll(this.ignoredNBTData);
         return this.versionHandler.isSimilar(itemStack, itemStack2, values);
      }
   }

   public String getMaterialName(String materialName) {
      String[] names = materialName.toLowerCase().replace("_", " ").split(" ");
      StringBuffer stringBuffer = new StringBuffer();

      for (int index = 0; index < names.length; index++) {
         stringBuffer.append(Character.toUpperCase(names[index].charAt(0))).append(names[index].substring(1)).append(" ");
      }

      return stringBuffer.toString().trim();
   }

   public List<String> getExamplePrices() {
      return new ArrayList<>(Arrays.asList("-1", "24.99", "100"));
   }

   private List<String> setSupportedMatNames() {
      ArrayList values = new ArrayList();

      for (XMaterial xMaterial : XMaterial.values()) {
         if (xMaterial.isSupported()) {
            values.add(xMaterial.name());
         }
      }

      return values;
   }

   private List<ItemStack> setSupportedItems() {
      ArrayList values = new ArrayList();

      for (XMaterial xMaterial : XMaterial.values()) {
         if (xMaterial.isSupported() && xMaterial.parseItem() != null && xMaterial.parseMaterial() != Material.AIR) {
            values.add(xMaterial.parseItem());
         }
      }

      if (this.version <= 113) {
         values = new ArrayList(new LinkedHashSet(values));
      }

      return values;
   }

   private List<ItemStack> setSupportedEntities() {
      ArrayList values = new ArrayList();

      for (EntityType entityType : EntityType.values()) {
         if (entityType.isSpawnable() && entityType.isAlive()) {
            Optional optional = XMaterial.matchXMaterial(entityType.toString().toUpperCase() + "_SPAWN_EGG");
            if (optional.isPresent() && ((XMaterial)optional.get()).isSupported()) {
               ItemStack itemStack = ((XMaterial)optional.get()).parseItem();
               if (itemStack == null) {
                  itemStack = XMaterial.SHEEP_SPAWN_EGG.parseItem();
               }

               ItemMeta itemMeta = itemStack.getItemMeta();
               itemMeta.setDisplayName(entityType.toString().toUpperCase());
               itemStack.setItemMeta(itemMeta);
               values.add(itemStack);
            }
         }
      }

      return values;
   }

   private List<Enchantment> setSupportedEnchantments() {
      ArrayList values = new ArrayList();

      for (XEnchantment xEnchantment : XEnchantment.values()) {
         if (xEnchantment.isSupported()) {
            values.add(xEnchantment.parseEnchantment());
         }
      }

      return values;
   }

   public void closeOpenGUIS() {
      try {
         for (Player player : this.getServer().getOnlinePlayers()) {
            if (InventoryUtil.getTopHolder(player) instanceof ShopInventory
               || InventoryUtil.getTopHolder(player) instanceof com.onyxshop.inventorys.editor.ItemEditor) {
               player.closeInventory();
            }
         }
      } catch (Exception exception) {
         SendMessage.logDebugMessage("Failed to close shop menu(s) for currently browsing player(s)");
         exception.printStackTrace();
      }
   }
}
