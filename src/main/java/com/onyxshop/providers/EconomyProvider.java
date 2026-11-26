package com.onyxshop.providers;

import com.onyxshop.OnyxShop;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.exceptions.EconomyLoadException;
import org.bukkit.OfflinePlayer;

public interface EconomyProvider {
   void setup(OnyxShop var1) throws EconomyLoadException;

   double getBalance(OfflinePlayer var1);

   void depositBalance(OfflinePlayer var1, double var2);

   boolean withdrawBalance(OfflinePlayer var1, double var2);

   EcoType getType();

   String getPlural();

   String getSingular();

   String getFriendly();

   boolean isDecimal();

   boolean doWait();

   double round(double var1);

   String formatPrice(double var1);

   void formatPlaceholders();
}
