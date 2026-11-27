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

   public static long getTime(String text) throws ParseException {
      long time = 0L;

      for (String text2 : text.split(" ")) {
         TimeUtils.TimeUnit timeUnit = TimeUtils.TimeUnit.fromFormat(text2.replaceAll("\\d", ""));
         if (timeUnit == null) {
            throw new ParseException("Invalid date object for '" + text2.replaceAll("\\d", "") + "'", 1);
         }

         try {
            time += timeUnit.toMillis(Integer.parseInt(text2.replaceAll("[a-zA-Z]", "")));
         } catch (NumberFormatException exception) {
            throw new ParseException("Could not parse the given date object", 1);
         }
      }

      return time;
   }

   public static String getReadableTime(long time) {
      StringBuilder builder = new StringBuilder();

      for (TimeUtils.TimeUnit timeUnit : TimeUtils.TimeUnit.VALUES) {
         long time2 = time / timeUnit.millis;
         if (time2 != 0L) {
            if (!builder.toString().isEmpty()) {
               builder.append(" ");
            }

            builder.append(time2).append(timeUnit.translated);
            time -= time2 * timeUnit.millis;
         }
      }

      return builder.toString();
   }

   private static String parseTime(long... values) {
      StringBuilder builder = new StringBuilder();

      for (int time = values.length - 1; time > -1; time--) {
         builder.append(values[time]).append(timeFormats[time]);
         if (time != 0) {
            builder.append(" ");
         }
      }

      return builder.toString();
   }

   public static String getDate(long value) {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("E, dd MMM yyyy");
      return simpleDateFormat.format(value);
   }

   public static String getDateTime(long time) {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return simpleDateFormat.format(time);
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

      private TimeUnit(long time, String text, String text2) {
         this.millis = time;
         this.format = text;
         this.translated = text2;
      }

      public static TimeUtils.TimeUnit fromFormat(String text) {
         for (TimeUtils.TimeUnit timeUnit : VALUES) {
            if (timeUnit.format.equals(text)) {
               return timeUnit;
            }
         }

         return null;
      }

      public long toMillis(long time) {
         return this.millis * time;
      }
   }
}
