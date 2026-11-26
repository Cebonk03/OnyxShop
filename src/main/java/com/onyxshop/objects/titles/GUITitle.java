package com.onyxshop.objects.titles;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.UserManager;
import org.bukkit.entity.Player;

public class GUITitle {
   private final Map<Locale, GUITitle.Localized> variants = new HashMap<>();

   public GUITitle(Lang var1, String var2) {
      for (Locale var4 : Lang.getLocales()) {
         Translatable var5 = var1.get(var4);
         Translatable var6 = Lang.getOrNull(var2, var4);
         this.variants.put(var4, new GUITitle.Localized(var5, var6 == null ? var5 : var6));
      }
   }

   public Translatable getTitle(Player var1) {
      GUITitle.Localized var2 = this.variants.getOrDefault(UserManager.getUser(var1).getLocale(), this.variants.get(Lang.getDefaultLocale()));
      if (UserManager.getUser(var1).isBedrock()) {
         return var2.bedrockPlaceholders ? var2.bedrockTitle.translate(var1) : var2.bedrockTitle;
      } else {
         return var2.placeholders ? var2.title.translate(var1) : var2.title;
      }
   }

   private static final class Localized {
      private final Translatable title;
      private final boolean placeholders;
      private final Translatable bedrockTitle;
      private final boolean bedrockPlaceholders;

      private Localized(Translatable var1, Translatable var2) {
         this.title = var1;
         this.placeholders = PlaceholderAPIHook.hasPlaceholders(var1.toString());
         this.bedrockTitle = var2;
         this.bedrockPlaceholders = PlaceholderAPIHook.hasPlaceholders(var2.toString());
      }
   }
}
