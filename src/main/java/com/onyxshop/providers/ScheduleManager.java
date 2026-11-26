package com.onyxshop.providers;

import java.util.Date;
import java.util.LinkedList;
import java.util.Objects;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.jobs.RestockItemLimit;
import com.onyxshop.objects.jobs.RestockItemStock;
import com.onyxshop.objects.jobs.RotationJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

public class ScheduleManager {
   private Scheduler scheduler;
   private final OnyxShop plugin;

   public ScheduleManager(OnyxShop var1) {
      this.plugin = var1;
   }

   private Scheduler getScheduler() {
      if (this.scheduler == null) {
         try {
            this.scheduler = new StdSchedulerFactory().getScheduler();
            this.scheduler.start();
         } catch (SchedulerException var2) {
            SendMessage.warnMessage("A unknown error occurred while creating a new cron scheduler");
            var2.printStackTrace();
         }
      }

      return this.scheduler;
   }

   public Date schedule(JobDetail var1, Trigger var2) throws SchedulerException {
      Class var3 = var1.getJobClass();
      try {
         if (var3 == RestockItemStock.class) {
            return this.getScheduler().scheduleJob(var1, var2);
         } else if (var3 == RestockItemLimit.class) {
            return this.getScheduler().scheduleJob(var1, var2);
         } else {
            return var3 == RotationJob.class ? this.getScheduler().scheduleJob(var1, var2) : null;
         }
      } catch (SchedulerException var4) {
         var4.printStackTrace();
         return null;
      }
   }

   public boolean hasJob(JobKey var1) {
      try {
         return this.getScheduler().checkExists(var1);
      } catch (SchedulerException var3) {
         return false;
      }
   }

   public Trigger getJob(TriggerKey var1) {
      try {
         return this.getScheduler().getTrigger(var1);
      } catch (SchedulerException var3) {
         return null;
      }
   }

   public Long getNextFire(TriggerKey var1) {
      try {
         return this.getScheduler().getTrigger(var1).getNextFireTime().toInstant().toEpochMilli();
      } catch (SchedulerException var3) {
         return null;
      }
   }

   public LinkedList<Date> validate(String var1) {
      CronTrigger var2 = TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder.cronSchedule(var1)).build();
      LinkedList var3 = new LinkedList();
      Date var4 = var2.getFireTimeAfter(new Date());
      if (var4 == null) {
         return var3;
      } else {
         var3.add(var4);

         for (int var5 = 0; var5 < 5; var5++) {
            var4 = var2.getFireTimeAfter(var4);
            if (var4 == null) {
               break;
            }

            var3.add(var4);
         }

         return var3;
      }
   }

   public void end() {
      if (this.scheduler != null) {
         try {
            SendMessage.logDebugMessage("Stopping cron scheduler, this can take some time if existing jobs are running...");
            this.scheduler.shutdown(true);
         } catch (SchedulerException var2) {
            SendMessage.warnMessage("A error occurred while stopping the cron scheduler");
            var2.printStackTrace();
         }
      }
   }

   public static class Item {
      public final String section;
      public final String path;
      public final int mode;

      public Item(String var1, int var2) {
         String[] var3 = var1.split("\\.", 2);
         this.section = var3[0];
         this.path = var3[1];
         this.mode = var2;
      }

      public String getItemPath() {
         return this.section + "." + this.path;
      }

      @Override
      public boolean equals(Object var1) {
         if (this == var1) {
            return true;
         } else if (var1 != null && this.getClass() == var1.getClass()) {
            ScheduleManager.Item var2 = (ScheduleManager.Item)var1;
            return this.section.equals(var2.section) && this.path.equals(var2.path) && this.mode == var2.mode;
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.section, this.path, this.mode);
      }
   }
}
