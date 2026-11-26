package com.onyxshop.objects.titles;

import com.onyxshop.files.Translatable;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.UserManager;
import org.bukkit.entity.Player;

public class ShopTitle {
   private final ShopTitle.Title title;
   private final ShopTitle.Title bedrockTitle;

   public ShopTitle(Translatable var1) {
      this(var1, null);
   }

   public ShopTitle(Translatable var1, Translatable var2) {
      this.title = new ShopTitle.Title(var1);
      this.bedrockTitle = var2 == null ? this.title : new ShopTitle.Title(var2);
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

   public Translatable getTitle(Player var1, int var2, int var3) {
      return (UserManager.getUser(var1).isBedrock() ? this.bedrockTitle : this.title).getTitle(var1, var2, var3);
   }

   public boolean isStatic() {
      return this.title.isStatic() && this.bedrockTitle.isStatic();
   }

   private static class Title {
      private final Translatable title;
      private final boolean dynamicPages;
      private final boolean placeholders;

      private Title(Translatable var1) {
         this.title = var1;
         this.dynamicPages = var1.toString().contains("%page%") || var1.toString().contains("%pages%");
         this.placeholders = PlaceholderAPIHook.hasPlaceholders(var1.toString().replace("%page%", "").replace("%pages%", ""));
      }

      private Translatable get() {
         return this.title;
      }

      private String getRaw() {
         return this.title.toString();
      }

      private Translatable getTitle(Player var1, int var2, int var3) {
         Translatable var4 = this.title;
         if (this.dynamicPages) {
            var4 = this.title.replace("%page%", var2 + "").replace("%pages%", var3 + "");
         }

         if (this.placeholders) {
            var4 = var4.translate(var1);
         }

         return var4;
      }

      private boolean isStatic() {
         return !this.dynamicPages && !this.placeholders;
      }
   }
}
