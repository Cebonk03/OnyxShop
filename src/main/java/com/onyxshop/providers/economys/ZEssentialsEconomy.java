package com.onyxshop.providers.economys;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.economy.Economy;
import fr.maxlego08.essentials.api.economy.EconomyManager;
import java.math.BigDecimal;
import java.util.Optional;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.EconomyProvider;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.economys.formatter.PriceFormatter;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.EconomyType;
import com.onyxshop.util.exceptions.EconomyLoadException;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class ZEssentialsEconomy implements EconomyProvider {
   private EconomyManager manager;
   private Economy cur;
   private String currency;
   private String friendly;
   private String singular;
   private String plural;
   private final boolean decimal = true;
   private boolean wait = false;
   private PriceFormatter.Format priceFormat;

   public ZEssentialsEconomy(String var1) {
      this.currency = var1;
   }

   @Override
   public void setup(OnyxShop var1) throws EconomyLoadException {
      if (var1.getServer().getPluginManager().isPluginEnabled("zEssentials")) {
         EssentialsPlugin var2 = (EssentialsPlugin)Bukkit.getServer().getPluginManager().getPlugin("zEssentials");
         if (var2 != null) {
            this.registerCurrency(var2.getEconomyManager());
         } else {
            throw new EconomyLoadException("Failed to hook into zEssentials");
         }
      } else {
         throw new EconomyLoadException("Could not find zEssentials");
      }
   }

   private void registerCurrency(EconomyManager var1) throws EconomyLoadException {
      String var2;
      if (this.currency != null && !this.currency.isEmpty()) {
         Optional var3 = var1.getEconomy(this.currency);
         if (!var3.isPresent()) {
            throw new EconomyLoadException("Failed to register a zEssentials currency named as '" + this.currency + "'");
         }

         this.cur = (Economy)var3.get();
         var2 = this.currency;
         SendMessage.infoMessage("Successfully registered currency '" + this.currency + "' from zEssentials");
      } else {
         try {
            this.cur = var1.getDefaultEconomy();
            var2 = this.cur.getName();
            SendMessage.infoMessage("Successfully hooked into zEssentials and using default currency: " + this.cur.getName() + "(" + this.cur.getSymbol() + ")");
         } catch (IndexOutOfBoundsException var4) {
            throw new EconomyLoadException("Failed to find a currency in zEssentials");
         }
      }

      String var5 = ConfigManager.getPathIgnoreCase("currency-formats.zEssentials:" + var2);
      if (!ConfigManager.getConfig().contains(var5)) {
         var5 = ConfigManager.getPathIgnoreCase("currency-formats.zEssentials");
      }

      this.manager = var1;
      this.formatSingular(var5);
      this.formatPlural(var5);
      this.formatFriendly(var5);
      this.priceFormat = PriceFormatter.getFormatter(var5);
   }

   @Override
   public double getBalance(OfflinePlayer var1) {
      return this.manager.getBalance(var1, this.cur).doubleValue();
   }

   @Override
   public void depositBalance(OfflinePlayer var1, double var2) {
      this.manager.deposit(var1.getUniqueId(), this.cur, BigDecimal.valueOf(var2));
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer var1, double var2) {
      this.manager.withdraw(var1.getUniqueId(), this.cur, BigDecimal.valueOf(var2));
      return true;
   }

   @Override
   public EcoType getType() {
      return new EcoType(EconomyType.ZESSENTIALS, this.currency);
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
      String var2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".singular", this.cur.getSymbol()));
      if (this.isSymbol(var2)) {
         this.singular = this.cur.getFormat().replace("%symbol%", var2);
      } else {
         this.singular = this.cur.getFormat().replace("%currency-format%", var2);
      }
   }

   private void formatPlural(String var1) {
      String var2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".plural", this.cur.getSymbol()));
      if (this.isSymbol(var2)) {
         this.plural = this.cur.getFormat().replace("%symbol%", var2);
      } else {
         this.plural = this.cur.getFormat().replace("%currency-format%", var2);
      }
   }

   private void formatFriendly(String var1) {
      this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".friendly", this.cur.getSymbol()));
   }

   private boolean isSymbol(String var1) {
      return var1.chars().count() == 1L;
   }
}
