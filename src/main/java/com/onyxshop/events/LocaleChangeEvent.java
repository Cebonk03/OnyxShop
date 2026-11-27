package com.onyxshop.events;

import java.util.Locale;
import com.onyxshop.objects.User;
import com.onyxshop.providers.UserManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;

public class LocaleChangeEvent implements Listener {
   public LocaleChangeEvent() {
   }

   @EventHandler
   public void onLocaleChange(PlayerLocaleChangeEvent event) {
      User user = UserManager.getUser(event.getPlayer());
      if (user != null) {
         Locale locale = UserManager.getLocale(event.getPlayer(), event.getLocale());
         user.setLocale(locale);
      } else {
         UserManager.loadUser(event.getPlayer());
      }
   }
}
