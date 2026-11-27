package com.onyxshop.objects.jobs;

import com.onyxshop.OnyxShop;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class RestockItemLimit implements Job {
   public RestockItemLimit() {
   }

   @Override
   public void execute(JobExecutionContext jobExecutionContext) {
      int value = (Integer)jobExecutionContext.getJobDetail().getJobDataMap().get("mode");
      String text = (String)jobExecutionContext.getJobDetail().getJobDataMap().get("item");
      OnyxShop.getInstance().getJobPoolProvider().executeRestockItemLimit(text, value, jobExecutionContext.getTrigger().getNextFireTime().getTime());
   }
}
