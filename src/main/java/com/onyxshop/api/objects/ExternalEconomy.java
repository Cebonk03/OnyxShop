package com.onyxshop.api.objects;

import com.onyxshop.providers.economys.ExternalEconomyProvider;
import org.bukkit.OfflinePlayer;

public abstract class ExternalEconomy extends ExternalEconomyProvider {
   public ExternalEconomy() {
   }

   @Override
   public abstract double getBalance(OfflinePlayer var1);

   @Override
   public abstract void depositBalance(OfflinePlayer var1, double var2);

   @Override
   public abstract boolean withdrawBalance(OfflinePlayer var1, double var2);

   @Override
   public abstract String getName();

   @Override
   public abstract String getPlural();

   @Override
   public abstract String getSingular();

   @Override
   public abstract String getFriendly();

   @Override
   public abstract boolean isDecimal();
}
