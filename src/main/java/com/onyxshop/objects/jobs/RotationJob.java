package com.onyxshop.objects.jobs;

import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.shops.types.RotatingShop;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class RotationJob implements Job {
   public RotationJob() {
   }

   @Override
   public void execute(JobExecutionContext jobExecutionContext) {
      String text = (String)jobExecutionContext.getJobDetail().getJobDataMap().get("section");
      OnyxShop plugin = OnyxShop.getInstance();
      RotatingShop rotatingShop = (RotatingShop)plugin.getSection(text);
      plugin.runTask(() -> rotatingShop.forceRotate(false));
      SendMessage.logDebugMessage("Cron task for rotating shop '" + text + "' completed and will next schedule at " + jobExecutionContext.getTrigger().getNextFireTime());
   }
}
