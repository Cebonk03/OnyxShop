package com.onyxshop.providers.requirements.items;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.RequirementManager;
import com.onyxshop.util.exceptions.ItemRequirementLoadException;
import org.bukkit.entity.Player;

public class TimeRequirement implements com.onyxshop.providers.requirements.ItemRequirement {
   private final TimeRequirement.TimeRange range;
   private final RequirementManager.RequirementLore lore;

   public TimeRequirement(String itemId, List<String> values) throws ItemRequirementLoadException {
      String[] values2 = itemId.split("-");

      try {
         this.range = new TimeRequirement.TimeRange(values2[0], values2[1]);
      } catch (ArrayIndexOutOfBoundsException exception) {
         throw new ItemRequirementLoadException("Invalid time range for " + itemId + ", should be formatted as 00:00-00:00");
      }

      this.lore = RequirementManager.getLore(values, Lang.TIME_REQUIREMENT_LORE.get().toString(), this.range.getMin(), this.range.getMax());
   }

   @Override
   public boolean isMet(Player player) {
      return this.range.isInRange(player.getWorld().getTime());
   }

   @Override
   public String[] getLore(Player player, boolean flag, boolean flag2) {
      return this.lore.get(player, flag, flag2, "%progress%", this.range.format12(player.getWorld().getTime()));
   }

   @Override
   public void sendNotMetMessage(Player player) {
      SendMessage.chatToPlayer(
         player,
         Lang.TIME_REQUIREMENT_NOT_MET
            .get(player)
            .replace("%minTime%", this.range.getMin())
            .replace("%maxTime%", this.range.getMax())
            .replace("%progress%", this.range.format12(player.getWorld().getTime()))
      );
   }

   private class TimeRange {
      private final int ticksAtMidnight = 18000;
      private final int ticksPerDay = 24000;
      private final int ticksPerHour = 1000;
      private final double ticksPerMinute = 16.666666666666668;
      private final double ticksPerSecond = 0.2777777777777778;
      private final SimpleDateFormat SDFTwentyFour = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
      private final SimpleDateFormat SDFTwelve = new SimpleDateFormat("h:mm aa", Locale.ENGLISH);
      private final long min;
      private final long max;

      public TimeRange(String itemId, String text) throws ItemRequirementLoadException {
         this.min = this.parse24(itemId);
         this.max = this.parse24(text);
      }

      public boolean isInRange(long value) {
         return this.min > this.max ? value >= this.min || value <= this.max : value >= this.min && value <= this.max;
      }

      public String getMin() {
         return this.format12(this.min);
      }

      public String getMax() {
         return this.format12(this.max);
      }

      private long parse24(String itemId) throws ItemRequirementLoadException {
         if (itemId.length() == 5 && itemId.contains(":")) {
            itemId = itemId.replace(":", "");
            if (!itemId.matches("^[0-9]{2}[^0-9]?[0-9]{2}$")) {
               throw new ItemRequirementLoadException("Invalud timestamp for " + itemId + ", can only contain numbers");
            } else {
               itemId = itemId.toLowerCase(Locale.ENGLISH).replaceAll("[^0-9]", "");
               int value = Integer.parseInt(itemId.substring(0, 2));
               if (value <= 24 && value >= 0) {
                  int value2 = Integer.parseInt(itemId.substring(2, 4));
                  if (value2 <= 60 && value2 >= 0) {
                     return this.hoursMinutesToTicks(value, value2);
                  } else {
                     throw new ItemRequirementLoadException("Invalid amount of minutes for timestamp " + itemId + " should be in range of 0-60");
                  }
               } else {
                  throw new ItemRequirementLoadException("Invalid amount of hours for timestamp " + itemId + " should be in range of 0-24");
               }
            }
         } else {
            throw new ItemRequirementLoadException("Invalid timestamp for " + itemId + ", should be formatted as 00:00");
         }
      }

      private long hoursMinutesToTicks(int value, int value2) {
         long value3 = 18000L;
         value3 += value * 1000;
         value3 = (long)(value3 + value2 / 60.0 * 1000.0);
         return value3 % 24000L;
      }

      public String format24(long value) {
         synchronized (this.SDFTwentyFour) {
            return this.formatDateFormat(value, this.SDFTwentyFour);
         }
      }

      public String format12(long value) {
         synchronized (this.SDFTwelve) {
            return this.formatDateFormat(value, this.SDFTwelve);
         }
      }

      private String formatDateFormat(long value, SimpleDateFormat simpleDateFormat) {
         Date date = this.ticksToDate(value);
         return simpleDateFormat.format(date);
      }

      private Date ticksToDate(long value) {
         value = value - 18000L + 24000L;
         long value2 = value / 24000L;
         value -= value2 * 24000L;
         long value3 = value / 1000L;
         value -= value3 * 1000L;
         long value4 = (long)Math.floor(value / 16.666666666666668);
         double amount = value - value4 * 16.666666666666668;
         long value5 = (long)Math.floor(amount / 0.2777777777777778);
         Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.ENGLISH);
         calendar.setLenient(true);
         calendar.set(0, 0, 1, 0, 0, 0);
         calendar.add(6, (int)value2);
         calendar.add(11, (int)value3);
         calendar.add(12, (int)value4);
         calendar.add(13, (int)value5 + 1);
         return calendar.getTime();
      }
   }
}
