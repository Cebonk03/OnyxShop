package org.quartz;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.quartz.spi.OperableTrigger;

public class TriggerUtils {
   private TriggerUtils() {
   }

   public static List<Date> computeFireTimes(OperableTrigger var0, Calendar var1, int var2) {
      LinkedList var3 = new LinkedList();
      OperableTrigger var4 = (OperableTrigger)var0.clone();
      if (var4.getNextFireTime() == null) {
         var4.computeFirstFireTime(var1);
      }

      for (int var5 = 0; var5 < var2; var5++) {
         Date var6 = var4.getNextFireTime();
         if (var6 == null) {
            break;
         }

         var3.add(var6);
         var4.triggered(var1);
      }

      return Collections.unmodifiableList(var3);
   }

   public static Date computeEndTimeToAllowParticularNumberOfFirings(OperableTrigger var0, Calendar var1, int var2) {
      OperableTrigger var3 = (OperableTrigger)var0.clone();
      if (var3.getNextFireTime() == null) {
         var3.computeFirstFireTime(var1);
      }

      int var4 = 0;
      Date var5 = null;

      for (int var6 = 0; var6 < var2; var6++) {
         Date var7 = var3.getNextFireTime();
         if (var7 == null) {
            break;
         }

         var4++;
         var3.triggered(var1);
         if (var4 == var2) {
            var5 = var7;
         }
      }

      return var5 == null ? null : new Date(var5.getTime() + 1000L);
   }

   public static List<Date> computeFireTimesBetween(OperableTrigger var0, Calendar var1, Date var2, Date var3) {
      LinkedList var4 = new LinkedList();
      OperableTrigger var5 = (OperableTrigger)var0.clone();
      if (var5.getNextFireTime() == null) {
         var5.setStartTime(var2);
         var5.setEndTime(var3);
         var5.computeFirstFireTime(var1);
      }

      while (true) {
         Date var6 = var5.getNextFireTime();
         if (var6 == null) {
            break;
         }

         if (var6.before(var2)) {
            var5.triggered(var1);
         } else {
            if (var6.after(var3)) {
               break;
            }

            var4.add(var6);
            var5.triggered(var1);
         }
      }

      return Collections.unmodifiableList(var4);
   }
}
