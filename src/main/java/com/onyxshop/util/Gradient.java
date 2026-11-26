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

   public Gradient(String var1, String var2, String var3) {
      int var4 = var3.indexOf(var1);
      int var5 = var3.indexOf(var2) + var2.length();
      this.start = var3.substring(0, var4);
      this.end = var3.substring(var5);
      this.format = this.getFormat(this.start);
      this.original = var3.substring(var4, var5).replace(var1, "").replace(var2, "");
      Color var6 = this.getColor(var1);
      if (var6 == null) {
         var6 = Color.decode("#D4AF37");
      }

      Color var7 = this.getColor(var2);
      if (var7 == null) {
         var7 = Color.decode("#FADA6F");
      }

      for (int var8 = 0; var8 < this.original.length(); var8++) {
         float var9 = (float)var8 / this.original.length();
         int var10 = (int)(var7.getRed() * var9 + var6.getRed() * (1.0F - var9));
         int var11 = (int)(var7.getGreen() * var9 + var6.getGreen() * (1.0F - var9));
         int var12 = (int)(var7.getBlue() * var9 + var6.getBlue() * (1.0F - var9));
         this.colors.add(ChatColor.of(new Color(var10, var11, var12)));
      }
   }

   private Color getColor(String var1) {
      Matcher var2 = this.rbgPattern.matcher(var1);
      if (var2.find()) {
         try {
            return Color.decode(var1.substring(var2.start(), var2.end()));
         } catch (NumberFormatException var4) {
            SendMessage.warnMessage("Invalid rgb format for '" + var1 + "', using default gradient colors...");
         }
      } else {
         SendMessage.warnMessage("Invalid rgb format for '" + var1 + "', using default gradient colors...");
      }

      return null;
   }

   public ChatColor getColor(int var1) {
      return this.colors.get(var1);
   }

   public String get() {
      int var1 = 0;
      StringBuilder var2 = new StringBuilder();
      var2.append(this.start);

      for (char var6 : this.original.toCharArray()) {
         var2.append(this.getColor(var1)).append(this.format).append(var6);
         var1++;
      }

      var2.append(this.end);
      return var2.toString();
   }

   private String getFormat(String var1) {
      String var2 = "";
      int var3 = var1.length();

      for (int var4 = var3 - 1; var4 > -1; var4--) {
         char var5 = var1.charAt(var4);
         if ((var5 == 167 || var5 == '&') && var4 < var3 - 1) {
            char var6 = var1.charAt(var4 + 1);
            org.bukkit.ChatColor var7 = org.bukkit.ChatColor.getByChar(var6);
            if (var7 != null && var7.isFormat()) {
               var2 = var7.toString() + var2;
               if (var7.isColor() || var7.equals(org.bukkit.ChatColor.RESET)) {
                  break;
               }
            }
         }
      }

      return var2;
   }
}
