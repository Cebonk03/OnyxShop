package org.quartz.core;

import org.quartz.SchedulerException;
import org.quartz.listeners.SchedulerListenerSupport;

class ErrorLogger extends SchedulerListenerSupport {
   ErrorLogger() {
   }

   @Override
   public void schedulerError(String var1, SchedulerException var2) {
      this.getLog().error(var1, var2);
   }
}
