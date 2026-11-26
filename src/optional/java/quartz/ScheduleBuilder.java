package org.quartz;

import org.quartz.spi.MutableTrigger;

public abstract class ScheduleBuilder<T extends Trigger> {
   public ScheduleBuilder() {
   }

   protected abstract MutableTrigger build();
}
