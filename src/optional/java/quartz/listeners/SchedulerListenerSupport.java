package org.quartz.listeners;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SchedulerListenerSupport implements SchedulerListener {
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public SchedulerListenerSupport() {
   }

   protected Logger getLog() {
      return this.log;
   }

   @Override
   public void jobAdded(JobDetail var1) {
   }

   @Override
   public void jobDeleted(JobKey var1) {
   }

   @Override
   public void jobPaused(JobKey var1) {
   }

   @Override
   public void jobResumed(JobKey var1) {
   }

   @Override
   public void jobScheduled(Trigger var1) {
   }

   @Override
   public void jobsPaused(String var1) {
   }

   @Override
   public void jobsResumed(String var1) {
   }

   @Override
   public void jobUnscheduled(TriggerKey var1) {
   }

   @Override
   public void schedulerError(String var1, SchedulerException var2) {
   }

   @Override
   public void schedulerInStandbyMode() {
   }

   @Override
   public void schedulerShutdown() {
   }

   @Override
   public void schedulerShuttingdown() {
   }

   @Override
   public void schedulerStarted() {
   }

   @Override
   public void schedulerStarting() {
   }

   @Override
   public void triggerFinalized(Trigger var1) {
   }

   @Override
   public void triggerPaused(TriggerKey var1) {
   }

   @Override
   public void triggerResumed(TriggerKey var1) {
   }

   @Override
   public void triggersPaused(String var1) {
   }

   @Override
   public void triggersResumed(String var1) {
   }

   @Override
   public void schedulingDataCleared() {
   }
}
