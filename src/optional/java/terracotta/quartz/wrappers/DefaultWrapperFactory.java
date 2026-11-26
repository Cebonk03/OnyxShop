package org.terracotta.quartz.wrappers;

import org.quartz.JobDetail;
import org.quartz.spi.OperableTrigger;

public class DefaultWrapperFactory implements WrapperFactory {
   public DefaultWrapperFactory() {
   }

   @Override
   public JobWrapper createJobWrapper(JobDetail var1) {
      return new JobWrapper(var1);
   }

   @Override
   public TriggerWrapper createTriggerWrapper(OperableTrigger var1, boolean var2) {
      return new TriggerWrapper(var1, var2);
   }
}
