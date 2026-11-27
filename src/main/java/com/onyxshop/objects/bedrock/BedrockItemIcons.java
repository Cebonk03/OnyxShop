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

   public static void init(OnyxShop plugin2) {
      plugin = plugin2;
      if (ConfigManager.getConfig().getBoolean("bedrock-forms.enable", true)) {
         cacheDir = new File(plugin2.getDataFolder(), "cache");
         cacheExpiryHours = ConfigManager.getConfig().getLong("bedrock-forms.item-ids.cache", 48L);
         forceUpdate = ConfigManager.getConfig().getBoolean("bedrock-forms.item-ids.force-update", false);
         configPaletteVersion = ConfigManager.getConfig().getString("bedrock-forms.item-ids.geyser-palette-version", "").trim();
         gridForms = ConfigManager.getConfig().getBoolean("bedrock-forms.use-grid-forms", false);
         plugin2.runKillableTask(BedrockItemIcons::load);
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
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to load bedrock item icons (" + exception.getMessage() + ")");
      }
   }

   private static void disableForceUpdate() {
      OnyxShop.getInstance().runTask(() -> {
         ConfigManager.getConfig().set("bedrock-forms.item-ids.force-update", false);
         ConfigManager.saveConfig();
      });
   }

   private static void loadGridIds() throws IOException {
      JsonElement jsonElement = loadItemStatesFromGeyser();
      if (jsonElement == null) {
         String text = configPaletteVersion.isEmpty() ? fetchLatestPaletteVersion() : configPaletteVersion;
         if (text == null) {
            SendMessage.errorMessage("Could not determine the latest Geyser item palette, icons for bedrock items in grid forms will not load!");
            return;
         }

         jsonElement = downloadItemStates(text);
      }

      HashMap map = new HashMap();

      for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
         JsonObject jsonObject = jsonElement2.getAsJsonObject();
         map.put(getValue(jsonObject.get("name").getAsString()), jsonObject.get("id").getAsInt());
      }

      ConcurrentHashMap map2 = new ConcurrentHashMap();

      for (Entry entry : loadJson(
            cacheDir, "geyser-items.json", "https://raw.githubusercontent.com/GeyserMC/mappings/master/items.json", cacheExpiryHours, forceUpdate
         )
         .getAsJsonObject()
         .entrySet()) {
         JsonObject jsonObject = ((JsonElement)entry.getValue()).getAsJsonObject();
         String value = getValue(jsonObject.get("bedrock_identifier").getAsString());
         Integer value2 = (Integer)map.get(value);
         if (value2 != null) {
            XMaterial xMaterial = XMaterial.matchXMaterial(getValue((String)entry.getKey())).orElse(null);
            if (xMaterial != null) {
               int value3 = jsonObject.has("bedrock_data") ? jsonObject.get("bedrock_data").getAsInt() : 0;
               map2.put(xMaterial, value2 * 65536 + value3);
            }
         }
      }

      ITEM_IDS = map2;
      SendMessage.infoMessage("Loaded " + map2.size() + " bedrock item ids for grid form icons");
   }

   private static void loadVanillaTexturePaths() throws IOException {
      HashMap<String, JsonElement> map = new HashMap<>();
      HashMap map2 = new HashMap();
      HashMap map3 = new HashMap();

      try {
         JsonObject jsonObject = loadJson(
               cacheDir,
               "item_texture.json",
               "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/textures/item_texture.json",
               cacheExpiryHours,
               forceUpdate
            )
            .getAsJsonObject()
            .getAsJsonObject("texture_data");
         if (jsonObject != null) {
            for (Entry entry : jsonObject.entrySet()) {
               map.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsJsonObject().get("textures"));
            }
         }

         JsonObject jsonObject2 = loadJson(
               cacheDir,
               "terrain_texture.json",
               "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/textures/terrain_texture.json",
               cacheExpiryHours,
               forceUpdate
            )
            .getAsJsonObject()
            .getAsJsonObject("texture_data");
         if (jsonObject2 != null) {
            for (Entry entry : jsonObject2.entrySet()) {
               map.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsJsonObject().get("textures"));
            }
         }

         for (Entry entry : map.entrySet()) {
            for (String text : getTexturePaths((JsonElement)entry.getValue())) {
               String text2 = text.substring(text.lastIndexOf(47) + 1);
               map2.putIfAbsent(text2, text);
            }
         }

         JsonObject jsonObject3 = loadJson(
               cacheDir,
               "blocks.json",
               "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/blocks.json",
               cacheExpiryHours,
               forceUpdate
            )
            .getAsJsonObject();

         for (Entry entry : jsonObject3.entrySet()) {
            if (((JsonElement)entry.getValue()).isJsonObject()) {
               JsonElement jsonElement = ((JsonElement)entry.getValue()).getAsJsonObject().get("textures");
               if (jsonElement != null && jsonElement.isJsonPrimitive()) {
                  map3.put((String)entry.getKey(), jsonElement.getAsString());
               }
            }
         }
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to load vanilla item textures (" + exception.getMessage() + "), icons for bedrock items in vanilla forms will not load!");
      }

      ConcurrentHashMap map4 = new ConcurrentHashMap();

      for (Entry entry : loadJson(
            cacheDir, "geyser-items.json", "https://raw.githubusercontent.com/GeyserMC/mappings/master/items.json", cacheExpiryHours, forceUpdate
         )
         .getAsJsonObject()
         .entrySet()) {
         JsonObject jsonObject = ((JsonElement)entry.getValue()).getAsJsonObject();
         XMaterial xMaterial = XMaterial.matchXMaterial(getValue((String)entry.getKey())).orElse(null);
         if (xMaterial != null) {
            String value = getValue(jsonObject.get("bedrock_identifier").getAsString());
            int value2 = jsonObject.has("bedrock_data") ? jsonObject.get("bedrock_data").getAsInt() : 0;

            try {
               String arrayTexturePath = getArrayTexturePath(map, map2, map3, value, value2);
               if (arrayTexturePath != null) {
                  map4.put(xMaterial, arrayTexturePath);
               }
            } catch (Exception exception) {
               SendMessage.warnMessage("Failed to resolve vanilla form texture for '" + value + "' (" + exception.getMessage() + "), it will show no icon");
            }
         }
      }

      ITEM_TEXTURE_PATHS = map4;
      SendMessage.infoMessage("Loaded " + map4.size() + " texture paths for form icons");
   }

   private static List<String> getTexturePaths(JsonElement jsonElement) {
      if (!jsonElement.isJsonArray()) {
         String texturePath = getTexturePath(jsonElement);
         return texturePath == null ? Collections.emptyList() : Collections.singletonList(texturePath);
      } else {
         ArrayList values = new ArrayList();

         for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
            String texturePath = getTexturePath(jsonElement2);
            if (texturePath != null) {
               values.add(texturePath);
            }
         }

         return values;
      }
   }

   private static String getTexturePath(JsonElement jsonElement) {
      if (jsonElement.isJsonPrimitive()) {
         return jsonElement.getAsString();
      } else {
         return jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("path") ? jsonElement.getAsJsonObject().get("path").getAsString() : null;
      }
   }

   private static String getArrayTexturePath(Map<String, JsonElement> map, Map<String, String> paths, Map<String, String> map2, String text, int value) {
      JsonElement jsonElement = (JsonElement)map.get(text);
      if (jsonElement != null) {
         return resolveVariant(jsonElement, value);
      } else {
         String text2 = (String)paths.get(text);
         if (text2 != null) {
            return text2;
         } else {
            int index = text.indexOf(95);
            if (index > 0 && text.indexOf(95, index + 1) < 0) {
               String text3 = text.substring(index + 1) + "_" + text.substring(0, index);
               JsonElement jsonElement2 = (JsonElement)map.get(text3);
               if (jsonElement2 != null) {
                  return resolveVariant(jsonElement2, value);
               }

               String text4 = (String)paths.get(text3);
               if (text4 != null) {
                  return text4;
               }
            }

            String text3 = (String)map2.get(text);
            JsonElement jsonElement2 = text3 == null ? null : (JsonElement)map.get(text3);
            return jsonElement2 == null ? null : resolveVariant(jsonElement2, value);
         }
      }
   }

   private static String resolveVariant(JsonElement jsonElement, int value) {
      if (!jsonElement.isJsonArray()) {
         return getTexturePath(jsonElement);
      } else {
         JsonArray jsonArray = jsonElement.getAsJsonArray();
         if (jsonArray.size() == 0) {
            return null;
         } else {
            int size = Math.min(Math.max(value, 0), jsonArray.size() - 1);
            return getTexturePath(jsonArray.get(size));
         }
      }
   }

   private static JsonElement loadItemStatesFromGeyser() {
      try {
         File file = new File(GeyserApi.api().getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
         ZipFile zipFile = new ZipFile(file);

         Object value;
         label73: {
            JsonElement jsonElement;
            try {
               ZipEntry zipEntry = null;
               int[] itemIds = null;
               Enumeration enumeration = zipFile.entries();

               while (enumeration.hasMoreElements()) {
                  ZipEntry zipEntry2 = (ZipEntry)enumeration.nextElement();
                  String name = zipEntry2.getName();
                  if (name.startsWith("bedrock/runtime_item_states.") && name.endsWith(".json")) {
                     int[] values = normalize(parseVersion(name.substring("bedrock/runtime_item_states.".length(), name.length() - ".json".length())));
                     if (values != null && (itemIds == null || compareVersion(values, itemIds) > 0)) {
                        zipEntry = zipEntry2;
                        itemIds = values;
                     }
                  }
               }

               if (zipEntry == null) {
                  value = null;
                  break label73;
               }

               BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(zipEntry), StandardCharsets.UTF_8));

               try {
                  SendMessage.infoMessage("Reading bedrock item ids from the installed Geyser instance (" + zipEntry.getName() + ")");
                  jsonElement = new JsonParser().parse(bufferedReader);
               } catch (Throwable exception) {
                  try {
                     bufferedReader.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }

                  throw exception;
               }

               bufferedReader.close();
            } catch (Throwable exception) {
               try {
                  zipFile.close();
               } catch (Throwable exception2) {
                  exception.addSuppressed(exception2);
               }

               throw exception;
            }

            zipFile.close();
            return jsonElement;
         }

         zipFile.close();
         return (JsonElement)value;
      } catch (Throwable exception) {
         SendMessage.warnMessage("Could not read item ids from the local Geyser instance (" + exception.getMessage() + "), falling back to a downloaded copy");
         return null;
      }
   }

   private static String fetchLatestPaletteVersion() throws IOException {
      String text = null;
      int[] values = null;

      for (JsonElement jsonElement : new JsonParser()
         .parse(download("https://api.github.com/repos/GeyserMC/Geyser/contents/core/src/main/resources/bedrock?ref=master"))
         .getAsJsonArray()) {
         String asString = jsonElement.getAsJsonObject().get("name").getAsString();
         if (asString.startsWith("runtime_item_states.") && asString.endsWith(".json")) {
            String itemId = asString.substring("runtime_item_states.".length(), asString.length() - ".json".length());
            int[] values2 = normalize(parseVersion(itemId));
            if (values2 != null && (values == null || compareVersion(values2, values) > 0)) {
               text = itemId;
               values = values2;
            }
         }
      }

      return text;
   }

   private static JsonElement downloadItemStates(String itemId) throws IOException {
      return loadJson(
         cacheDir,
         "runtime_item_states." + itemId + ".json",
         "https://raw.githubusercontent.com/GeyserMC/Geyser/master/core/src/main/resources/bedrock/runtime_item_states.%version%.json"
            .replace("%version%", itemId),
         cacheExpiryHours,
         forceUpdate
      );
   }

   private static int[] parseVersion(String text) {
      String[] values = text.split("[._]");
      int[] value = new int[values.length];

      for (int index = 0; index < values.length; index++) {
         try {
            value[index] = Integer.parseInt(values[index]);
         } catch (NumberFormatException exception) {
            return null;
         }
      }

      return value;
   }

   private static int[] normalize(int[] values) {
      return values != null && values.length > 1 && values[0] == 1 ? Arrays.copyOfRange(values, 1, values.length) : values;
   }

   private static int compareVersion(int[] values, int[] values2) {
      for (int index = 0; index < Math.max(values.length, values2.length); index++) {
         int value2 = Integer.compare(index < values.length ? values[index] : 0, index < values2.length ? values2[index] : 0);
         if (value2 != 0) {
            return value2;
         }
      }

      return 0;
   }

   private static JsonElement loadJson(File file, String text, String text2, long value, boolean flag) throws IOException {
      File file2 = new File(file, text);
      if (!flag && value >= 0L && file2.isFile() && System.currentTimeMillis() - file2.lastModified() < TimeUnit.HOURS.toMillis(value)) {
         try {
            BufferedReader bufferedReader = Files.newBufferedReader(file2.toPath(), StandardCharsets.UTF_8);

            JsonElement jsonElement;
            try {
               jsonElement = new JsonParser().parse(bufferedReader);
            } catch (Throwable exception) {
               if (bufferedReader != null) {
                  try {
                     bufferedReader.close();
                  } catch (Throwable exception2) {
                     exception.addSuppressed(exception2);
                  }
               }

               throw exception;
            }

            if (bufferedReader != null) {
               bufferedReader.close();
            }

            return jsonElement;
         } catch (Exception exception) {
            SendMessage.warnMessage("Failed to read cached '" + text + "' (" + exception.getMessage() + "), retrying...");
         }
      }

      String text3 = download(text2);
      JsonElement jsonElement = new JsonParser().parse(text3);
      if (value >= 0L) {
         try {
            if (file.isDirectory() || file.mkdirs()) {
               Files.write(file2.toPath(), text3.getBytes(StandardCharsets.UTF_8));
            }
         } catch (IOException exception) {
            SendMessage.warnMessage("Failed to cache '" + text + "' (" + exception.getMessage() + ")");
         }
      }

      return jsonElement;
   }

   private static String download(String text) throws MalformedURLException, IOException {
      HttpURLConnection httpURLConnection = (HttpURLConnection)new URL(text).openConnection();
      httpURLConnection.setConnectTimeout(10000);
      httpURLConnection.setReadTimeout(15000);
      httpURLConnection.setRequestProperty("User-Agent", "OnyxShop");
      int responseCode = httpURLConnection.getResponseCode();
      if (responseCode != 200) {
         throw new IOException("HTTP " + responseCode + " from " + text + ", make sure the server has a valid ethernet connection");
      } else {
         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8));

         String text2;
         try {
            StringBuilder builder = new StringBuilder();
            char[] character = new char[8192];

            int value;
            while ((value = bufferedReader.read(character)) != -1) {
               builder.append(character, 0, value);
            }

            text2 = builder.toString();
         } catch (Throwable exception) {
            try {
               bufferedReader.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }

            throw exception;
         }

         bufferedReader.close();
         return text2;
      }
   }

   private static String getValue(String text) {
      int index = text.indexOf(58);
      return (index >= 0 ? text.substring(index + 1) : text).toLowerCase(Locale.ROOT);
   }

   public static String getIcon(ItemStack itemStack, Player player) {
      boolean data = plugin.versionHandler.hasData(itemStack, NBTMaps.ENCHANTMENTS.getTag());
      if (itemStack.getType() == Material.PLAYER_HEAD
         || plugin.versionHandler.hasData(itemStack, NBTMaps.CUSTOM_MODEL_DATA.getTag())
         || ServerInfo.supportsComponents() && plugin.versionHandler.hasData(itemStack, NBTMaps.ITEM_MODEL.getTag())) {
         String icon = GeyserCustomIcons.getIcon(itemStack, itemStack.getType().getKey().toString(), player, data);
         return icon == null ? getIcon(itemStack, data) : icon;
      } else {
         return getIcon(itemStack, data);
      }
   }

   public static String getTexturePath(ItemStack itemStack) {
      return ITEM_TEXTURE_PATHS.get(XMaterial.matchXMaterial(itemStack));
   }

   private static String getIcon(ItemStack itemStack, boolean flag) {
      Integer value = ITEM_IDS.get(XMaterial.matchXMaterial(itemStack));
      return value == null ? null : String.valueOf(flag ? value + 32768 : value);
   }
}
