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

   public ConfigManager(OnyxShop var1) {
      this.plugin = var1;
      this.sectionsFolder = new File(this.plugin.getDataFolder() + "/sections/");
      this.shopsFolder = new File(this.plugin.getDataFolder() + "/shops/");
   }

   public static Config getConfig() {
      return config;
   }

   public static String getPathIgnoreCase(String var0) {
      String[] var1 = var0.split("\\.");
      ConfigurationSection var2 = config;
      StringBuilder var3 = new StringBuilder();

      for (int var4 = 0; var4 < var1.length; var4++) {
         String var5 = var1[var4];
         if (var2 != null && !var2.contains(var5)) {
            for (String var7 : var2.getKeys(false)) {
               if (var7.replace('_', '-').equalsIgnoreCase(var5)) {
                  var5 = var7;
                  break;
               }
            }
         }

         if (var4 > 0) {
            var3.append('.');
         }

         var3.append(var5);
         if (var2 != null) {
            var2 = var2.getConfigurationSection(var5);
         }
      }

      return var3.toString();
   }

   public static void saveConfig() {
      try {
         config.save();
         config.reload();
      } catch (ConfigLoadException | ConfigSaveException var1) {
         SendMessage.errorMessage(var1.getMessage());
         badYaml = "config.yml";
      }
   }

   public static Config getSection(String var0) {
      return sections.get(var0);
   }

   public static Set<String> getSections() {
      return sections.keySet();
   }

   public static void saveSection(String var0) {
      try {
         Config var1 = sections.get(var0);
         if (var1 == null) {
            SendMessage.errorMessage("Cannot save unknown section '" + var0 + "'");
            badYaml = var0 + ".yml";
            return;
         }

         var1.save();
         var1.reload();
      } catch (ConfigLoadException | ConfigSaveException var2) {
         SendMessage.errorMessage(var2.getMessage());
         badYaml = var0 + ".yml";
      }
   }

   public Config createSectionConfig(String var1) {
      File var2 = new File(this.plugin.getDataFolder() + File.separator + "sections" + File.separator, var1 + ".yml");

      try {
         if (!var2.exists()) {
            var2.createNewFile();
         }

         sections.put(var1, this.loadConf(var2, false));
         return sections.get(var1);
      } catch (IOException var4) {
         SendMessage.warnMessage("A unknown IOE exception occurred while creating a new section config at " + var2.getAbsolutePath());
         return null;
      }
   }

   public void deleteSection(String var1) {
      try {
         Config var2 = sections.remove(var1);
         ConfigUtil.createBackup(var2.getFilePath());
         Files.deleteIfExists(var2.getFilePath());
      } catch (IOException var3) {
         SendMessage.errorMessage("Failed to delete '" + var1 + "' sections config");
         var3.printStackTrace();
      }
   }

   public ShopType getShopType(String var1) {
      Config var2 = sections.get(var1);
      return var2.getString("shop-type", "CATEGORY").equals("ROTATING") ? ShopType.ROTATING : ShopType.CATEGORY;
   }

   public static Config getShop(String var0) {
      return shops.get(var0);
   }

   public static Set<String> getShops() {
      return shops.keySet();
   }

   public static void saveShop(String var0) {
      try {
         Config var1 = shops.get(var0);
         if (var1 == null) {
            SendMessage.errorMessage("Cannot save unknown shop '" + var0 + "'");
            badYaml = var0 + ".yml";
            return;
         }

         var1.save();
         var1.reload();
      } catch (ConfigLoadException | ConfigSaveException var2) {
         SendMessage.errorMessage(var2.getMessage());
         badYaml = var0 + ".yml";
      }
   }

   public List<String> getRotatingItemsRaw(String var1) {
      ArrayList var2 = new ArrayList();
      Config var3 = shops.get(var1);
      if (var3.contains("items")) {
         var3.getConfigurationSection("items").getKeys(false).forEach(var1x -> var2.add("items." + var1x));
      }

      return var2;
   }

   public List<String> getItemsRaw(String var1) {
      if (this.getShopType(var1) == ShopType.ROTATING) {
         return this.getRotatingItemsRaw(var1);
      } else {
         ArrayList var2 = new ArrayList();

         for (String var4 : this.getPages(var1, false)) {
            shops.get(var1).getConfigurationSection("pages." + var4 + ".items").getKeys(false).forEach(var2x -> var2.add(var4 + ".items." + var2x));
         }

         return var2;
      }
   }

   public List<String> getPages(String var1, boolean var2) {
      LinkedList var3 = new LinkedList();
      Config var4 = shops.get(var1);
      if (var4.contains("pages")) {
         for (String var6 : var4.getConfigurationSection("pages").getKeys(false)) {
            if (var2 || var4.contains("pages." + var6 + ".items")) {
               var3.add(var6);
            }
         }
      }

      return var3;
   }

   public void deleteShop(String var1) {
      try {
         Config var2 = shops.remove(var1.split("\\.")[0]);
         ConfigUtil.createBackup(var2.getFilePath());
         Files.deleteIfExists(var2.getFilePath());
      } catch (IOException var3) {
         SendMessage.errorMessage("Failed to delete '" + var1.split("\\.")[0] + "' shops config");
         var3.printStackTrace();
      }
   }

   public Config createShopConfig(String var1) {
      File var2 = new File(this.plugin.getDataFolder() + File.separator + "shops" + File.separator, var1 + ".yml");

      try {
         if (!var2.exists()) {
            var2.createNewFile();
         }

         shops.put(var1, this.loadConf(var2, false));
         return shops.get(var1);
      } catch (IOException var4) {
         SendMessage.warnMessage("A unknown IOE exception occurred while creating a new shop config at " + var2.getAbsolutePath());
         return null;
      }
   }

   public void deleteShopsConfig(String var1) {
      try {
         Config var2 = shops.remove(var1);
         ConfigUtil.createBackup(var2.getFilePath());
         Files.deleteIfExists(var2.getFilePath());
      } catch (IOException var3) {
         SendMessage.errorMessage("Failed to delete '" + var1 + "' shops config");
         var3.printStackTrace();
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
      } catch (ConfigSaveException | ConfigLoadException var2) {
         SendMessage.errorMessage(var2.getMessage());
         return false;
      }
   }

   public boolean reload() {
      try {
         if (config != null) {
            File var1 = config.getFilePath().toFile();
            if (!var1.exists()) {
               this.plugin.saveResource(var1.getName(), false);
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
      } catch (ConfigLoadException | ConfigSaveException var3) {
         SendMessage.errorMessage(var3.getMessage());
         badYaml = "config.yml";
         return false;
      }

      try {
         this.loadSections();
         this.loadShops();
         return true;
      } catch (ConfigSaveException | ConfigLoadException var2) {
         SendMessage.errorMessage(var2.getMessage());
         return false;
      }
   }

   public void loadConfig() {
      boolean var1 = false;
      File var2 = new File(this.plugin.getDataFolder(), "config.yml");
      if (!var2.exists()) {
         this.plugin.saveResource("config.yml", false);
         var1 = true;
      }

      config = this.loadConf(var2, true);
      if (config != null) {
         try {
            config.reload();
            config.save();
         } catch (ConfigLoadException | ConfigSaveException var4) {
            SendMessage.errorMessage(var4.getMessage());
            badYaml = "config.yml";
         }

         if (var1) {
            this.initIgnoredTags();
            if (ServerInfo.getVersion() != ServerInfo.Version.UNKOWN_VERSION) {
               NBTUtils.NBTVersion.getFromConfig().saveToConfig();
            }
         }
      }
   }

   private void initIgnoredTags() {
      if (ServerInfo.supportsComponents()) {
         List var1 = config.getStringList("sold-items-ignored-NBTtags");

         for (int var2 = 0; var2 < var1.size(); var2++) {
            String var3 = (String)var1.get(var2);
            var1.set(var2, NBTMaps.getTag(var3, null, "sold-items-ignored-NBTtags", false));
         }

         config.set("sold-items-ignored-NBTtags", var1);
         saveConfig();
      }
   }

   private void loadSections() throws ConfigLoadException, ConfigSaveException {
      if (!sections.isEmpty()) {
         sections.clear();
      }

      int var1 = 0;
      if (!this.sectionsFolder.exists() || !this.sectionsFolder.isDirectory()) {
         String var2 = ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v26_1) ? "2" : "1";
         this.saveResources(this.sectionsFolder, var2 + "-sections");
         this.createExampleSection();

         try {
            Files.move(new File(this.plugin.getDataFolder() + File.separator + var2 + "-sections" + File.separator).toPath(), this.sectionsFolder.toPath());
         } catch (IOException var7) {
            throw new ConfigLoadException("A unknown IOE occurred while renaming sections directory.\n" + this.stackTraceToString(var7));
         }
      }

      for (File var5 : this.sectionsFolder.listFiles()) {
         if (var5.isDirectory()) {
            Map var6 = this.loadFromDir(var5, false, 1);
            var1 += var6.size();
            sections.putAll(var6);
         } else if (var5.getName().toLowerCase(java.util.Locale.ENGLISH).endsWith(".yml")) {
            Config var9 = this.loadConf(var5, false);
            if (var9 != null) {
               var1++;
               sections.put(var5.getName().substring(0, var5.getName().length() - 4), var9);
            }
         } else {
            SendMessage.logDebugMessage("Skipping non-config file " + var5.getName());
         }
      }

      SendMessage.infoMessage(Lang.LOADED_SECTION_CONFIGURATIONS.get().replace("%total%", String.valueOf(var1)));
   }

   private void saveSections() throws ConfigSaveException {
      try {
         InputStream var1 = this.getClass().getClassLoader().getResourceAsStream("example.yml");
         FileOutputStream var2 = new FileOutputStream(this.plugin.getDataFolder().getAbsolutePath() + "/example.yml");
         byte[] var3 = new byte[1024];

         int var4;
         while ((var4 = var1.read(var3)) > 0) {
            var2.write(var3, 0, var4);
         }

         var2.close();
         var1.close();
         SendMessage.infoMessage(Lang.DEFAULT_SECTIONS_CREATED.get().replace("%path%", this.sectionsFolder.getAbsolutePath()));
      } catch (IOException var5) {
         throw new ConfigSaveException(
            "A unknown IOE exception occurred while creating the default sections folder at "
               + this.sectionsFolder.getPath()
               + ":\n"
               + this.stackTraceToString(var5)
         );
      }
   }

   private void loadShops() throws ConfigLoadException, ConfigSaveException {
      if (!shops.isEmpty()) {
         shops.clear();
      }

      int var1 = 0;
      if (!this.shopsFolder.exists() || !this.shopsFolder.isDirectory()) {
         String var2 = this.plugin.version + "-shops";
         if (this.getClass().getClassLoader().getResourceAsStream(var2) == null) {
            var2 = (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v26_1) ? "261" : "121") + "-shops";
            SendMessage.warnMessage(Lang.CREATING_LATEST_SHOPS.get().replace("%version%", String.valueOf(this.plugin.version)));
         }

         this.saveResources(this.shopsFolder, var2);

         try {
            Files.move(new File(this.plugin.getDataFolder() + File.separator + var2 + File.separator).toPath(), this.shopsFolder.toPath());
            SendMessage.infoMessage(
               Lang.DEFAULT_SHOPS_CREATED.get().replace("%version%", String.valueOf(this.plugin.version)).replace("%path%", this.shopsFolder.getAbsolutePath())
            );
         } catch (IOException var7) {
            throw new ConfigLoadException("A unknown IOE occurred while renaming shops directory.\n" + this.stackTraceToString(var7));
         }

         this.createExampleShop();
      }

      for (File var5 : this.shopsFolder.listFiles()) {
         if (var5.isDirectory()) {
            Map var6 = this.loadFromDir(var5, false, 1);
            var1 += var6.size();
            shops.putAll(var6);
         } else if (var5.getName().toLowerCase(java.util.Locale.ENGLISH).endsWith(".yml")) {
            Config var9 = this.loadConf(var5, false);
            if (var9 != null) {
               var1++;
               shops.put(var5.getName().substring(0, var5.getName().length() - 4), var9);
            }
         } else {
            SendMessage.logDebugMessage("Skipping non-config file " + var5.getName());
         }
      }

      SendMessage.infoMessage(Lang.LOADED_SHOP_CONFIGURATIONS.get().replace("%total%", String.valueOf(var1)));
   }

   private Map<String, Config> loadFromDir(File var1, boolean var2, int var3) {
      HashMap var4 = new HashMap();

      for (File var8 : var1.listFiles()) {
         if (var8.isDirectory() && var3 <= 2) {
            return this.loadFromDir(var8, var2, ++var3);
         }

         if (var8.getName().toLowerCase(java.util.Locale.ENGLISH).endsWith(".yml")) {
            Config var9 = this.loadConf(var8, var2);
            if (var9 != null) {
               var4.put(var8.getName().substring(0, var8.getName().length() - 4), var9);
            }
         } else {
            SendMessage.logDebugMessage("Skipping non-config file " + var8.getName());
         }
      }

      return var4;
   }

   public void validate() {
      SendMessage.infoMessage(Lang.UPDATING_SHOP_SETTINGS.get());
      ArrayList<String> var1 = new ArrayList<>(getShops());
      ArrayList<String> var2 = new ArrayList<>(getSections());
      Set<String> var3 = var2.stream().filter(var0 -> sections.get(var0).getBoolean("display-item")).collect(Collectors.toSet());
      if (!var3.isEmpty()) {
         var2.removeAll(var3);
         var1.removeAll(var3);
      }

      if (var1.size() != var2.size()) {
         if (var1.size() < var2.size()) {
            for (String var5 : var2) {
               if (!var1.contains(var5)) {
                  SendMessage.warnMessage(Lang.CREATING_NEW_SHOP_CONFIG.get().replace("%section%", var5));
                  Config var6 = this.createShopConfig(var5);
                  if (var6 != null) {
                     var6.set("pages.page1.items.example-item", this.getExampleItem(var5));
                     saveShop(var5);
                     var1.add(var5);
                     shops.put(var5, var6);
                  }
               }
            }
         } else {
            for (String var10 : var1) {
               if (!var2.contains(var10)) {
                  SendMessage.warnMessage(Lang.CREATING_NEW_SECTION_CONFIG.get().replace("%section%", var10));
                  Config var13 = this.createSectionConfig(var10);
                  if (var13 == null) {
                     return;
                  }

                  this.getExampleShop(var10).forEach(var13::set);
                  saveSection(var10);
                  var2.add(var10);
                  sections.put(var10, var13);
                  this.plugin.getSectionTitles().put(var10, SendMessage.fromConfig("&a" + var10));
               }
            }
         }
      }

      for (String var11 : new ArrayList<>(var2)) {
         if (!var1.contains(var11)) {
            String var14 = var1.stream().filter(var1x -> !var2.contains(var1x)).findFirst().orElse(null);
            if (var14 != null) {
               shops.remove(var14);
            }

            var2.remove(var11);
            sections.remove(var11);
            SendMessage.errorMessage("Failed to match section file with shop file " + var11 + ".yml");
            SendMessage.errorMessage(
               "Both files inside the /shops/ and /sections/ directory should have the same naming schema for this shop category, skipping..."
            );
         }
      }

      ArrayList var9 = new ArrayList();

      for (String var15 : var2) {
         if (!sections.get(var15).getBoolean("enable", true)) {
            var9.add(var15);
         }
      }

      var2.removeAll(var9);
      var2.addAll(var3);
      this.plugin.setShopSections(var2);
   }

   private Map<String, Object> getExampleShop(String var1) {
      HashMap var2 = new HashMap();
      var2.put("enable", true);
      var2.put("slot", 0);
      var2.put("display-item", false);
      var2.put("hidden", false);
      var2.put("title", "&c&lExample section");
      var2.put("fill-item.material", "AIR");
      var2.put("nav-bar.mode", "INHERIT");
      var2.put("item.material", "BEACON");
      var2.put("item.displayname", "&a" + var1);
      var2.put("item.enchantment-glint", false);
      var2.put(
         "item.lore",
         Arrays.asList(
            "&4This example section was automatically added",
            "&4because the shops config still exists,",
            "&4but there was no sections config found",
            "&4for section &c'" + var1 + "'.",
            "&fThis is a commun mistake while removing shops,",
            "&fmake sure both section and shop config files are removed."
         )
      );
      return var2;
   }

   private Map<String, Object> getExampleItem(String var1) {
      HashMap var2 = new HashMap();
      var2.put("material", "STICK");
      var2.put("name", "&c&lPower stick v2");
      var2.put("displayname", "&c&lExample power stick");
      var2.put("buy", 1234);
      var2.put("sell", 123);
      var2.put("lore", Arrays.asList("&4This example item", "&4was added to shop", "&4because the shops.yml config", "&4did not contain section &c" + var1));
      var2.put(
         "enchantments",
         Arrays.asList(
            "KNOCKBACK:2", "SHARPNESS:5", "FIRE_ASPECT:5", "BANE_OF_ARTHROPODS:5", "SMITE:5", "UNBREAKING:3", "LOOTING:5", "MENDING", "SWEEPING_EDGE:3"
         )
      );
      var2.put("slot", 5);
      var2.put("stack-size", 2);
      var2.put("stock-limit-global", 500);
      var2.put("auto-restock-global", "12h 30m");
      var2.put("hidePricingLore", true);
      var2.put("NBTData", "{HideFlags:1}");
      return var2;
   }

   private Config loadConf(File var1, boolean var2) {
      try {
         if (badYaml != null && badYaml.equals(var1.getName())) {
            badYaml = null;
         }

         return (Config)(var2 ? new CommentedConfig(this.plugin, var1, this.plugin.getResource(var1.getName())) : new Config(var1));
      } catch (ConfigLoadException var4) {
         SendMessage.errorMessage(var4.getMessage());
         badYaml = var1.getName();
         return null;
      }
   }

   public void saveResources(File var1, String var2) throws ConfigSaveException {
      try {
         JarFile var3 = new JarFile(OnyxShop.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
         Enumeration var4 = var3.entries();

         while (var4.hasMoreElements()) {
            JarEntry var5 = (JarEntry)var4.nextElement();
            if (var5.getName().startsWith(var2) && !var5.getName().endsWith("/")) {
               this.plugin.saveResource(var5.getName(), false);
            }
         }
      } catch (URISyntaxException | IOException var6) {
         throw new ConfigSaveException(
            "A unknown IOE exception occurred while saving the default "
               + var2
               + " folder at "
               + var1.getAbsolutePath()
               + ":\n"
               + this.stackTraceToString(var6)
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

   private String stackTraceToString(Exception var1) {
      StringWriter var2 = new StringWriter();
      PrintWriter var3 = new PrintWriter(var2);
      var1.printStackTrace(var3);
      return var2.toString();
   }

   public static final class CFile {
      public final ConfigManager.CFile.Type type;
      final String name;

      public CFile(ConfigManager.CFile.Type var1, String var2) {
         this.type = var1;
         this.name = var2;
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
      public boolean equals(Object var1) {
         if (this == var1) {
            return true;
         } else if (!(var1 instanceof ConfigManager.CFile)) {
            return false;
         } else {
            ConfigManager.CFile var2 = (ConfigManager.CFile)var1;
            return this.type.equals(var2.type) && this.name.equals(var2.name);
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
