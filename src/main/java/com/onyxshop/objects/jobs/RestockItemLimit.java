package com.onyxshop.objects.jobs;

import com.onyxshop.OnyxShop;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class RestockItemLimit implements Job {
   public RestockItemLimit() {
   }

   @Override
   public void execute(JobExecutionContext var1) {
      int var2 = (Integer)var1.getJobDetail().getJobDataMap().get("mode");
      String var3 = (String)var1.getJobDetail().getJobDataMap().get("item");
      OnyxShop.getInstance().getJobPoolProvider().executeRestockItemLimit(var3, var2, var1.getTrigger().getNextFireTime().getTime());
   }
}
