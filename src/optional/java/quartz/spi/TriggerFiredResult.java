package org.quartz.spi;

public class TriggerFiredResult {
   private TriggerFiredBundle triggerFiredBundle;
   private Exception exception;

   public TriggerFiredResult(TriggerFiredBundle var1) {
      this.triggerFiredBundle = var1;
   }

   public TriggerFiredResult(Exception var1) {
      this.exception = var1;
   }

   public TriggerFiredBundle getTriggerFiredBundle() {
      return this.triggerFiredBundle;
   }

   public Exception getException() {
      return this.exception;
   }
}
