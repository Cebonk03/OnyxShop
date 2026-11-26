package su.nightexpress.coinsengine.api.currency;

public interface Currency {
   String getId();

   String getName();

   String getSymbol();

   String getFormat();

   boolean isDecimal();
}
