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
   public final void setup(OnyxShop var1) throws EconomyLoadException {
      String var2 = "currency-formats." + this.getType().getType().name() + ":" + this.getName();
      String var3 = this.getCurrencyName() != null ? ConfigManager.getPathIgnoreCase(var2 + ":" + this.getCurrencyName()) : null;
      if (var3 == null || !ConfigManager.getConfig().contains(var3)) {
         var3 = ConfigManager.getPathIgnoreCase(var2);
      }

      this.formatter = PriceFormatter.getFormatter(var3);
      SendMessage.infoMessage(
         "Successfully registered economy provider EXTERNAL:"
            + this.getName()
            + (this.getCurrencyName() != null ? " and using currency " + this.getCurrencyName() : "")
      );
   }

   @Override
   public double getBalance(OfflinePlayer var1) {
      return 0.0;
   }

   @Override
   public void depositBalance(OfflinePlayer var1, double var2) {
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer var1, double var2) {
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
   public double round(double var1) {
      return this.formatter.round(var1);
   }

   @Override
   public String formatPrice(double var1) {
      return this.formatter.format(this, var1);
   }

   @Override
   public final void formatPlaceholders() {
   }
}
