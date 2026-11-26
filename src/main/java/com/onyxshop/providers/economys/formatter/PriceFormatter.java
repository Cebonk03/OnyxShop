package com.onyxshop.providers.economys.formatter;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.EconomyProvider;
import com.onyxshop.util.EconomyHandler;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public class PriceFormatter {
   public PriceFormatter() {
   }

   public static PriceFormatter.Format getFormatter() {
      Locale var0 = Locale.forLanguageTag(ConfigManager.getConfig().getString("locale", "en-US"));
      NumberFormat var1 = getCurrencyFormat(var0, ConfigManager.getConfig().getString("currency-format", "#,##0.00"), "currency-format");
      List<String> var2 = getMagnitudes((String)null);
      return (PriceFormatter.Format)(var2.isEmpty()
         ? new PriceFormatter.LongFormat(var0, var1)
         : new PriceFormatter.ShortFormat(var0, var1, var2.toArray(new String[0])));
   }

   public static PriceFormatter.Format getFormatter(String var0) {
      if (!ConfigManager.getConfig().contains(var0 + ".format")
         && !ConfigManager.getConfig().contains(var0 + ".locale")
         && !ConfigManager.getConfig().contains(var0 + ".abbreviations")) {
         return EconomyHandler.getDefaultFormatter();
      } else {
         Locale var1 = Locale.forLanguageTag(getLocale(var0));
         NumberFormat var2 = getCurrencyFormat(var1, getFormat(var0), var0 + ".format");
         List<String> var3 = getMagnitudes(var0);
         return (PriceFormatter.Format)(var3.isEmpty()
            ? new PriceFormatter.LongFormat(var1, var2)
            : new PriceFormatter.ShortFormat(var1, var2, var3.toArray(new String[0])));
      }
   }

   private static String getFormat(String var0) {
      String var1 = ConfigManager.getConfig().getString("currency-format", "#,##0.00");
      return ConfigManager.getConfig().getString(var0 + ".format", var1);
   }

   private static String getLocale(String var0) {
      String var1 = ConfigManager.getConfig().getString("locale", "en-US");
      return ConfigManager.getConfig().getString(var0 + ".locale", var1);
   }

   private static List<String> getMagnitudes(@Nullable String var0) {
      if (var0 == null || !ConfigManager.getConfig().contains(var0 + ".abbreviations.enabled")) {
         return (List<String>)(ConfigManager.getConfig().getBoolean("abbreviations.enabled")
            ? ConfigManager.getConfig().getStringList("abbreviations.formats")
            : new ArrayList<>());
      } else {
         return (List<String>)(ConfigManager.getConfig().getBoolean(var0 + ".abbreviations.enabled")
            ? ConfigManager.getConfig().getStringList(var0 + ".abbreviations.formats")
            : new ArrayList<>());
      }
   }

   private static List<String> getMagnitudes(ConfigurationSection var0) {
      ArrayList var1 = new ArrayList();

      for (String var3 : var0.getKeys(false)) {
         var1.add(var0.getString(var3));
      }

      return var1;
   }

   private static NumberFormat getCurrencyFormat(Locale var0, String var1, String var2) {
      DecimalFormat var3;
      try {
         var3 = new DecimalFormat(var1, DecimalFormatSymbols.getInstance(var0));
      } catch (IllegalArgumentException var5) {
         SendMessage.warnMessage("Could not load currency format of '" + var1 + "' at " + var2 + ", with reason: " + var5.getMessage());
         var3 = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(var0));
      }

      var3.setRoundingMode(RoundingMode.FLOOR);
      return var3;
   }

   public interface Format {
      String format(EconomyProvider var1, double var2);

      double round(double var1);
   }

   public static final class LongFormat implements PriceFormatter.Format {
      private final int decimalPoints;
      private final Locale locale;
      private final NumberFormat format;

      public LongFormat(Locale var1, NumberFormat var2) {
         this.locale = var1;
         this.format = var2;
         this.decimalPoints = var2.getMaximumFractionDigits();
      }

      @Override
      public String format(EconomyProvider var1, double var2) {
         return !var1.isDecimal()
            ? (var2 == 1.0 ? var1.getSingular() : var1.getPlural()).replace("%price%", String.format(this.locale, "%,.0f", (double)Math.round(var2)))
            : (var2 == 1.0 ? var1.getSingular() : var1.getPlural()).replace("%price%", this.format.format(var2));
      }

      @Override
      public double round(double var1) {
         long var3 = (long)Math.pow(10.0, this.decimalPoints);
         var1 *= var3;
         long var5 = Math.round(var1);
         return (double)var5 / var3;
      }
   }

   public static final class ShortFormat implements PriceFormatter.Format {
      private final int decimalPoints;
      private final Locale locale;
      private final NumberFormat format;
      private final String[] magnitudes;

      public ShortFormat(Locale var1, NumberFormat var2, String[] var3) {
         this.locale = var1;
         this.format = var2;
         this.magnitudes = var3;
         this.decimalPoints = var2.getMaximumFractionDigits();
      }

      @Override
      public String format(EconomyProvider var1, double var2) {
         if (var2 >= 1000.0) {
            return var1.getPlural().replace("%price%", this.formatShort(var2));
         } else {
            return !var1.isDecimal()
               ? (var2 == 1.0 ? var1.getSingular() : var1.getPlural()).replace("%price%", String.format(this.locale, "%,.0f", (double)Math.round(var2)))
               : (var2 == 1.0 ? var1.getSingular() : var1.getPlural()).replace("%price%", this.format.format(var2));
         }
      }

      @Override
      public double round(double var1) {
         long var3 = (long)Math.pow(10.0, this.decimalPoints);
         var1 *= var3;
         long var5 = Math.round(var1);
         return (double)var5 / var3;
      }

      private String formatShort(double var1) {
         int var3 = 0;

         while (true) {
            var1 /= 1000.0;
            if (var1 < 1000.0) {
               return var1 == (int)var1 ? (int)var1 + this.magnitudes[var3] : String.format(this.locale, "%,.1f", var1) + this.magnitudes[var3];
            }

            var3++;
         }
      }
   }
}
