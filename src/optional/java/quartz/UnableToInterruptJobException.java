package org.quartz;

public class UnableToInterruptJobException extends SchedulerException {
   private static final long serialVersionUID = -490863760696463776L;

   public UnableToInterruptJobException(String var1) {
      super(var1);
   }

   public UnableToInterruptJobException(Throwable var1) {
      super(var1);
   }
}
