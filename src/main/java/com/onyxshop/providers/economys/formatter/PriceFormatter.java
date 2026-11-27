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
      Locale locale = Locale.forLanguageTag(ConfigManager.getConfig().getString("locale", "en-US"));
      NumberFormat numberFormat = getCurrencyFormat(locale, ConfigManager.getConfig().getString("currency-format", "#,##0.00"), "currency-format");
      List<String> magnitudes = getMagnitudes((String)null);
      return (PriceFormatter.Format)(magnitudes.isEmpty()
         ? new PriceFormatter.LongFormat(locale, numberFormat)
         : new PriceFormatter.ShortFormat(locale, numberFormat, magnitudes.toArray(new String[0])));
   }

   public static PriceFormatter.Format getFormatter(String price) {
      if (!ConfigManager.getConfig().contains(price + ".format")
         && !ConfigManager.getConfig().contains(price + ".locale")
         && !ConfigManager.getConfig().contains(price + ".abbreviations")) {
         return EconomyHandler.getDefaultFormatter();
      } else {
         Locale locale = Locale.forLanguageTag(getLocale(price));
         NumberFormat numberFormat = getCurrencyFormat(locale, getFormat(price), price + ".format");
         List<String> magnitudes = getMagnitudes(price);
         return (PriceFormatter.Format)(magnitudes.isEmpty()
            ? new PriceFormatter.LongFormat(locale, numberFormat)
            : new PriceFormatter.ShortFormat(locale, numberFormat, magnitudes.toArray(new String[0])));
      }
   }

   private static String getFormat(String text) {
      String text2 = ConfigManager.getConfig().getString("currency-format", "#,##0.00");
      return ConfigManager.getConfig().getString(text + ".format", text2);
   }

   private static String getLocale(String text) {
      String text2 = ConfigManager.getConfig().getString("locale", "en-US");
      return ConfigManager.getConfig().getString(text + ".locale", text2);
   }

   private static List<String> getMagnitudes(@Nullable String text) {
      if (text == null || !ConfigManager.getConfig().contains(text + ".abbreviations.enabled")) {
         return (List<String>)(ConfigManager.getConfig().getBoolean("abbreviations.enabled")
            ? ConfigManager.getConfig().getStringList("abbreviations.formats")
            : new ArrayList<>());
      } else {
         return (List<String>)(ConfigManager.getConfig().getBoolean(text + ".abbreviations.enabled")
            ? ConfigManager.getConfig().getStringList(text + ".abbreviations.formats")
            : new ArrayList<>());
      }
   }

   private static List<String> getMagnitudes(ConfigurationSection section) {
      ArrayList values = new ArrayList();

      for (String text : section.getKeys(false)) {
         values.add(section.getString(text));
      }

      return values;
   }

   private static NumberFormat getCurrencyFormat(Locale locale, String text, String text2) {
      DecimalFormat decimalFormat;
      try {
         decimalFormat = new DecimalFormat(text, DecimalFormatSymbols.getInstance(locale));
      } catch (IllegalArgumentException exception) {
         SendMessage.warnMessage("Could not load currency format of '" + text + "' at " + text2 + ", with reason: " + exception.getMessage());
         decimalFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(locale));
      }

      decimalFormat.setRoundingMode(RoundingMode.FLOOR);
      return decimalFormat;
   }

   public interface Format {
      String format(EconomyProvider economy, double amount);

      double round(double amount);
   }

   public static final class LongFormat implements PriceFormatter.Format {
      private final int decimalPoints;
      private final Locale locale;
      private final NumberFormat format;

      public LongFormat(Locale locale, NumberFormat numberFormat) {
         this.locale = locale;
         this.format = numberFormat;
         this.decimalPoints = numberFormat.getMaximumFractionDigits();
      }

      @Override
      public String format(EconomyProvider economy, double amount) {
         return !economy.isDecimal()
            ? (amount == 1.0 ? economy.getSingular() : economy.getPlural()).replace("%price%", String.format(this.locale, "%,.0f", (double)Math.round(amount)))
            : (amount == 1.0 ? economy.getSingular() : economy.getPlural()).replace("%price%", this.format.format(amount));
      }

      @Override
      public double round(double amount) {
         long value = (long)Math.pow(10.0, this.decimalPoints);
         amount *= value;
         long value2 = Math.round(amount);
         return (double)value2 / value;
      }
   }

   public static final class ShortFormat implements PriceFormatter.Format {
      private final int decimalPoints;
      private final Locale locale;
      private final NumberFormat format;
      private final String[] magnitudes;

      public ShortFormat(Locale locale, NumberFormat numberFormat, String[] values) {
         this.locale = locale;
         this.format = numberFormat;
         this.magnitudes = values;
         this.decimalPoints = numberFormat.getMaximumFractionDigits();
      }

      @Override
      public String format(EconomyProvider economy, double amount) {
         if (amount >= 1000.0) {
            return economy.getPlural().replace("%price%", this.formatShort(amount));
         } else {
            return !economy.isDecimal()
               ? (amount == 1.0 ? economy.getSingular() : economy.getPlural()).replace("%price%", String.format(this.locale, "%,.0f", (double)Math.round(amount)))
               : (amount == 1.0 ? economy.getSingular() : economy.getPlural()).replace("%price%", this.format.format(amount));
         }
      }

      @Override
      public double round(double amount) {
         long value = (long)Math.pow(10.0, this.decimalPoints);
         amount *= value;
         long value2 = Math.round(amount);
         return (double)value2 / value;
      }

      private String formatShort(double amount) {
         int value = 0;

         while (true) {
            amount /= 1000.0;
            if (amount < 1000.0) {
               return amount == (int)amount ? (int)amount + this.magnitudes[value] : String.format(this.locale, "%,.1f", amount) + this.magnitudes[value];
            }

            value++;
         }
      }
   }
}
