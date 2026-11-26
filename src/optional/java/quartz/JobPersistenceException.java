package org.quartz;

public class JobPersistenceException extends SchedulerException {
   private static final long serialVersionUID = -8924958757341995694L;

   public JobPersistenceException(String var1) {
      super(var1);
   }

   public JobPersistenceException(String var1, Throwable var2) {
      super(var1, var2);
   }
}
