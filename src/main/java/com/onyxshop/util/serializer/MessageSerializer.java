package com.onyxshop.util.serializer;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.onyxshop.util.serializer.components.ListComponent;
import com.onyxshop.util.serializer.components.StringComponent;
import com.onyxshop.util.serializer.components.components.FormatComponent;
import com.onyxshop.util.serializer.components.components.TextComponent;
import org.bukkit.ChatColor;

public class MessageSerializer {
   private static final Pattern formatPattern = Pattern.compile("§[0-9A-FK-ORa-fk-or]|§x(§[0-9A-Fa-f]){6}");

   public MessageSerializer() {
   }

   public String serializeToJson(String var1) {
      return this.toComponent(var1).serialize().toJSONString();
   }

   public String serializeToJson(List<String> var1) {
      ListComponent var2 = new ListComponent();

      for (String var4 : var1) {
         var2.addComponent(this.toComponent(var4));
      }

      return var2.serialize().toJSONString();
   }

   public StringComponent toComponent(String var1) {
      StringComponent var2 = new StringComponent();
      if (var1.contains("§")) {
         if (!var1.startsWith("§")) {
            String var3 = var1.substring(0, var1.indexOf("§"));
            var2.addComponent(new TextComponent(var3));
            var1 = var1.substring(var1.indexOf("§"));
            return this.formatString(var2, var1);
         } else {
            return this.formatString(var1);
         }
      } else {
         var2.addComponent(new TextComponent(var1));
         return var2;
      }
   }

   private StringComponent formatString(String var1) {
      return this.formatString(new StringComponent(), var1);
   }

   private StringComponent formatString(StringComponent var1, String var2) {
      FormatComponent var3 = new FormatComponent(0);
      MessageSerializer.Line var4 = new MessageSerializer.Line();
      boolean var5 = false;
      int var6 = 0;
      int var7 = 0;

      for (Matcher var8 = formatPattern.matcher(var2); var8.find(); var7 = var8.end()) {
         String var9 = var8.group();
         var6++;
         var4.append(var2.substring(var7, var8.start()));
         if (var9.charAt(1) == 'x') {
            if (var3.isReset() || var5) {
               var3.setText(var4.get());
               var1.addComponent(var3);
               var3 = new FormatComponent(var6);
            }

            var3.useItalic();
            var3.setHex(var9.substring(2).replace("§", ""));
            var5 = true;
         } else {
            ChatColor var10 = ChatColor.getByChar(var9.charAt(1));
            if (var10.isColor()) {
               if (var3.isReset() || var5) {
                  var3.setText(var4.get());
                  var1.addComponent(var3);
                  var3 = new FormatComponent(var6);
               }

               var3.useItalic();
               var3.setColor(var10.name().toLowerCase(Locale.ENGLISH));
               var5 = true;
            } else if (var10.isFormat()) {
               if (var3.isReset()) {
                  var3.setText(var4.get());
                  var1.addComponent(var3);
                  var3 = new FormatComponent(var6);
               }

               var5 = true;
               switch (var10) {
                  case BOLD:
                     var3.setBold();
                     break;
                  case MAGIC:
                     var3.setObfuscated();
                     break;
                  case ITALIC:
                     var3.setItalic();
                     break;
                  case UNDERLINE:
                     var3.setUnderlined();
                     break;
                  case STRIKETHROUGH:
                     var3.setStrikethrough();
               }
            } else if (var10 == ChatColor.RESET) {
               var3.setText(var4.get());
               var1.addComponent(var3);
               var3 = new FormatComponent(var6);
               var3.setReset();
               var5 = true;
            }
         }
      }

      var4.append(var2.substring(var7));
      var3.setText(var4.get());
      var1.addComponent(var3);
      return var1;
   }

   private static final class Line {
      private StringBuilder text = new StringBuilder();

      public Line() {
      }

      public void append(String var1) {
         this.text.append(var1);
      }

      public String get() {
         String var1 = this.text.toString();
         this.text = new StringBuilder();
         return var1;
      }
   }
}
