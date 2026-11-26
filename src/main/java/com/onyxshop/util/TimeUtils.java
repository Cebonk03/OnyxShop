package com.onyxshop.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import com.onyxshop.files.Lang;

public class TimeUtils {
   private static String[] timeFormats = new String[]{
      Lang.TIME_FORMAT_SECONDS.get().getLegacy(),
      Lang.TIME_FORMAT_MINUTES.get().getLegacy(),
      Lang.TIME_FORMAT_HOURS.get().getLegacy(),
      Lang.TIME_FORMAT_DAYS.get().getLegacy()
   };

   public TimeUtils() {
   }

   public static long getTime(String var0) throws ParseException {
      long var1 = 0L;

      for (String var6 : var0.split(" ")) {
         TimeUtils.TimeUnit var7 = TimeUtils.TimeUnit.fromFormat(var6.replaceAll("\\d", ""));
         if (var7 == null) {
            throw new ParseException("Invalid date object for '" + var6.replaceAll("\\d", "") + "'", 1);
         }

         try {
            var1 += var7.toMillis(Integer.parseInt(var6.replaceAll("[a-zA-Z]", "")));
         } catch (NumberFormatException var9) {
            throw new ParseException("Could not parse the given date object", 1);
         }
      }

      return var1;
   }

   public static String getReadableTime(long var0) {
      StringBuilder var2 = new StringBuilder();

      for (TimeUtils.TimeUnit var6 : TimeUtils.TimeUnit.VALUES) {
         long var7 = var0 / var6.millis;
         if (var7 != 0L) {
            if (!var2.toString().isEmpty()) {
               var2.append(" ");
            }

            var2.append(var7).append(var6.translated);
            var0 -= var7 * var6.millis;
         }
      }

      return var2.toString();
   }

   private static String parseTime(long... var0) {
      StringBuilder var1 = new StringBuilder();

      for (int var2 = var0.length - 1; var2 > -1; var2--) {
         var1.append(var0[var2]).append(timeFormats[var2]);
         if (var2 != 0) {
            var1.append(" ");
         }
      }

      return var1.toString();
   }

   public static String getDate(long var0) {
      SimpleDateFormat var2 = new SimpleDateFormat("E, dd MMM yyyy");
      return var2.format(var0);
   }

   public static String getDateTime(long var0) {
      SimpleDateFormat var2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return var2.format(var0);
   }

   private static enum TimeUnit {
      SECONDS(1000L, "s", Lang.TIME_FORMAT_SECONDS.get().getLegacy()),
      MINUTES(SECONDS.toMillis(60L), "m", Lang.TIME_FORMAT_MINUTES.get().getLegacy()),
      HOURS(MINUTES.toMillis(60L), "h", Lang.TIME_FORMAT_HOURS.get().getLegacy()),
      DAYS(HOURS.toMillis(24L), "d", Lang.TIME_FORMAT_DAYS.get().getLegacy()),
      WEEKS(DAYS.toMillis(7L), "w", "w"),
      MONTHS(DAYS.toMillis(30L), "mo", "mo");

      private final long millis;
      public final String format;
      public final String translated;
      public static final TimeUtils.TimeUnit[] VALUES = new TimeUtils.TimeUnit[]{MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS};

      private TimeUnit(long nullxx, String nullxxx, String nullxxxx) {
         this.millis = nullxx;
         this.format = nullxxx;
         this.translated = nullxxxx;
      }

      public static TimeUtils.TimeUnit fromFormat(String var0) {
         for (TimeUtils.TimeUnit var4 : VALUES) {
            if (var4.format.equals(var0)) {
               return var4;
            }
         }

         return null;
      }

      public long toMillis(long var1) {
         return this.millis * var1;
      }
   }
}
