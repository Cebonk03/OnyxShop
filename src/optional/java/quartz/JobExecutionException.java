package org.quartz;

public class JobExecutionException extends SchedulerException {
   private static final long serialVersionUID = 1326342535829043325L;
   private boolean refire = false;
   private boolean unscheduleTrigg = false;
   private boolean unscheduleAllTriggs = false;

   public JobExecutionException() {
   }

   public JobExecutionException(Throwable var1) {
      super(var1);
   }

   public JobExecutionException(String var1) {
      super(var1);
   }

   public JobExecutionException(boolean var1) {
      this.refire = var1;
   }

   public JobExecutionException(Throwable var1, boolean var2) {
      super(var1);
      this.refire = var2;
   }

   public JobExecutionException(String var1, Throwable var2) {
      super(var1, var2);
   }

   public JobExecutionException(String var1, Throwable var2, boolean var3) {
      super(var1, var2);
      this.refire = var3;
   }

   public JobExecutionException(String var1, boolean var2) {
      super(var1);
      this.refire = var2;
   }

   public void setRefireImmediately(boolean var1) {
      this.refire = var1;
   }

   public boolean refireImmediately() {
      return this.refire;
   }

   public void setUnscheduleFiringTrigger(boolean var1) {
      this.unscheduleTrigg = var1;
   }

   public boolean unscheduleFiringTrigger() {
      return this.unscheduleTrigg;
   }

   public void setUnscheduleAllTriggers(boolean var1) {
      this.unscheduleAllTriggs = var1;
   }

   public boolean unscheduleAllTriggers() {
      return this.unscheduleAllTriggs;
   }
}
