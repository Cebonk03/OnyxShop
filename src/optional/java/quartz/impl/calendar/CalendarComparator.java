package org.quartz.impl.calendar;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Comparator;

class CalendarComparator implements Comparator<Calendar>, Serializable {
   private static final long serialVersionUID = 7346867105876610961L;

   public CalendarComparator() {
   }

   public int compare(Calendar var1, Calendar var2) {
      int var3 = var1.get(2);
      int var4 = var2.get(2);
      int var5 = var1.get(5);
      int var6 = var2.get(5);
      if (var3 < var4) {
         return -1;
      } else if (var3 > var4) {
         return 1;
      } else if (var5 < var6) {
         return -1;
      } else {
         return var5 > var6 ? 1 : 0;
      }
   }
}
