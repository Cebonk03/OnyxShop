package com.onyxshop.util;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;

public class ChatUtil {
   private static final Pattern rgbPattern = Pattern.compile("#[a-fA-F0-9]{6}");
   private static final Pattern startGradientPattern = Pattern.compile("<gradient:#[a-fA-F0-9]{6}>");
   private static final Pattern endGradientPattern = Pattern.compile("</gradient:#[a-fA-F0-9]{6}>");
   private static final AdventureUtils adventureUtils = ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_19_R1)
      ? (AdventureUtils)ReflectUtil.getPaperFeature("util.AdventureUtil")
      : null;
   private static final boolean supportsRGB = ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_16_R1);

   public ChatUtil() {
   }

   public static AdventureUtils getAdventureUtils() {
      return adventureUtils;
   }

   public static String getLastColor(String var0) {
      String var1 = "";
      int var2 = var0.length();

      for (int var3 = var2 - 1; var3 > -1; var3--) {
         char var4 = var0.charAt(var3);
         if ((var4 == 167 || var4 == '&') && var3 < var2 - 1) {
            char var8 = var0.charAt(var3 + 1);
            ChatColor var6 = ChatColor.getByChar(var8);
            if (var6 != null) {
               if (var6.isColor() || var6.equals(ChatColor.RESET)) {
                  var1 = var6.toString() + var1;
                  break;
               }

               if (var6.isFormat()) {
                  var1 = var6.toString() + var1;
               }
            }
         } else if (var4 == '#' && var3 < var2 - 6 && supportsRGB && ServerInfo.supportsSpigot()) {
            String var5 = var0.substring(var3, var3 + 6);

            try {
               Color.decode(var5);
               return var4 + var5;
            } catch (NumberFormatException var7) {
            }
         }
      }

      return var1;
   }

   private static String formatLegacy(String var0) {
      if (supportsRGB) {
         Gradient var1 = findGradient(var0);
         if (var1 != null) {
            var0 = var1.get();
         }

         for (Matcher var2 = rgbPattern.matcher(var0); var2.find(); var2 = rgbPattern.matcher(var0)) {
            String var3 = var0.substring(var2.start(), var2.end());
            net.md_5.bungee.api.ChatColor var4 = net.md_5.bungee.api.ChatColor.of(var3);
            if (var2.start() != 0 && var2.end() != var0.length()) {
               if (var0.charAt(var2.start() - 1) == '&') {
                  var3 = "&" + var3;
               }

               if (var0.charAt(var2.start() - 1) == '{' && var0.charAt(var2.end()) == '}') {
                  var3 = "{" + var3 + "}";
               }
            }

            var0 = var0.replace(var3, String.valueOf(var4));
         }
      }

      return ChatColor.translateAlternateColorCodes('&', var0).replace("\\n", "\n");
   }

   private static Gradient findGradient(String var0) {
      Matcher var1 = startGradientPattern.matcher(var0);
      if (var1.find()) {
         String var2 = var0.substring(var1.start(), var1.end());
         var1 = endGradientPattern.matcher(var0);
         if (var1.find()) {
            String var3 = var0.substring(var1.start(), var1.end());
            return new Gradient(var2, var3, var0);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static String formatVanilla(String var0) {
      if (supportsRGB) {
         for (Matcher var1 = rgbPattern.matcher(var0); var1.find(); var1 = rgbPattern.matcher(var0)) {
            String var2 = var0.substring(var1.start(), var1.end());
            net.md_5.bungee.api.ChatColor var3 = net.md_5.bungee.api.ChatColor.of(var2);
            if (var1.start() != 0 && var1.end() != var0.length()) {
               if (var0.charAt(var1.start() - 1) == '&') {
                  var2 = "&" + var2;
               }

               if (var0.charAt(var1.start() - 1) == '{' && var0.charAt(var1.end()) == '}') {
                  var2 = "{" + var2 + "}";
               }
            }

            var0 = var0.replace(var2, String.valueOf(var3));
         }
      }

      return ChatColor.translateAlternateColorCodes('&', var0).replace("\\n", "\n");
   }

   public static boolean hasRGB(String var0) {
      if (ServerInfo.getVersion().olderOrEqualAs(ServerInfo.Version.v1_15_R1)) {
         return false;
      } else {
         if (adventureUtils != null) {
            var0 = adventureUtils.formatMiniToLegacy(var0);
         }

         String var1 = formatLegacy(var0).replace("§x", "#").replaceAll("§", "");
         return rgbPattern.matcher(var1).find();
      }
   }

   public static String formatColors(String var0) {
      if (adventureUtils != null) {
         var0 = adventureUtils.formatMiniToLegacy(var0);
      }

      return formatLegacy(var0);
   }

   public static String stripColor(String var0) {
      return adventureUtils != null ? adventureUtils.stripColor(var0) : ChatColor.stripColor(var0);
   }

   public static String getLastColors(String var0) {
      return adventureUtils != null ? adventureUtils.getLastColors(var0) : getLastColor(var0);
   }

   public static String toMini(String var0) {
      return adventureUtils.formatLegacyToMini(var0);
   }

   public static String getGsonComponent(String var0) {
      return adventureUtils.getGsonComponent(var0);
   }

   private static boolean isMiniLibAvailable() {
      try {
         Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
         return true;
      } catch (ClassNotFoundException var1) {
         return false;
      }
   }
}
