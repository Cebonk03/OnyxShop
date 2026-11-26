package su.nightexpress.coinsengine.api.currency;

import java.util.Collection;
import java.util.Collections;

public interface CurrencyManager {
   default Currency getCurrency(String var1) {
      return null;
   }

   default Collection<Currency> getCurrencies() {
      return Collections.emptyList();
   }
}
