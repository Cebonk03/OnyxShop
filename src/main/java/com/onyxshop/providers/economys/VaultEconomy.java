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
   public void setup(OnyxShop plugin) throws EconomyLoadException {
      if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
         throw new EconomyLoadException("Vault is not installed");
      } else {
         RegisteredServiceProvider registeredServiceProvider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
         if (registeredServiceProvider == null) {
            throw new EconomyLoadException("No supported economy provider for Vault detected, please install a plugin such as Essentials");
         } else {
            this.economy = (Economy)registeredServiceProvider.getProvider();
            if (!Bukkit.getPluginManager().isPluginEnabled(this.economy.getName()) && "TheNewEconomy".equals(this.economy.getName())) {
               this.wait = true;
               plugin.runTaskLater(this::init, 20L);
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
         String pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.vault");
         this.formatSingular(pathIgnoreCase);
         this.formatPlural(pathIgnoreCase);
         this.decimal = this.economy.fractionalDigits() == -1 || this.economy.fractionalDigits() > 0;
         this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(pathIgnoreCase + ".friendly", "money"));
         this.priceFormat = PriceFormatter.getFormatter(pathIgnoreCase);
         SendMessage.infoMessage("Successfully hooked into Vault and using " + this.economy.getName() + " as economy provider");
         if (this.wait) {
            this.wait = false;
            OnyxShop.getInstance().getEcoHandler().mayContinue();
         }
      }
   }

   @Override
   public double getBalance(OfflinePlayer offlinePlayer) {
      return this.economy.getBalance(offlinePlayer);
   }

   @Override
   public void depositBalance(OfflinePlayer offlinePlayer, double balance) {
      this.economy.depositPlayer(offlinePlayer, balance);
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance) {
      return this.economy.withdrawPlayer(offlinePlayer, balance).transactionSuccess();
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
   public double round(double amount) {
      return this.priceFormat.round(amount);
   }

   @Override
   public String formatPrice(double price) {
      return this.priceFormat.format(this, price);
   }

   @Override
   public void formatPlaceholders() {
      this.singular = PlaceholderAPIHook.translate(null, this.singular);
      this.plural = PlaceholderAPIHook.translate(null, this.plural);
      this.friendly = PlaceholderAPIHook.translate(null, this.friendly);
   }

   private void formatSingular(String text) {
      String name = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".singular", this.economy.currencyNameSingular()));
      if (!name.isEmpty()) {
         if (!name.contains("%currency-symbol%")) {
            if (this.isSymbol(name)) {
               this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", name).getLegacy();
            } else {
               this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", name).getLegacy();
            }
         } else {
            this.singular = Lang.CURRENCYSYMBOL.get().getLegacy();
         }
      } else {
         this.singular = Lang.CURRENCYSYMBOL.get().getLegacy();
      }
   }

   private void formatPlural(String text) {
      String name = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".plural", this.economy.currencyNamePlural()));
      if (!name.isEmpty()) {
         if (!name.contains("%currency-symbol%")) {
            if (this.isSymbol(name)) {
               this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", name).getLegacy();
            } else {
               this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", name).getLegacy();
            }
         } else {
            this.plural = Lang.CURRENCYSYMBOL.get().getLegacy();
         }
      } else {
         this.plural = Lang.CURRENCYSYMBOL.get().getLegacy();
      }
   }

   private boolean isSymbol(String text) {
      return text.chars().count() == 1L;
   }
}
