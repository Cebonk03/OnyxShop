package com.onyxshop.objects.titles;

import com.onyxshop.files.Translatable;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.UserManager;
import org.bukkit.entity.Player;

public class ShopTitle {
   private final ShopTitle.Title title;
   private final ShopTitle.Title bedrockTitle;

   public ShopTitle(Translatable text) {
      this(text, null);
   }

   public ShopTitle(Translatable text, Translatable text2) {
      this.title = new ShopTitle.Title(text);
      this.bedrockTitle = text2 == null ? this.title : new ShopTitle.Title(text2);
   }

   public Translatable getTitle() {
      return this.title.get();
   }

   public Translatable getBedrockTitle() {
      return this.bedrockTitle.get();
   }

   public String getRaw() {
      return this.title.getRaw();
   }

   public Translatable getTitle(Player player, int value, int value2) {
      return (UserManager.getUser(player).isBedrock() ? this.bedrockTitle : this.title).getTitle(player, value, value2);
   }

   public boolean isStatic() {
      return this.title.isStatic() && this.bedrockTitle.isStatic();
   }

   private static class Title {
      private final Translatable title;
      private final boolean dynamicPages;
      private final boolean placeholders;

      private Title(Translatable text) {
         this.title = text;
         this.dynamicPages = text.toString().contains("%page%") || text.toString().contains("%pages%");
         this.placeholders = PlaceholderAPIHook.hasPlaceholders(text.toString().replace("%page%", "").replace("%pages%", ""));
      }

      private Translatable get() {
         return this.title;
      }

      private String getRaw() {
         return this.title.toString();
      }

      private Translatable getTitle(Player player, int value, int value2) {
         Translatable text = this.title;
         if (this.dynamicPages) {
            text = this.title.replace("%page%", value + "").replace("%pages%", value2 + "");
         }

         if (this.placeholders) {
            text = text.translate(player);
         }

         return text;
      }

      private boolean isStatic() {
         return !this.dynamicPages && !this.placeholders;
      }
   }
}
