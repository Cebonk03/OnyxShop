package org.quartz;

import org.quartz.utils.Key;

public final class JobKey extends Key<JobKey> {
   private static final long serialVersionUID = -6073883950062574010L;

   public JobKey(String var1) {
      super(var1, null);
   }

   public JobKey(String var1, String var2) {
      super(var1, var2);
   }

   public static JobKey jobKey(String var0) {
      return new JobKey(var0, null);
   }

   public static JobKey jobKey(String var0, String var1) {
      return new JobKey(var0, var1);
   }
}
