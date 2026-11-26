package com.onyxshop.providers.economys;

import com.bencodez.votingplugin.VotingPluginMain;
import com.bencodez.votingplugin.user.VotingPluginUser;
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

public class VotingPluginEconomy implements EconomyProvider {
   private String friendly;
   private String singular;
   private String plural;
   private boolean wait = false;
   private final boolean decimal = false;
   private PriceFormatter.Format priceFormat;

   public VotingPluginEconomy() {
   }

   @Override
   public void setup(OnyxShop var1) throws EconomyLoadException {
      if (var1.getServer().getPluginManager().getPlugin("VotingPlugin") != null) {
         SendMessage.infoMessage("Successfully hooked into VotingPlugin");
         String var2 = ConfigManager.getPathIgnoreCase("currency-formats.voting-plugin");
         this.formatSingular(var2);
         this.formatPlural(var2);
         this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(var2 + ".friendly", "Points"));
         this.priceFormat = PriceFormatter.getFormatter(var2);
      } else {
         throw new EconomyLoadException("VotingPlugin is not installed");
      }
   }

   @Override
   public double getBalance(OfflinePlayer var1) {
      VotingPluginUser var2 = VotingPluginMain.plugin.getUser(var1.getUniqueId());
      return var2.getPoints();
   }

   @Override
   public void depositBalance(OfflinePlayer var1, double var2) {
      VotingPluginUser var4 = VotingPluginMain.plugin.getUser(var1.getUniqueId());
      var4.setPoints((int)this.getBalance(var1) + (int)Math.round(var2));
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer var1, double var2) {
      VotingPluginUser var4 = VotingPluginMain.plugin.getUser(var1.getUniqueId());
      if (var4 == null) {
         return false;
      }

      var4.removePoints((int)Math.round(var2));
      return true;
   }

   @Override
   public EcoType getType() {
      return new EcoType(EconomyType.VOTING_PLUGIN);
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
      return false;
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
      String var2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".singular", "Point"));
      if (this.isSymbol(var2)) {
         this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var2).getLegacy();
      } else {
         this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var2).getLegacy();
      }
   }

   private void formatPlural(String var1) {
      String var2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".plural", "Points"));
      if (this.isSymbol(var2)) {
         this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var2).getLegacy();
      } else {
         this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var2).getLegacy();
      }
   }

   private boolean isSymbol(String var1) {
      return var1.chars().count() == 1L;
   }
}
