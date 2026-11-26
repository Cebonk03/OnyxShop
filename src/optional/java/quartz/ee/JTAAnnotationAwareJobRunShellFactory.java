package org.quartz.ee.jta;

import org.quartz.ExecuteInJTATransaction;
import org.quartz.Scheduler;
import org.quartz.core.JobRunShell;
import org.quartz.core.JobRunShellFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.utils.ClassUtils;

public class JTAAnnotationAwareJobRunShellFactory implements JobRunShellFactory {
   private Scheduler scheduler;

   public JTAAnnotationAwareJobRunShellFactory() {
   }

   @Override
   public void initialize(Scheduler var1) {
      this.scheduler = var1;
   }

   @Override
   public JobRunShell createJobRunShell(TriggerFiredBundle var1) {
      ExecuteInJTATransaction var2 = ClassUtils.getAnnotation(var1.getJobDetail().getJobClass(), ExecuteInJTATransaction.class);
      if (var2 == null) {
         return new JobRunShell(this.scheduler, var1);
      } else {
         int var3 = var2.timeout();
         return var3 >= 0 ? new JTAJobRunShell(this.scheduler, var1, var3) : new JTAJobRunShell(this.scheduler, var1);
      }
   }
}
