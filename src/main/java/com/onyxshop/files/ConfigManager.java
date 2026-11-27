package com.onyxshop.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.config.CommentedConfig;
import com.onyxshop.files.config.Config;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.util.ConfigUtil;
import com.onyxshop.util.NBTMaps;
import com.onyxshop.util.NBTUtils;
import com.onyxshop.util.ServerInfo;
import com.onyxshop.util.exceptions.ConfigLoadException;
import com.onyxshop.util.exceptions.ConfigSaveException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
   private final OnyxShop plugin;
   public static String badYaml = null;
   private static Config config;
   private final File sectionsFolder;
   private final File shopsFolder;
   private static final Map<String, Config> sections = new HashMap<>();
   private static final Map<String, Config> shops = new HashMap<>();

   public ConfigManager(OnyxShop plugin) {
      this.plugin = plugin;
      this.sectionsFolder = new File(this.plugin.getDataFolder() + "/sections/");
      this.shopsFolder = new File(this.plugin.getDataFolder() + "/shops/");
   }

   public static Config getConfig() {
      return config;
   }

   public static String getPathIgnoreCase(String path) {
      String[] values = path.split("\\.");
      ConfigurationSection section = config;
      StringBuilder builder = new StringBuilder();

      for (int index = 0; index < values.length; index++) {
         String text = values[index];
         if (section != null && !section.contains(text)) {
            for (String text2 : section.getKeys(false)) {
               if (text2.replace('_', '-').equalsIgnoreCase(text)) {
                  text = text2;
                  break;
               }
            }
         }

         if (index > 0) {
            builder.append('.');
         }

         builder.append(text);
         if (section != null) {
            section = section.getConfigurationSection(text);
         }
      }

      return builder.toString();
   }

   public static void saveConfig() {
      try {
         config.save();
         config.reload();
      } catch (ConfigLoadException | ConfigSaveException exception) {
         SendMessage.errorMessage(exception.getMessage());
         badYaml = "config.yml";
      }
   }

   public static Config getSection(String sectionId) {
      return sections.get(sectionId);
   }

   public static Set<String> getSections() {
      return sections.keySet();
   }

   public static void saveSection(String sectionId) {
      try {
         Config config = sections.get(sectionId);
         if (config == null) {
            SendMessage.errorMessage("Cannot save unknown section '" + sectionId + "'");
            badYaml = sectionId + ".yml";
            return;
         }

         config.save();
         config.reload();
      } catch (ConfigLoadException | ConfigSaveException exception) {
         SendMessage.errorMessage(exception.getMessage());
         badYaml = sectionId + ".yml";
      }
   }

   public Config createSectionConfig(String sectionId) {
      File file = new File(this.plugin.getDataFolder() + File.separator + "sections" + File.separator, sectionId + ".yml");

      try {
         if (!file.exists()) {
            file.createNewFile();
         }

         sections.put(sectionId, this.loadConf(file, false));
         return sections.get(sectionId);
      } catch (IOException exception) {
         SendMessage.warnMessage("A unknown IOE exception occurred while creating a new section config at " + file.getAbsolutePath());
         return null;
      }
   }

   public void deleteSection(String sectionId) {
      try {
         Config config = sections.remove(sectionId);
         ConfigUtil.createBackup(config.getFilePath());
         Files.deleteIfExists(config.getFilePath());
      } catch (IOException exception) {
         SendMessage.errorMessage("Failed to delete '" + sectionId + "' sections config");
         exception.printStackTrace();
      }
   }

   public ShopType getShopType(String shopId) {
      Config config = sections.get(shopId);
      return config.getString("shop-type", "CATEGORY").equals("ROTATING") ? ShopType.ROTATING : ShopType.CATEGORY;
   }

   public static Config getShop(String shopId) {
      return shops.get(shopId);
   }

   public static Set<String> getShops() {
      return shops.keySet();
   }

   public static void saveShop(String shopId) {
      try {
         Config config = shops.get(shopId);
         if (config == null) {
            SendMessage.errorMessage("Cannot save unknown shop '" + shopId + "'");
            badYaml = shopId + ".yml";
            return;
         }

         config.save();
         config.reload();
      } catch (ConfigLoadException | ConfigSaveException exception) {
         SendMessage.errorMessage(exception.getMessage());
         badYaml = shopId + ".yml";
      }
   }

   public List<String> getRotatingItemsRaw(String text) {
      ArrayList values = new ArrayList();
      Config config = shops.get(text);
      if (config.contains("items")) {
         config.getConfigurationSection("items").getKeys(false).forEach(entry -> values.add("items." + entry));
      }

      return values;
   }

   public List<String> getItemsRaw(String text) {
      if (this.getShopType(text) == ShopType.ROTATING) {
         return this.getRotatingItemsRaw(text);
      } else {
         ArrayList values = new ArrayList();

         for (String text2 : this.getPages(text, false)) {
            shops.get(text).getConfigurationSection("pages." + text2 + ".items").getKeys(false).forEach(entry -> values.add(text2 + ".items." + entry));
         }

         return values;
      }
   }

   public List<String> getPages(String text, boolean flag) {
      LinkedList values = new LinkedList();
      Config config = shops.get(text);
      if (config.contains("pages")) {
         for (String sectionId : config.getConfigurationSection("pages").getKeys(false)) {
            if (flag || config.contains("pages." + sectionId + ".items")) {
               values.add(sectionId);
            }
         }
      }

      return values;
   }

   public void deleteShop(String shopId) {
      try {
         Config config = shops.remove(shopId.split("\\.")[0]);
         ConfigUtil.createBackup(config.getFilePath());
         Files.deleteIfExists(config.getFilePath());
      } catch (IOException exception) {
         SendMessage.errorMessage("Failed to delete '" + shopId.split("\\.")[0] + "' shops config");
         exception.printStackTrace();
      }
   }

   public Config createShopConfig(String shopId) {
      File file = new File(this.plugin.getDataFolder() + File.separator + "shops" + File.separator, shopId + ".yml");

      try {
         if (!file.exists()) {
            file.createNewFile();
         }

         shops.put(shopId, this.loadConf(file, false));
         return shops.get(shopId);
      } catch (IOException exception) {
         SendMessage.warnMessage("A unknown IOE exception occurred while creating a new shop config at " + file.getAbsolutePath());
         return null;
      }
   }

   public void deleteShopsConfig(String text) {
      try {
         Config config = shops.remove(text);
         ConfigUtil.createBackup(config.getFilePath());
         Files.deleteIfExists(config.getFilePath());
      } catch (IOException exception) {
         SendMessage.errorMessage("Failed to delete '" + text + "' shops config");
         exception.printStackTrace();
      }
   }

   public static String getBadYaml() {
      return badYaml;
   }

   public boolean init() {
      if (badYaml == null) {
         config.setDef(YamlConfiguration.loadConfiguration(new InputStreamReader(this.plugin.getResource("config.yml"))));
      }

      try {
         this.loadSections();
         this.loadShops();
         return true;
      } catch (ConfigSaveException | ConfigLoadException exception) {
         SendMessage.errorMessage(exception.getMessage());
         return false;
      }
   }

   public boolean reload() {
      try {
         if (config != null) {
            File file = config.getFilePath().toFile();
            if (!file.exists()) {
               this.plugin.saveResource(file.getName(), false);
               config.reload();
               this.initIgnoredTags();
               NBTUtils.NBTVersion.getFromConfig().saveToConfig();
            } else {
               config.reload();
               config.save();
            }
         } else {
            this.loadConfig();
         }

         if (badYaml != null && badYaml.equals("config.yml")) {
            badYaml = null;
         }
      } catch (ConfigLoadException | ConfigSaveException exception) {
         SendMessage.errorMessage(exception.getMessage());
         badYaml = "config.yml";
         return false;
      }

      try {
         this.loadSections();
         this.loadShops();
         return true;
      } catch (ConfigSaveException | ConfigLoadException exception) {
         SendMessage.errorMessage(exception.getMessage());
         return false;
      }
   }

   public void loadConfig() {
      boolean flag = false;
      File file = new File(this.plugin.getDataFolder(), "config.yml");
      if (!file.exists()) {
         this.plugin.saveResource("config.yml", false);
         flag = true;
      }

      config = this.loadConf(file, true);
      if (config != null) {
         try {
            config.reload();
            config.save();
         } catch (ConfigLoadException | ConfigSaveException exception) {
            SendMessage.errorMessage(exception.getMessage());
            badYaml = "config.yml";
         }

         if (flag) {
            this.initIgnoredTags();
            if (ServerInfo.getVersion() != ServerInfo.Version.UNKOWN_VERSION) {
               NBTUtils.NBTVersion.getFromConfig().saveToConfig();
            }
         }
      }
   }

   private void initIgnoredTags() {
      if (ServerInfo.supportsComponents()) {
         List stringList = config.getStringList("sold-items-ignored-NBTtags");

         for (int size = 0; size < stringList.size(); size++) {
            String text = (String)stringList.get(size);
            stringList.set(size, NBTMaps.getTag(text, null, "sold-items-ignored-NBTtags", false));
         }

         config.set("sold-items-ignored-NBTtags", stringList);
         saveConfig();
      }
   }

   private void loadSections() throws ConfigLoadException, ConfigSaveException {
      if (!sections.isEmpty()) {
         sections.clear();
      }

      int value = 0;
      if (!this.sectionsFolder.exists() || !this.sectionsFolder.isDirectory()) {
         String text = ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v26_1) ? "2" : "1";
         this.saveResources(this.sectionsFolder, text + "-sections");
         this.createExampleSection();

         try {
            Files.move(new File(this.plugin.getDataFolder() + File.separator + text + "-sections" + File.separator).toPath(), this.sectionsFolder.toPath());
         } catch (IOException exception) {
            throw new ConfigLoadException("A unknown IOE occurred while renaming sections directory.\n" + this.stackTraceToString(exception));
         }
      }

      for (File file : this.sectionsFolder.listFiles()) {
         if (file.isDirectory()) {
            Map map = this.loadFromDir(file, false, 1);
            value += map.size();
            sections.putAll(map);
         } else if (file.getName().toLowerCase(java.util.Locale.ENGLISH).endsWith(".yml")) {
            Config config = this.loadConf(file, false);
            if (config != null) {
               value++;
               sections.put(file.getName().substring(0, file.getName().length() - 4), config);
            }
         } else {
            SendMessage.logDebugMessage("Skipping non-config file " + file.getName());
         }
      }

      SendMessage.infoMessage(Lang.LOADED_SECTION_CONFIGURATIONS.get().replace("%total%", String.valueOf(value)));
   }

   private void saveSections() throws ConfigSaveException {
      try {
         InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("example.yml");
         FileOutputStream fileOutputStream = new FileOutputStream(this.plugin.getDataFolder().getAbsolutePath() + "/example.yml");
         byte[] value = new byte[1024];

         int value2;
         while ((value2 = inputStream.read(value)) > 0) {
            fileOutputStream.write(value, 0, value2);
         }

         fileOutputStream.close();
         inputStream.close();
         SendMessage.infoMessage(Lang.DEFAULT_SECTIONS_CREATED.get().replace("%path%", this.sectionsFolder.getAbsolutePath()));
      } catch (IOException exception) {
         throw new ConfigSaveException(
            "A unknown IOE exception occurred while creating the default sections folder at "
               + this.sectionsFolder.getPath()
               + ":\n"
               + this.stackTraceToString(exception)
         );
      }
   }

   private void loadShops() throws ConfigLoadException, ConfigSaveException {
      if (!shops.isEmpty()) {
         shops.clear();
      }

      int value = 0;
      if (!this.shopsFolder.exists() || !this.shopsFolder.isDirectory()) {
         String text = this.plugin.version + "-shops";
         if (this.getClass().getClassLoader().getResourceAsStream(text) == null) {
            text = (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v26_1) ? "261" : "121") + "-shops";
            SendMessage.warnMessage(Lang.CREATING_LATEST_SHOPS.get().replace("%version%", String.valueOf(this.plugin.version)));
         }

         this.saveResources(this.shopsFolder, text);

         try {
            Files.move(new File(this.plugin.getDataFolder() + File.separator + text + File.separator).toPath(), this.shopsFolder.toPath());
            SendMessage.infoMessage(
               Lang.DEFAULT_SHOPS_CREATED.get().replace("%version%", String.valueOf(this.plugin.version)).replace("%path%", this.shopsFolder.getAbsolutePath())
            );
         } catch (IOException exception) {
            throw new ConfigLoadException("A unknown IOE occurred while renaming shops directory.\n" + this.stackTraceToString(exception));
         }

         this.createExampleShop();
      }

      for (File file : this.shopsFolder.listFiles()) {
         if (file.isDirectory()) {
            Map map = this.loadFromDir(file, false, 1);
            value += map.size();
            shops.putAll(map);
         } else if (file.getName().toLowerCase(java.util.Locale.ENGLISH).endsWith(".yml")) {
            Config config = this.loadConf(file, false);
            if (config != null) {
               value++;
               shops.put(file.getName().substring(0, file.getName().length() - 4), config);
            }
         } else {
            SendMessage.logDebugMessage("Skipping non-config file " + file.getName());
         }
      }

      SendMessage.infoMessage(Lang.LOADED_SHOP_CONFIGURATIONS.get().replace("%total%", String.valueOf(value)));
   }

   private Map<String, Config> loadFromDir(File file, boolean flag, int value) {
      HashMap map = new HashMap();

      for (File file2 : file.listFiles()) {
         if (file2.isDirectory() && value <= 2) {
            return this.loadFromDir(file2, flag, ++value);
         }

         if (file2.getName().toLowerCase(java.util.Locale.ENGLISH).endsWith(".yml")) {
            Config config = this.loadConf(file2, flag);
            if (config != null) {
               map.put(file2.getName().substring(0, file2.getName().length() - 4), config);
            }
         } else {
            SendMessage.logDebugMessage("Skipping non-config file " + file2.getName());
         }
      }

      return map;
   }

   public void validate() {
      SendMessage.infoMessage(Lang.UPDATING_SHOP_SETTINGS.get());
      ArrayList<String> values = new ArrayList<>(getShops());
      ArrayList<String> values2 = new ArrayList<>(getSections());
      Set<String> itemIds = values2.stream().filter(element -> sections.get(element).getBoolean("display-item")).collect(Collectors.toSet());
      if (!itemIds.isEmpty()) {
         values2.removeAll(itemIds);
         values.removeAll(itemIds);
      }

      if (values.size() != values2.size()) {
         if (values.size() < values2.size()) {
            for (String text : values2) {
               if (!values.contains(text)) {
                  SendMessage.warnMessage(Lang.CREATING_NEW_SHOP_CONFIG.get().replace("%section%", text));
                  Config config = this.createShopConfig(text);
                  if (config != null) {
                     config.set("pages.page1.items.example-item", this.getExampleItem(text));
                     saveShop(text);
                     values.add(text);
                     shops.put(text, config);
                  }
               }
            }
         } else {
            for (String text : values) {
               if (!values2.contains(text)) {
                  SendMessage.warnMessage(Lang.CREATING_NEW_SECTION_CONFIG.get().replace("%section%", text));
                  Config config = this.createSectionConfig(text);
                  if (config == null) {
                     return;
                  }

                  this.getExampleShop(text).forEach(config::set);
                  saveSection(text);
                  values2.add(text);
                  sections.put(text, config);
                  this.plugin.getSectionTitles().put(text, SendMessage.fromConfig("&a" + text));
               }
            }
         }
      }

      for (String text : new ArrayList<>(values2)) {
         if (!values.contains(text)) {
            String text2 = values.stream().filter(element -> !values2.contains(element)).findFirst().orElse(null);
            if (text2 != null) {
               shops.remove(text2);
            }

            values2.remove(text);
            sections.remove(text);
            SendMessage.errorMessage("Failed to match section file with shop file " + text + ".yml");
            SendMessage.errorMessage(
               "Both files inside the /shops/ and /sections/ directory should have the same naming schema for this shop category, skipping..."
            );
         }
      }

      ArrayList values3 = new ArrayList();

      for (String text : values2) {
         if (!sections.get(text).getBoolean("enable", true)) {
            values3.add(text);
         }
      }

      values2.removeAll(values3);
      values2.addAll(itemIds);
      this.plugin.setShopSections(values2);
   }

   private Map<String, Object> getExampleShop(String shopId) {
      HashMap map = new HashMap();
      map.put("enable", true);
      map.put("slot", 0);
      map.put("display-item", false);
      map.put("hidden", false);
      map.put("title", "&c&lExample section");
      map.put("fill-item.material", "AIR");
      map.put("nav-bar.mode", "INHERIT");
      map.put("item.material", "BEACON");
      map.put("item.displayname", "&a" + shopId);
      map.put("item.enchantment-glint", false);
      map.put(
         "item.lore",
         Arrays.asList(
            "&4This example section was automatically added",
            "&4because the shops config still exists,",
            "&4but there was no sections config found",
            "&4for section &c'" + shopId + "'.",
            "&fThis is a commun mistake while removing shops,",
            "&fmake sure both section and shop config files are removed."
         )
      );
      return map;
   }

   private Map<String, Object> getExampleItem(String itemId) {
      HashMap map = new HashMap();
      map.put("material", "STICK");
      map.put("name", "&c&lPower stick v2");
      map.put("displayname", "&c&lExample power stick");
      map.put("buy", 1234);
      map.put("sell", 123);
      map.put("lore", Arrays.asList("&4This example item", "&4was added to shop", "&4because the shops.yml config", "&4did not contain section &c" + itemId));
      map.put(
         "enchantments",
         Arrays.asList(
            "KNOCKBACK:2", "SHARPNESS:5", "FIRE_ASPECT:5", "BANE_OF_ARTHROPODS:5", "SMITE:5", "UNBREAKING:3", "LOOTING:5", "MENDING", "SWEEPING_EDGE:3"
         )
      );
      map.put("slot", 5);
      map.put("stack-size", 2);
      map.put("stock-limit-global", 500);
      map.put("auto-restock-global", "12h 30m");
      map.put("hidePricingLore", true);
      map.put("NBTData", "{HideFlags:1}");
      return map;
   }

   private Config loadConf(File file, boolean flag) {
      try {
         if (badYaml != null && badYaml.equals(file.getName())) {
            badYaml = null;
         }

         return (Config)(flag ? new CommentedConfig(this.plugin, file, this.plugin.getResource(file.getName())) : new Config(file));
      } catch (ConfigLoadException exception) {
         SendMessage.errorMessage(exception.getMessage());
         badYaml = file.getName();
         return null;
      }
   }

   public void saveResources(File file, String text) throws ConfigSaveException {
      try {
         JarFile jarFile = new JarFile(OnyxShop.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
         Enumeration enumeration = jarFile.entries();

         while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry)enumeration.nextElement();
            if (jarEntry.getName().startsWith(text) && !jarEntry.getName().endsWith("/")) {
               this.plugin.saveResource(jarEntry.getName(), false);
            }
         }
      } catch (URISyntaxException | IOException exception) {
         throw new ConfigSaveException(
            "A unknown IOE exception occurred while saving the default "
               + text
               + " folder at "
               + file.getAbsolutePath()
               + ":\n"
               + this.stackTraceToString(exception)
         );
      }
   }

   private void createExampleShop() {
      if (!new File(this.plugin.getDataFolder(), "ExampleShop.yml").exists()) {
         this.plugin.saveResource("ExampleShop.yml", false);
      }
   }

   private void createExampleSection() {
      if (!new File(this.plugin.getDataFolder(), "ExampleSection.yml").exists()) {
         this.plugin.saveResource("ExampleSection.yml", false);
      }
   }

   private String stackTraceToString(Exception exception) {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      exception.printStackTrace(printWriter);
      return stringWriter.toString();
   }

   public static final class CFile {
      public final ConfigManager.CFile.Type type;
      final String name;

      public CFile(ConfigManager.CFile.Type type, String type2) {
         this.type = type;
         this.name = type2;
      }

      public String getName() {
         switch (this.type) {
            case CONFIG:
            default:
               return this.name + ".yml";
            case SECTION:
               return "sections/" + this.name + ".yml";
            case SHOP:
               return "shops/" + this.name + ".yml";
         }
      }

      public Config get() {
         switch (this.type) {
            case CONFIG:
               return ConfigManager.getConfig();
            case SECTION:
               return ConfigManager.getSection(this.name);
            case SHOP:
               return ConfigManager.getShop(this.name);
            default:
               return null;
         }
      }

      public void save() {
         switch (this.type) {
            case SHOP:
               ConfigManager.saveShop(this.name);
               break;
            case SECTION:
               ConfigManager.saveSection(this.name);
               break;
            case CONFIG:
               ConfigManager.saveConfig();
               break;
         }
      }

      @Override
      public boolean equals(Object value) {
         if (this == value) {
            return true;
         } else if (!(value instanceof ConfigManager.CFile)) {
            return false;
         } else {
            ConfigManager.CFile cFile = (ConfigManager.CFile)value;
            return this.type.equals(cFile.type) && this.name.equals(cFile.name);
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.type, this.name);
      }

      public static enum Type {
         CONFIG,
         SECTION,
         SHOP;

         private Type() {
         }
      }
   }
}
