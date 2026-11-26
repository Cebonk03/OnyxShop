package org.quartz.listeners;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TriggerListenerSupport implements TriggerListener {
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public TriggerListenerSupport() {
   }

   protected Logger getLog() {
      return this.log;
   }

   @Override
   public void triggerFired(Trigger var1, JobExecutionContext var2) {
   }

   @Override
   public boolean vetoJobExecution(Trigger var1, JobExecutionContext var2) {
      return false;
   }

   @Override
   public void triggerMisfired(Trigger var1) {
   }

   @Override
   public void triggerComplete(Trigger var1, JobExecutionContext var2, Trigger.CompletedExecutionInstruction var3) {
   }
}
