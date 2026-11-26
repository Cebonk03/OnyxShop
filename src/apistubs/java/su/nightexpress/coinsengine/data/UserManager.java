package su.nightexpress.coinsengine.data;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import su.nightexpress.coinsengine.data.impl.CoinsUser;

public interface UserManager {
   default CoinsUser getUserData(UUID var1) {
      return null;
   }

   default CompletableFuture<CoinsUser> getUserDataAsync(UUID var1) {
      return CompletableFuture.completedFuture(null);
   }

   default void saveAsync(CoinsUser var1) {
   }
}
