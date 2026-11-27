package com.onyxshop.files.lang;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.files.Translatable;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.util.AdventureUtil;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.meta.PaperMeta;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class TranslatableComponent implements Translatable {
   private final String miniMessage;

   public TranslatableComponent(String text) {
      this.miniMessage = ChatUtil.getAdventureUtils().formatLegacyToMini(text);
   }

   public TranslatableComponent(String text, boolean flag) {
      this.miniMessage = text;
   }

   public TranslatableComponent(Component component) {
      this.miniMessage = AdventureUtil.serialize(component);
   }

   @Override
   public Translatable replace(String text, String text2) {
      return this.miniMessage.indexOf(text) == -1
         ? this
         : new TranslatableComponent(this.miniMessage.replace(text, ChatUtil.getAdventureUtils().formatLegacyToMini(text2)), false);
   }

   @Override
   public Translatable replace(String text, Translatable text2) {
      return this.miniMessage.indexOf(text) == -1
         ? this
         : new TranslatableComponent(this.miniMessage.replace(text, ChatUtil.getAdventureUtils().formatLegacyToMini(text2.toString())), false);
   }

   @Override
   public String getLegacy() {
      return AdventureUtil.toLegacy((Component)this.get());
   }

   @Override
   public String getBedrockLegacy() {
      return AdventureUtil.toBedrockLegacy((Component)this.get());
   }

   @Override
   public String toString() {
      return this.miniMessage;
   }

   @Override
   public Object get() {
      return ChatUtil.getAdventureUtils().formatMini(this.miniMessage);
   }

   @Override
   public Translatable translate(Player player) {
      return new TranslatableComponent(PlaceholderAPIHook.translate(player, this.miniMessage));
   }

   @Override
   public Translatable get(Player player) {
      return new TranslatableComponent(PlaceholderAPIHook.translate(player, this.miniMessage));
   }

   @Override
   public List<Translatable> getLines() {
      return Arrays.asList(this.toString().replaceAll("</\\w+>", "").replace("\\\\n", "\n").split("\n"))
         .stream()
         .map(element -> new TranslatableComponent(element, true))
         .collect(Collectors.toList());
   }

   @Override
   public boolean isEmpty() {
      return this.miniMessage.isEmpty();
   }

   @Override
   public Translatable.Builder builder() {
      return new TranslatableComponent.ComponentBuilder(this);
   }

   public static class ComponentBuilder implements Translatable.Builder {
      private StringBuilder component;

      public ComponentBuilder(TranslatableComponent translatableComponent) {
         this.component = new StringBuilder(translatableComponent.toString());
      }

      @Override
      public Translatable.Builder append(Translatable text) {
         this.component.append(text.toString());
         return this;
      }

      @Override
      public Translatable.Builder append(String text) {
         this.component.append(ChatUtil.getAdventureUtils().formatLegacyToMini(text));
         return this;
      }

      @Override
      public Translatable.Builder color(char character) {
         this.component.append("<").append(PaperMeta.COLOR_MAP.get(character).toString()).append(">");
         return this;
      }

      @Override
      public Translatable build() {
         return new TranslatableComponent(this.component.toString(), true);
      }
   }
}
