package com.onyxshop.methodes;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.NoSuchFileException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Map.Entry;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;

public class UpdateHandler {
   public static String s = "";
   public static String i = "";
   public static final long token = new SecureRandom().nextLong();

   private static String getEndpoint() {
      String text = ConfigManager.getConfig().getString("cloud-features.endpoint", "");
      return text.isEmpty() ? s : text;
   }

   public static String getData() {
      return "i/?user_id="
         + ConfigManager.getConfig().getString("cloud-features.user-id", "0")
         + "&nonce="
         + ConfigManager.getConfig().getString("cloud-features.nonce", "0")
         + "&key="
         + token
         + "&uuid=";
   }

   public UpdateHandler() {
   }

   public static void checkForUpdate(Player player) {
      if (getEndpoint().isEmpty()) {
         SendMessage.errorMessage(player, Lang.CLOUD_DISABLED.get());
         return;
      }

      OnyxShop.getInstance()
         .runKillableTask(
            () -> {
               SendMessage.chatToPlayer(player, ChatColor.translateAlternateColorCodes('&', "&aChecking for updates..."));

               try {
                  String post = OnyxShop.getInstance()
                     .getPost(getEndpoint() + ":2096/val/update/" + getData() + i + "&version=" + OnyxShop.getInstance().getDescription().getVersion());
                  if (post != null) {
                     Map map = (Map)new Gson().fromJson(post, Map.class);
                     if (getVersion(OnyxShop.getInstance().getDescription().getVersion()) < getVersion((String)map.get("version"))) {
                        SendMessage.chatToPlayer(
                           player,
                           Lang.UPDATE_DETAILS
                              .get()
                              .replace("%version%", (String)map.get("version"))
                              .replace("%title%", (String)map.get("title"))
                              .replace(
                                 "%timestamp%",
                                 new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Double.valueOf(String.valueOf(map.get("time"))).longValue() * 1000L)
                              )
                              .replace("%changelog%", (String)map.get("description"))
                        );
                        TextComponent textComponent = new TextComponent(TextComponent.fromLegacyText(Lang.DISCORD_VALIDATION_REQUIRED.get().getLegacy()));
                        textComponent.setClickEvent(new ClickEvent(Action.OPEN_URL, getEndpoint() + ":2096/val/authorize/?token=" + (String)map.get("token")));
                        SendMessage.chatToPlayer(player, textComponent);
                        checkVerification(player, (String)map.get("token"), (String)map.get("version"), 6000L);
                     } else {
                        SendMessage.chatToPlayer(player, Lang.NO_NEW_VERSION_FOUND.get());
                     }
                  } else {
                     SendMessage.chatToPlayer(player, Lang.COULD_NOT_CHECK_FOR_UPDATES.get());
                  }
               } catch (JsonSyntaxException exception) {
                  SendMessage.chatToPlayer(player, Lang.UNABLE_TO_CHECK_FOR_UPDATES.get());
                  exception.printStackTrace();
               }
            }
         );
   }

   private static double getVersion(String text) {
      String text2 = text.replace(".", "");
      return text2.matches(".*[a-zA-Z].*") ? Double.parseDouble(text2.replace("b", ".").replace("-", "").replace("a", ".")) : Double.parseDouble(text2);
   }

   private static void checkVerification(Player player, String playerName, String text, long value) {
      long value2;
      long value3 = value2 = value - 100L;
      OnyxShop.getInstance().runKillableTaskLater(() -> {
         try {
            if (((HttpURLConnection)new URL(getEndpoint() + ":2096/val/check/" + getData() + i + "&token=" + playerName).openConnection()).getResponseCode() == 202) {
               SendMessage.chatToPlayer(player, Lang.VALIDATION_SUCCESSFUL.get());
               downloadLatest(player, playerName, text);
            } else if (value3 != 0L) {
               checkVerification(player, playerName, text, value3);
            } else {
               SendMessage.chatToPlayer(player, Lang.VALIDATION_EXPIRED.get());
            }
         } catch (IOException exception) {
            exception.printStackTrace();
         }
      }, 100L);
   }

   private static void downloadLatest(Player player, String playerName, String text) {
      String shopId = OnyxShop.getInstance().getDataFolder().getParent()
         + File.separator
         + Bukkit.getServer().getUpdateFolder()
         + File.separator
         + "OnyxShop-"
         + text
         + ".jar";
      File file = new File(shopId).getParentFile();
      if (!file.exists()) {
         file.mkdirs();
      }

      if (downloadFile(playerName, new File(shopId))) {
         SendMessage.chatToPlayer(player, Lang.UPDATE_DOWNLOAD_SUCCESS_RESTART.get());
      } else {
         SendMessage.chatToPlayer(player, Lang.UPDATE_DOWNLOAD_FAILED.get());
      }
   }

   private static boolean reloadPlugin(String text) {
      try {
         UpdateHandler.Reloader.unload(OnyxShop.getInstance());
         File file = new File(text);
         if (!file.exists()) {
            throw new NoSuchFileException("Failed to load downloaded file '" + text + "'");
         } else {
            UpdateHandler.Reloader.load(file);
            return true;
         }
      } catch (Exception exception) {
         exception.printStackTrace();
         return false;
      }
   }

   private static boolean downloadFile(String text, File file) {
      String text2 = ConfigManager.getConfig().getString("update-checker.endpoint", "");
      String text3 = ConfigManager.getConfig().getString("update-checker.download-token", "");
      if (text2.isEmpty() || text3.isEmpty()) {
         SendMessage.errorMessage(Lang.UPDATE_DISABLED.get());
         return false;
      }

      try {
         URL url = new URL(text2 + "?token=" + text3 + "&version=" + text);
         BufferedInputStream bufferedInputStream = new BufferedInputStream(url.openStream());
         FileOutputStream fileOutputStream = new FileOutputStream(file);
         byte[] value = new byte[1024];

         int value2;
         while ((value2 = bufferedInputStream.read(value, 0, 1024)) != -1) {
            fileOutputStream.write(value, 0, value2);
         }

         fileOutputStream.close();
         bufferedInputStream.close();
         return file.exists();
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to download update");
         exception.printStackTrace();
         return false;
      }
   }

   private static class Reloader {
      private Reloader() {
      }

      public static void unload(Plugin plugin) throws NoSuchFieldException , IllegalAccessException, IOException{
         Bukkit.getPluginManager().disablePlugin(plugin);

         try {
            Field field = Bukkit.getPluginManager().getClass().getDeclaredField("listeners");
            field.setAccessible(true);
            Map<Object, java.util.List<org.bukkit.plugin.RegisteredListener>> map = (Map<Object, java.util.List<org.bukkit.plugin.RegisteredListener>>)field.get(
               Bukkit.getPluginManager()
            );
            map.values().forEach(entry -> entry.removeIf(element -> element.getPlugin() == plugin));
         } catch (Exception exception) {
         }

         Field field = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
         field.setAccessible(true);
         SimpleCommandMap simpleCommandMap = (SimpleCommandMap)field.get(Bukkit.getPluginManager());
         Field field2 = SimpleCommandMap.class.getDeclaredField("knownCommands");
         field2.setAccessible(true);
         Map<String, org.bukkit.command.Command> map = (Map<String, org.bukkit.command.Command>)field2.get(simpleCommandMap);

         for (Entry<String, org.bukkit.command.Command> entry : map.entrySet()) {
            if (entry.getValue() instanceof PluginCommand) {
               PluginCommand pluginCommand = (PluginCommand)entry.getValue();
               if (pluginCommand.getPlugin() == plugin) {
                  pluginCommand.unregister(simpleCommandMap);
               }
            }
         }

         ClassLoader classLoader = plugin.getClass().getClassLoader();
         if (classLoader instanceof URLClassLoader) {
            Field field3 = classLoader.getClass().getDeclaredField("plugin");
            field3.setAccessible(true);
            field3.set(classLoader, null);
            Field field4 = classLoader.getClass().getDeclaredField("pluginInit");
            field4.setAccessible(true);
            field4.set(classLoader, null);
            ((URLClassLoader)classLoader).close();
         }

         System.gc();
      }

      public static void load(File file) throws InvalidPluginException, InvalidDescriptionException {
         Plugin plugin = Bukkit.getPluginManager().loadPlugin(file);
         plugin.onLoad();
         Bukkit.getPluginManager().enablePlugin(plugin);
      }
   }
}
