package com.onyxshop.providers;

import com.onyxshop.OnyxShop;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.exceptions.EconomyLoadException;
import org.bukkit.OfflinePlayer;

public interface EconomyProvider {
   void setup(OnyxShop plugin) throws EconomyLoadException;

   double getBalance(OfflinePlayer offlinePlayer);

   void depositBalance(OfflinePlayer offlinePlayer, double balance);

   boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance);

   EcoType getType();

   String getPlural();

   String getSingular();

   String getFriendly();

   boolean isDecimal();

   boolean doWait();

   double round(double amount);

   String formatPrice(double price);

   void formatPlaceholders();
}
