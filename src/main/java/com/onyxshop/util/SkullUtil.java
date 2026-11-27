package com.onyxshop.util;

import com.google.common.collect.LinkedHashMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.files.SkullCache;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.exceptions.ItemLoadException;
import net.minecraft.world.item.component.ResolvableProfile;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SkullUtil {
   private static final String VALUE_PROPERTY = "{\"textures\":{\"SKIN\":";
   private static OnyxShop plugin;
   private static SkullCache SKULL_CACHE;
   private static Field VALUE;
   private static Field MODERN_PROFILE = null;
   private static Method PROFILE_SETTER = null;
   private static Field PROFILE;
   private static Field VAL;
   private static Field SIG;
   private static Field propertyMap;
   private static Method createResolved;
   private static boolean rateLimit = false;
   private static final Function<GameProfile, PropertyMap> propertiesMethod = getPropertiesMethod();
   private static final Function<GameProfile, String> nameMethod = getNameMethod();
   private static Class<?> profileClass;

   public static PropertyMap getProperties(GameProfile gameProfile) {
      return propertiesMethod.apply(gameProfile);
   }

   public static String getName(GameProfile gameProfile) {
      return nameMethod.apply(gameProfile);
   }

   public SkullUtil(OnyxShop plugin2) {
      SKULL_CACHE = new SkullCache(plugin2);
      plugin = plugin2;
   }

   public void saveCache() {
      SKULL_CACHE.write();
   }

   public static ItemStack getHDBHead(String itemId) throws ItemLoadException {
      if (plugin.getItemManager().getHdbHook() == null) {
         throw new ItemLoadException(Lang.CANNOT_CREATE_ITEM_FROM_PLUGIN.get());
      } else {
         return plugin.getItemManager().getHdbHook().getItem(itemId);
      }
   }

   private static String getPost(String text) throws IOException {
      URL url = new URL(text);
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
      StringBuilder builder = new StringBuilder();

      for (String text2 : bufferedReader.lines().collect(Collectors.toList())) {
         builder.append(text2);
      }

      bufferedReader.close();
      return builder.toString().replaceAll(" ", "");
   }

   private static void cache(Consumer<String> task, ItemStack itemStack, String itemId) {
      if (!itemId.equals("CS-CoreLib")) {
         try {
            String post = getPost("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + itemId);
            String text = (String)((JSONObject)new JSONParser().parse(post)).get("id");
            String post2 = getPost("https://sessionserver.mojang.com/session/minecraft/profile/" + text + "?unsigned=false");
            JSONObject jsonObject = (JSONObject)((JSONArray)((JSONObject)new JSONParser().parse(post2)).get("properties")).get(0);
            task.accept(getTextureURL(JsonUtil.getString(jsonObject, "value")));
         } catch (FileNotFoundException exception) {
            task.accept(" ");
         } catch (SocketException exception) {
            SendMessage.logDebugMessage(
               "Socket exception occurred while trying to retrieve skull texture for player '" + itemId + "', trying again in 5 seconds..."
            );
            plugin.runTaskLaterAsync(() -> cache(task, itemStack, itemId), 100L);
         } catch (UnknownHostException exception) {
         } catch (IOException exception) {
            if (exception.getMessage().contains("429")) {
               if (!rateLimit) {
                  SendMessage.logDebugMessage(
                     "Mojang started to rate limit our connections while trying to retrieve a skull texture, trying again in 60 seconds..."
                  );
                  rateLimit = true;
               }

               plugin.runTaskLaterAsync(() -> {
                  cache(task, itemStack, itemId);
                  rateLimit = false;
               }, 1200L);
            } else {
               SendMessage.warnMessage("Failed to retrieve skull texture for player '" + itemId + "'");
               exception.printStackTrace();
            }
         } catch (Exception exception) {
            SendMessage.warnMessage("Failed to retrieve skull texture for player '" + itemId + "'");
            exception.printStackTrace();
         }
      }
   }

   public static String getPlayerSkullTexture(Object value) {
      try {
         String playerName = new String(Base64.getDecoder().decode((String)VALUE.get(value)));
         JsonObject jsonObject = ((JsonObject)new Gson().fromJson(playerName, JsonObject.class)).get("textures").getAsJsonObject();
         jsonObject.get("SKIN").getAsJsonObject().remove("metadata");
         return Base64.getEncoder().encodeToString(("{\"textures\":" + jsonObject + "}").getBytes());
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to get skull texture from item");
         exception.printStackTrace();
         return null;
      }
   }

   private static String getSkullProfile(SkullMeta skullMeta) {
      try {
         return getName(getProfile(skullMeta));
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to get skull texture from item");
         exception.printStackTrace();
         return null;
      }
   }

   public static String getSkullTexture(SkullMeta skullMeta) {
      try {
         return getSkullTexture(getProfile(skullMeta));
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to get skull texture from item");
         exception.printStackTrace();
         return null;
      }
   }

   private static GameProfile getProfile(SkullMeta skullMeta) throws IllegalAccessException {
      Object value = PROFILE.get(skullMeta);
      return (GameProfile)(profileClass == GameProfile.class ? value : MODERN_PROFILE.get(value));
   }

   private static String getSkullTexture(GameProfile gameProfile) {
      if (gameProfile != null && !getProperties(gameProfile).get("textures").isEmpty()) {
         for (Property property : getProperties(gameProfile).get("textures")) {
            String value = getValue(property);
            if (!value.isEmpty()) {
               return value;
            }
         }
      }

      return null;
   }

   public static String getTextureURL(String text) {
      String text2 = new String(Base64.getDecoder().decode(text));

      try {
         JSONObject jsonObject = (JSONObject)new JSONParser().parse(text2);
         String text3 = ((JSONObject)jsonObject.get("textures")).get("SKIN").toString().replace("\\", "").replace("https://", "http://");
         return encodeTexture(text3);
      } catch (ParseException exception) {
         return "";
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to get skull texture from item: " + text2);
         exception.printStackTrace();
         return null;
      }
   }

   private static String encodeTexture(String text) {
      return Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":" + text + "}}").getBytes());
   }

   public static boolean hasSkullOwner(SkullMeta skullMeta) {
      try {
         GameProfile gameProfile = getProfile(skullMeta);
         if (gameProfile != null && !getProperties(gameProfile).get("textures").isEmpty()) {
            for (Property property : getProperties(gameProfile).get("textures")) {
               String signature = getSignature(property);
               if (signature != null && !signature.isEmpty()) {
                  return true;
               }
            }
         }
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to get skull owner from item " + skullMeta.toString());
         exception.printStackTrace();
      }

      return false;
   }

   public static boolean isSimilarSkull(ItemStack itemStack, ItemStack itemStack2) {
      return getSkullTexture((SkullMeta)itemStack.getItemMeta()).equals(getSkullTexture((SkullMeta)itemStack2.getItemMeta()));
   }

   public static boolean isSimilarTexture(GameProfile gameProfile, GameProfile gameProfile2) {
      try {
         return getSkullTexture(gameProfile).equals(getSkullTexture(gameProfile2));
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to match skull texture from item");
         exception.printStackTrace();
         return false;
      }
   }

   public static void setSkullTextureFromHash(SkullMeta skullMeta, String text) {
      GameProfile gameProfile = createNewProfile(UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8)), skullMeta.hasOwner() ? getSkullProfile(skullMeta) : "");
      getProperties(gameProfile).put("textures", new Property("textures", text));
      applySkullProfile(gameProfile, skullMeta);
   }

   public static void setSkullTexture(ItemStack itemStack, SkullMeta skullMeta, String itemId, boolean flag) {
      String skull = SKULL_CACHE.getSkull(itemId);
      if (skull == null) {
         Consumer<String> task = element -> {
            SKULL_CACHE.cacheSkull(itemId, element);
            plugin.runTaskLater(() -> {
               SkullMeta skullMeta2 = (SkullMeta)itemStack.getItemMeta();
               setSkullTexture(skullMeta2, itemId, element);
               itemStack.setItemMeta(skullMeta2);
            }, 1L);
         };
         if (flag) {
            plugin.runTaskLaterAsync(() -> cache(task, itemStack, itemId), 1L);
         } else {
            cache(task, itemStack, itemId);
         }
      } else if (skullMeta == null) {
         SkullMeta skullMeta2 = (SkullMeta)itemStack.getItemMeta();
         setSkullTexture(skullMeta2, itemId, skull);
         itemStack.setItemMeta(skullMeta2);
      } else {
         setSkullTexture(skullMeta, itemId, skull);
      }
   }

   public static void setSkullTexture(Consumer<Object> task, ItemStack itemStack, SkullMeta skullMeta, String itemId, String text) {
      String text2 = text == null ? SKULL_CACHE.getSkull(itemId) : text;
      if (text2 == null) {
         plugin.runTaskLaterAsync(() -> cache(element -> {
            SKULL_CACHE.cacheSkull(itemId, element);
            plugin.runTask(() -> {
               try {
                  task.accept(getProfile(itemId, element));
               } catch (Exception exception) {
                  SendMessage.warnMessage("Failed to retrieve skull texture for player '" + itemId + "'");
                  exception.printStackTrace();
               }
            });
         }, itemStack, itemId), 1L);
      } else if (skullMeta == null) {
         SkullMeta skullMeta2 = (SkullMeta)itemStack.getItemMeta();
         setSkullTexture(skullMeta2, itemId, text2);
         itemStack.setItemMeta(skullMeta2);
      } else {
         setSkullTexture(skullMeta, itemId, text2);
      }
   }

   private static void setSkullTexture(SkullMeta skullMeta, String text, String text2) {
      applySkullProfile(getProfile(text, text2), skullMeta);
   }

   private static Object getProfile(String text, String text2) {
      GameProfile gameProfile = createNewProfile(UUID.nameUUIDFromBytes(text2.getBytes(StandardCharsets.UTF_8)), text);
      getProperties(gameProfile).put("textures", new Property("textures", text2));
      return gameProfile;
   }

   public static void applySkullProfile(Object value, SkullMeta skullMeta) {
      try {
         if (PROFILE_SETTER != null) {
            PROFILE_SETTER.invoke(skullMeta, profileClass == GameProfile.class ? value : getModernProfile((GameProfile)value));
         } else {
            PROFILE.set(skullMeta, value);
         }
      } catch (IllegalAccessException | IllegalArgumentException | ExceptionInInitializerError | InvocationTargetException exception) {
         SendMessage.logDebugMessage("Failed to create skull texture");
         exception.printStackTrace();
      }
   }

   public static void updateTexture(Player player) {
      String text = null;

      try {
         Method method = player.getClass().getDeclaredMethod(plugin.version > 117 ? "getPlayerProfile" : "getProfile");
         method.setAccessible(true);
         if (plugin.version > 117) {
            String playerName = ((PlayerProfile)method.invoke(player)).getTextures().getSkin().toString();
            if (playerName == null) {
               return;
            }

            text = encodeTexture("{\"url\":\"" + playerName + "\"}");
         } else {
            text = getSkullTexture((GameProfile)method.invoke(player));
            if (text == null) {
               return;
            }

            text = getTextureURL(text);
         }
      } catch (Exception exception) {
      }

      if (text != null) {
         SkullCache.PlayerTexture playerTexture = SKULL_CACHE.getTexture(player.getName());
         if (playerTexture == null) {
            SKULL_CACHE.cacheSkull(player.getName(), text);
         } else {
            try {
               if (!text.equalsIgnoreCase(playerTexture.getTexture())) {
                  SendMessage.logDebugMessage("Detected skin change of " + player.getName() + ", updating cached skin...");
                  SKULL_CACHE.cacheSkull(player.getName(), text);
               }
            } catch (ArrayIndexOutOfBoundsException exception) {
            }
         }
      }
   }

   private static String getValue(Property property) {
      try {
         return (String)VAL.get(property);
      } catch (IllegalAccessException exception) {
         SendMessage.warnMessage("A error occurred while reading skull");
         exception.printStackTrace();
         return "";
      }
   }

   private static String getSignature(Property property) {
      try {
         return (String)SIG.get(property);
      } catch (IllegalAccessException exception) {
         SendMessage.warnMessage("A error occurred while reading skull");
         exception.printStackTrace();
         return null;
      }
   }

   private static GameProfile createNewProfile(UUID uniqueId, String text) {
      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R6)) {
         try {
            PropertyMap propertyMap2 = new PropertyMap(LinkedHashMultimap.create());
            propertyMap.set(propertyMap2, LinkedHashMultimap.create());
            return new GameProfile(uniqueId, text, propertyMap2);
         } catch (IllegalAccessException exception) {
            SendMessage.errorMessage("Failed to create modern player profile properties map");
            throw new RuntimeException(exception);
         }
      } else {
         return new GameProfile(uniqueId, text);
      }
   }

   private static ResolvableProfile getModernProfile(GameProfile gameProfile) {
      try {
         return ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R6)
            ? (ResolvableProfile)createResolved.invoke(null, gameProfile)
            : ResolvableProfile.class.getDeclaredConstructor(GameProfile.class).newInstance(gameProfile);
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to create modern player profile instance");
         throw new RuntimeException(exception);
      }
   }

   private static Function<GameProfile, PropertyMap> getPropertiesMethod() {
      try {
         Method method = GameProfile.class.getMethod("getProperties");
         return element -> {
            try {
               return (PropertyMap)method.invoke(element);
            } catch (Exception exception) {
               throw new RuntimeException(exception);
            }
         };
      } catch (NoSuchMethodException exception) {
         return GameProfile::properties;
      }
   }

   private static Function<GameProfile, String> getNameMethod() {
      try {
         Method method = GameProfile.class.getMethod("getName");
         return element -> {
            try {
               return (String)method.invoke(element);
            } catch (Exception exception) {
               throw new RuntimeException(exception);
            }
         };
      } catch (NoSuchMethodException exception) {
         return GameProfile::name;
      }
   }

   static {
      SkullMeta skullMeta = (SkullMeta)new ItemStack(XMaterial.PLAYER_HEAD.parseMaterial()).getItemMeta();

      try {
         if (skullMeta.getClass().getDeclaredField("profile").getType() == GameProfile.class) {
            throw new NoSuchFieldException();
         }

         profileClass = Class.forName("net.minecraft.world.item.component.ResolvableProfile");

         for (Field field : profileClass.getDeclaredFields()) {
            if (field.getType().equals(GameProfile.class)) {
               MODERN_PROFILE = field;
               field.setAccessible(true);
               break;
            }
         }
      } catch (SecurityException | ClassNotFoundException | LinkageError | NoSuchFieldException exception) {
         profileClass = GameProfile.class;
      }

      try {
         PROFILE_SETTER = skullMeta.getClass().getDeclaredMethod("setProfile", profileClass);
         PROFILE_SETTER.setAccessible(true);
      } catch (SecurityException | NoSuchMethodException exception) {
      }

      try {
         createResolved = Class.forName("net.minecraft.world.item.component.ResolvableProfile")
            .getDeclaredMethod(ServerInfo.usesMojangMappings() ? "createResolved" : "a", GameProfile.class);
         createResolved.setAccessible(true);
      } catch (NoSuchMethodException exception) {
      } catch (ClassNotFoundException exception) {
      }

      try {
         PROFILE = skullMeta.getClass().getDeclaredField("profile");
         PROFILE.setAccessible(true);
      } catch (SecurityException | NoSuchFieldException exception) {
         exception.printStackTrace();
      }

      Property property = new Property("", "");

      try {
         VAL = property.getClass().getDeclaredField("value");
         VAL.setAccessible(true);
         SIG = property.getClass().getDeclaredField("signature");
         SIG.setAccessible(true);
      } catch (SecurityException | NoSuchFieldException exception) {
         exception.printStackTrace();
      }

      try {
         Class type = Class.forName("com.mojang.authlib.properties.PropertyMap");
         propertyMap = type.getDeclaredField("properties");
         propertyMap.setAccessible(true);
         Class type2 = Class.forName("org.geysermc.floodgate.skin.SkinDataImpl");
         VALUE = type2.getDeclaredField("value");
         VALUE.setAccessible(true);
      } catch (SecurityException | NoSuchFieldException exception) {
         exception.printStackTrace();
      } catch (ClassNotFoundException exception) {
      }
   }
}
