package su.nightexpress.coinsengine.api;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import su.nightexpress.coinsengine.api.currency.Currency;
import su.nightexpress.coinsengine.api.currency.CurrencyManager;
import su.nightexpress.coinsengine.data.UserManager;

public final class CoinsEngineAPI {
   public static final Plugin PLUGIN = null;

   private CoinsEngineAPI() {
   }

   public static CurrencyManager getCurrencyManager() {
      return null;
   }

   public static UserManager getUserManager() {
      return null;
   }

   public static Currency getCurrency(String var0) {
      return null;
   }

   public static Collection<Currency> getCurrencies() {
      return Collections.emptyList();
   }

   public static double getBalance(Player var0, Currency var1) {
      return 0.0;
   }

   public static double getBalance(UUID var0, Currency var1) {
      return 0.0;
   }

   public static void addBalance(UUID var0, Currency var1, double var2) {
   }

   public static void removeBalance(UUID var0, Currency var1, double var2) {
   }
}
