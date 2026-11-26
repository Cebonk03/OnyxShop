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

   public static PropertyMap getProperties(GameProfile var0) {
      return propertiesMethod.apply(var0);
   }

   public static String getName(GameProfile var0) {
      return nameMethod.apply(var0);
   }

   public SkullUtil(OnyxShop var1) {
      SKULL_CACHE = new SkullCache(var1);
      plugin = var1;
   }

   public void saveCache() {
      SKULL_CACHE.write();
   }

   public static ItemStack getHDBHead(String var0) throws ItemLoadException {
      if (plugin.getItemManager().getHdbHook() == null) {
         throw new ItemLoadException(Lang.CANNOT_CREATE_ITEM_FROM_PLUGIN.get());
      } else {
         return plugin.getItemManager().getHdbHook().getItem(var0);
      }
   }

   private static String getPost(String var0) throws IOException {
      URL var1 = new URL(var0);
      BufferedReader var2 = new BufferedReader(new InputStreamReader(var1.openStream()));
      StringBuilder var3 = new StringBuilder();

      for (String var5 : var2.lines().collect(Collectors.toList())) {
         var3.append(var5);
      }

      var2.close();
      return var3.toString().replaceAll(" ", "");
   }

   private static void cache(Consumer<String> var0, ItemStack var1, String var2) {
      if (!var2.equals("CS-CoreLib")) {
         try {
            String var3 = getPost("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + var2);
            String var4 = (String)((JSONObject)new JSONParser().parse(var3)).get("id");
            String var5 = getPost("https://sessionserver.mojang.com/session/minecraft/profile/" + var4 + "?unsigned=false");
            JSONObject var6 = (JSONObject)((JSONArray)((JSONObject)new JSONParser().parse(var5)).get("properties")).get(0);
            var0.accept(getTextureURL(JsonUtil.getString(var6, "value")));
         } catch (FileNotFoundException var7) {
            var0.accept(" ");
         } catch (SocketException var8) {
            SendMessage.logDebugMessage(
               "Socket exception occurred while trying to retrieve skull texture for player '" + var2 + "', trying again in 5 seconds..."
            );
            plugin.runTaskLaterAsync(() -> cache(var0, var1, var2), 100L);
         } catch (UnknownHostException var9) {
         } catch (IOException var10) {
            if (var10.getMessage().contains("429")) {
               if (!rateLimit) {
                  SendMessage.logDebugMessage(
                     "Mojang started to rate limit our connections while trying to retrieve a skull texture, trying again in 60 seconds..."
                  );
                  rateLimit = true;
               }

               plugin.runTaskLaterAsync(() -> {
                  cache(var0, var1, var2);
                  rateLimit = false;
               }, 1200L);
            } else {
               SendMessage.warnMessage("Failed to retrieve skull texture for player '" + var2 + "'");
               var10.printStackTrace();
            }
         } catch (Exception var11) {
            SendMessage.warnMessage("Failed to retrieve skull texture for player '" + var2 + "'");
            var11.printStackTrace();
         }
      }
   }

   public static String getPlayerSkullTexture(Object var0) {
      try {
         String var1 = new String(Base64.getDecoder().decode((String)VALUE.get(var0)));
         JsonObject var2 = ((JsonObject)new Gson().fromJson(var1, JsonObject.class)).get("textures").getAsJsonObject();
         var2.get("SKIN").getAsJsonObject().remove("metadata");
         return Base64.getEncoder().encodeToString(("{\"textures\":" + var2 + "}").getBytes());
      } catch (Exception var3) {
         SendMessage.errorMessage("Failed to get skull texture from item");
         var3.printStackTrace();
         return null;
      }
   }

   private static String getSkullProfile(SkullMeta var0) {
      try {
         return getName(getProfile(var0));
      } catch (Exception var2) {
         SendMessage.errorMessage("Failed to get skull texture from item");
         var2.printStackTrace();
         return null;
      }
   }

   public static String getSkullTexture(SkullMeta var0) {
      try {
         return getSkullTexture(getProfile(var0));
      } catch (Exception var2) {
         SendMessage.errorMessage("Failed to get skull texture from item");
         var2.printStackTrace();
         return null;
      }
   }

   private static GameProfile getProfile(SkullMeta var0) throws IllegalAccessException {
      Object var1 = PROFILE.get(var0);
      return (GameProfile)(profileClass == GameProfile.class ? var1 : MODERN_PROFILE.get(var1));
   }

   private static String getSkullTexture(GameProfile var0) {
      if (var0 != null && !getProperties(var0).get("textures").isEmpty()) {
         for (Property var2 : getProperties(var0).get("textures")) {
            String var3 = getValue(var2);
            if (!var3.isEmpty()) {
               return var3;
            }
         }
      }

      return null;
   }

   public static String getTextureURL(String var0) {
      String var1 = new String(Base64.getDecoder().decode(var0));

      try {
         JSONObject var2 = (JSONObject)new JSONParser().parse(var1);
         String var3 = ((JSONObject)var2.get("textures")).get("SKIN").toString().replace("\\", "").replace("https://", "http://");
         return encodeTexture(var3);
      } catch (ParseException var4) {
         return "";
      } catch (Exception var5) {
         SendMessage.errorMessage("Failed to get skull texture from item: " + var1);
         var5.printStackTrace();
         return null;
      }
   }

   private static String encodeTexture(String var0) {
      return Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":" + var0 + "}}").getBytes());
   }

   public static boolean hasSkullOwner(SkullMeta var0) {
      try {
         GameProfile var1 = getProfile(var0);
         if (var1 != null && !getProperties(var1).get("textures").isEmpty()) {
            for (Property var3 : getProperties(var1).get("textures")) {
               String var4 = getSignature(var3);
               if (var4 != null && !var4.isEmpty()) {
                  return true;
               }
            }
         }
      } catch (Exception var5) {
         SendMessage.errorMessage("Failed to get skull owner from item " + var0.toString());
         var5.printStackTrace();
      }

      return false;
   }

   public static boolean isSimilarSkull(ItemStack var0, ItemStack var1) {
      return getSkullTexture((SkullMeta)var0.getItemMeta()).equals(getSkullTexture((SkullMeta)var1.getItemMeta()));
   }

   public static boolean isSimilarTexture(GameProfile var0, GameProfile var1) {
      try {
         return getSkullTexture(var0).equals(getSkullTexture(var1));
      } catch (Exception var3) {
         SendMessage.errorMessage("Failed to match skull texture from item");
         var3.printStackTrace();
         return false;
      }
   }

   public static void setSkullTextureFromHash(SkullMeta var0, String var1) {
      GameProfile var2 = createNewProfile(UUID.nameUUIDFromBytes(var1.getBytes(StandardCharsets.UTF_8)), var0.hasOwner() ? getSkullProfile(var0) : "");
      getProperties(var2).put("textures", new Property("textures", var1));
      applySkullProfile(var2, var0);
   }

   public static void setSkullTexture(ItemStack var0, SkullMeta var1, String var2, boolean var3) {
      String var4 = SKULL_CACHE.getSkull(var2);
      if (var4 == null) {
         Consumer<String> var5 = var2x -> {
            SKULL_CACHE.cacheSkull(var2, var2x);
            plugin.runTaskLater(() -> {
               SkullMeta var3x = (SkullMeta)var0.getItemMeta();
               setSkullTexture(var3x, var2, var2x);
               var0.setItemMeta(var3x);
            }, 1L);
         };
         if (var3) {
            plugin.runTaskLaterAsync(() -> cache(var5, var0, var2), 1L);
         } else {
            cache(var5, var0, var2);
         }
      } else if (var1 == null) {
         SkullMeta var6 = (SkullMeta)var0.getItemMeta();
         setSkullTexture(var6, var2, var4);
         var0.setItemMeta(var6);
      } else {
         setSkullTexture(var1, var2, var4);
      }
   }

   public static void setSkullTexture(Consumer<Object> var0, ItemStack var1, SkullMeta var2, String var3, String var4) {
      String var5 = var4 == null ? SKULL_CACHE.getSkull(var3) : var4;
      if (var5 == null) {
         plugin.runTaskLaterAsync(() -> cache(var2xx -> {
            SKULL_CACHE.cacheSkull(var3, var2xx);
            var0.accept(getProfile(var3, var2xx));
         }, var1, var3), 1L);
      } else if (var2 == null) {
         SkullMeta var6 = (SkullMeta)var1.getItemMeta();
         setSkullTexture(var6, var3, var5);
         var1.setItemMeta(var6);
      } else {
         setSkullTexture(var2, var3, var5);
      }
   }

   private static void setSkullTexture(SkullMeta var0, String var1, String var2) {
      applySkullProfile(getProfile(var1, var2), var0);
   }

   private static Object getProfile(String var0, String var1) {
      GameProfile var2 = createNewProfile(UUID.nameUUIDFromBytes(var1.getBytes(StandardCharsets.UTF_8)), var0);
      getProperties(var2).put("textures", new Property("textures", var1));
      return var2;
   }

   public static void applySkullProfile(Object var0, SkullMeta var1) {
      try {
         if (PROFILE_SETTER != null) {
            PROFILE_SETTER.invoke(var1, profileClass == GameProfile.class ? var0 : getModernProfile((GameProfile)var0));
         } else {
            PROFILE.set(var1, var0);
         }
      } catch (IllegalAccessException | IllegalArgumentException | ExceptionInInitializerError | InvocationTargetException var3) {
         SendMessage.logDebugMessage("Failed to create skull texture");
         var3.printStackTrace();
      }
   }

   public static void updateTexture(Player var0) {
      String var1 = null;

      try {
         Method var2 = var0.getClass().getDeclaredMethod(plugin.version > 117 ? "getPlayerProfile" : "getProfile");
         var2.setAccessible(true);
         if (plugin.version > 117) {
            String var3 = ((PlayerProfile)var2.invoke(var0)).getTextures().getSkin().toString();
            if (var3 == null) {
               return;
            }

            var1 = encodeTexture("{\"url\":\"" + var3 + "\"}");
         } else {
            var1 = getSkullTexture((GameProfile)var2.invoke(var0));
            if (var1 == null) {
               return;
            }

            var1 = getTextureURL(var1);
         }
      } catch (Exception var5) {
      }

      if (var1 != null) {
         SkullCache.PlayerTexture var6 = SKULL_CACHE.getTexture(var0.getName());
         if (var6 == null) {
            SKULL_CACHE.cacheSkull(var0.getName(), var1);
         } else {
            try {
               if (!var1.equalsIgnoreCase(var6.getTexture())) {
                  SendMessage.logDebugMessage("Detected skin change of " + var0.getName() + ", updating cached skin...");
                  SKULL_CACHE.cacheSkull(var0.getName(), var1);
               }
            } catch (ArrayIndexOutOfBoundsException var4) {
            }
         }
      }
   }

   private static String getValue(Property var0) {
      try {
         return (String)VAL.get(var0);
      } catch (IllegalAccessException var2) {
         SendMessage.warnMessage("A error occurred while reading skull");
         var2.printStackTrace();
         return "";
      }
   }

   private static String getSignature(Property var0) {
      try {
         return (String)SIG.get(var0);
      } catch (IllegalAccessException var2) {
         SendMessage.warnMessage("A error occurred while reading skull");
         var2.printStackTrace();
         return null;
      }
   }

   private static GameProfile createNewProfile(UUID var0, String var1) {
      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R6)) {
         try {
            PropertyMap var2 = new PropertyMap(LinkedHashMultimap.create());
            propertyMap.set(var2, LinkedHashMultimap.create());
            return new GameProfile(var0, var1, var2);
         } catch (IllegalAccessException var3) {
            SendMessage.errorMessage("Failed to create modern player profile properties map");
            throw new RuntimeException(var3);
         }
      } else {
         return new GameProfile(var0, var1);
      }
   }

   private static ResolvableProfile getModernProfile(GameProfile var0) {
      try {
         return ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R6)
            ? (ResolvableProfile)createResolved.invoke(null, var0)
            : ResolvableProfile.class.getDeclaredConstructor(GameProfile.class).newInstance(var0);
      } catch (Exception var2) {
         SendMessage.errorMessage("Failed to create modern player profile instance");
         throw new RuntimeException(var2);
      }
   }

   private static Function<GameProfile, PropertyMap> getPropertiesMethod() {
      try {
         Method var0 = GameProfile.class.getMethod("getProperties");
         return var1x -> {
            try {
               return (PropertyMap)var0.invoke(var1x);
            } catch (Exception var3) {
               throw new RuntimeException(var3);
            }
         };
      } catch (NoSuchMethodException var1) {
         return GameProfile::properties;
      }
   }

   private static Function<GameProfile, String> getNameMethod() {
      try {
         Method var0 = GameProfile.class.getMethod("getName");
         return var1x -> {
            try {
               return (String)var0.invoke(var1x);
            } catch (Exception var3) {
               throw new RuntimeException(var3);
            }
         };
      } catch (NoSuchMethodException var1) {
         return GameProfile::name;
      }
   }

   static {
      SkullMeta var0 = (SkullMeta)new ItemStack(XMaterial.PLAYER_HEAD.parseMaterial()).getItemMeta();

      try {
         if (var0.getClass().getDeclaredField("profile").getType() == GameProfile.class) {
            throw new NoSuchFieldException();
         }

         profileClass = Class.forName("net.minecraft.world.item.component.ResolvableProfile");

         for (Field var4 : profileClass.getDeclaredFields()) {
            if (var4.getType().equals(GameProfile.class)) {
               MODERN_PROFILE = var4;
               var4.setAccessible(true);
               break;
            }
         }
      } catch (SecurityException | ClassNotFoundException | LinkageError | NoSuchFieldException var12) {
         profileClass = GameProfile.class;
      }

      try {
         PROFILE_SETTER = var0.getClass().getDeclaredMethod("setProfile", profileClass);
         PROFILE_SETTER.setAccessible(true);
      } catch (SecurityException | NoSuchMethodException var11) {
      }

      try {
         createResolved = Class.forName("net.minecraft.world.item.component.ResolvableProfile")
            .getDeclaredMethod(ServerInfo.usesMojangMappings() ? "createResolved" : "a", GameProfile.class);
         createResolved.setAccessible(true);
      } catch (NoSuchMethodException var9) {
      } catch (ClassNotFoundException var10) {
      }

      try {
         PROFILE = var0.getClass().getDeclaredField("profile");
         PROFILE.setAccessible(true);
      } catch (SecurityException | NoSuchFieldException var8) {
         var8.printStackTrace();
      }

      Property var13 = new Property("", "");

      try {
         VAL = var13.getClass().getDeclaredField("value");
         VAL.setAccessible(true);
         SIG = var13.getClass().getDeclaredField("signature");
         SIG.setAccessible(true);
      } catch (SecurityException | NoSuchFieldException var7) {
         var7.printStackTrace();
      }

      try {
         Class var14 = Class.forName("com.mojang.authlib.properties.PropertyMap");
         propertyMap = var14.getDeclaredField("properties");
         propertyMap.setAccessible(true);
         Class var15 = Class.forName("org.geysermc.floodgate.skin.SkinDataImpl");
         VALUE = var15.getDeclaredField("value");
         VALUE.setAccessible(true);
      } catch (SecurityException | NoSuchFieldException var5) {
         var5.printStackTrace();
      } catch (ClassNotFoundException var6) {
      }
   }
}
