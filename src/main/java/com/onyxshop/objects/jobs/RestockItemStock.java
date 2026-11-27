package com.onyxshop.objects.jobs;

import com.onyxshop.OnyxShop;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class RestockItemStock implements Job {
   public RestockItemStock() {
   }

   @Override
   public void execute(JobExecutionContext jobExecutionContext) {
      int value = (Integer)jobExecutionContext.getJobDetail().getJobDataMap().get("mode");
      String text = (String)jobExecutionContext.getJobDetail().getJobDataMap().get("item");
      OnyxShop.getInstance().getJobPoolProvider().executeRestockItemStock(text, value, jobExecutionContext.getTrigger().getNextFireTime().getTime());
   }
}
