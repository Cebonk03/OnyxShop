package com.onyxshop.objects.jobs;

import com.onyxshop.OnyxShop;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class RestockItemStock implements Job {
   public RestockItemStock() {
   }

   @Override
   public void execute(JobExecutionContext var1) {
      int var2 = (Integer)var1.getJobDetail().getJobDataMap().get("mode");
      String var3 = (String)var1.getJobDetail().getJobDataMap().get("item");
      OnyxShop.getInstance().getJobPoolProvider().executeRestockItemStock(var3, var2, var1.getTrigger().getNextFireTime().getTime());
   }
}
