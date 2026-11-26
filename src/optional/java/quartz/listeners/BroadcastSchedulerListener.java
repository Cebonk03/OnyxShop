package org.quartz.listeners;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

public class BroadcastSchedulerListener implements SchedulerListener {
   private List<SchedulerListener> listeners = new LinkedList<>();

   public BroadcastSchedulerListener() {
   }

   public BroadcastSchedulerListener(List<SchedulerListener> var1) {
      this();
      this.listeners.addAll(var1);
   }

   public void addListener(SchedulerListener var1) {
      this.listeners.add(var1);
   }

   public boolean removeListener(SchedulerListener var1) {
      return this.listeners.remove(var1);
   }

   public List<SchedulerListener> getListeners() {
      return Collections.unmodifiableList(this.listeners);
   }

   @Override
   public void jobAdded(JobDetail var1) {
      for (SchedulerListener var3 : this.listeners) {
         var3.jobAdded(var1);
      }
   }

   @Override
   public void jobDeleted(JobKey var1) {
      for (SchedulerListener var3 : this.listeners) {
         var3.jobDeleted(var1);
      }
   }

   @Override
   public void jobScheduled(Trigger var1) {
      for (SchedulerListener var3 : this.listeners) {
         var3.jobScheduled(var1);
      }
   }

   @Override
   public void jobUnscheduled(TriggerKey var1) {
      for (SchedulerListener var3 : this.listeners) {
         var3.jobUnscheduled(var1);
      }
   }

   @Override
   public void triggerFinalized(Trigger var1) {
      for (SchedulerListener var3 : this.listeners) {
         var3.triggerFinalized(var1);
      }
   }

   @Override
   public void triggerPaused(TriggerKey var1) {
      for (SchedulerListener var3 : this.listeners) {
         var3.triggerPaused(var1);
      }
   }

   @Override
   public void triggersPaused(String var1) {
      for (SchedulerListener var3 : this.listeners) {
         var3.triggersPaused(var1);
      }
   }

   @Override
   public void triggerResumed(TriggerKey var1) {
      for (SchedulerListener var3 : this.listeners) {
         var3.triggerResumed(var1);
      }
   }

   @Override
   public void triggersResumed(String var1) {
      for (SchedulerListener var3 : this.listeners) {
         var3.triggersResumed(var1);
      }
   }

   @Override
   public void schedulingDataCleared() {
      for (SchedulerListener var2 : this.listeners) {
         var2.schedulingDataCleared();
      }
   }

   @Override
   public void jobPaused(JobKey var1) {
      for (SchedulerListener var3 : this.listeners) {
         var3.jobPaused(var1);
      }
   }

   @Override
   public void jobsPaused(String var1) {
      for (SchedulerListener var3 : this.listeners) {
         var3.jobsPaused(var1);
      }
   }

   @Override
   public void jobResumed(JobKey var1) {
      for (SchedulerListener var3 : this.listeners) {
         var3.jobResumed(var1);
      }
   }

   @Override
   public void jobsResumed(String var1) {
      for (SchedulerListener var3 : this.listeners) {
         var3.jobsResumed(var1);
      }
   }

   @Override
   public void schedulerError(String var1, SchedulerException var2) {
      for (SchedulerListener var4 : this.listeners) {
         var4.schedulerError(var1, var2);
      }
   }

   @Override
   public void schedulerStarted() {
      for (SchedulerListener var2 : this.listeners) {
         var2.schedulerStarted();
      }
   }

   @Override
   public void schedulerStarting() {
      for (SchedulerListener var2 : this.listeners) {
         var2.schedulerStarting();
      }
   }

   @Override
   public void schedulerInStandbyMode() {
      for (SchedulerListener var2 : this.listeners) {
         var2.schedulerInStandbyMode();
      }
   }

   @Override
   public void schedulerShutdown() {
      for (SchedulerListener var2 : this.listeners) {
         var2.schedulerShutdown();
      }
   }

   @Override
   public void schedulerShuttingdown() {
      for (SchedulerListener var2 : this.listeners) {
         var2.schedulerShuttingdown();
      }
   }
}
