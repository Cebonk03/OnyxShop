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

   public TranslatableComponent(String var1) {
      this.miniMessage = ChatUtil.getAdventureUtils().formatLegacyToMini(var1);
   }

   public TranslatableComponent(String var1, boolean var2) {
      this.miniMessage = var1;
   }

   public TranslatableComponent(Component var1) {
      this.miniMessage = AdventureUtil.serialize(var1);
   }

   @Override
   public Translatable replace(String var1, String var2) {
      return this.miniMessage.indexOf(var1) == -1
         ? this
         : new TranslatableComponent(this.miniMessage.replace(var1, ChatUtil.getAdventureUtils().formatLegacyToMini(var2)), false);
   }

   @Override
   public Translatable replace(String var1, Translatable var2) {
      return this.miniMessage.indexOf(var1) == -1
         ? this
         : new TranslatableComponent(this.miniMessage.replace(var1, ChatUtil.getAdventureUtils().formatLegacyToMini(var2.toString())), false);
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
   public Translatable translate(Player var1) {
      return new TranslatableComponent(PlaceholderAPIHook.translate(var1, this.miniMessage));
   }

   @Override
   public Translatable get(Player var1) {
      return new TranslatableComponent(PlaceholderAPIHook.translate(var1, this.miniMessage));
   }

   @Override
   public List<Translatable> getLines() {
      return Arrays.asList(this.toString().replaceAll("</\\w+>", "").replace("\\\\n", "\n").split("\n"))
         .stream()
         .map(var0 -> new TranslatableComponent(var0, true))
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

      public ComponentBuilder(TranslatableComponent var1) {
         this.component = new StringBuilder(var1.toString());
      }

      @Override
      public Translatable.Builder append(Translatable var1) {
         this.component.append(var1.toString());
         return this;
      }

      @Override
      public Translatable.Builder append(String var1) {
         this.component.append(ChatUtil.getAdventureUtils().formatLegacyToMini(var1));
         return this;
      }

      @Override
      public Translatable.Builder color(char var1) {
         this.component.append("<").append(PaperMeta.COLOR_MAP.get(var1).toString()).append(">");
         return this;
      }

      @Override
      public Translatable build() {
         return new TranslatableComponent(this.component.toString(), true);
      }
   }
}
