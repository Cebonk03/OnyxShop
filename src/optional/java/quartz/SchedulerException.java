package org.quartz;

public class SchedulerException extends Exception {
   private static final long serialVersionUID = 174841398690789156L;

   public SchedulerException() {
   }

   public SchedulerException(String var1) {
      super(var1);
   }

   public SchedulerException(Throwable var1) {
      super(var1);
   }

   public SchedulerException(String var1, Throwable var2) {
      super(var1, var2);
   }

   public Throwable getUnderlyingException() {
      return super.getCause();
   }

   @Override
   public String toString() {
      Throwable var1 = this.getUnderlyingException();
      return var1 != null && var1 != this ? super.toString() + " [See nested exception: " + var1 + "]" : super.toString();
   }
}
