package org.quartz.ee.jta;

import org.quartz.Scheduler;
import org.quartz.core.JobRunShell;
import org.quartz.core.JobRunShellFactory;
import org.quartz.spi.TriggerFiredBundle;

public class JTAJobRunShellFactory implements JobRunShellFactory {
   private Scheduler scheduler;

   public JTAJobRunShellFactory() {
   }

   @Override
   public void initialize(Scheduler var1) {
      this.scheduler = var1;
   }

   @Override
   public JobRunShell createJobRunShell(TriggerFiredBundle var1) {
      return new JTAJobRunShell(this.scheduler, var1);
   }
}
