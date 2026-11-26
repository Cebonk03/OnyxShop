package org.quartz.impl.jdbcjobstore;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.DateBuilder;
import org.quartz.TimeOfDay;
import org.quartz.impl.triggers.DailyTimeIntervalTriggerImpl;
import org.quartz.spi.OperableTrigger;

public class DailyTimeIntervalTriggerPersistenceDelegate extends SimplePropertiesTriggerPersistenceDelegateSupport {
   public DailyTimeIntervalTriggerPersistenceDelegate() {
   }

   @Override
   public boolean canHandleTriggerType(OperableTrigger var1) {
      return var1 instanceof DailyTimeIntervalTrigger && !((DailyTimeIntervalTriggerImpl)var1).hasAdditionalProperties();
   }

   @Override
   public String getHandledTriggerTypeDiscriminator() {
      return "DAILY_I";
   }

   @Override
   protected SimplePropertiesTriggerProperties getTriggerProperties(OperableTrigger var1) {
      DailyTimeIntervalTriggerImpl var2 = (DailyTimeIntervalTriggerImpl)var1;
      SimplePropertiesTriggerProperties var3 = new SimplePropertiesTriggerProperties();
      var3.setInt1(var2.getRepeatInterval());
      var3.setString1(var2.getRepeatIntervalUnit().name());
      var3.setInt2(var2.getTimesTriggered());
      Set var4 = var2.getDaysOfWeek();
      String var5 = this.join(var4, ",");
      var3.setString2(var5);
      StringBuilder var6 = new StringBuilder();
      TimeOfDay var7 = var2.getStartTimeOfDay();
      if (var7 != null) {
         var6.append(var7.getHour()).append(",");
         var6.append(var7.getMinute()).append(",");
         var6.append(var7.getSecond()).append(",");
      } else {
         var6.append(",,,");
      }

      TimeOfDay var8 = var2.getEndTimeOfDay();
      if (var8 != null) {
         var6.append(var8.getHour()).append(",");
         var6.append(var8.getMinute()).append(",");
         var6.append(var8.getSecond());
      } else {
         var6.append(",,,");
      }

      var3.setString3(var6.toString());
      var3.setLong1(var2.getRepeatCount());
      return var3;
   }

   private String join(Set<Integer> var1, String var2) {
      StringBuilder var3 = new StringBuilder();
      if (var1 != null && var1.size() > 0) {
         Iterator var4 = var1.iterator();
         var3.append(var4.next());

         while (var4.hasNext()) {
            var3.append(var2).append(var4.next());
         }

         return var3.toString();
      } else {
         return "";
      }
   }

   @Override
   protected TriggerPersistenceDelegate.TriggerPropertyBundle getTriggerPropertyBundle(SimplePropertiesTriggerProperties var1) {
      int var2 = (int)var1.getLong1();
      int var3 = var1.getInt1();
      String var4 = var1.getString1();
      String var5 = var1.getString2();
      String var6 = var1.getString3();
      DateBuilder.IntervalUnit var7 = DateBuilder.IntervalUnit.valueOf(var4);
      DailyTimeIntervalScheduleBuilder var8 = DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule().withInterval(var3, var7).withRepeatCount(var2);
      if (var5 != null) {
         HashSet var9 = new HashSet();
         String[] var10 = var5.split(",");
         if (var10.length > 0) {
            for (String var14 : var10) {
               var9.add(Integer.parseInt(var14));
            }

            var8.onDaysOfTheWeek(var9);
         }
      } else {
         var8.onDaysOfTheWeek(DailyTimeIntervalScheduleBuilder.ALL_DAYS_OF_THE_WEEK);
      }

      if (var6 != null) {
         String[] var15 = var6.split(",");
         TimeOfDay var17;
         if (var15.length >= 3) {
            int var19 = Integer.parseInt(var15[0]);
            int var22 = Integer.parseInt(var15[1]);
            int var24 = Integer.parseInt(var15[2]);
            var17 = new TimeOfDay(var19, var22, var24);
         } else {
            var17 = TimeOfDay.hourMinuteAndSecondOfDay(0, 0, 0);
         }

         var8.startingDailyAt(var17);
         TimeOfDay var20;
         if (var15.length >= 6) {
            int var23 = Integer.parseInt(var15[3]);
            int var25 = Integer.parseInt(var15[4]);
            int var26 = Integer.parseInt(var15[5]);
            var20 = new TimeOfDay(var23, var25, var26);
         } else {
            var20 = TimeOfDay.hourMinuteAndSecondOfDay(23, 59, 59);
         }

         var8.endingDailyAt(var20);
      } else {
         var8.startingDailyAt(TimeOfDay.hourMinuteAndSecondOfDay(0, 0, 0));
         var8.endingDailyAt(TimeOfDay.hourMinuteAndSecondOfDay(23, 59, 59));
      }

      int var16 = var1.getInt2();
      String[] var18 = new String[]{"timesTriggered"};
      Object[] var21 = new Object[]{var16};
      return new TriggerPersistenceDelegate.TriggerPropertyBundle(var8, var18, var21);
   }
}
