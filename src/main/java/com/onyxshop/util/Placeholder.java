package com.onyxshop.util;

import com.onyxshop.providers.PlaceholderAPIHook;
import org.bukkit.entity.Player;

public class Placeholder {
   String original;
   Placeholder.Type type;

   public Placeholder(String var1, String... var2) {
      this.original = var1;
      this.type = getPlaceholderType(var1, var2);
   }

   public static boolean isPlaceholder(String var0, String... var1) {
      return getPlaceholderType(var0, var1) != Placeholder.Type.NONE;
   }

   public String getRaw() {
      return this.original;
   }

   public String translate(Player var1, Replacement... var2) {
      return this.translate(this.original, var1, var2);
   }

   public String translate(Player var1, String... var2) {
      return this.translate(this.original, var1, var2);
   }

   public String translate(String var1, Player var2, Replacement... var3) {
      if (this.type != Placeholder.Type.PAPI) {
         var1 = this.replace(var1, var3);
      }

      return this.type != Placeholder.Type.LOCAL ? PlaceholderAPIHook.translate(var2, var1) : var1;
   }

   public String translate(String var1, Player var2, String... var3) {
      if (this.type != Placeholder.Type.PAPI) {
         var1 = this.replace(var1, var3);
      }

      return this.type != Placeholder.Type.LOCAL ? PlaceholderAPIHook.translate(var2, var1) : var1;
   }

   private String replace(String var1, Replacement... var2) {
      for (Replacement var6 : var2) {
         if (var1.contains(var6.key)) {
            var1 = var1.replace(var6.key, var6.value.get());
         }
      }

      return var1;
   }

   private String replace(String var1, String... var2) {
      for (byte var3 = 0; var3 < var2.length; var3 += 2) {
         if (var1.contains(var2[var3])) {
            var1 = var1.replace(var2[var3], var2[var3 + 1]);
         }
      }

      return var1;
   }

   private static Placeholder.Type getPlaceholderType(String var0, String... var1) {
      String var2 = var0;
      Placeholder.Type var3 = Placeholder.Type.NONE;

      for (String var7 : var1) {
         if (var2.contains(var7)) {
            var2 = var2.replaceAll(var7, "");
            var3 = Placeholder.Type.LOCAL;
         }
      }

      return PlaceholderAPIHook.hasPlaceholders(var2)
         ? (var3 == Placeholder.Type.LOCAL ? Placeholder.Type.BOTH : Placeholder.Type.PAPI)
         : (var3 == Placeholder.Type.LOCAL ? Placeholder.Type.LOCAL : Placeholder.Type.NONE);
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
