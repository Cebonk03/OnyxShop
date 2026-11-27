package com.onyxshop.providers.economys;

import com.onyxshop.OnyxShop;
import com.onyxshop.util.exceptions.EconomyLoadException;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.EconomyProvider;
import com.onyxshop.providers.economys.formatter.PriceFormatter;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.EconomyType;
import org.bukkit.OfflinePlayer;

public class ExternalEconomyProvider implements EconomyProvider {
   private PriceFormatter.Format formatter;

   public ExternalEconomyProvider() {
   }

   @Override
   public final void setup(OnyxShop plugin) throws EconomyLoadException {
      String type = "currency-formats." + this.getType().getType().name() + ":" + this.getName();
      String name = this.getCurrencyName() != null ? ConfigManager.getPathIgnoreCase(type + ":" + this.getCurrencyName()) : null;
      if (name == null || !ConfigManager.getConfig().contains(name)) {
         name = ConfigManager.getPathIgnoreCase(type);
      }

      this.formatter = PriceFormatter.getFormatter(name);
      SendMessage.infoMessage(
         "Successfully registered economy provider EXTERNAL:"
            + this.getName()
            + (this.getCurrencyName() != null ? " and using currency " + this.getCurrencyName() : "")
      );
   }

   @Override
   public double getBalance(OfflinePlayer offlinePlayer) {
      return 0.0;
   }

   @Override
   public void depositBalance(OfflinePlayer offlinePlayer, double balance) {
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance) {
      return true;
   }

   public String getName() {
      return "EconomyProviderName";
   }

   public String getCurrencyName() {
      return null;
   }

   @Override
   public final EcoType getType() {
      return new EcoType(EconomyType.EXTERNAL, this.getName() + (this.getCurrencyName() != null ? ":" + this.getCurrencyName() : ""));
   }

   @Override
   public String getPlural() {
      return "&d&l$&f%price%";
   }

   @Override
   public String getSingular() {
      return "&d&l$&f%price%";
   }

   @Override
   public String getFriendly() {
      return "&fmoney";
   }

   @Override
   public boolean isDecimal() {
      return false;
   }

   @Override
   public final boolean doWait() {
      return false;
   }

   @Override
   public double round(double amount) {
      return this.formatter.round(amount);
   }

   @Override
   public String formatPrice(double price) {
      return this.formatter.format(this, price);
   }

   @Override
   public final void formatPlaceholders() {
   }
}
