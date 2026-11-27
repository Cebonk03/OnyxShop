package com.onyxshop.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.onyxshop.methodes.SendMessage;
import net.md_5.bungee.api.ChatColor;

public class Gradient {
   private final String format;
   private final String original;
   private final String start;
   private final String end;
   private final List<ChatColor> colors = new ArrayList<>();
   private final Pattern rbgPattern = Pattern.compile("#[a-fA-F0-9]{6}");

   public Gradient(String text, String text2, String text3) {
      int index = text3.indexOf(text);
      int value = text3.indexOf(text2) + text2.length();
      this.start = text3.substring(0, index);
      this.end = text3.substring(value);
      this.format = this.getFormat(this.start);
      this.original = text3.substring(index, value).replace(text, "").replace(text2, "");
      Color color = this.getColor(text);
      if (color == null) {
         color = Color.decode("#D4AF37");
      }

      Color color2 = this.getColor(text2);
      if (color2 == null) {
         color2 = Color.decode("#FADA6F");
      }

      for (int index2 = 0; index2 < this.original.length(); index2++) {
         float amount = (float)index2 / this.original.length();
         int value3 = (int)(color2.getRed() * amount + color.getRed() * (1.0F - amount));
         int value4 = (int)(color2.getGreen() * amount + color.getGreen() * (1.0F - amount));
         int value5 = (int)(color2.getBlue() * amount + color.getBlue() * (1.0F - amount));
         this.colors.add(ChatColor.of(new Color(value3, value4, value5)));
      }
   }

   private Color getColor(String text) {
      Matcher matcher = this.rbgPattern.matcher(text);
      if (matcher.find()) {
         try {
            return Color.decode(text.substring(matcher.start(), matcher.end()));
         } catch (NumberFormatException exception) {
            SendMessage.warnMessage("Invalid rgb format for '" + text + "', using default gradient colors...");
         }
      } else {
         SendMessage.warnMessage("Invalid rgb format for '" + text + "', using default gradient colors...");
      }

      return null;
   }

   public ChatColor getColor(int value) {
      return this.colors.get(value);
   }

   public String get() {
      int value = 0;
      StringBuilder builder = new StringBuilder();
      builder.append(this.start);

      for (char character : this.original.toCharArray()) {
         builder.append(this.getColor(value)).append(this.format).append(character);
         value++;
      }

      builder.append(this.end);
      return builder.toString();
   }

   private String getFormat(String text) {
      String text2 = "";
      int value = text.length();

      for (int index = value - 1; index > -1; index--) {
         char character = text.charAt(index);
         if ((character == 167 || character == '&') && index < value - 1) {
            char character2 = text.charAt(index + 1);
            org.bukkit.ChatColor chatColor = org.bukkit.ChatColor.getByChar(character2);
            if (chatColor != null && chatColor.isFormat()) {
               text2 = chatColor.toString() + text2;
               if (chatColor.isColor() || chatColor.equals(org.bukkit.ChatColor.RESET)) {
                  break;
               }
            }
         }
      }

      return text2;
   }
}
