package com.onyxshop.providers.economys;

import java.math.BigDecimal;
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
import net.milkbowl.vault2.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultUnlockedEconomy implements EconomyProvider {
   private static final String PLUGIN = OnyxShop.getInstance().getName();
   private static boolean MULTI_CURRENCY = false;
   private Economy economy;
   private String currency;
   private String friendly;
   private String singular;
   private String plural;
   private boolean decimal;
   private boolean wait = false;
   private PriceFormatter.Format priceFormat;

   public VaultUnlockedEconomy(String var1) {
      this.currency = var1;
   }

   @Override
   public void setup(OnyxShop var1) throws EconomyLoadException {
      if (var1.getServer().getPluginManager().getPlugin("Vault") == null) {
         throw new EconomyLoadException("VaultUnlocked is not installed");
      } else {
         RegisteredServiceProvider var2 = var1.getServer().getServicesManager().getRegistration(Economy.class);
         if (var2 == null) {
            throw new EconomyLoadException("No supported economy provider for VaultUnlocked detected, please install a plugin such as LiteEco");
         } else {
            this.economy = (Economy)var2.getProvider();
            MULTI_CURRENCY = this.economy.hasMultiCurrencySupport();
            if ((Bukkit.getPluginManager().isPluginEnabled(this.economy.getName()) || !"TheNewEconomy".equals(this.economy.getName()))
               && (!MULTI_CURRENCY || !this.economy.currencies().isEmpty())) {
               this.init();
            } else {
               this.wait = true;
               var1.runTaskLater(this::init, 20L);
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
         if (MULTI_CURRENCY) {
            try {
               this.registerCurrency();
            } catch (EconomyLoadException var2) {
               SendMessage.warnMessage(var2.getMessage());
               if (this.wait) {
                  this.wait = false;
                  OnyxShop.getInstance().getEcoHandler().mayContinue();
               }

               return;
            }
         } else {
            String var1 = ConfigManager.getPathIgnoreCase("currency-formats.vaultunlocked");
            this.formatSingular(var1);
            this.formatPlural(var1);
            this.formatFriendly(var1);
            this.decimal = this.economy.fractionalDigits(PLUGIN) == -1 || this.economy.fractionalDigits(PLUGIN) > 0;
            this.priceFormat = PriceFormatter.getFormatter(var1);
         }

         if (this.wait) {
            this.wait = false;
            OnyxShop.getInstance().getEcoHandler().mayContinue();
         }
      }
   }

   private void registerCurrency() throws EconomyLoadException {
      String var1;
      if (this.currency != null && !this.currency.isEmpty()) {
         var1 = this.currency;
         if (!this.economy.hasCurrency(this.currency)) {
            throw new EconomyLoadException("Failed to register a " + this.economy.getName() + " currency named as '" + this.currency + "' thru VaultUnlocked");
         }

         SendMessage.infoMessage("Successfully registered currency '" + this.currency + "' from " + this.economy.getName() + " thru VaultUnlocked");
      } else {
         var1 = this.economy.getDefaultCurrency(PLUGIN);
         if (!this.economy.hasCurrency(var1)) {
            throw new EconomyLoadException("Failed to find default currency from " + this.economy.getName() + " thru VaultUnlocked");
         }

         SendMessage.infoMessage("Successfully hooked into " + this.economy.getName() + " thru VaultUnlocked and using default currency: " + var1);
         this.currency = var1;
      }

      String var2 = ConfigManager.getPathIgnoreCase("currency-formats.vaultunlocked:" + var1);
      if (!ConfigManager.getConfig().contains(var2)) {
         var2 = ConfigManager.getPathIgnoreCase("currency-formats.vaultunlocked");
      }

      this.formatSingular(var2);
      this.formatPlural(var2);
      this.formatFriendly(var2);
      this.decimal = this.economy.fractionalDigits(PLUGIN, this.currency) == -1 || this.economy.fractionalDigits(PLUGIN, this.currency) > 0;
      this.priceFormat = PriceFormatter.getFormatter(var2);
   }

   @Override
   public double getBalance(OfflinePlayer var1) {
      return MULTI_CURRENCY
         ? this.economy.balance(PLUGIN, var1.getUniqueId(), this.getWorld(var1.getLocation()), this.currency).doubleValue()
         : this.economy.balance(PLUGIN, var1.getUniqueId(), this.getWorld(var1.getLocation())).doubleValue();
   }

   @Override
   public void depositBalance(OfflinePlayer var1, double var2) {
      if (MULTI_CURRENCY) {
         this.economy.deposit(PLUGIN, var1.getUniqueId(), this.getWorld(var1.getLocation()), this.currency, new BigDecimal(var2));
      } else {
         this.economy.deposit(PLUGIN, var1.getUniqueId(), this.getWorld(var1.getLocation()), new BigDecimal(var2));
      }
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer var1, double var2) {
      if (MULTI_CURRENCY) {
         this.economy.withdraw(PLUGIN, var1.getUniqueId(), this.getWorld(var1.getLocation()), this.currency, new BigDecimal(var2));
      } else {
         this.economy.withdraw(PLUGIN, var1.getUniqueId(), this.getWorld(var1.getLocation()), new BigDecimal(var2));
      }

      return true;
   }

   private String getWorld(Location var1) {
      if (var1 == null) {
         return "";
      } else {
         try {
            return var1.getWorld().getName();
         } catch (IllegalArgumentException | NullPointerException var3) {
            return "";
         }
      }
   }

   @Override
   public EcoType getType() {
      return new EcoType(
         EconomyType.VAULTUNLOCKED,
         this.currency != null && !this.currency.isEmpty() && !this.currency.equals(this.economy.getDefaultCurrency(PLUGIN)) ? this.currency : null
      );
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
      String var2 = this.currency != null && !this.currency.isEmpty() && !this.currency.equals(this.economy.getDefaultCurrency(PLUGIN))
         ? this.currency
         : this.economy.defaultCurrencyNameSingular(PLUGIN);
      String var3 = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".singular", var2));
      if (this.isSymbol(var3)) {
         this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var3).getLegacy();
      } else {
         this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var3).getLegacy();
      }
   }

   private void formatPlural(String var1) {
      String var2 = this.currency != null && !this.currency.isEmpty() && !this.currency.equals(this.economy.getDefaultCurrency(PLUGIN))
         ? this.currency
         : this.economy.defaultCurrencyNamePlural(PLUGIN);
      String var3 = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".plural", var2));
      if (this.isSymbol(var3)) {
         this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var3).getLegacy();
      } else {
         this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var3).getLegacy();
      }
   }

   private void formatFriendly(String var1) {
      String var2 = this.currency != null && !this.currency.isEmpty() && !this.currency.equals(this.economy.getDefaultCurrency(PLUGIN))
         ? this.currency
         : this.economy.defaultCurrencyNamePlural(PLUGIN);
      this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".friendly", var2));
   }

   private boolean isSymbol(String var1) {
      return var1.chars().count() == 1L;
   }
}
