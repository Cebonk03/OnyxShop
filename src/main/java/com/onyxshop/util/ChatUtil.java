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

   public static String getLastColor(String text) {
      String text2 = "";
      int value = text.length();

      for (int index = value - 1; index > -1; index--) {
         char character = text.charAt(index);
         if ((character == 167 || character == '&') && index < value - 1) {
            char character2 = text.charAt(index + 1);
            ChatColor chatColor = ChatColor.getByChar(character2);
            if (chatColor != null) {
               if (chatColor.isColor() || chatColor.equals(ChatColor.RESET)) {
                  text2 = chatColor.toString() + text2;
                  break;
               }

               if (chatColor.isFormat()) {
                  text2 = chatColor.toString() + text2;
               }
            }
         } else if (character == '#' && index < value - 6 && supportsRGB && ServerInfo.supportsSpigot()) {
            String text3 = text.substring(index, index + 6);

            try {
               Color.decode(text3);
               return character + text3;
            } catch (NumberFormatException exception) {
            }
         }
      }

      return text2;
   }

   private static String formatLegacy(String text) {
      if (supportsRGB) {
         Gradient gradient = findGradient(text);
         if (gradient != null) {
            text = gradient.get();
         }

         for (Matcher matcher = rgbPattern.matcher(text); matcher.find(); matcher = rgbPattern.matcher(text)) {
            String text2 = text.substring(matcher.start(), matcher.end());
            net.md_5.bungee.api.ChatColor chatColor = net.md_5.bungee.api.ChatColor.of(text2);
            if (matcher.start() != 0 && matcher.end() != text.length()) {
               if (text.charAt(matcher.start() - 1) == '&') {
                  text2 = "&" + text2;
               }

               if (text.charAt(matcher.start() - 1) == '{' && text.charAt(matcher.end()) == '}') {
                  text2 = "{" + text2 + "}";
               }
            }

            text = text.replace(text2, String.valueOf(chatColor));
         }
      }

      return ChatColor.translateAlternateColorCodes('&', text).replace("\\n", "\n");
   }

   private static Gradient findGradient(String text) {
      Matcher matcher = startGradientPattern.matcher(text);
      if (matcher.find()) {
         String text2 = text.substring(matcher.start(), matcher.end());
         matcher = endGradientPattern.matcher(text);
         if (matcher.find()) {
            String text3 = text.substring(matcher.start(), matcher.end());
            return new Gradient(text2, text3, text);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static String formatVanilla(String text) {
      if (supportsRGB) {
         for (Matcher matcher = rgbPattern.matcher(text); matcher.find(); matcher = rgbPattern.matcher(text)) {
            String text2 = text.substring(matcher.start(), matcher.end());
            net.md_5.bungee.api.ChatColor chatColor = net.md_5.bungee.api.ChatColor.of(text2);
            if (matcher.start() != 0 && matcher.end() != text.length()) {
               if (text.charAt(matcher.start() - 1) == '&') {
                  text2 = "&" + text2;
               }

               if (text.charAt(matcher.start() - 1) == '{' && text.charAt(matcher.end()) == '}') {
                  text2 = "{" + text2 + "}";
               }
            }

            text = text.replace(text2, String.valueOf(chatColor));
         }
      }

      return ChatColor.translateAlternateColorCodes('&', text).replace("\\n", "\n");
   }

   public static boolean hasRGB(String text) {
      if (ServerInfo.getVersion().olderOrEqualAs(ServerInfo.Version.v1_15_R1)) {
         return false;
      } else {
         if (adventureUtils != null) {
            text = adventureUtils.formatMiniToLegacy(text);
         }

         String text2 = formatLegacy(text).replace("§x", "#").replaceAll("§", "");
         return rgbPattern.matcher(text2).find();
      }
   }

   public static String formatColors(String text) {
      if (adventureUtils != null) {
         text = adventureUtils.formatMiniToLegacy(text);
      }

      return formatLegacy(text);
   }

   public static String stripColor(String text) {
      return adventureUtils != null ? adventureUtils.stripColor(text) : ChatColor.stripColor(text);
   }

   public static String getLastColors(String text) {
      return adventureUtils != null ? adventureUtils.getLastColors(text) : getLastColor(text);
   }

   public static String toMini(String text) {
      return adventureUtils.formatLegacyToMini(text);
   }

   public static String getGsonComponent(String text) {
      return adventureUtils.getGsonComponent(text);
   }

   private static boolean isMiniLibAvailable() {
      try {
         Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
         return true;
      } catch (ClassNotFoundException exception) {
         return false;
      }
   }
}
