package com.onyxshop.util;

import com.onyxshop.providers.PlaceholderAPIHook;
import org.bukkit.entity.Player;

public class Placeholder {
   String original;
   Placeholder.Type type;

   public Placeholder(String text, String... values) {
      this.original = text;
      this.type = getPlaceholderType(text, values);
   }

   public static boolean isPlaceholder(String text, String... values) {
      return getPlaceholderType(text, values) != Placeholder.Type.NONE;
   }

   public String getRaw() {
      return this.original;
   }

   public String translate(Player player, Replacement... playerNames) {
      return this.translate(this.original, player, playerNames);
   }

   public String translate(Player player, String... playerNames) {
      return this.translate(this.original, player, playerNames);
   }

   public String translate(String playerName, Player player, Replacement... values) {
      if (this.type != Placeholder.Type.PAPI) {
         playerName = this.replace(playerName, values);
      }

      return this.type != Placeholder.Type.LOCAL ? PlaceholderAPIHook.translate(player, playerName) : playerName;
   }

   public String translate(String playerName, Player player, String... values) {
      if (this.type != Placeholder.Type.PAPI) {
         playerName = this.replace(playerName, values);
      }

      return this.type != Placeholder.Type.LOCAL ? PlaceholderAPIHook.translate(player, playerName) : playerName;
   }

   private String replace(String text, Replacement... values) {
      for (Replacement replacement : values) {
         if (text.contains(replacement.key)) {
            text = text.replace(replacement.key, replacement.value.get());
         }
      }

      return text;
   }

   private String replace(String text, String... values) {
      for (byte index = 0; index < values.length; index += 2) {
         if (text.contains(values[index])) {
            text = text.replace(values[index], values[index + 1]);
         }
      }

      return text;
   }

   private static Placeholder.Type getPlaceholderType(String type, String... values) {
      String text = type;
      Placeholder.Type type2 = Placeholder.Type.NONE;

      for (String text2 : values) {
         if (text.contains(text2)) {
            text = text.replaceAll(text2, "");
            type2 = Placeholder.Type.LOCAL;
         }
      }

      return PlaceholderAPIHook.hasPlaceholders(text)
         ? (type2 == Placeholder.Type.LOCAL ? Placeholder.Type.BOTH : Placeholder.Type.PAPI)
         : (type2 == Placeholder.Type.LOCAL ? Placeholder.Type.LOCAL : Placeholder.Type.NONE);
   }

   private static enum Type {
      LOCAL,
      PAPI,
      BOTH,
      NONE;

      private Type() {
      }
   }
}
