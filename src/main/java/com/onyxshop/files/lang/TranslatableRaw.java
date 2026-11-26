package com.onyxshop.files.lang;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.files.Translatable;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.util.ChatUtil;
import org.bukkit.entity.Player;

public class TranslatableRaw implements Translatable {
   private final String s;

   public TranslatableRaw(String var1) {
      this.s = ChatUtil.formatColors(var1);
   }

   @Override
   public Translatable replace(String var1, String var2) {
      return new TranslatableRaw(this.s.replace(var1, var2));
   }

   @Override
   public Translatable replace(String var1, Translatable var2) {
      return new TranslatableRaw(this.s.replace(var1, (String)var2.get()));
   }

   @Override
   public String getLegacy() {
      return this.s;
   }

   @Override
   public String getBedrockLegacy() {
      return this.s;
   }

   @Override
   public String toString() {
      return this.s;
   }

   @Override
   public Object get() {
      return this.s;
   }

   @Override
   public List<Translatable> getLines() {
      return Arrays.stream(this.s.split("\n")).map(TranslatableRaw::new).collect(Collectors.toList());
   }

   @Override
   public Translatable translate(Player var1) {
      return new TranslatableRaw(PlaceholderAPIHook.translate(var1, this.s));
   }

   @Override
   public Translatable get(Player var1) {
      return PlaceholderAPIHook.hasPlaceholders(this.s) ? new TranslatableRaw(PlaceholderAPIHook.translate(var1, this.s)) : this;
   }

   @Override
   public boolean isEmpty() {
      return this.s.isEmpty();
   }

   @Override
   public Translatable.Builder builder() {
      return new TranslatableRaw.StringBuilder(this.s);
   }

   public class StringBuilder implements Translatable.Builder {
      private final java.lang.StringBuilder builder = new java.lang.StringBuilder();

      public StringBuilder(String nullx) {
         this.builder.append(nullx);
      }

      @Override
      public Translatable.Builder append(Translatable var1) {
         this.builder.append(var1.get());
         return this;
      }

      @Override
      public Translatable.Builder append(String var1) {
         this.builder.append(var1);
         return this;
      }

      @Override
      public Translatable.Builder color(char var1) {
         this.builder.append("§").append(var1);
         return this;
      }

      @Override
      public Translatable build() {
         return new TranslatableRaw(this.builder.toString());
      }
   }
}
