package com.onyxshop.providers.economys;

import java.util.Optional;
import me.TechsCode.UltraEconomy.UltraEconomyAPI;
import me.TechsCode.UltraEconomy.objects.Account;
import me.TechsCode.UltraEconomy.objects.Currency;
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
import org.bukkit.OfflinePlayer;

public class UltraEconomy implements EconomyProvider {
   private UltraEconomyAPI ue;
   private Currency cur;
   private String currency;
   private String friendly;
   private String singular;
   private String plural;
   private final boolean decimal = true;
   private boolean wait = false;
   private PriceFormatter.Format priceFormat;

   public UltraEconomy(String var1) {
      this.currency = var1;
   }

   @Override
   public void setup(OnyxShop var1) throws EconomyLoadException {
      if (var1.getServer().getPluginManager().isPluginEnabled("UltraEconomy")) {
         this.ue = me.TechsCode.UltraEconomy.UltraEconomy.getAPI();
         if (this.ue != null) {
            this.registerCurrency();
         } else {
            throw new EconomyLoadException("Failed to hook into UltraEconomy");
         }
      } else {
         throw new EconomyLoadException("Could not find UltraEconomy");
      }
   }

   private void registerCurrency() throws EconomyLoadException {
      String var1;
      if (this.currency != null && !this.currency.isEmpty()) {
         Optional var2 = this.ue.getCurrencies().name(this.currency);
         if (!var2.isPresent()) {
            throw new EconomyLoadException("Failed to register a UltraEconomy currency named as '" + this.currency + "'");
         }

         this.cur = (Currency)var2.get();
         var1 = this.currency;
         SendMessage.infoMessage("Successfully registered currency '" + this.currency + "' from UltraEconomy");
      } else {
         try {
            this.cur = (Currency)this.ue.getCurrencies().get(0);
            var1 = this.cur.getName();
            SendMessage.infoMessage("Successfully hooked into UltraEconomy and using default currency: " + this.cur.getKey() + "(" + this.cur.getName() + ")");
         } catch (IndexOutOfBoundsException var3) {
            throw new EconomyLoadException("Failed to find a currency in UltraEconomy");
         }
      }

      String var4 = ConfigManager.getPathIgnoreCase("currency-formats.ultra-economy:" + var1);
      if (!ConfigManager.getConfig().contains(var4)) {
         var4 = ConfigManager.getPathIgnoreCase("currency-formats.ultra-economy");
      }

      this.formatSingular(var4);
      this.formatPlural(var4);
      this.formatFriendly(var4);
      this.priceFormat = PriceFormatter.getFormatter(var4);
   }

   @Override
   public double getBalance(OfflinePlayer var1) {
      Optional var2 = this.ue.getAccounts().uuid(var1.getUniqueId());
      if (var2.isPresent()) {
         return ((Account)var2.get()).getBalance(this.cur).getOnHand();
      } else {
         SendMessage.warnMessage("UltraEconomy Failed to retrieve the account from player '" + var1.getName() + "'");
         return 0.0;
      }
   }

   @Override
   public void depositBalance(OfflinePlayer var1, double var2) {
      Optional var4 = this.ue.getAccounts().uuid(var1.getUniqueId());
      if (var4.isPresent()) {
         ((Account)var4.get()).getBalance(this.cur).addHand((float)var2);
      } else {
         SendMessage.warnMessage("UltraEconomy Failed to retrieve the account from player '" + var1.getName() + "'");
      }
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer var1, double var2) {
      Optional var4 = this.ue.getAccounts().uuid(var1.getUniqueId());
      if (var4.isPresent()) {
         ((Account)var4.get()).getBalance(this.cur).removeHand((float)var2);
         return true;
      } else {
         SendMessage.warnMessage("UltraEconomy Failed to retrieve the account from player '" + var1.getName() + "'");
         return false;
      }
   }

   @Override
   public EcoType getType() {
      return new EcoType(EconomyType.ULTRA_ECONOMY, this.currency);
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
      return true;
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
      String var2 = ChatUtil.formatColors(
         ConfigManager.getConfig()
            .getString(var1 + ".singular", this.cur.getFormat().getSingularFormat().replace("{Amount} ", "").replace(" {Amount}", "").replace("{Amount}", ""))
      );
      if (this.isSymbol(var2)) {
         this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var2).getLegacy();
      } else {
         this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var2).getLegacy();
      }
   }

   private void formatPlural(String var1) {
      String var2 = ChatUtil.formatColors(
         ConfigManager.getConfig()
            .getString(var1 + ".plural", this.cur.getFormat().getPluralFormat().replace("{Amount} ", "").replace(" {Amount}", "").replace("{Amount}", ""))
      );
      if (this.isSymbol(var2)) {
         this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var2).getLegacy();
      } else {
         this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var2).getLegacy();
      }
   }

   private void formatFriendly(String var1) {
      this.friendly = ChatUtil.formatColors(
         ConfigManager.getConfig()
            .getString(var1 + ".friendly", this.cur.getFormat().getPluralFormat().replace("{Amount} ", "").replace(" {Amount}", "").replace("{Amount}", ""))
      );
   }

   private boolean isSymbol(String var1) {
      return var1.chars().count() == 1L;
   }
}
