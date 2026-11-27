package su.nightexpress.coinsengine.data;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import su.nightexpress.coinsengine.data.impl.CoinsUser;

public interface UserManager {
   default CoinsUser getUserData(UUID uniqueId) {
      return null;
   }

   default CompletableFuture<CoinsUser> getUserDataAsync(UUID uniqueId) {
      return CompletableFuture.completedFuture(null);
   }

   default void saveAsync(CoinsUser coinsUser) {
   }
}
