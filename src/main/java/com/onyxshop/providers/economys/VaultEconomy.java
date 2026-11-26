package com.onyxshop.providers.economys;

import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.EconomyProvider;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.economys.formatter.PriceFormatter;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.EconomyType;
import com.onyxshop.util.exceptions.EconomyLoadException;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomy implements EconomyProvider {
   private Economy economy;
   private String friendly;
   private String singular;
   private String plural;
   private boolean decimal;
   private boolean wait = false;
   private PriceFormatter.Format priceFormat;

   public VaultEconomy() {
   }

   @Override
   public void setup(OnyxShop var1) throws EconomyLoadException {
      if (var1.getServer().getPluginManager().getPlugin("Vault") == null) {
         throw new EconomyLoadException("Vault is not installed");
      } else {
         RegisteredServiceProvider var2 = var1.getServer().getServicesManager().getRegistration(Economy.class);
         if (var2 == null) {
            throw new EconomyLoadException("No supported economy provider for Vault detected, please install a plugin such as Essentials");
         } else {
            this.economy = (Economy)var2.getProvider();
            if (!Bukkit.getPluginManager().isPluginEnabled(this.economy.getName()) && "TheNewEconomy".equals(this.economy.getName())) {
               this.wait = true;
               var1.runTaskLater(this::init, 20L);
            } else {
               this.init();
            }
         }
      }
   }

   private void init() {
      if (this.wait && !Bukkit.getPluginManager().isPluginEnabled(this.economy.getName())) {
         this.wait = false;
         SendMessage.warnMessage(
            this.economy.getName() + " still didn't finish loading, aborting without " + this.economy.getName() + "... Please make sure it is up-to date."
         );
         OnyxShop.getInstance().getEcoHandler().mayContinue();
      } else {
         String var1 = ConfigManager.getPathIgnoreCase("currency-formats.vault");
         this.formatSingular(var1);
         this.formatPlural(var1);
         this.decimal = this.economy.fractionalDigits() == -1 || this.economy.fractionalDigits() > 0;
         this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".friendly", "money"));
         this.priceFormat = PriceFormatter.getFormatter(var1);
         SendMessage.infoMessage("Successfully hooked into Vault and using " + this.economy.getName() + " as economy provider");
         if (this.wait) {
            this.wait = false;
            OnyxShop.getInstance().getEcoHandler().mayContinue();
         }
      }
   }

   @Override
   public double getBalance(OfflinePlayer var1) {
      return this.economy.getBalance(var1);
   }

   @Override
   public void depositBalance(OfflinePlayer var1, double var2) {
      this.economy.depositPlayer(var1, var2);
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer var1, double var2) {
      return this.economy.withdrawPlayer(var1, var2).transactionSuccess();
   }

   @Override
   public EcoType getType() {
      return new EcoType(EconomyType.VAULT);
   }

   @Override
   public String getPlural() {
      return this.plural;
   }

   @Override
   public String getSingular() {
      return this.singular;
   }

   @Override
   public String getFriendly() {
      return this.friendly;
   }

   @Override
   public boolean isDecimal() {
      return this.decimal;
   }

   @Override
   public boolean doWait() {
      return this.wait;
   }

   @Override
   public double round(double var1) {
      return this.priceFormat.round(var1);
   }

   @Override
   public String formatPrice(double var1) {
      return this.priceFormat.format(this, var1);
   }

   @Override
   public void formatPlaceholders() {
      this.singular = PlaceholderAPIHook.translate(null, this.singular);
      this.plural = PlaceholderAPIHook.translate(null, this.plural);
      this.friendly = PlaceholderAPIHook.translate(null, this.friendly);
   }

   private void formatSingular(String var1) {
      String var2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".singular", this.economy.currencyNameSingular()));
      if (!var2.isEmpty()) {
         if (!var2.contains("%currency-symbol%")) {
            if (this.isSymbol(var2)) {
               this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var2).getLegacy();
            } else {
               this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var2).getLegacy();
            }
         } else {
            this.singular = Lang.CURRENCYSYMBOL.get().getLegacy();
         }
      } else {
         this.singular = Lang.CURRENCYSYMBOL.get().getLegacy();
      }
   }

   private void formatPlural(String var1) {
      String var2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".plural", this.economy.currencyNamePlural()));
      if (!var2.isEmpty()) {
         if (!var2.contains("%currency-symbol%")) {
            if (this.isSymbol(var2)) {
               this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var2).getLegacy();
            } else {
               this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var2).getLegacy();
            }
         } else {
            this.plural = Lang.CURRENCYSYMBOL.get().getLegacy();
         }
      } else {
         this.plural = Lang.CURRENCYSYMBOL.get().getLegacy();
      }
   }

   private boolean isSymbol(String var1) {
      return var1.chars().count() == 1L;
   }
}
