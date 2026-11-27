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

   public VaultUnlockedEconomy(String text) {
      this.currency = text;
   }

   @Override
   public void setup(OnyxShop plugin) throws EconomyLoadException {
      if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
         throw new EconomyLoadException("VaultUnlocked is not installed");
      } else {
         RegisteredServiceProvider registeredServiceProvider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
         if (registeredServiceProvider == null) {
            throw new EconomyLoadException("No supported economy provider for VaultUnlocked detected, please install a plugin such as LiteEco");
         } else {
            this.economy = (Economy)registeredServiceProvider.getProvider();
            MULTI_CURRENCY = this.economy.hasMultiCurrencySupport();
            if ((Bukkit.getPluginManager().isPluginEnabled(this.economy.getName()) || !"TheNewEconomy".equals(this.economy.getName()))
               && (!MULTI_CURRENCY || !this.economy.currencies().isEmpty())) {
               this.init();
            } else {
               this.wait = true;
               plugin.runTaskLater(this::init, 20L);
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
            } catch (EconomyLoadException exception) {
               SendMessage.warnMessage(exception.getMessage());
               if (this.wait) {
                  this.wait = false;
                  OnyxShop.getInstance().getEcoHandler().mayContinue();
               }

               return;
            }
         } else {
            String pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.vaultunlocked");
            this.formatSingular(pathIgnoreCase);
            this.formatPlural(pathIgnoreCase);
            this.formatFriendly(pathIgnoreCase);
            this.decimal = this.economy.fractionalDigits(PLUGIN) == -1 || this.economy.fractionalDigits(PLUGIN) > 0;
            this.priceFormat = PriceFormatter.getFormatter(pathIgnoreCase);
         }

         if (this.wait) {
            this.wait = false;
            OnyxShop.getInstance().getEcoHandler().mayContinue();
         }
      }
   }

   private void registerCurrency() throws EconomyLoadException {
      String text;
      if (this.currency != null && !this.currency.isEmpty()) {
         text = this.currency;
         if (!this.economy.hasCurrency(this.currency)) {
            throw new EconomyLoadException("Failed to register a " + this.economy.getName() + " currency named as '" + this.currency + "' thru VaultUnlocked");
         }

         SendMessage.infoMessage("Successfully registered currency '" + this.currency + "' from " + this.economy.getName() + " thru VaultUnlocked");
      } else {
         text = this.economy.getDefaultCurrency(PLUGIN);
         if (!this.economy.hasCurrency(text)) {
            throw new EconomyLoadException("Failed to find default currency from " + this.economy.getName() + " thru VaultUnlocked");
         }

         SendMessage.infoMessage("Successfully hooked into " + this.economy.getName() + " thru VaultUnlocked and using default currency: " + text);
         this.currency = text;
      }

      String pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.vaultunlocked:" + text);
      if (!ConfigManager.getConfig().contains(pathIgnoreCase)) {
         pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.vaultunlocked");
      }

      this.formatSingular(pathIgnoreCase);
      this.formatPlural(pathIgnoreCase);
      this.formatFriendly(pathIgnoreCase);
      this.decimal = this.economy.fractionalDigits(PLUGIN, this.currency) == -1 || this.economy.fractionalDigits(PLUGIN, this.currency) > 0;
      this.priceFormat = PriceFormatter.getFormatter(pathIgnoreCase);
   }

   @Override
   public double getBalance(OfflinePlayer offlinePlayer) {
      return MULTI_CURRENCY
         ? this.economy.balance(PLUGIN, offlinePlayer.getUniqueId(), this.getWorld(offlinePlayer.getLocation()), this.currency).doubleValue()
         : this.economy.balance(PLUGIN, offlinePlayer.getUniqueId(), this.getWorld(offlinePlayer.getLocation())).doubleValue();
   }

   @Override
   public void depositBalance(OfflinePlayer offlinePlayer, double balance) {
      if (MULTI_CURRENCY) {
         this.economy.deposit(PLUGIN, offlinePlayer.getUniqueId(), this.getWorld(offlinePlayer.getLocation()), this.currency, new BigDecimal(balance));
      } else {
         this.economy.deposit(PLUGIN, offlinePlayer.getUniqueId(), this.getWorld(offlinePlayer.getLocation()), new BigDecimal(balance));
      }
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance) {
      if (MULTI_CURRENCY) {
         this.economy.withdraw(PLUGIN, offlinePlayer.getUniqueId(), this.getWorld(offlinePlayer.getLocation()), this.currency, new BigDecimal(balance));
      } else {
         this.economy.withdraw(PLUGIN, offlinePlayer.getUniqueId(), this.getWorld(offlinePlayer.getLocation()), new BigDecimal(balance));
      }

      return true;
   }

   private String getWorld(Location location) {
      if (location == null) {
         return "";
      } else {
         try {
            return location.getWorld().getName();
         } catch (IllegalArgumentException | NullPointerException exception) {
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
      String text2 = this.currency != null && !this.currency.isEmpty() && !this.currency.equals(this.economy.getDefaultCurrency(PLUGIN))
         ? this.currency
         : this.economy.defaultCurrencyNameSingular(PLUGIN);
      String text3 = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".singular", text2));
      if (this.isSymbol(text3)) {
         this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text3).getLegacy();
      } else {
         this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text3).getLegacy();
      }
   }

   private void formatPlural(String text) {
      String text2 = this.currency != null && !this.currency.isEmpty() && !this.currency.equals(this.economy.getDefaultCurrency(PLUGIN))
         ? this.currency
         : this.economy.defaultCurrencyNamePlural(PLUGIN);
      String text3 = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".plural", text2));
      if (this.isSymbol(text3)) {
         this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text3).getLegacy();
      } else {
         this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text3).getLegacy();
      }
   }

   private void formatFriendly(String text) {
      String text2 = this.currency != null && !this.currency.isEmpty() && !this.currency.equals(this.economy.getDefaultCurrency(PLUGIN))
         ? this.currency
         : this.economy.defaultCurrencyNamePlural(PLUGIN);
      this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".friendly", text2));
   }

   private boolean isSymbol(String text) {
      return text.chars().count() == 1L;
   }
}
