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

   public String serializeToJson(String text) {
      return this.toComponent(text).serialize().toJSONString();
   }

   public String serializeToJson(List<String> values) {
      ListComponent listComponent = new ListComponent();

      for (String text : values) {
         listComponent.addComponent(this.toComponent(text));
      }

      return listComponent.serialize().toJSONString();
   }

   public StringComponent toComponent(String text) {
      StringComponent stringComponent = new StringComponent();
      if (text.contains("§")) {
         if (!text.startsWith("§")) {
            String text2 = text.substring(0, text.indexOf("§"));
            stringComponent.addComponent(new TextComponent(text2));
            text = text.substring(text.indexOf("§"));
            return this.formatString(stringComponent, text);
         } else {
            return this.formatString(text);
         }
      } else {
         stringComponent.addComponent(new TextComponent(text));
         return stringComponent;
      }
   }

   private StringComponent formatString(String text) {
      return this.formatString(new StringComponent(), text);
   }

   private StringComponent formatString(StringComponent stringComponent, String text) {
      FormatComponent formatComponent = new FormatComponent(0);
      MessageSerializer.Line line = new MessageSerializer.Line();
      boolean flag = false;
      int value = 0;
      int value2 = 0;

      for (Matcher matcher = formatPattern.matcher(text); matcher.find(); value2 = matcher.end()) {
         String text2 = matcher.group();
         value++;
         line.append(text.substring(value2, matcher.start()));
         if (text2.charAt(1) == 'x') {
            if (formatComponent.isReset() || flag) {
               formatComponent.setText(line.get());
               stringComponent.addComponent(formatComponent);
               formatComponent = new FormatComponent(value);
            }

            formatComponent.useItalic();
            formatComponent.setHex(text2.substring(2).replace("§", ""));
            flag = true;
         } else {
            ChatColor chatColor = ChatColor.getByChar(text2.charAt(1));
            if (chatColor.isColor()) {
               if (formatComponent.isReset() || flag) {
                  formatComponent.setText(line.get());
                  stringComponent.addComponent(formatComponent);
                  formatComponent = new FormatComponent(value);
               }

               formatComponent.useItalic();
               formatComponent.setColor(chatColor.name().toLowerCase(Locale.ENGLISH));
               flag = true;
            } else if (chatColor.isFormat()) {
               if (formatComponent.isReset()) {
                  formatComponent.setText(line.get());
                  stringComponent.addComponent(formatComponent);
                  formatComponent = new FormatComponent(value);
               }

               flag = true;
               switch (chatColor) {
                  case BOLD:
                     formatComponent.setBold();
                     break;
                  case MAGIC:
                     formatComponent.setObfuscated();
                     break;
                  case ITALIC:
                     formatComponent.setItalic();
                     break;
                  case UNDERLINE:
                     formatComponent.setUnderlined();
                     break;
                  case STRIKETHROUGH:
                     formatComponent.setStrikethrough();
               }
            } else if (chatColor == ChatColor.RESET) {
               formatComponent.setText(line.get());
               stringComponent.addComponent(formatComponent);
               formatComponent = new FormatComponent(value);
               formatComponent.setReset();
               flag = true;
            }
         }
      }

      line.append(text.substring(value2));
      formatComponent.setText(line.get());
      stringComponent.addComponent(formatComponent);
      return stringComponent;
   }

   private static final class Line {
      private StringBuilder text = new StringBuilder();

      public Line() {
      }

      public void append(String text) {
         this.text.append(text);
      }

      public String get() {
         String text = this.text.toString();
         this.text = new StringBuilder();
         return text;
      }
   }
}
