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

   public TranslatableRaw(String text) {
      this.s = ChatUtil.formatColors(text);
   }

   @Override
   public Translatable replace(String text, String text2) {
      return new TranslatableRaw(this.s.replace(text, text2));
   }

   @Override
   public Translatable replace(String text, Translatable text2) {
      return new TranslatableRaw(this.s.replace(text, (String)text2.get()));
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
   public Translatable translate(Player player) {
      return new TranslatableRaw(PlaceholderAPIHook.translate(player, this.s));
   }

   @Override
   public Translatable get(Player player) {
      return PlaceholderAPIHook.hasPlaceholders(this.s) ? new TranslatableRaw(PlaceholderAPIHook.translate(player, this.s)) : this;
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

      public StringBuilder(String text) {
         this.builder.append(text);
      }

      @Override
      public Translatable.Builder append(Translatable text) {
         this.builder.append(text.get());
         return this;
      }

      @Override
      public Translatable.Builder append(String text) {
         this.builder.append(text);
         return this;
      }

      @Override
      public Translatable.Builder color(char character) {
         this.builder.append("§").append(character);
         return this;
      }

      @Override
      public Translatable build() {
         return new TranslatableRaw(this.builder.toString());
      }
   }
}
