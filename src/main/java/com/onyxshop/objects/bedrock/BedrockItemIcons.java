package com.onyxshop.objects.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.MalformedURLException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.NBTMaps;
import com.onyxshop.util.ServerInfo;
import com.onyxshop.util.XMaterial;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.geyser.api.GeyserApi;

public class BedrockItemIcons {
   private static final String GEYSER_ITEM_STATES = "https://raw.githubusercontent.com/GeyserMC/Geyser/master/core/src/main/resources/bedrock/runtime_item_states.%version%.json";
   private static final String GEYSER_BEDROCK_CONTENTS = "https://api.github.com/repos/GeyserMC/Geyser/contents/core/src/main/resources/bedrock?ref=master";
   private static final String GEYSER_MAPPINGS = "https://raw.githubusercontent.com/GeyserMC/mappings/master/items.json";
   private static final String VANILLA_ITEM_TEXTURES = "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/textures/item_texture.json";
   private static final String VANILLA_TERRAIN_TEXTURES = "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/textures/terrain_texture.json";
   private static final String VANILLA_BLOCKS = "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/blocks.json";
   private static final String ITEM_STATES_PREFIX = "runtime_item_states.";
   private static final int ENCHANTED_OFFSET = 32768;
   private static volatile Map<XMaterial, Integer> ITEM_IDS = new ConcurrentHashMap<>();
   private static volatile Map<XMaterial, String> ITEM_TEXTURE_PATHS = new ConcurrentHashMap<>();
   private static OnyxShop plugin;
   private static File cacheDir;
   private static long cacheExpiryHours;
   private static boolean forceUpdate;
   private static String configPaletteVersion;
   private static boolean gridForms;

   public BedrockItemIcons() {
   }

   public static void init(OnyxShop var0) {
      plugin = var0;
      if (ConfigManager.getConfig().getBoolean("bedrock-forms.enable", true)) {
         cacheDir = new File(var0.getDataFolder(), "cache");
         cacheExpiryHours = ConfigManager.getConfig().getLong("bedrock-forms.item-ids.cache", 48L);
         forceUpdate = ConfigManager.getConfig().getBoolean("bedrock-forms.item-ids.force-update", false);
         configPaletteVersion = ConfigManager.getConfig().getString("bedrock-forms.item-ids.geyser-palette-version", "").trim();
         gridForms = ConfigManager.getConfig().getBoolean("bedrock-forms.use-grid-forms", false);
         var0.runKillableTask(BedrockItemIcons::load);
      }
   }

   private static void load() {
      try {
         if (gridForms) {
            loadGridIds();
         } else {
            loadVanillaTexturePaths();
         }

         if (forceUpdate) {
            disableForceUpdate();
         }
      } catch (Exception var1) {
         SendMessage.errorMessage("Failed to load bedrock item icons (" + var1.getMessage() + ")");
      }
   }

   private static void disableForceUpdate() {
      OnyxShop.getInstance().runTask(() -> {
         ConfigManager.getConfig().set("bedrock-forms.item-ids.force-update", false);
         ConfigManager.saveConfig();
      });
   }

   private static void loadGridIds() throws IOException {
      JsonElement var0 = loadItemStatesFromGeyser();
      if (var0 == null) {
         String var1 = configPaletteVersion.isEmpty() ? fetchLatestPaletteVersion() : configPaletteVersion;
         if (var1 == null) {
            SendMessage.errorMessage("Could not determine the latest Geyser item palette, icons for bedrock items in grid forms will not load!");
            return;
         }

         var0 = downloadItemStates(var1);
      }

      HashMap var10 = new HashMap();

      for (JsonElement var3 : var0.getAsJsonArray()) {
         JsonObject var4 = var3.getAsJsonObject();
         var10.put(getValue(var4.get("name").getAsString()), var4.get("id").getAsInt());
      }

      ConcurrentHashMap var11 = new ConcurrentHashMap();

      for (Entry var13 : loadJson(
            cacheDir, "geyser-items.json", "https://raw.githubusercontent.com/GeyserMC/mappings/master/items.json", cacheExpiryHours, forceUpdate
         )
         .getAsJsonObject()
         .entrySet()) {
         JsonObject var5 = ((JsonElement)var13.getValue()).getAsJsonObject();
         String var6 = getValue(var5.get("bedrock_identifier").getAsString());
         Integer var7 = (Integer)var10.get(var6);
         if (var7 != null) {
            XMaterial var8 = XMaterial.matchXMaterial(getValue((String)var13.getKey())).orElse(null);
            if (var8 != null) {
               int var9 = var5.has("bedrock_data") ? var5.get("bedrock_data").getAsInt() : 0;
               var11.put(var8, var7 * 65536 + var9);
            }
         }
      }

      ITEM_IDS = var11;
      SendMessage.infoMessage("Loaded " + var11.size() + " bedrock item ids for grid form icons");
   }

   private static void loadVanillaTexturePaths() throws IOException {
      HashMap<String, JsonElement> var0 = new HashMap<>();
      HashMap var1 = new HashMap();
      HashMap var2 = new HashMap();

      try {
         JsonObject var3 = loadJson(
               cacheDir,
               "item_texture.json",
               "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/textures/item_texture.json",
               cacheExpiryHours,
               forceUpdate
            )
            .getAsJsonObject()
            .getAsJsonObject("texture_data");
         if (var3 != null) {
            for (Entry var5 : var3.entrySet()) {
               var0.put((String)var5.getKey(), ((JsonElement)var5.getValue()).getAsJsonObject().get("textures"));
            }
         }

         JsonObject var14 = loadJson(
               cacheDir,
               "terrain_texture.json",
               "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/textures/terrain_texture.json",
               cacheExpiryHours,
               forceUpdate
            )
            .getAsJsonObject()
            .getAsJsonObject("texture_data");
         if (var14 != null) {
            for (Entry var6 : var14.entrySet()) {
               var0.put((String)var6.getKey(), ((JsonElement)var6.getValue()).getAsJsonObject().get("textures"));
            }
         }

         for (Entry var20 : var0.entrySet()) {
            for (String var8 : getTexturePaths((JsonElement)var20.getValue())) {
               String var9 = var8.substring(var8.lastIndexOf(47) + 1);
               var1.putIfAbsent(var9, var8);
            }
         }

         JsonObject var18 = loadJson(
               cacheDir,
               "blocks.json",
               "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/blocks.json",
               cacheExpiryHours,
               forceUpdate
            )
            .getAsJsonObject();

         for (Entry var23 : var18.entrySet()) {
            if (((JsonElement)var23.getValue()).isJsonObject()) {
               JsonElement var25 = ((JsonElement)var23.getValue()).getAsJsonObject().get("textures");
               if (var25 != null && var25.isJsonPrimitive()) {
                  var2.put((String)var23.getKey(), var25.getAsString());
               }
            }
         }
      } catch (Exception var12) {
         SendMessage.warnMessage("Failed to load vanilla item textures (" + var12.getMessage() + "), icons for bedrock items in vanilla forms will not load!");
      }

      ConcurrentHashMap var13 = new ConcurrentHashMap();

      for (Entry var19 : loadJson(
            cacheDir, "geyser-items.json", "https://raw.githubusercontent.com/GeyserMC/mappings/master/items.json", cacheExpiryHours, forceUpdate
         )
         .getAsJsonObject()
         .entrySet()) {
         JsonObject var22 = ((JsonElement)var19.getValue()).getAsJsonObject();
         XMaterial var24 = XMaterial.matchXMaterial(getValue((String)var19.getKey())).orElse(null);
         if (var24 != null) {
            String var26 = getValue(var22.get("bedrock_identifier").getAsString());
            int var27 = var22.has("bedrock_data") ? var22.get("bedrock_data").getAsInt() : 0;

            try {
               String var10 = getArrayTexturePath(var0, var1, var2, var26, var27);
               if (var10 != null) {
                  var13.put(var24, var10);
               }
            } catch (Exception var11) {
               SendMessage.warnMessage("Failed to resolve vanilla form texture for '" + var26 + "' (" + var11.getMessage() + "), it will show no icon");
            }
         }
      }

      ITEM_TEXTURE_PATHS = var13;
      SendMessage.infoMessage("Loaded " + var13.size() + " texture paths for form icons");
   }

   private static List<String> getTexturePaths(JsonElement var0) {
      if (!var0.isJsonArray()) {
         String var5 = getTexturePath(var0);
         return var5 == null ? Collections.emptyList() : Collections.singletonList(var5);
      } else {
         ArrayList var1 = new ArrayList();

         for (JsonElement var3 : var0.getAsJsonArray()) {
            String var4 = getTexturePath(var3);
            if (var4 != null) {
               var1.add(var4);
            }
         }

         return var1;
      }
   }

   private static String getTexturePath(JsonElement var0) {
      if (var0.isJsonPrimitive()) {
         return var0.getAsString();
      } else {
         return var0.isJsonObject() && var0.getAsJsonObject().has("path") ? var0.getAsJsonObject().get("path").getAsString() : null;
      }
   }

   private static String getArrayTexturePath(Map<String, JsonElement> var0, Map<String, String> var1, Map<String, String> var2, String var3, int var4) {
      JsonElement var5 = (JsonElement)var0.get(var3);
      if (var5 != null) {
         return resolveVariant(var5, var4);
      } else {
         String var6 = (String)var1.get(var3);
         if (var6 != null) {
            return var6;
         } else {
            int var7 = var3.indexOf(95);
            if (var7 > 0 && var3.indexOf(95, var7 + 1) < 0) {
               String var8 = var3.substring(var7 + 1) + "_" + var3.substring(0, var7);
               JsonElement var9 = (JsonElement)var0.get(var8);
               if (var9 != null) {
                  return resolveVariant(var9, var4);
               }

               String var10 = (String)var1.get(var8);
               if (var10 != null) {
                  return var10;
               }
            }

            String var11 = (String)var2.get(var3);
            JsonElement var12 = var11 == null ? null : (JsonElement)var0.get(var11);
            return var12 == null ? null : resolveVariant(var12, var4);
         }
      }
   }

   private static String resolveVariant(JsonElement var0, int var1) {
      if (!var0.isJsonArray()) {
         return getTexturePath(var0);
      } else {
         JsonArray var2 = var0.getAsJsonArray();
         if (var2.size() == 0) {
            return null;
         } else {
            int var3 = Math.min(Math.max(var1, 0), var2.size() - 1);
            return getTexturePath(var2.get(var3));
         }
      }
   }

   private static JsonElement loadItemStatesFromGeyser() {
      try {
         File var0 = new File(GeyserApi.api().getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
         ZipFile var1 = new ZipFile(var0);

         Object var14;
         label73: {
            JsonElement var15;
            try {
               ZipEntry var2 = null;
               int[] var3 = null;
               Enumeration var4 = var1.entries();

               while (var4.hasMoreElements()) {
                  ZipEntry var5 = (ZipEntry)var4.nextElement();
                  String var6 = var5.getName();
                  if (var6.startsWith("bedrock/runtime_item_states.") && var6.endsWith(".json")) {
                     int[] var7 = normalize(parseVersion(var6.substring("bedrock/runtime_item_states.".length(), var6.length() - ".json".length())));
                     if (var7 != null && (var3 == null || compareVersion(var7, var3) > 0)) {
                        var2 = var5;
                        var3 = var7;
                     }
                  }
               }

               if (var2 == null) {
                  var14 = null;
                  break label73;
               }

               BufferedReader var13 = new BufferedReader(new InputStreamReader(var1.getInputStream(var2), StandardCharsets.UTF_8));

               try {
                  SendMessage.infoMessage("Reading bedrock item ids from the installed Geyser instance (" + var2.getName() + ")");
                  var15 = new JsonParser().parse(var13);
               } catch (Throwable var10) {
                  try {
                     var13.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }

                  throw var10;
               }

               var13.close();
            } catch (Throwable var11) {
               try {
                  var1.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }

               throw var11;
            }

            var1.close();
            return var15;
         }

         var1.close();
         return (JsonElement)var14;
      } catch (Throwable var12) {
         SendMessage.warnMessage("Could not read item ids from the local Geyser instance (" + var12.getMessage() + "), falling back to a downloaded copy");
         return null;
      }
   }

   private static String fetchLatestPaletteVersion() throws IOException {
      String var0 = null;
      int[] var1 = null;

      for (JsonElement var3 : new JsonParser()
         .parse(download("https://api.github.com/repos/GeyserMC/Geyser/contents/core/src/main/resources/bedrock?ref=master"))
         .getAsJsonArray()) {
         String var4 = var3.getAsJsonObject().get("name").getAsString();
         if (var4.startsWith("runtime_item_states.") && var4.endsWith(".json")) {
            String var5 = var4.substring("runtime_item_states.".length(), var4.length() - ".json".length());
            int[] var6 = normalize(parseVersion(var5));
            if (var6 != null && (var1 == null || compareVersion(var6, var1) > 0)) {
               var0 = var5;
               var1 = var6;
            }
         }
      }

      return var0;
   }

   private static JsonElement downloadItemStates(String var0) throws IOException {
      return loadJson(
         cacheDir,
         "runtime_item_states." + var0 + ".json",
         "https://raw.githubusercontent.com/GeyserMC/Geyser/master/core/src/main/resources/bedrock/runtime_item_states.%version%.json"
            .replace("%version%", var0),
         cacheExpiryHours,
         forceUpdate
      );
   }

   private static int[] parseVersion(String var0) {
      String[] var1 = var0.split("[._]");
      int[] var2 = new int[var1.length];

      for (int var3 = 0; var3 < var1.length; var3++) {
         try {
            var2[var3] = Integer.parseInt(var1[var3]);
         } catch (NumberFormatException var5) {
            return null;
         }
      }

      return var2;
   }

   private static int[] normalize(int[] var0) {
      return var0 != null && var0.length > 1 && var0[0] == 1 ? Arrays.copyOfRange(var0, 1, var0.length) : var0;
   }

   private static int compareVersion(int[] var0, int[] var1) {
      for (int var2 = 0; var2 < Math.max(var0.length, var1.length); var2++) {
         int var3 = Integer.compare(var2 < var0.length ? var0[var2] : 0, var2 < var1.length ? var1[var2] : 0);
         if (var3 != 0) {
            return var3;
         }
      }

      return 0;
   }

   private static JsonElement loadJson(File var0, String var1, String var2, long var3, boolean var5) throws IOException {
      File var6 = new File(var0, var1);
      if (!var5 && var3 >= 0L && var6.isFile() && System.currentTimeMillis() - var6.lastModified() < TimeUnit.HOURS.toMillis(var3)) {
         try {
            BufferedReader var14 = Files.newBufferedReader(var6.toPath(), StandardCharsets.UTF_8);

            JsonElement var15;
            try {
               var15 = new JsonParser().parse(var14);
            } catch (Throwable var12) {
               if (var14 != null) {
                  try {
                     var14.close();
                  } catch (Throwable var10) {
                     var12.addSuppressed(var10);
                  }
               }

               throw var12;
            }

            if (var14 != null) {
               var14.close();
            }

            return var15;
         } catch (Exception var13) {
            SendMessage.warnMessage("Failed to read cached '" + var1 + "' (" + var13.getMessage() + "), retrying...");
         }
      }

      String var7 = download(var2);
      JsonElement var8 = new JsonParser().parse(var7);
      if (var3 >= 0L) {
         try {
            if (var0.isDirectory() || var0.mkdirs()) {
               Files.write(var6.toPath(), var7.getBytes(StandardCharsets.UTF_8));
            }
         } catch (IOException var11) {
            SendMessage.warnMessage("Failed to cache '" + var1 + "' (" + var11.getMessage() + ")");
         }
      }

      return var8;
   }

   private static String download(String var0) throws MalformedURLException, IOException {
      HttpURLConnection var1 = (HttpURLConnection)new URL(var0).openConnection();
      var1.setConnectTimeout(10000);
      var1.setReadTimeout(15000);
      var1.setRequestProperty("User-Agent", "OnyxShop");
      int var2 = var1.getResponseCode();
      if (var2 != 200) {
         throw new IOException("HTTP " + var2 + " from " + var0 + ", make sure the server has a valid ethernet connection");
      } else {
         BufferedReader var3 = new BufferedReader(new InputStreamReader(var1.getInputStream(), StandardCharsets.UTF_8));

         String var7;
         try {
            StringBuilder var4 = new StringBuilder();
            char[] var5 = new char[8192];

            int var6;
            while ((var6 = var3.read(var5)) != -1) {
               var4.append(var5, 0, var6);
            }

            var7 = var4.toString();
         } catch (Throwable var9) {
            try {
               var3.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         var3.close();
         return var7;
      }
   }

   private static String getValue(String var0) {
      int var1 = var0.indexOf(58);
      return (var1 >= 0 ? var0.substring(var1 + 1) : var0).toLowerCase(Locale.ROOT);
   }

   public static String getIcon(ItemStack var0, Player var1) {
      boolean var2 = plugin.versionHandler.hasData(var0, NBTMaps.ENCHANTMENTS.getTag());
      if (var0.getType() == Material.PLAYER_HEAD
         || plugin.versionHandler.hasData(var0, NBTMaps.CUSTOM_MODEL_DATA.getTag())
         || ServerInfo.supportsComponents() && plugin.versionHandler.hasData(var0, NBTMaps.ITEM_MODEL.getTag())) {
         String var3 = GeyserCustomIcons.getIcon(var0, var0.getType().getKey().toString(), var1, var2);
         return var3 == null ? getIcon(var0, var2) : var3;
      } else {
         return getIcon(var0, var2);
      }
   }

   public static String getTexturePath(ItemStack var0) {
      return ITEM_TEXTURE_PATHS.get(XMaterial.matchXMaterial(var0));
   }

   private static String getIcon(ItemStack var0, boolean var1) {
      Integer var2 = ITEM_IDS.get(XMaterial.matchXMaterial(var0));
      return var2 == null ? null : String.valueOf(var1 ? var2 + 32768 : var2);
   }
}
