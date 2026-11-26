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
   public void onLocaleChange(PlayerLocaleChangeEvent var1) {
      User var2 = UserManager.getUser(var1.getPlayer());
      if (var2 != null) {
         Locale var3 = UserManager.getLocale(var1.getPlayer(), var1.getLocale());
         var2.setLocale(var3);
      } else {
         UserManager.loadUser(var1.getPlayer());
      }
   }
}
