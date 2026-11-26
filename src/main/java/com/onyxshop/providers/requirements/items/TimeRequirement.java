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

   public TimeRequirement(String var1, List<String> var2) throws ItemRequirementLoadException {
      String[] var3 = var1.split("-");

      try {
         this.range = new TimeRequirement.TimeRange(var3[0], var3[1]);
      } catch (ArrayIndexOutOfBoundsException var5) {
         throw new ItemRequirementLoadException("Invalid time range for " + var1 + ", should be formatted as 00:00-00:00");
      }

      this.lore = RequirementManager.getLore(var2, Lang.TIME_REQUIREMENT_LORE.get().toString(), this.range.getMin(), this.range.getMax());
   }

   @Override
   public boolean isMet(Player var1) {
      return this.range.isInRange(var1.getWorld().getTime());
   }

   @Override
   public String[] getLore(Player var1, boolean var2, boolean var3) {
      return this.lore.get(var1, var2, var3, "%progress%", this.range.format12(var1.getWorld().getTime()));
   }

   @Override
   public void sendNotMetMessage(Player var1) {
      SendMessage.chatToPlayer(
         var1,
         Lang.TIME_REQUIREMENT_NOT_MET
            .get(var1)
            .replace("%minTime%", this.range.getMin())
            .replace("%maxTime%", this.range.getMax())
            .replace("%progress%", this.range.format12(var1.getWorld().getTime()))
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

      public TimeRange(String nullx, String nullxx) throws ItemRequirementLoadException {
         this.min = this.parse24(nullx);
         this.max = this.parse24(nullxx);
      }

      public boolean isInRange(long var1) {
         return this.min > this.max ? var1 >= this.min || var1 <= this.max : var1 >= this.min && var1 <= this.max;
      }

      public String getMin() {
         return this.format12(this.min);
      }

      public String getMax() {
         return this.format12(this.max);
      }

      private long parse24(String var1) throws ItemRequirementLoadException {
         if (var1.length() == 5 && var1.contains(":")) {
            var1 = var1.replace(":", "");
            if (!var1.matches("^[0-9]{2}[^0-9]?[0-9]{2}$")) {
               throw new ItemRequirementLoadException("Invalud timestamp for " + var1 + ", can only contain numbers");
            } else {
               var1 = var1.toLowerCase(Locale.ENGLISH).replaceAll("[^0-9]", "");
               int var2 = Integer.parseInt(var1.substring(0, 2));
               if (var2 <= 24 && var2 >= 0) {
                  int var3 = Integer.parseInt(var1.substring(2, 4));
                  if (var3 <= 60 && var3 >= 0) {
                     return this.hoursMinutesToTicks(var2, var3);
                  } else {
                     throw new ItemRequirementLoadException("Invalid amount of minutes for timestamp " + var1 + " should be in range of 0-60");
                  }
               } else {
                  throw new ItemRequirementLoadException("Invalid amount of hours for timestamp " + var1 + " should be in range of 0-24");
               }
            }
         } else {
            throw new ItemRequirementLoadException("Invalid timestamp for " + var1 + ", should be formatted as 00:00");
         }
      }

      private long hoursMinutesToTicks(int var1, int var2) {
         long var3 = 18000L;
         var3 += var1 * 1000;
         var3 = (long)(var3 + var2 / 60.0 * 1000.0);
         return var3 % 24000L;
      }

      public String format24(long var1) {
         synchronized (this.SDFTwentyFour) {
            return this.formatDateFormat(var1, this.SDFTwentyFour);
         }
      }

      public String format12(long var1) {
         synchronized (this.SDFTwelve) {
            return this.formatDateFormat(var1, this.SDFTwelve);
         }
      }

      private String formatDateFormat(long var1, SimpleDateFormat var3) {
         Date var4 = this.ticksToDate(var1);
         return var3.format(var4);
      }

      private Date ticksToDate(long var1) {
         var1 = var1 - 18000L + 24000L;
         long var3 = var1 / 24000L;
         var1 -= var3 * 24000L;
         long var5 = var1 / 1000L;
         var1 -= var5 * 1000L;
         long var7 = (long)Math.floor(var1 / 16.666666666666668);
         double var9 = var1 - var7 * 16.666666666666668;
         long var11 = (long)Math.floor(var9 / 0.2777777777777778);
         Calendar var13 = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.ENGLISH);
         var13.setLenient(true);
         var13.set(0, 0, 1, 0, 0, 0);
         var13.add(6, (int)var3);
         var13.add(11, (int)var5);
         var13.add(12, (int)var7);
         var13.add(13, (int)var11 + 1);
         return var13.getTime();
      }
   }
}
