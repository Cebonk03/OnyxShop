package org.quartz.impl;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.quartz.core.RemotableQuartzScheduler;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;

public class RemoteScheduler implements Scheduler {
   private RemotableQuartzScheduler rsched;
   private String schedId;
   private String rmiHost;
   private int rmiPort;

   public RemoteScheduler(String var1, String var2, int var3) {
      this.schedId = var1;
      this.rmiHost = var2;
      this.rmiPort = var3;
   }

   protected RemotableQuartzScheduler getRemoteScheduler() {
      if (this.rsched != null) {
         return this.rsched;
      } else {
         try {
            Registry var1 = LocateRegistry.getRegistry(this.rmiHost, this.rmiPort);
            this.rsched = (RemotableQuartzScheduler)var1.lookup(this.schedId);
         } catch (Exception var3) {
            SchedulerException var2 = new SchedulerException("Could not get handle to remote scheduler: " + var3.getMessage(), var3);
            throw var2;
         }

         return this.rsched;
      }
   }

   protected SchedulerException invalidateHandleCreateException(String var1, Exception var2) {
      this.rsched = null;
      return new SchedulerException(var1, var2);
   }

   @Override
   public String getSchedulerName() {
      try {
         return this.getRemoteScheduler().getSchedulerName();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public String getSchedulerInstanceId() {
      try {
         return this.getRemoteScheduler().getSchedulerInstanceId();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public SchedulerMetaData getMetaData() {
      try {
         RemotableQuartzScheduler var1 = this.getRemoteScheduler();
         return new SchedulerMetaData(
            this.getSchedulerName(),
            this.getSchedulerInstanceId(),
            this.getClass(),
            true,
            this.isStarted(),
            this.isInStandbyMode(),
            this.isShutdown(),
            var1.runningSince(),
            var1.numJobsExecuted(),
            var1.getJobStoreClass(),
            var1.supportsPersistence(),
            var1.isClustered(),
            var1.getThreadPoolClass(),
            var1.getThreadPoolSize(),
            var1.getVersion()
         );
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public SchedulerContext getContext() {
      try {
         return this.getRemoteScheduler().getSchedulerContext();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public void start() {
      try {
         this.getRemoteScheduler().start();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public void startDelayed(int var1) {
      try {
         this.getRemoteScheduler().startDelayed(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public void standby() {
      try {
         this.getRemoteScheduler().standby();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public boolean isStarted() {
      try {
         return this.getRemoteScheduler().runningSince() != null;
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public boolean isInStandbyMode() {
      try {
         return this.getRemoteScheduler().isInStandbyMode();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public void shutdown() {
      try {
         String var1 = this.getSchedulerName();
         this.getRemoteScheduler().shutdown();
         SchedulerRepository.getInstance().remove(var1);
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public void shutdown(boolean var1) {
      try {
         String var2 = this.getSchedulerName();
         this.getRemoteScheduler().shutdown(var1);
         SchedulerRepository.getInstance().remove(var2);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public boolean isShutdown() {
      try {
         return this.getRemoteScheduler().isShutdown();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public List<JobExecutionContext> getCurrentlyExecutingJobs() {
      try {
         return this.getRemoteScheduler().getCurrentlyExecutingJobs();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public Date scheduleJob(JobDetail var1, Trigger var2) {
      try {
         return this.getRemoteScheduler().scheduleJob(var1, var2);
      } catch (RemoteException var4) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var4);
      }
   }

   @Override
   public Date scheduleJob(Trigger var1) {
      try {
         return this.getRemoteScheduler().scheduleJob(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public void addJob(JobDetail var1, boolean var2) {
      try {
         this.getRemoteScheduler().addJob(var1, var2);
      } catch (RemoteException var4) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var4);
      }
   }

   @Override
   public void addJob(JobDetail var1, boolean var2, boolean var3) {
      try {
         this.getRemoteScheduler().addJob(var1, var2, var3);
      } catch (RemoteException var5) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var5);
      }
   }

   @Override
   public boolean deleteJobs(List<JobKey> var1) {
      try {
         return this.getRemoteScheduler().deleteJobs(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> var1, boolean var2) {
      try {
         this.getRemoteScheduler().scheduleJobs(var1, var2);
      } catch (RemoteException var4) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var4);
      }
   }

   @Override
   public void scheduleJob(JobDetail var1, Set<? extends Trigger> var2, boolean var3) {
      try {
         this.getRemoteScheduler().scheduleJob(var1, var2, var3);
      } catch (RemoteException var5) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var5);
      }
   }

   @Override
   public boolean unscheduleJobs(List<TriggerKey> var1) {
      try {
         return this.getRemoteScheduler().unscheduleJobs(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public boolean deleteJob(JobKey var1) {
      try {
         return this.getRemoteScheduler().deleteJob(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public boolean unscheduleJob(TriggerKey var1) {
      try {
         return this.getRemoteScheduler().unscheduleJob(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public Date rescheduleJob(TriggerKey var1, Trigger var2) {
      try {
         return this.getRemoteScheduler().rescheduleJob(var1, var2);
      } catch (RemoteException var4) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var4);
      }
   }

   @Override
   public void triggerJob(JobKey var1) {
      this.triggerJob(var1, null);
   }

   @Override
   public void triggerJob(JobKey var1, JobDataMap var2) {
      try {
         this.getRemoteScheduler().triggerJob(var1, var2);
      } catch (RemoteException var4) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var4);
      }
   }

   @Override
   public void pauseTrigger(TriggerKey var1) {
      try {
         this.getRemoteScheduler().pauseTrigger(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public void pauseTriggers(GroupMatcher<TriggerKey> var1) {
      try {
         this.getRemoteScheduler().pauseTriggers(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public void pauseJob(JobKey var1) {
      try {
         this.getRemoteScheduler().pauseJob(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public void pauseJobs(GroupMatcher<JobKey> var1) {
      try {
         this.getRemoteScheduler().pauseJobs(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public void resumeTrigger(TriggerKey var1) {
      try {
         this.getRemoteScheduler().resumeTrigger(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public void resumeTriggers(GroupMatcher<TriggerKey> var1) {
      try {
         this.getRemoteScheduler().resumeTriggers(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public void resumeJob(JobKey var1) {
      try {
         this.getRemoteScheduler().resumeJob(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public void resumeJobs(GroupMatcher<JobKey> var1) {
      try {
         this.getRemoteScheduler().resumeJobs(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public void pauseAll() {
      try {
         this.getRemoteScheduler().pauseAll();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public void resumeAll() {
      try {
         this.getRemoteScheduler().resumeAll();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public List<String> getJobGroupNames() {
      try {
         return this.getRemoteScheduler().getJobGroupNames();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public Set<JobKey> getJobKeys(GroupMatcher<JobKey> var1) {
      try {
         return this.getRemoteScheduler().getJobKeys(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public List<? extends Trigger> getTriggersOfJob(JobKey var1) {
      try {
         return this.getRemoteScheduler().getTriggersOfJob(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public List<String> getTriggerGroupNames() {
      try {
         return this.getRemoteScheduler().getTriggerGroupNames();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> var1) {
      try {
         return this.getRemoteScheduler().getTriggerKeys(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public JobDetail getJobDetail(JobKey var1) {
      try {
         return this.getRemoteScheduler().getJobDetail(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public boolean checkExists(JobKey var1) {
      try {
         return this.getRemoteScheduler().checkExists(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public boolean checkExists(TriggerKey var1) {
      try {
         return this.getRemoteScheduler().checkExists(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public void clear() {
      try {
         this.getRemoteScheduler().clear();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public Trigger getTrigger(TriggerKey var1) {
      try {
         return this.getRemoteScheduler().getTrigger(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public Trigger.TriggerState getTriggerState(TriggerKey var1) {
      try {
         return this.getRemoteScheduler().getTriggerState(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public void resetTriggerFromErrorState(TriggerKey var1) {
      try {
         this.getRemoteScheduler().resetTriggerFromErrorState(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public void addCalendar(String var1, Calendar var2, boolean var3, boolean var4) {
      try {
         this.getRemoteScheduler().addCalendar(var1, var2, var3, var4);
      } catch (RemoteException var6) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var6);
      }
   }

   @Override
   public boolean deleteCalendar(String var1) {
      try {
         return this.getRemoteScheduler().deleteCalendar(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public Calendar getCalendar(String var1) {
      try {
         return this.getRemoteScheduler().getCalendar(var1);
      } catch (RemoteException var3) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3);
      }
   }

   @Override
   public List<String> getCalendarNames() {
      try {
         return this.getRemoteScheduler().getCalendarNames();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public Set<String> getPausedTriggerGroups() {
      try {
         return this.getRemoteScheduler().getPausedTriggerGroups();
      } catch (RemoteException var2) {
         throw this.invalidateHandleCreateException("Error communicating with remote scheduler.", var2);
      }
   }

   @Override
   public ListenerManager getListenerManager() throws SchedulerException {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }

   @Override
   public boolean interrupt(JobKey var1) {
      try {
         return this.getRemoteScheduler().interrupt(var1);
      } catch (RemoteException var3) {
         throw new UnableToInterruptJobException(this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3));
      } catch (SchedulerException var4) {
         throw new UnableToInterruptJobException(var4);
      }
   }

   @Override
   public boolean interrupt(String var1) {
      try {
         return this.getRemoteScheduler().interrupt(var1);
      } catch (RemoteException var3) {
         throw new UnableToInterruptJobException(this.invalidateHandleCreateException("Error communicating with remote scheduler.", var3));
      } catch (SchedulerException var4) {
         throw new UnableToInterruptJobException(var4);
      }
   }

   @Override
   public void setJobFactory(JobFactory var1) {
      throw new SchedulerException("Operation not supported for remote schedulers.");
   }
}
