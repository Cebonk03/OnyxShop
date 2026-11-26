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
   public void execute(JobExecutionContext var1) {
      String var2 = (String)var1.getJobDetail().getJobDataMap().get("section");
      OnyxShop var3 = OnyxShop.getInstance();
      RotatingShop var4 = (RotatingShop)var3.getSection(var2);
      var3.runTask(() -> var4.forceRotate(false));
      SendMessage.logDebugMessage("Cron task for rotating shop '" + var2 + "' completed and will next schedule at " + var1.getTrigger().getNextFireTime());
   }
}
