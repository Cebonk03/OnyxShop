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

   public GUITitle(Lang lang, String title) {
      for (Locale locale : Lang.getLocales()) {
         Translatable text = lang.get(locale);
         Translatable text2 = Lang.getOrNull(title, locale);
         this.variants.put(locale, new GUITitle.Localized(text, text2 == null ? text : text2));
      }
   }

   public Translatable getTitle(Player player) {
      GUITitle.Localized localized = this.variants.getOrDefault(UserManager.getUser(player).getLocale(), this.variants.get(Lang.getDefaultLocale()));
      if (UserManager.getUser(player).isBedrock()) {
         return localized.bedrockPlaceholders ? localized.bedrockTitle.translate(player) : localized.bedrockTitle;
      } else {
         return localized.placeholders ? localized.title.translate(player) : localized.title;
      }
   }

   private static final class Localized {
      private final Translatable title;
      private final boolean placeholders;
      private final Translatable bedrockTitle;
      private final boolean bedrockPlaceholders;

      private Localized(Translatable text, Translatable text2) {
         this.title = text;
         this.placeholders = PlaceholderAPIHook.hasPlaceholders(text.toString());
         this.bedrockTitle = text2;
         this.bedrockPlaceholders = PlaceholderAPIHook.hasPlaceholders(text2.toString());
      }
   }
}
