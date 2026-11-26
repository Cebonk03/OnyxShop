package org.quartz.impl;

import org.quartz.Scheduler;
import org.quartz.core.JobRunShell;
import org.quartz.core.JobRunShellFactory;
import org.quartz.spi.TriggerFiredBundle;

public class StdJobRunShellFactory implements JobRunShellFactory {
   private Scheduler scheduler;

   public StdJobRunShellFactory() {
   }

   @Override
   public void initialize(Scheduler var1) {
      this.scheduler = var1;
   }

   @Override
   public JobRunShell createJobRunShell(TriggerFiredBundle var1) {
      return new JobRunShell(this.scheduler, var1);
   }
}
