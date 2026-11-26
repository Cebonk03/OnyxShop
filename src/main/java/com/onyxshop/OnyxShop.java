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
      long var1 = System.currentTimeMillis();
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
            SendMessage.infoMessage(Lang.DONE.get().replace("%millis%", String.valueOf(System.currentTimeMillis() - var1)));
         } else {
            DelayedLoadListener var3 = new DelayedLoadListener(
               this, this::loadDelayed, this.itemManager.doWait(), this.spawnerManager.doWait(), this.ecoHandler.doWait(), this.papiHook.doWait()
            );
            this.getServer().getPluginManager().registerEvents(var3, this);
         }

         this.updateChecker = new UpdateChecker();
      } else {
         SendMessage.errorMessage("Skipping all settings and item loading because the configuration files could not be loaded...");
      }

      if (this.checkForGson()) {
         int var5 = ConfigManager.getConfig().getInt("metrics.bstats-id", 0);
         if (var5 > 0) {
            new Metrics(this, (short)var5);
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

   public ShopItem getShopItem(String var1) {
      ShopSection var2 = this.sections.get(var1.split("\\.")[0]);
      return var2 == null ? null : var2.getShopItem(var1);
   }

   public ShopItem getShopItem(String var1, String var2) {
      ShopSection var3 = this.sections.get(var1);
      return var3 == null ? null : var3.getShopItem(var1 + "." + var2);
   }

   public DisplayItem getDisplayItem(String var1) {
      ShopSection var2 = this.sections.get(var1.split("\\.")[0]);
      return var2 == null ? null : var2.getDisplayItem(var1);
   }

   public DisplayItem getDisplayItem(String var1, String var2) {
      ShopSection var3 = this.sections.get(var1);
      return var3 == null ? null : var3.getDisplayItem(var1 + "." + var2);
   }

   public void resetDynamicPricing(Object var1) {
      this.providerManager.resetDynamicPricing(var1);
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

   public String getMainMenuSectionForSlot(int var1) {
      return this.mainMenuItemSlots.get(var1);
   }

   public void setMainMenuItemSlots(Map<Integer, String> var1) {
      this.mainMenuItemSlots = var1;
   }

   public Map<String, SectionItem> getSectionItems() {
      return this.sectionItems;
   }

   public void setSectionItems(Map<String, SectionItem> var1) {
      this.sectionItems = var1;
   }

   public SectionItem getSectionItem(String var1) {
      return this.sectionItems.get(var1);
   }

   public void addSection(String var1, ShopSection var2) {
      this.sections.put(var1, var2);
   }

   public Map<String, ShopSection> getSections() {
      return this.sections;
   }

   public ShopSection getSection(String var1) {
      return this.sections.get(var1);
   }

   public ShopType getShopType(String var1) {
      ShopSection var2 = this.sections.get(var1);
      return var2 != null ? var2.getType() : this.configManager.getShopType(var1);
   }

   public void setShopSections(List<String> var1) {
      this.shopSections = var1;
   }

   public void removeShopSection(String var1) {
      this.shopSections.remove(var1);
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

   public void addSpawnerName(EntityType var1, Translatable var2) {
      this.spawnerNames.put(var1, var2);
   }

   public Translatable getSpawnerName(EntityType var1) {
      return this.spawnerNames.get(var1);
   }

   public boolean isSeasonalPricing() {
      return this.modifierManager.hasSeasonalPrices();
   }

   private void setModifierManager(ModifierManager var1) {
      this.modifierManager = var1;
   }

   public void reloadModifiers() {
      this.modifierManager.reloadModifiers();
   }

   public void setPapiHook(PlaceholderAPIHook var1) {
      this.papiHook = var1;
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

   private void setScheduleCache(ScheduleCache var1) {
      this.scheduleCache = var1;
   }

   public void setStandProvider(StandProvider var1) {
      this.standProvider = var1;
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

   public HashMap<String, Double> getDiscounts(String var1) {
      return this.discounts.get(var1);
   }

   public void setDiscounts(HashMap<String, HashMap<String, Double>> var1) {
      if (!this.discounts.isEmpty()) {
         this.discounts.clear();
      }

      this.discounts.putAll(var1);
   }

   public boolean hasDiscount(String var1) {
      return this.discounts.containsKey(var1);
   }

   public HashMap<String, Double> getMultipliers(String var1) {
      return this.sellMultipliers.get(var1);
   }

   public boolean hasMultiplier(String var1) {
      return this.sellMultipliers.containsKey(var1);
   }

   public void setSellMultipliers(HashMap<String, HashMap<String, Double>> var1) {
      if (!this.sellMultipliers.isEmpty()) {
         this.sellMultipliers.clear();
      }

      this.sellMultipliers.putAll(var1);
   }

   public Map<String, Translatable> getSectionTitles() {
      return this.sectionTitles;
   }

   public void setSectionTitles(HashMap<String, Translatable> var1) {
      this.sectionTitles = var1;
   }

   public Translatable getSectionTitle(String var1) {
      return this.sectionTitles.get(var1);
   }

   public Map<String, Translatable> getBedrockSectionTitles() {
      return this.bedrockSectionTitles;
   }

   public void setBedrockSectionTitles(HashMap<String, Translatable> var1) {
      this.bedrockSectionTitles = var1;
   }

   public Translatable getBedrockSectionTitle(String var1) {
      return this.bedrockSectionTitles.get(var1);
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public void setLoreFormatter(LoreFormatter var1) {
      this.loreFormatter = var1;
   }

   public LoreFormatter getLoreFormatter() {
      return this.loreFormatter;
   }

   public String getDisplayName(Player var1, String var2) {
      try {
         return this.chat != null && this.prefixSuffix
            ? this.getPlayerPrefix(var1) + ChatColor.getLastColors(var2.split("%player_displayname%")[0]) + var1.getDisplayName() + this.getPlayerSuffix(var1)
            : var1.getDisplayName();
      } catch (ArrayIndexOutOfBoundsException var4) {
         return this.getPlayerPrefix(var1) + var1.getDisplayName() + this.getPlayerSuffix(var1);
      }
   }

   public String getDisplayName(Player var1) {
      return this.chat != null && this.prefixSuffix ? this.getPlayerPrefix(var1) + var1.getDisplayName() + this.getPlayerSuffix(var1) : var1.getDisplayName();
   }

   private String getPlayerPrefix(Player var1) {
      return !this.chat.getPlayerPrefix(var1).isEmpty() ? ChatUtil.formatColors(this.chat.getPlayerPrefix(var1) + "&r ").replace("&", "") : "";
   }

   private String getPlayerSuffix(Player var1) {
      return !this.chat.getPlayerSuffix(var1).isEmpty() ? ChatUtil.formatColors("&r " + this.chat.getPlayerSuffix(var1)).replace("&", "") : "";
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

   private void setPermManager(PermissionManager var1) {
      this.permissionManager = var1;
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

         for (ShopSection var2 : this.sections.values()) {
            this.registerItemPermissions(var2);
         }
      } catch (IllegalArgumentException var3) {
         SendMessage.logDebugMessage("Failed to register permissions, ignore this if you reloaded the plugin.");
         SendMessage.logDebugMessage(var3.getMessage());
      }
   }

   private void registerPermission(String var1, String var2) {
      org.bukkit.permissions.Permission var3 = new org.bukkit.permissions.Permission(
         "OnyxShop." + var1 + ".all", var2, PermissionDefault.TRUE, this.getChildren(var1)
      );
      this.getServer().getPluginManager().addPermission(var3);
   }

   private Map<String, Boolean> getChildren(String var1) {
      HashMap var2 = new HashMap();

      for (ShopSection var4 : this.sections.values()) {
         var2.put("OnyxShop." + var1 + "." + var4.getSection().toLowerCase(Locale.ENGLISH), true);
      }

      return var2;
   }

   private void registerItemPermissions(ShopSection var1) {
      for (ShopItem var3 : var1.getShopItems()) {
         try {
            if (var3.getPermission() != null && !var3.getPermission().isEmpty()) {
               HashMap var4 = new HashMap();

               for (String var8 : new String[]{"sellall", "sellallitem", "sellallhand", "sellgui"}) {
                  var4.put("OnyxShop." + var8 + "." + var1.getSection().toLowerCase(Locale.ENGLISH) + ".item." + var3.getPermission(), true);
               }

               this.getServer()
                  .getPluginManager()
                  .addPermission(
                     new org.bukkit.permissions.Permission(
                        "OnyxShop.shop." + var1.getSection().toLowerCase(Locale.ENGLISH) + ".item." + var3.getPermission(), PermissionDefault.FALSE, var4
                     )
                  );
            }
         } catch (IllegalArgumentException var9) {
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
         Field var1 = Bukkit.getServer().getClass().getDeclaredField("commandMap");
         var1.setAccessible(true);
         CommandMap var2 = (CommandMap)var1.get(Bukkit.getServer());
         if (ConfigManager.badYaml != null || ConfigManager.getConfig().getBoolean("commands.shopgive", true)) {
            var2.register("onyxshop", new ShopGive(this, this.getDisabledWorlds("shopgive")));
         }

         if (ConfigManager.badYaml != null || ConfigManager.getConfig().getBoolean("commands.editshop", true)) {
            var2.register("onyxshop", new CommandManager(this, this.getDisabledWorlds("editshop")));
         }

         if (ConfigManager.badYaml != null || ConfigManager.getConfig().getBoolean("commands.sreload", true)) {
            var2.register("onyxshop", new Sreload(this, this.getDisabledWorlds("sreload")));
         }

         if (ConfigManager.badYaml != null || ConfigManager.getConfig().getBoolean("commands.shop", true)) {
            List<String> var3 = ConfigManager.badYaml != null ? new ArrayList<>() : ConfigManager.getConfig().getStringList("shop-commands");
            if (var3.isEmpty()) {
               var3.add("shop");
            }

            var2.register("onyxshop", new Shop(this, (List<String>)var3, this.getDisabledWorlds("shop")));
         }

         if (ConfigManager.badYaml != null || ConfigManager.getConfig().getBoolean("commands.sellall", true)) {
            List<String> var5 = ConfigManager.badYaml != null ? new ArrayList<>() : ConfigManager.getConfig().getStringList("sellall-commands");
            if (var5.isEmpty()) {
               var5.add("sellall");
            }

            if (ConfigManager.badYaml == null && Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
               this.overrideSellCommand(ConfigManager.getConfig().getBoolean("override-sell-command", true));
            }

            var2.register("onyxshop", new Sellall(this, (List<String>)var5, this.getDisabledWorlds("sellall")));
         }

         if (ConfigManager.badYaml != null || ConfigManager.getConfig().getBoolean("commands.sellgui", true)) {
            List<String> var6 = ConfigManager.badYaml != null ? new ArrayList<>() : ConfigManager.getConfig().getStringList("sellgui-commands");
            if (var6.isEmpty()) {
               var6.add("sellgui");
            }

            var2.register("onyxshop", new SellGUI(this, (List<String>)var6, this.getDisabledWorlds("sellgui")));
         }
      } catch (IllegalAccessException | NoSuchFieldException var4) {
         SendMessage.errorMessage("Error registering commands.");
         var4.printStackTrace();
      }
   }

   public void stopRotatingShops() {
      for (ShopSection var2 : this.sections.values()) {
         if (var2.getType() == ShopType.ROTATING) {
            ((RotatingShop)var2).stopCurrent();
         }
      }
   }

   private void setupPlaceholderStore() {
      if (this.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
         this.placeholderProvider = new PlaceholderProvider(this);
      }
   }

   private List<String> getDisabledWorlds(String var1) {
      return (List<String>)(ConfigManager.badYaml == null && ConfigManager.getConfig().getBoolean("enable-disabled-worlds", false)
         ? ConfigManager.getConfig().getStringList("disabled-worlds-per-command." + var1)
         : new ArrayList<>());
   }

   public void loadChat() {
      RegisteredServiceProvider var1 = this.getServer().getServicesManager().getRegistration(Chat.class);
      if (var1 != null) {
         this.chat = (Chat)var1.getProvider();
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

   public String initLore(String var1) {
      ItemStack var2 = new ItemStack(Material.BARRIER);
      ItemMeta var3 = var2.getItemMeta();
      var3.setLore(Collections.singletonList(var1));
      var2.setItemMeta(var3);
      return this.versionHandler.getLoreAsNBT(var2);
   }

   public YamlConfiguration loadConfiguration(File var1) throws ConfigLoadException {
      Validate.notNull(var1, "Cannot load " + var1.getName() + " config file.");
      YamlConfiguration var2 = new YamlConfiguration();

      try {
         var2.load(new InputStreamReader(new FileInputStream(var1), StandardCharsets.UTF_8));
         if (ConfigManager.badYaml != null && ConfigManager.badYaml.equals(var1.getName())) {
            ConfigManager.badYaml = null;
         }

         return var2;
      } catch (FileNotFoundException var6) {
      } catch (IOException var7) {
         var7.printStackTrace();
      } catch (InvalidConfigurationException var8) {
         StringWriter var4 = new StringWriter();
         PrintWriter var5 = new PrintWriter(var4);
         var8.printStackTrace(var5);
         throw new ConfigLoadException(
            "Cannot read "
               + var1.getName()
               + " config because it is mis-configured, use a online Yaml parser with the error underneath here to find out the cause of the problem and to solve it.\n"
               + var4.toString()
         );
      }

      ConfigManager.badYaml = var1.getName();
      return null;
   }

   public YamlConfiguration loadConfiguration(File var1, String var2) {
      return this.loadConfiguration(var1, var2, false);
   }

   public YamlConfiguration loadConfiguration(File var1, String var2, boolean var3) {
      Validate.notNull(var1, "Cannot load " + var2 + " config file.");
      YamlConfiguration var4 = new YamlConfiguration();

      try {
         var4.load(new InputStreamReader(new FileInputStream(var1), StandardCharsets.UTF_8));
         if (!var3 && ConfigManager.badYaml != null && ConfigManager.badYaml.equals(var2)) {
            ConfigManager.badYaml = null;
         }

         return var4;
      } catch (FileNotFoundException var6) {
      } catch (IOException var7) {
         var7.printStackTrace();
      } catch (InvalidConfigurationException var8) {
         if (!var3) {
            SendMessage.errorMessage(
               "Cannot read "
                  + var2
                  + " config because it is mis-configured, use a online Yaml parser with the error underneath here to find out the cause of the problem and to solve it."
            );
            var8.printStackTrace();
         }
      }

      if (!var3) {
         ConfigManager.badYaml = var2;
      }

      return null;
   }

   public YamlConfiguration loadConfiguration(BufferedReader var1, String var2) {
      return this.loadConfiguration(var1, var2, false);
   }

   public YamlConfiguration loadConfiguration(BufferedReader var1, String var2, boolean var3) {
      Validate.notNull(var1, "Cannot load " + var2 + " config file.");
      YamlConfiguration var4 = new YamlConfiguration();

      try {
         var4.load(var1);
         if (!var3 && ConfigManager.badYaml != null && ConfigManager.badYaml.equals(var2)) {
            ConfigManager.badYaml = null;
         }

         return var4;
      } catch (FileNotFoundException var6) {
      } catch (IOException var7) {
         var7.printStackTrace();
      } catch (InvalidConfigurationException var8) {
         if (!var3) {
            SendMessage.errorMessage(
               "Cannot read "
                  + var2
                  + " config because it is mis-configured, use a online Yaml parser with the error underneath here to find out the cause of the problem and to solve it."
            );
            var8.printStackTrace();
         }
      }

      if (!var3) {
         ConfigManager.badYaml = var2;
      }

      return null;
   }

   public void createConfigBackup(String var1) {
      SendMessage.infoMessage(Lang.CREATING_BACKUP.get().replace("%fileName%", var1));
      DateTimeFormatter var2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
      LocalDateTime var3 = LocalDateTime.now();
      Path var4 = Paths.get(getInstance().getDataFolder() + "/backups/");
      if (!Files.exists(var4) || !Files.isDirectory(var4)) {
         try {
            Files.createDirectory(var4);
         } catch (FileAlreadyExistsException var9) {
         } catch (IOException var10) {
            var10.printStackTrace();
         }
      }

      File var5 = new File(getInstance().getDataFolder(), var1);

      try {
         try {
            Files.copy(
               var5.toPath(), Paths.get(getInstance().getDataFolder() + "/backups/", var1.substring(0, var1.indexOf(".")) + " " + var2.format(var3) + ".yml")
            );
         } catch (FileAlreadyExistsException var7) {
         }
      } catch (Exception var8) {
         var8.printStackTrace();
      }
   }

   public void createFileBackup(String var1) {
      SendMessage.logDebugMessage(Lang.CREATING_BACKUP.get().replace("%fileName%", var1));
      DateTimeFormatter var2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
      LocalDateTime var3 = LocalDateTime.now();
      Path var4 = Paths.get(getInstance().getDataFolder() + "/backups/");
      if (!Files.exists(var4) || !Files.isDirectory(var4)) {
         try {
            Files.createDirectory(var4);
         } catch (FileAlreadyExistsException var9) {
         } catch (IOException var10) {
            var10.printStackTrace();
         }
      }

      File var5 = new File(getInstance().getDataFolder(), var1);

      try {
         try {
            Files.copy(
               var5.toPath(),
               Paths.get(getInstance().getDataFolder() + "/backups/", var1.split("\\.")[0] + " " + var2.format(var3) + "." + var1.split("\\.")[1])
            );
         } catch (FileAlreadyExistsException var7) {
         }
      } catch (Exception var8) {
         var8.printStackTrace();
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

   public ScheduledTask runTask(Runnable var1) {
      return this.scheduler.runTask(this, var1);
   }

   public ScheduledTask runTaskFor(Runnable var1, Player var2) {
      return this.scheduler.runTaskFor(this, var1, var2);
   }

   public ScheduledTask runTaskLater(Runnable var1, long var2) {
      return this.scheduler.runTaskLater(this, var1, var2);
   }

   public ScheduledTask runTaskTimer(Runnable var1, long var2, long var4) {
      return this.scheduler.runTaskTimer(this, var1, var2, var4);
   }

   public ScheduledTask runTaskAsyncTimer(Runnable var1, long var2, long var4) {
      return this.scheduler.runTaskAsyncTimer(this, var1, var2, var4);
   }

   public ScheduledTask runTaskLaterAsync(Runnable var1, long var2) {
      return this.scheduler.runTaskLaterAsync(this, var1, var2);
   }

   public ScheduledTask runTaskAsync(Runnable var1) {
      return this.scheduler.runTaskAsync(this, var1);
   }

   public void runKillableTask(Runnable var1) {
      this.scheduler.runKillableTask(var1);
   }

   public ScheduledTask runKillableTaskLater(Runnable var1, long var2) {
      return this.scheduler.runKillableTaskLater(var1, var2);
   }

   public ScheduledTask runKillableTaskTimer(Runnable var1, long var2, long var4) {
      return this.scheduler.runKillableTaskTimer(var1, var2, var4);
   }

   public String formatPrices(String var1, Set<EcoType> var2, boolean var3) {
      String var4 = "";
      if (var1 != null) {
         var4 = ChatUtil.getLastColor(var1);
      }

      StringBuilder var5 = new StringBuilder();
      int var6 = var2.size();

      for (EcoType var8 : var2) {
         if (var6 == 1) {
            if (var2.size() == 1) {
               return this.ecoHandler.getEcon(var8).getFriendly();
            }

            var5.append(var4).append(var3 ? Lang.AND_PRICE_SEPARATOR_LONG.get() : Lang.OR_PRICE_SEPARATOR_LONG.get());
         } else {
            if (var6 != var2.size()) {
               var5.append(var4).append(var3 ? Lang.AND_PRICE_SEPARATOR_SHORT.get() : Lang.OR_PRICE_SEPARATOR_SHORT.get());
            }

            var6--;
         }

         var5.append(this.ecoHandler.getEcon(var8).getFriendly());
      }

      return var5.toString();
   }

   public String formatPrices(String var1, Map<EcoType, Double> var2, boolean var3) {
      String var4 = "";
      if (var1 != null) {
         var4 = ChatUtil.getLastColor(var1);
      }

      StringBuilder var5 = new StringBuilder();
      int var6 = var2.size();

      for (Entry var8 : var2.entrySet()) {
         if (var6 == 1) {
            if (var2.size() == 1) {
               return this.formatPrice((EcoType)var8.getKey(), (Double)var8.getValue());
            }

            var5.append(var4).append(var3 ? Lang.AND_PRICE_SEPARATOR_LONG.get() : Lang.OR_PRICE_SEPARATOR_LONG.get());
         } else {
            if (var6 != var2.size()) {
               var5.append(var4).append(var3 ? Lang.AND_PRICE_SEPARATOR_SHORT.get() : Lang.OR_PRICE_SEPARATOR_SHORT.get());
            }

            var6--;
         }

         var5.append(var4).append(this.formatPrice((EcoType)var8.getKey(), (Double)var8.getValue()));
      }

      return var5.toString();
   }

   public String formatNullablePrice(String var1, double var2) {
      EcoType var4 = EconomyType.getFromString(var1);
      if (var4 == null) {
         return null;
      } else {
         EconomyProvider var5 = this.ecoHandler.getEcon(var4);
         return var5 == null ? null : var5.formatPrice(var2);
      }
   }

   public String formatPrice(EcoType var1, double var2) {
      return this.ecoHandler.getEcon(var1).formatPrice(var2);
   }

   public String formatPrice(EconomyProvider var1, double var2) {
      return var1.formatPrice(var2);
   }

   public String formatPrice(String var1, double var2) {
      return this.ecoHandler.getEcon(var1).formatPrice(var2);
   }

   public double round(EcoType var1, double var2) {
      return this.ecoHandler.getEcon(var1).round(var2);
   }

   private NumberFormat getCurrencyFormat() {
      String var1 = ConfigManager.getConfig().getString("locale", "en-US");
      this.locale = Locale.forLanguageTag(var1);
      String var3 = ConfigManager.getConfig().getString("currency-format", "#,##0.00");

      DecimalFormat var2;
      try {
         var2 = new DecimalFormat(var3, DecimalFormatSymbols.getInstance(this.locale));
      } catch (IllegalArgumentException var5) {
         SendMessage.warnMessage("Could not load currency format of '" + var3 + "', with reason: " + var5.getMessage());
         var2 = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(this.locale));
      }

      var2.setRoundingMode(RoundingMode.FLOOR);
      return var2;
   }

   private boolean checkForGson() {
      return ServerInfo.getVersion() != ServerInfo.Version.v1_8_R1 && ServerInfo.getVersion() != ServerInfo.Version.v1_8_R2;
   }

   private boolean isSpigotServer() {
      try {
         Class.forName("org.spigotmc.SpigotConfig");
         return true;
      } catch (ClassNotFoundException var2) {
         return false;
      }
   }

   private void overrideSellCommand(boolean var1) {
      boolean var2 = false;
      String var3 = new File("commands.yml").getAbsolutePath();
      YamlConfiguration var4 = this.loadConfiguration(new File(var3), var3);
      List var5 = var4.getStringList("aliases.sell");
      if (var1) {
         if (var5.isEmpty()) {
            var4.set("aliases.sell", Collections.singletonList("onyxshop:sellall $1-"));
            var2 = true;
         } else {
            if (!var5.contains("onyxshop:sellall $1-")) {
               var5.add("onyxshop:sellall $1-");
               var2 = true;
            }

            if (var5.remove("economyshopgui:sellall $1-")) {
               var2 = true;
            }

            if (var2) {
               var4.set("aliases.sell", var5);
            }
         }
      } else if (var5.contains("onyxshop:sellall $1-")) {
         if (var5.size() == 1) {
            var4.set("aliases.sell", null);
         } else {
            var5.remove("onyxshop:sellall $1-");
            var4.set("aliases.sell", var5);
            var2 = true;
         }
      }

      if (var2) {
         try {
            var4.save(var3);
         } catch (IOException var7) {
            SendMessage.errorMessage("Failed to save " + var3);
         }
      }
   }

   public String getPost(String var1) {
      try {
         HttpURLConnection var2 = (HttpURLConnection)new URL(var1).openConnection();
         var2.setConnectTimeout(5000);
         var2.connect();
         return new BufferedReader(new InputStreamReader(var2.getInputStream())).readLine();
      } catch (Exception var3) {
         return null;
      }
   }

   public String getInvTitle(String var1) {
      if (this.version == 18) {
         return var1.length() > 32 ? var1.substring(0, 32) : var1;
      } else {
         return this.version == 113 ? this.getTitle(var1) : var1;
      }
   }

   private String getTitle(String var1) {
      return this.versionHandler.getTitle(Bukkit.createInventory(null, 9, var1));
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

   public boolean isSimilar(ItemStack var1, ItemStack var2) {
      return var1 == var2 ? true : this.versionHandler.isSimilar(var1, var2, this.ignoredNBTData);
   }

   public boolean isSimilar(ItemStack var1, ItemStack var2, List<String> var3) {
      if (var1 == var2) {
         return true;
      } else {
         List<String> var4 = new ArrayList<>(var3);
         var4.addAll(this.ignoredNBTData);
         return this.versionHandler.isSimilar(var1, var2, var4);
      }
   }

   public String getMaterialName(String var1) {
      String[] var2 = var1.toLowerCase().replace("_", " ").split(" ");
      StringBuffer var3 = new StringBuffer();

      for (int var4 = 0; var4 < var2.length; var4++) {
         var3.append(Character.toUpperCase(var2[var4].charAt(0))).append(var2[var4].substring(1)).append(" ");
      }

      return var3.toString().trim();
   }

   public List<String> getExamplePrices() {
      return new ArrayList<>(Arrays.asList("-1", "24.99", "100"));
   }

   private List<String> setSupportedMatNames() {
      ArrayList var1 = new ArrayList();

      for (XMaterial var5 : XMaterial.values()) {
         if (var5.isSupported()) {
            var1.add(var5.name());
         }
      }

      return var1;
   }

   private List<ItemStack> setSupportedItems() {
      ArrayList var1 = new ArrayList();

      for (XMaterial var5 : XMaterial.values()) {
         if (var5.isSupported() && var5.parseItem() != null && var5.parseMaterial() != Material.AIR) {
            var1.add(var5.parseItem());
         }
      }

      if (this.version <= 113) {
         var1 = new ArrayList(new LinkedHashSet(var1));
      }

      return var1;
   }

   private List<ItemStack> setSupportedEntities() {
      ArrayList var1 = new ArrayList();

      for (EntityType var5 : EntityType.values()) {
         if (var5.isSpawnable() && var5.isAlive()) {
            Optional var6 = XMaterial.matchXMaterial(var5.toString().toUpperCase() + "_SPAWN_EGG");
            if (var6.isPresent() && ((XMaterial)var6.get()).isSupported()) {
               ItemStack var7 = ((XMaterial)var6.get()).parseItem();
               if (var7 == null) {
                  var7 = XMaterial.SHEEP_SPAWN_EGG.parseItem();
               }

               ItemMeta var8 = var7.getItemMeta();
               var8.setDisplayName(var5.toString().toUpperCase());
               var7.setItemMeta(var8);
               var1.add(var7);
            }
         }
      }

      return var1;
   }

   private List<Enchantment> setSupportedEnchantments() {
      ArrayList var1 = new ArrayList();

      for (XEnchantment var5 : XEnchantment.values()) {
         if (var5.isSupported()) {
            var1.add(var5.parseEnchantment());
         }
      }

      return var1;
   }

   public void closeOpenGUIS() {
      try {
         for (Player var2 : this.getServer().getOnlinePlayers()) {
            if (InventoryUtil.getTopHolder(var2) instanceof ShopInventory
               || InventoryUtil.getTopHolder(var2) instanceof com.onyxshop.inventorys.editor.ItemEditor) {
               var2.closeInventory();
            }
         }
      } catch (Exception var3) {
         SendMessage.logDebugMessage("Failed to close shop menu(s) for currently browsing player(s)");
         var3.printStackTrace();
      }
   }
}
