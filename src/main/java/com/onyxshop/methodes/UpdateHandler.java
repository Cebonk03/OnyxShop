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
      String var0 = ConfigManager.getConfig().getString("cloud-features.endpoint", "");
      return var0.isEmpty() ? s : var0;
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

   public static void checkForUpdate(Player var0) {
      if (getEndpoint().isEmpty()) {
         SendMessage.errorMessage(var0, Lang.CLOUD_DISABLED.get());
         return;
      }

      OnyxShop.getInstance()
         .runKillableTask(
            () -> {
               SendMessage.chatToPlayer(var0, ChatColor.translateAlternateColorCodes('&', "&aChecking for updates..."));

               try {
                  String var1 = OnyxShop.getInstance()
                     .getPost(getEndpoint() + ":2096/val/update/" + getData() + i + "&version=" + OnyxShop.getInstance().getDescription().getVersion());
                  if (var1 != null) {
                     Map var2 = (Map)new Gson().fromJson(var1, Map.class);
                     if (getVersion(OnyxShop.getInstance().getDescription().getVersion()) < getVersion((String)var2.get("version"))) {
                        SendMessage.chatToPlayer(
                           var0,
                           Lang.UPDATE_DETAILS
                              .get()
                              .replace("%version%", (String)var2.get("version"))
                              .replace("%title%", (String)var2.get("title"))
                              .replace(
                                 "%timestamp%",
                                 new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Double.valueOf(String.valueOf(var2.get("time"))).longValue() * 1000L)
                              )
                              .replace("%changelog%", (String)var2.get("description"))
                        );
                        TextComponent var3 = new TextComponent(TextComponent.fromLegacyText(Lang.DISCORD_VALIDATION_REQUIRED.get().getLegacy()));
                        var3.setClickEvent(new ClickEvent(Action.OPEN_URL, getEndpoint() + ":2096/val/authorize/?token=" + (String)var2.get("token")));
                        SendMessage.chatToPlayer(var0, var3);
                        checkVerification(var0, (String)var2.get("token"), (String)var2.get("version"), 6000L);
                     } else {
                        SendMessage.chatToPlayer(var0, Lang.NO_NEW_VERSION_FOUND.get());
                     }
                  } else {
                     SendMessage.chatToPlayer(var0, Lang.COULD_NOT_CHECK_FOR_UPDATES.get());
                  }
               } catch (JsonSyntaxException var4) {
                  SendMessage.chatToPlayer(var0, Lang.UNABLE_TO_CHECK_FOR_UPDATES.get());
                  var4.printStackTrace();
               }
            }
         );
   }

   private static double getVersion(String var0) {
      String var1 = var0.replace(".", "");
      return var1.matches(".*[a-zA-Z].*") ? Double.parseDouble(var1.replace("b", ".").replace("-", "").replace("a", ".")) : Double.parseDouble(var1);
   }

   private static void checkVerification(Player var0, String var1, String var2, long var3) {
      long var7;
      long var5 = var7 = var3 - 100L;
      OnyxShop.getInstance().runKillableTaskLater(() -> {
         try {
            if (((HttpURLConnection)new URL(getEndpoint() + ":2096/val/check/" + getData() + i + "&token=" + var1).openConnection()).getResponseCode() == 202) {
               SendMessage.chatToPlayer(var0, Lang.VALIDATION_SUCCESSFUL.get());
               downloadLatest(var0, var1, var2);
            } else if (var5 != 0L) {
               checkVerification(var0, var1, var2, var5);
            } else {
               SendMessage.chatToPlayer(var0, Lang.VALIDATION_EXPIRED.get());
            }
         } catch (IOException var6) {
            var6.printStackTrace();
         }
      }, 100L);
   }

   private static void downloadLatest(Player var0, String var1, String var2) {
      String var3 = OnyxShop.getInstance().getDataFolder().getParent()
         + File.separator
         + Bukkit.getServer().getUpdateFolder()
         + File.separator
         + "OnyxShop-"
         + var2
         + ".jar";
      File var4 = new File(var3).getParentFile();
      if (!var4.exists()) {
         var4.mkdirs();
      }

      if (downloadFile(var1, new File(var3))) {
         SendMessage.chatToPlayer(var0, Lang.UPDATE_DOWNLOAD_SUCCESS_RESTART.get());
      } else {
         SendMessage.chatToPlayer(var0, Lang.UPDATE_DOWNLOAD_FAILED.get());
      }
   }

   private static boolean reloadPlugin(String var0) {
      try {
         UpdateHandler.Reloader.unload(OnyxShop.getInstance());
         File var1 = new File(var0);
         if (!var1.exists()) {
            throw new NoSuchFileException("Failed to load downloaded file '" + var0 + "'");
         } else {
            UpdateHandler.Reloader.load(var1);
            return true;
         }
      } catch (Exception var2) {
         var2.printStackTrace();
         return false;
      }
   }

   private static boolean downloadFile(String var0, File var1) {
      String var2 = ConfigManager.getConfig().getString("update-checker.endpoint", "");
      String var3 = ConfigManager.getConfig().getString("update-checker.download-token", "");
      if (var2.isEmpty() || var3.isEmpty()) {
         SendMessage.errorMessage(Lang.UPDATE_DISABLED.get());
         return false;
      }

      try {
         URL var4 = new URL(var2 + "?token=" + var3 + "&version=" + var0);
         BufferedInputStream var5 = new BufferedInputStream(var4.openStream());
         FileOutputStream var6 = new FileOutputStream(var1);
         byte[] var7 = new byte[1024];

         int var8;
         while ((var8 = var5.read(var7, 0, 1024)) != -1) {
            var6.write(var7, 0, var8);
         }

         var6.close();
         var5.close();
         return var1.exists();
      } catch (Exception var9) {
         SendMessage.warnMessage("Failed to download update");
         var9.printStackTrace();
         return false;
      }
   }

   private static class Reloader {
      private Reloader() {
      }

      public static void unload(Plugin var0) throws NoSuchFieldException , IllegalAccessException, IOException{
         Bukkit.getPluginManager().disablePlugin(var0);

         try {
            Field var1 = Bukkit.getPluginManager().getClass().getDeclaredField("listeners");
            var1.setAccessible(true);
            Map<Object, java.util.List<org.bukkit.plugin.RegisteredListener>> var2 = (Map<Object, java.util.List<org.bukkit.plugin.RegisteredListener>>)var1.get(
               Bukkit.getPluginManager()
            );
            var2.values().forEach(var1x -> var1x.removeIf(var1xx -> var1xx.getPlugin() == var0));
         } catch (Exception var8) {
         }

         Field var9 = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
         var9.setAccessible(true);
         SimpleCommandMap var3 = (SimpleCommandMap)var9.get(Bukkit.getPluginManager());
         Field var10 = SimpleCommandMap.class.getDeclaredField("knownCommands");
         var10.setAccessible(true);
         Map<String, org.bukkit.command.Command> var4 = (Map<String, org.bukkit.command.Command>)var10.get(var3);

         for (Entry<String, org.bukkit.command.Command> var6 : var4.entrySet()) {
            if (var6.getValue() instanceof PluginCommand) {
               PluginCommand var7 = (PluginCommand)var6.getValue();
               if (var7.getPlugin() == var0) {
                  var7.unregister(var3);
               }
            }
         }

         ClassLoader var11 = var0.getClass().getClassLoader();
         if (var11 instanceof URLClassLoader) {
            Field var12 = var11.getClass().getDeclaredField("plugin");
            var12.setAccessible(true);
            var12.set(var11, null);
            Field var13 = var11.getClass().getDeclaredField("pluginInit");
            var13.setAccessible(true);
            var13.set(var11, null);
            ((URLClassLoader)var11).close();
         }

         System.gc();
      }

      public static void load(File var0) throws InvalidPluginException, InvalidDescriptionException {
         Plugin var1 = Bukkit.getPluginManager().loadPlugin(var0);
         var1.onLoad();
         Bukkit.getPluginManager().enablePlugin(var1);
      }
   }
}
