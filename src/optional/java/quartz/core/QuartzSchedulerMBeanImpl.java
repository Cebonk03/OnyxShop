package org.quartz.core;

import java.beans.IntrospectionException;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.core.jmx.JobDetailSupport;
import org.quartz.core.jmx.JobExecutionContextSupport;
import org.quartz.core.jmx.QuartzSchedulerMBean;
import org.quartz.core.jmx.TriggerSupport;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.AbstractTrigger;
import org.quartz.spi.OperableTrigger;

public class QuartzSchedulerMBeanImpl extends StandardMBean implements NotificationEmitter, QuartzSchedulerMBean, JobListener, SchedulerListener {
   private static final MBeanNotificationInfo[] NOTIFICATION_INFO;
   private final QuartzScheduler scheduler;
   private boolean sampledStatisticsEnabled;
   private SampledStatistics sampledStatistics;
   private static final SampledStatistics NULL_SAMPLED_STATISTICS = new NullSampledStatisticsImpl();
   protected final QuartzSchedulerMBeanImpl.Emitter emitter = new QuartzSchedulerMBeanImpl.Emitter();
   protected final AtomicLong sequenceNumber = new AtomicLong();

   protected QuartzSchedulerMBeanImpl(QuartzScheduler var1) throws javax.management.NotCompliantMBeanException {
      super(QuartzSchedulerMBean.class);
      this.scheduler = var1;
      this.scheduler.addInternalJobListener(this);
      this.scheduler.addInternalSchedulerListener(this);
      this.sampledStatistics = NULL_SAMPLED_STATISTICS;
      this.sampledStatisticsEnabled = false;
   }

   @Override
   public TabularData getCurrentlyExecutingJobs() throws Exception {
      try {
         List var1 = this.scheduler.getCurrentlyExecutingJobs();
         return JobExecutionContextSupport.toTabularData(var1);
      } catch (Exception var2) {
         throw this.newPlainException(var2);
      }
   }

   @Override
   public TabularData getAllJobDetails() throws Exception {
      try {
         ArrayList<JobDetail> var1 = new ArrayList();

         for (String var3 : this.scheduler.getJobGroupNames()) {
            for (JobKey var5 : this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(var3))) {
               var1.add(this.scheduler.getJobDetail(var5));
            }
         }

         return JobDetailSupport.toTabularData(var1.toArray(new JobDetail[var1.size()]));
      } catch (Exception var6) {
         throw this.newPlainException(var6);
      }
   }

   @Override
   public List<CompositeData> getAllTriggers() throws Exception {
      try {
         ArrayList var1 = new ArrayList();

         for (String var3 : this.scheduler.getTriggerGroupNames()) {
            for (TriggerKey var5 : this.scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(var3))) {
               var1.add(this.scheduler.getTrigger(var5));
            }
         }

         return TriggerSupport.toCompositeList(var1);
      } catch (Exception var6) {
         throw this.newPlainException(var6);
      }
   }

   @Override
   public void addJob(CompositeData var1, boolean var2) throws Exception {
      try {
         this.scheduler.addJob(JobDetailSupport.newJobDetail(var1), var2);
      } catch (Exception var4) {
         throw this.newPlainException(var4);
      }
   }

   private static void invokeSetter(Object var0, String var1, Object var2) throws Exception {
      String var3 = "set" + Character.toUpperCase(var1.charAt(0)) + var1.substring(1);
      Class[] var4 = new Class[]{var2.getClass()};
      Method var5 = findMethod(var0.getClass(), var3, var4);
      if (var5 != null) {
         var5.invoke(var0, var2);
      } else {
         throw new Exception("Unable to find setter for attribute '" + var1 + "' and value '" + var2 + "'");
      }
   }

   private static Class<?> getWrapperIfPrimitive(Class<?> var0) {
      Class var1 = var0;

      try {
         Field var2 = var0.getField("TYPE");
         var2.setAccessible(true);
         var1 = (Class)var2.get(null);
      } catch (Exception var3) {
      }

      return var1;
   }

   private static Method findMethod(Class<?> var0, String var1, Class<?>[] var2) throws IntrospectionException {
      BeanInfo var3 = Introspector.getBeanInfo(var0);
      if (var3 != null) {
         for (MethodDescriptor var7 : var3.getMethodDescriptors()) {
            Method var8 = var7.getMethod();
            Class[] var9 = var8.getParameterTypes();
            if (var1.equals(var8.getName()) && var2.length == var9.length) {
               boolean var10 = true;

               for (int var11 = 0; var11 < var2.length; var11++) {
                  if (getWrapperIfPrimitive(var2[var11]) != var9[var11]) {
                     var10 = false;
                     break;
                  }
               }

               if (var10) {
                  return var8;
               }
            }
         }
      }

      return null;
   }

   @Override
   public void scheduleBasicJob(Map<String, Object> var1, Map<String, Object> var2) throws Exception {
      try {
         JobDetail var3 = JobDetailSupport.newJobDetail(var1);
         OperableTrigger var4 = TriggerSupport.newTrigger(var2);
         this.scheduler.deleteJob(var3.getKey());
         this.scheduler.scheduleJob(var3, var4);
      } catch (ParseException var5) {
         throw var5;
      } catch (Exception var6) {
         throw this.newPlainException(var6);
      }
   }

   @Override
   public void scheduleJob(Map<String, Object> var1, Map<String, Object> var2) throws Exception {
      try {
         String var3 = (String)var2.remove("triggerClass");
         if (var3 == null) {
            throw new IllegalArgumentException("No triggerClass specified");
         } else {
            Class var4 = Class.forName(var3);
            Trigger var5 = (Trigger)var4.newInstance();
            String var6 = (String)var1.remove("jobDetailClass");
            if (var6 == null) {
               throw new IllegalArgumentException("No jobDetailClass specified");
            } else {
               Class var7 = Class.forName(var6);
               JobDetail var8 = (JobDetail)var7.newInstance();
               String var9 = (String)var1.remove("jobClass");
               if (var9 == null) {
                  throw new IllegalArgumentException("No jobClass specified");
               } else {
                  Class var10 = Class.forName(var9);
                  var1.put("jobClass", var10);

                  for (Entry var12 : var2.entrySet()) {
                     String var13 = (String)var12.getKey();
                     Object var14 = var12.getValue();
                     if ("jobDataMap".equals(var13)) {
                        var14 = new JobDataMap((Map<?, ?>)var14);
                     }

                     invokeSetter(var5, var13, var14);
                  }

                  for (Entry var18 : var1.entrySet()) {
                     String var20 = (String)var18.getKey();
                     Object var21 = var18.getValue();
                     if ("jobDataMap".equals(var20)) {
                        var21 = new JobDataMap((Map<?, ?>)var21);
                     }

                     invokeSetter(var8, var20, var21);
                  }

                  AbstractTrigger var17 = (AbstractTrigger)var5;
                  var17.setKey(new TriggerKey(var17.getName(), var17.getGroup()));
                  Date var19 = var17.getStartTime();
                  if (var19 == null || var19.before(new Date())) {
                     var17.setStartTime(new Date());
                  }

                  this.scheduler.deleteJob(var8.getKey());
                  this.scheduler.scheduleJob(var8, var5);
               }
            }
         }
      } catch (Exception var15) {
         throw this.newPlainException(var15);
      }
   }

   @Override
   public void scheduleJob(String var1, String var2, Map<String, Object> var3) throws Exception {
      try {
         JobKey var4 = new JobKey(var1, var2);
         JobDetail var5 = this.scheduler.getJobDetail(var4);
         if (var5 == null) {
            throw new IllegalArgumentException("No such job '" + var4 + "'");
         } else {
            String var6 = (String)var3.remove("triggerClass");
            if (var6 == null) {
               throw new IllegalArgumentException("No triggerClass specified");
            } else {
               Class var7 = Class.forName(var6);
               Trigger var8 = (Trigger)var7.newInstance();

               for (Entry var10 : var3.entrySet()) {
                  String var11 = (String)var10.getKey();
                  Object var12 = var10.getValue();
                  if ("jobDataMap".equals(var11)) {
                     var12 = new JobDataMap((Map<?, ?>)var12);
                  }

                  invokeSetter(var8, var11, var12);
               }

               AbstractTrigger var14 = (AbstractTrigger)var8;
               var14.setKey(new TriggerKey(var14.getName(), var14.getGroup()));
               Date var15 = var14.getStartTime();
               if (var15 == null || var15.before(new Date())) {
                  var14.setStartTime(new Date());
               }

               this.scheduler.scheduleJob(var8);
            }
         }
      } catch (Exception var13) {
         throw this.newPlainException(var13);
      }
   }

   @Override
   public void addJob(Map<String, Object> var1, boolean var2) throws Exception {
      try {
         String var3 = (String)var1.remove("jobDetailClass");
         if (var3 == null) {
            throw new IllegalArgumentException("No jobDetailClass specified");
         } else {
            Class var4 = Class.forName(var3);
            JobDetail var5 = (JobDetail)var4.newInstance();
            String var6 = (String)var1.remove("jobClass");
            if (var6 == null) {
               throw new IllegalArgumentException("No jobClass specified");
            } else {
               Class var7 = Class.forName(var6);
               var1.put("jobClass", var7);

               for (Entry var9 : var1.entrySet()) {
                  String var10 = (String)var9.getKey();
                  Object var11 = var9.getValue();
                  if ("jobDataMap".equals(var10)) {
                     var11 = new JobDataMap((Map<?, ?>)var11);
                  }

                  invokeSetter(var5, var10, var11);
               }

               this.scheduler.addJob(var5, var2);
            }
         }
      } catch (Exception var12) {
         throw this.newPlainException(var12);
      }
   }

   private Exception newPlainException(Exception var1) {
      String var2 = var1.getClass().getName();
      if (!var2.startsWith("java.") && !var2.startsWith("javax.")) {
         Exception var3 = new Exception(var1.getMessage());
         var3.setStackTrace(var1.getStackTrace());
         return var3;
      } else {
         return var1;
      }
   }

   @Override
   public void deleteCalendar(String var1) throws Exception {
      try {
         this.scheduler.deleteCalendar(var1);
      } catch (Exception var3) {
         throw this.newPlainException(var3);
      }
   }

   @Override
   public boolean deleteJob(String var1, String var2) throws Exception {
      try {
         return this.scheduler.deleteJob(JobKey.jobKey(var1, var2));
      } catch (Exception var4) {
         throw this.newPlainException(var4);
      }
   }

   @Override
   public List<String> getCalendarNames() throws Exception {
      try {
         return this.scheduler.getCalendarNames();
      } catch (Exception var2) {
         throw this.newPlainException(var2);
      }
   }

   @Override
   public CompositeData getJobDetail(String var1, String var2) throws Exception {
      try {
         JobDetail var3 = this.scheduler.getJobDetail(JobKey.jobKey(var1, var2));
         return JobDetailSupport.toCompositeData(var3);
      } catch (Exception var4) {
         throw this.newPlainException(var4);
      }
   }

   @Override
   public List<String> getJobGroupNames() throws Exception {
      try {
         return this.scheduler.getJobGroupNames();
      } catch (Exception var2) {
         throw this.newPlainException(var2);
      }
   }

   @Override
   public List<String> getJobNames(String var1) throws Exception {
      try {
         ArrayList var2 = new ArrayList();

         for (JobKey var4 : this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(var1))) {
            var2.add(var4.getName());
         }

         return var2;
      } catch (Exception var5) {
         throw this.newPlainException(var5);
      }
   }

   @Override
   public String getJobStoreClassName() {
      return this.scheduler.getJobStoreClass().getName();
   }

   @Override
   public Set<String> getPausedTriggerGroups() throws Exception {
      try {
         return this.scheduler.getPausedTriggerGroups();
      } catch (Exception var2) {
         throw this.newPlainException(var2);
      }
   }

   @Override
   public CompositeData getTrigger(String var1, String var2) throws Exception {
      try {
         Trigger var3 = this.scheduler.getTrigger(TriggerKey.triggerKey(var1, var2));
         return TriggerSupport.toCompositeData(var3);
      } catch (Exception var4) {
         throw this.newPlainException(var4);
      }
   }

   @Override
   public List<String> getTriggerGroupNames() throws Exception {
      try {
         return this.scheduler.getTriggerGroupNames();
      } catch (Exception var2) {
         throw this.newPlainException(var2);
      }
   }

   @Override
   public List<String> getTriggerNames(String var1) throws Exception {
      try {
         ArrayList var2 = new ArrayList();

         for (TriggerKey var4 : this.scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(var1))) {
            var2.add(var4.getName());
         }

         return var2;
      } catch (Exception var5) {
         throw this.newPlainException(var5);
      }
   }

   @Override
   public String getTriggerState(String var1, String var2) throws Exception {
      try {
         TriggerKey var3 = TriggerKey.triggerKey(var1, var2);
         Trigger.TriggerState var4 = this.scheduler.getTriggerState(var3);
         return var4.name();
      } catch (Exception var5) {
         throw this.newPlainException(var5);
      }
   }

   @Override
   public List<CompositeData> getTriggersOfJob(String var1, String var2) throws Exception {
      try {
         JobKey var3 = JobKey.jobKey(var1, var2);
         return TriggerSupport.toCompositeList(this.scheduler.getTriggersOfJob(var3));
      } catch (Exception var4) {
         throw this.newPlainException(var4);
      }
   }

   @Override
   public boolean interruptJob(String var1, String var2) throws Exception {
      try {
         return this.scheduler.interrupt(JobKey.jobKey(var1, var2));
      } catch (Exception var4) {
         throw this.newPlainException(var4);
      }
   }

   @Override
   public boolean interruptJob(String var1) throws Exception {
      try {
         return this.scheduler.interrupt(var1);
      } catch (Exception var3) {
         throw this.newPlainException(var3);
      }
   }

   @Override
   public Date scheduleJob(String var1, String var2, String var3, String var4) throws Exception {
      try {
         JobKey var5 = JobKey.jobKey(var1, var2);
         JobDetail var6 = this.scheduler.getJobDetail(var5);
         if (var6 == null) {
            throw new IllegalArgumentException("No such job: " + var5);
         } else {
            TriggerKey var7 = TriggerKey.triggerKey(var3, var4);
            Trigger var8 = this.scheduler.getTrigger(var7);
            if (var8 == null) {
               throw new IllegalArgumentException("No such trigger: " + var7);
            } else {
               return this.scheduler.scheduleJob(var6, var8);
            }
         }
      } catch (Exception var9) {
         throw this.newPlainException(var9);
      }
   }

   @Override
   public boolean unscheduleJob(String var1, String var2) throws Exception {
      try {
         return this.scheduler.unscheduleJob(TriggerKey.triggerKey(var1, var2));
      } catch (Exception var4) {
         throw this.newPlainException(var4);
      }
   }

   @Override
   public void clear() throws Exception {
      try {
         this.scheduler.clear();
      } catch (Exception var2) {
         throw this.newPlainException(var2);
      }
   }

   @Override
   public String getVersion() {
      return this.scheduler.getVersion();
   }

   @Override
   public boolean isShutdown() {
      return this.scheduler.isShutdown();
   }

   @Override
   public boolean isStarted() {
      return this.scheduler.isStarted();
   }

   @Override
   public void start() throws Exception {
      try {
         this.scheduler.start();
      } catch (Exception var2) {
         throw this.newPlainException(var2);
      }
   }

   @Override
   public void shutdown() {
      this.scheduler.shutdown();
   }

   @Override
   public void standby() {
      this.scheduler.standby();
   }

   @Override
   public boolean isStandbyMode() {
      return this.scheduler.isInStandbyMode();
   }

   @Override
   public String getSchedulerName() {
      return this.scheduler.getSchedulerName();
   }

   @Override
   public String getSchedulerInstanceId() {
      return this.scheduler.getSchedulerInstanceId();
   }

   @Override
   public String getThreadPoolClassName() {
      return this.scheduler.getThreadPoolClass().getName();
   }

   @Override
   public int getThreadPoolSize() {
      return this.scheduler.getThreadPoolSize();
   }

   @Override
   public void pauseJob(String var1, String var2) throws Exception {
      try {
         this.scheduler.pauseJob(JobKey.jobKey(var1, var2));
      } catch (Exception var4) {
         throw this.newPlainException(var4);
      }
   }

   public void pauseJobs(GroupMatcher<JobKey> var1) throws Exception {
      try {
         this.scheduler.pauseJobs(var1);
      } catch (Exception var3) {
         throw this.newPlainException(var3);
      }
   }

   @Override
   public void pauseJobGroup(String var1) throws Exception {
      this.pauseJobs(GroupMatcher.groupEquals(var1));
   }

   @Override
   public void pauseJobsStartingWith(String var1) throws Exception {
      this.pauseJobs(GroupMatcher.groupStartsWith(var1));
   }

   @Override
   public void pauseJobsEndingWith(String var1) throws Exception {
      this.pauseJobs(GroupMatcher.groupEndsWith(var1));
   }

   @Override
   public void pauseJobsContaining(String var1) throws Exception {
      this.pauseJobs(GroupMatcher.groupContains(var1));
   }

   @Override
   public void pauseJobsAll() throws Exception {
      this.pauseJobs(GroupMatcher.anyJobGroup());
   }

   @Override
   public void pauseAllTriggers() throws Exception {
      try {
         this.scheduler.pauseAll();
      } catch (Exception var2) {
         throw this.newPlainException(var2);
      }
   }

   private void pauseTriggers(GroupMatcher<TriggerKey> var1) throws Exception {
      try {
         this.scheduler.pauseTriggers(var1);
      } catch (Exception var3) {
         throw this.newPlainException(var3);
      }
   }

   @Override
   public void pauseTriggerGroup(String var1) throws Exception {
      this.pauseTriggers(GroupMatcher.groupEquals(var1));
   }

   @Override
   public void pauseTriggersStartingWith(String var1) throws Exception {
      this.pauseTriggers(GroupMatcher.groupStartsWith(var1));
   }

   @Override
   public void pauseTriggersEndingWith(String var1) throws Exception {
      this.pauseTriggers(GroupMatcher.groupEndsWith(var1));
   }

   @Override
   public void pauseTriggersContaining(String var1) throws Exception {
      this.pauseTriggers(GroupMatcher.groupContains(var1));
   }

   @Override
   public void pauseTriggersAll() throws Exception {
      this.pauseTriggers(GroupMatcher.anyTriggerGroup());
   }

   @Override
   public void pauseTrigger(String var1, String var2) throws Exception {
      try {
         this.scheduler.pauseTrigger(TriggerKey.triggerKey(var1, var2));
      } catch (Exception var4) {
         throw this.newPlainException(var4);
      }
   }

   @Override
   public void resumeAllTriggers() throws Exception {
      try {
         this.scheduler.resumeAll();
      } catch (Exception var2) {
         throw this.newPlainException(var2);
      }
   }

   @Override
   public void resumeJob(String var1, String var2) throws Exception {
      try {
         this.scheduler.resumeJob(JobKey.jobKey(var1, var2));
      } catch (Exception var4) {
         throw this.newPlainException(var4);
      }
   }

   public void resumeJobs(GroupMatcher<JobKey> var1) throws Exception {
      try {
         this.scheduler.resumeJobs(var1);
      } catch (Exception var3) {
         throw this.newPlainException(var3);
      }
   }

   @Override
   public void resumeJobGroup(String var1) throws Exception {
      this.resumeJobs(GroupMatcher.groupEquals(var1));
   }

   @Override
   public void resumeJobsStartingWith(String var1) throws Exception {
      this.resumeJobs(GroupMatcher.groupStartsWith(var1));
   }

   @Override
   public void resumeJobsEndingWith(String var1) throws Exception {
      this.resumeJobs(GroupMatcher.groupEndsWith(var1));
   }

   @Override
   public void resumeJobsContaining(String var1) throws Exception {
      this.resumeJobs(GroupMatcher.groupContains(var1));
   }

   @Override
   public void resumeJobsAll() throws Exception {
      this.resumeJobs(GroupMatcher.anyJobGroup());
   }

   @Override
   public void resumeTrigger(String var1, String var2) throws Exception {
      try {
         this.scheduler.resumeTrigger(TriggerKey.triggerKey(var1, var2));
      } catch (Exception var4) {
         throw this.newPlainException(var4);
      }
   }

   private void resumeTriggers(GroupMatcher<TriggerKey> var1) throws Exception {
      try {
         this.scheduler.resumeTriggers(var1);
      } catch (Exception var3) {
         throw this.newPlainException(var3);
      }
   }

   @Override
   public void resumeTriggerGroup(String var1) throws Exception {
      this.resumeTriggers(GroupMatcher.groupEquals(var1));
   }

   @Override
   public void resumeTriggersStartingWith(String var1) throws Exception {
      this.resumeTriggers(GroupMatcher.groupStartsWith(var1));
   }

   @Override
   public void resumeTriggersEndingWith(String var1) throws Exception {
      this.resumeTriggers(GroupMatcher.groupEndsWith(var1));
   }

   @Override
   public void resumeTriggersContaining(String var1) throws Exception {
      this.resumeTriggers(GroupMatcher.groupContains(var1));
   }

   @Override
   public void resumeTriggersAll() throws Exception {
      this.resumeTriggers(GroupMatcher.anyTriggerGroup());
   }

   @Override
   public void triggerJob(String var1, String var2, Map<String, String> var3) throws Exception {
      try {
         this.scheduler.triggerJob(JobKey.jobKey(var1, var2), new JobDataMap(var3));
      } catch (Exception var5) {
         throw this.newPlainException(var5);
      }
   }

   public void triggerJob(CompositeData var1) throws Exception {
      try {
         this.scheduler.triggerJob(TriggerSupport.newTrigger(var1));
      } catch (Exception var3) {
         throw this.newPlainException(var3);
      }
   }

   @Override
   public void jobAdded(JobDetail var1) {
      this.sendNotification("jobAdded", JobDetailSupport.toCompositeData(var1));
   }

   @Override
   public void jobDeleted(JobKey var1) {
      HashMap var2 = new HashMap();
      var2.put("jobName", var1.getName());
      var2.put("jobGroup", var1.getGroup());
      this.sendNotification("jobDeleted", var2);
   }

   @Override
   public void jobScheduled(Trigger var1) {
      this.sendNotification("jobScheduled", TriggerSupport.toCompositeData(var1));
   }

   @Override
   public void jobUnscheduled(TriggerKey var1) {
      HashMap var2 = new HashMap();
      var2.put("triggerName", var1.getName());
      var2.put("triggerGroup", var1.getGroup());
      this.sendNotification("jobUnscheduled", var2);
   }

   @Override
   public void schedulingDataCleared() {
      this.sendNotification("schedulingDataCleared");
   }

   @Override
   public void jobPaused(JobKey var1) {
      HashMap var2 = new HashMap();
      var2.put("jobName", var1.getName());
      var2.put("jobGroup", var1.getGroup());
      this.sendNotification("jobsPaused", var2);
   }

   @Override
   public void jobsPaused(String var1) {
      HashMap var2 = new HashMap();
      var2.put("jobName", null);
      var2.put("jobGroup", var1);
      this.sendNotification("jobsPaused", var2);
   }

   @Override
   public void jobsResumed(String var1) {
      HashMap var2 = new HashMap();
      var2.put("jobName", null);
      var2.put("jobGroup", var1);
      this.sendNotification("jobsResumed", var2);
   }

   @Override
   public void jobResumed(JobKey var1) {
      HashMap var2 = new HashMap();
      var2.put("jobName", var1.getName());
      var2.put("jobGroup", var1.getGroup());
      this.sendNotification("jobsResumed", var2);
   }

   @Override
   public void schedulerError(String var1, SchedulerException var2) {
      this.sendNotification("schedulerError", var2.getMessage());
   }

   @Override
   public void schedulerStarted() {
      this.sendNotification("schedulerStarted");
   }

   @Override
   public void schedulerStarting() {
   }

   @Override
   public void schedulerInStandbyMode() {
      this.sendNotification("schedulerPaused");
   }

   @Override
   public void schedulerShutdown() {
      this.scheduler.removeInternalSchedulerListener(this);
      this.scheduler.removeInternalJobListener(this.getName());
      this.sendNotification("schedulerShutdown");
   }

   @Override
   public void schedulerShuttingdown() {
   }

   @Override
   public void triggerFinalized(Trigger var1) {
      HashMap var2 = new HashMap();
      var2.put("triggerName", var1.getKey().getName());
      var2.put("triggerGroup", var1.getKey().getGroup());
      this.sendNotification("triggerFinalized", var2);
   }

   @Override
   public void triggersPaused(String var1) {
      HashMap var2 = new HashMap();
      var2.put("triggerName", null);
      var2.put("triggerGroup", var1);
      this.sendNotification("triggersPaused", var2);
   }

   @Override
   public void triggerPaused(TriggerKey var1) {
      HashMap var2 = new HashMap();
      if (var1 != null) {
         var2.put("triggerName", var1.getName());
         var2.put("triggerGroup", var1.getGroup());
      }

      this.sendNotification("triggersPaused", var2);
   }

   @Override
   public void triggersResumed(String var1) {
      HashMap var2 = new HashMap();
      var2.put("triggerName", null);
      var2.put("triggerGroup", var1);
      this.sendNotification("triggersResumed", var2);
   }

   @Override
   public void triggerResumed(TriggerKey var1) {
      HashMap var2 = new HashMap();
      if (var1 != null) {
         var2.put("triggerName", var1.getName());
         var2.put("triggerGroup", var1.getGroup());
      }

      this.sendNotification("triggersResumed", var2);
   }

   @Override
   public String getName() {
      return "QuartzSchedulerMBeanImpl.listener";
   }

   @Override
   public void jobExecutionVetoed(JobExecutionContext var1) {
      try {
         this.sendNotification("jobExecutionVetoed", JobExecutionContextSupport.toCompositeData(var1));
      } catch (Exception var3) {
         throw new RuntimeException(this.newPlainException(var3));
      }
   }

   @Override
   public void jobToBeExecuted(JobExecutionContext var1) {
      try {
         this.sendNotification("jobToBeExecuted", JobExecutionContextSupport.toCompositeData(var1));
      } catch (Exception var3) {
         throw new RuntimeException(this.newPlainException(var3));
      }
   }

   @Override
   public void jobWasExecuted(JobExecutionContext var1, JobExecutionException var2) {
      try {
         this.sendNotification("jobWasExecuted", JobExecutionContextSupport.toCompositeData(var1));
      } catch (Exception var4) {
         throw new RuntimeException(this.newPlainException(var4));
      }
   }

   public void sendNotification(String var1) {
      this.sendNotification(var1, null, null);
   }

   public void sendNotification(String var1, Object var2) {
      this.sendNotification(var1, var2, null);
   }

   public void sendNotification(String var1, Object var2, String var3) {
      Notification var4 = new Notification(var1, this, this.sequenceNumber.incrementAndGet(), System.currentTimeMillis(), var3);
      if (var2 != null) {
         var4.setUserData(var2);
      }

      this.emitter.sendNotification(var4);
   }

   @Override
   public void addNotificationListener(NotificationListener var1, NotificationFilter var2, Object var3) {
      this.emitter.addNotificationListener(var1, var2, var3);
   }

   @Override
   public MBeanNotificationInfo[] getNotificationInfo() {
      return NOTIFICATION_INFO;
   }

   @Override
   public void removeNotificationListener(NotificationListener var1) throws ListenerNotFoundException {
      this.emitter.removeNotificationListener(var1);
   }

   @Override
   public void removeNotificationListener(NotificationListener var1, NotificationFilter var2, Object var3) throws ListenerNotFoundException {
      this.emitter.removeNotificationListener(var1, var2, var3);
   }

   @Override
   public synchronized boolean isSampledStatisticsEnabled() {
      return this.sampledStatisticsEnabled;
   }

   @Override
   public void setSampledStatisticsEnabled(boolean var1) {
      if (var1 != this.sampledStatisticsEnabled) {
         this.sampledStatisticsEnabled = var1;
         if (var1) {
            this.sampledStatistics = new SampledStatisticsImpl(this.scheduler);
         } else {
            this.sampledStatistics.shutdown();
            this.sampledStatistics = NULL_SAMPLED_STATISTICS;
         }

         this.sendNotification("sampledStatisticsEnabled", var1);
      }
   }

   @Override
   public long getJobsCompletedMostRecentSample() {
      return this.sampledStatistics.getJobsCompletedMostRecentSample();
   }

   @Override
   public long getJobsExecutedMostRecentSample() {
      return this.sampledStatistics.getJobsExecutingMostRecentSample();
   }

   @Override
   public long getJobsScheduledMostRecentSample() {
      return this.sampledStatistics.getJobsScheduledMostRecentSample();
   }

   @Override
   public Map<String, Long> getPerformanceMetrics() {
      HashMap var1 = new HashMap();
      var1.put("JobsCompleted", this.getJobsCompletedMostRecentSample());
      var1.put("JobsExecuted", this.getJobsExecutedMostRecentSample());
      var1.put("JobsScheduled", this.getJobsScheduledMostRecentSample());
      return var1;
   }

   static {
      String[] var0 = new String[]{"schedulerStarted", "schedulerPaused", "schedulerShutdown"};
      String var1 = Notification.class.getName();
      String var2 = "QuartzScheduler JMX Event";
      NOTIFICATION_INFO = new MBeanNotificationInfo[]{new MBeanNotificationInfo(var0, var1, "QuartzScheduler JMX Event")};
   }

   private class Emitter extends NotificationBroadcasterSupport {
      private Emitter() {
      }

      @Override
      public MBeanNotificationInfo[] getNotificationInfo() {
         return QuartzSchedulerMBeanImpl.this.getNotificationInfo();
      }
   }
}
