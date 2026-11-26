package org.quartz.impl.jdbcjobstore;

import java.util.TimeZone;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.DateBuilder;
import org.quartz.impl.triggers.CalendarIntervalTriggerImpl;
import org.quartz.spi.OperableTrigger;

public class CalendarIntervalTriggerPersistenceDelegate extends SimplePropertiesTriggerPersistenceDelegateSupport {
   public CalendarIntervalTriggerPersistenceDelegate() {
   }

   @Override
   public boolean canHandleTriggerType(OperableTrigger var1) {
      return var1 instanceof CalendarIntervalTriggerImpl && !((CalendarIntervalTriggerImpl)var1).hasAdditionalProperties();
   }

   @Override
   public String getHandledTriggerTypeDiscriminator() {
      return "CAL_INT";
   }

   @Override
   protected SimplePropertiesTriggerProperties getTriggerProperties(OperableTrigger var1) {
      CalendarIntervalTriggerImpl var2 = (CalendarIntervalTriggerImpl)var1;
      SimplePropertiesTriggerProperties var3 = new SimplePropertiesTriggerProperties();
      var3.setInt1(var2.getRepeatInterval());
      var3.setString1(var2.getRepeatIntervalUnit().name());
      var3.setInt2(var2.getTimesTriggered());
      var3.setString2(var2.getTimeZone().getID());
      var3.setBoolean1(var2.isPreserveHourOfDayAcrossDaylightSavings());
      var3.setBoolean2(var2.isSkipDayIfHourDoesNotExist());
      return var3;
   }

   @Override
   protected TriggerPersistenceDelegate.TriggerPropertyBundle getTriggerPropertyBundle(SimplePropertiesTriggerProperties var1) {
      TimeZone var2 = null;
      String var3 = var1.getString2();
      if (var3 != null && var3.trim().length() != 0) {
         var2 = TimeZone.getTimeZone(var3);
      }

      CalendarIntervalScheduleBuilder var4 = CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
         .withInterval(var1.getInt1(), DateBuilder.IntervalUnit.valueOf(var1.getString1()))
         .inTimeZone(var2)
         .preserveHourOfDayAcrossDaylightSavings(var1.isBoolean1())
         .skipDayIfHourDoesNotExist(var1.isBoolean2());
      int var5 = var1.getInt2();
      String[] var6 = new String[]{"timesTriggered"};
      Object[] var7 = new Object[]{var5};
      return new TriggerPersistenceDelegate.TriggerPropertyBundle(var4, var6, var7);
   }
}
