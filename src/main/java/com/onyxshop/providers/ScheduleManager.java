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

   public ScheduleManager(OnyxShop plugin) {
      this.plugin = plugin;
   }

   private Scheduler getScheduler() {
      if (this.scheduler == null) {
         try {
            this.scheduler = new StdSchedulerFactory().getScheduler();
            this.scheduler.start();
         } catch (SchedulerException exception) {
            SendMessage.warnMessage("A unknown error occurred while creating a new cron scheduler");
            exception.printStackTrace();
         }
      }

      return this.scheduler;
   }

   public Date schedule(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
      Class type = jobDetail.getJobClass();
      try {
         if (type == RestockItemStock.class) {
            return this.getScheduler().scheduleJob(jobDetail, trigger);
         } else if (type == RestockItemLimit.class) {
            return this.getScheduler().scheduleJob(jobDetail, trigger);
         } else {
            return type == RotationJob.class ? this.getScheduler().scheduleJob(jobDetail, trigger) : null;
         }
      } catch (SchedulerException exception) {
         exception.printStackTrace();
         return null;
      }
   }

   public boolean hasJob(JobKey jobKey) {
      try {
         return this.getScheduler().checkExists(jobKey);
      } catch (SchedulerException exception) {
         return false;
      }
   }

   public Trigger getJob(TriggerKey triggerKey) {
      try {
         return this.getScheduler().getTrigger(triggerKey);
      } catch (SchedulerException exception) {
         return null;
      }
   }

   public Long getNextFire(TriggerKey triggerKey) {
      try {
         return this.getScheduler().getTrigger(triggerKey).getNextFireTime().toInstant().toEpochMilli();
      } catch (SchedulerException exception) {
         return null;
      }
   }

   public LinkedList<Date> validate(String text) {
      CronTrigger cronTrigger = TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder.cronSchedule(text)).build();
      LinkedList values = new LinkedList();
      Date date = cronTrigger.getFireTimeAfter(new Date());
      if (date == null) {
         return values;
      } else {
         values.add(date);

         for (int index = 0; index < 5; index++) {
            date = cronTrigger.getFireTimeAfter(date);
            if (date == null) {
               break;
            }

            values.add(date);
         }

         return values;
      }
   }

   public void end() {
      if (this.scheduler != null) {
         try {
            SendMessage.logDebugMessage("Stopping cron scheduler, this can take some time if existing jobs are running...");
            this.scheduler.shutdown(true);
         } catch (SchedulerException exception) {
            SendMessage.warnMessage("A error occurred while stopping the cron scheduler");
            exception.printStackTrace();
         }
      }
   }

   public static class Item {
      public final String section;
      public final String path;
      public final int mode;

      public Item(String itemId, int value) {
         String[] values = itemId.split("\\.", 2);
         this.section = values[0];
         this.path = values[1];
         this.mode = value;
      }

      public String getItemPath() {
         return this.section + "." + this.path;
      }

      @Override
      public boolean equals(Object value) {
         if (this == value) {
            return true;
         } else if (value != null && this.getClass() == value.getClass()) {
            ScheduleManager.Item item = (ScheduleManager.Item)value;
            return this.section.equals(item.section) && this.path.equals(item.path) && this.mode == item.mode;
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
