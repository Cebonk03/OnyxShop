package org.quartz.simpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.matchers.StringMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.spi.TriggerFiredResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RAMJobStore implements JobStore {
   protected HashMap<JobKey, JobWrapper> jobsByKey = new HashMap<>(1000);
   protected HashMap<TriggerKey, TriggerWrapper> triggersByKey = new HashMap<>(1000);
   protected HashMap<String, HashMap<JobKey, JobWrapper>> jobsByGroup = new HashMap<>(25);
   protected HashMap<String, HashMap<TriggerKey, TriggerWrapper>> triggersByGroup = new HashMap<>(25);
   protected TreeSet<TriggerWrapper> timeTriggers = new TreeSet<>(new TriggerWrapperComparator());
   protected HashMap<String, Calendar> calendarsByName = new HashMap<>(25);
   protected Map<JobKey, List<TriggerWrapper>> triggersByJob = new HashMap<>(1000);
   protected final Object lock = new Object();
   protected HashSet<String> pausedTriggerGroups = new HashSet<>();
   protected HashSet<String> pausedJobGroups = new HashSet<>();
   protected HashSet<JobKey> blockedJobs = new HashSet<>();
   protected long misfireThreshold = 5000L;
   protected SchedulerSignaler signaler;
   private final Logger log = LoggerFactory.getLogger(this.getClass());
   private static final AtomicLong ftrCtr = new AtomicLong(System.currentTimeMillis());

   public RAMJobStore() {
   }

   protected Logger getLog() {
      return this.log;
   }

   @Override
   public void initialize(ClassLoadHelper var1, SchedulerSignaler var2) {
      this.signaler = var2;
      this.getLog().info("RAMJobStore initialized.");
   }

   @Override
   public void schedulerStarted() {
   }

   @Override
   public void schedulerPaused() {
   }

   @Override
   public void schedulerResumed() {
   }

   public long getMisfireThreshold() {
      return this.misfireThreshold;
   }

   public void setMisfireThreshold(long var1) {
      if (var1 < 1L) {
         throw new IllegalArgumentException("Misfire threshold must be larger than 0");
      } else {
         this.misfireThreshold = var1;
      }
   }

   @Override
   public void shutdown() {
   }

   @Override
   public boolean supportsPersistence() {
      return false;
   }

   @Override
   public void clearAllSchedulingData() {
      synchronized (this.lock) {
         for (String var4 : this.getTriggerGroupNames()) {
            for (TriggerKey var7 : this.getTriggerKeys(GroupMatcher.triggerGroupEquals(var4))) {
               this.removeTrigger(var7);
            }
         }

         for (String var14 : this.getJobGroupNames()) {
            for (JobKey var18 : this.getJobKeys(GroupMatcher.jobGroupEquals(var14))) {
               this.removeJob(var18);
            }
         }

         for (String var15 : this.getCalendarNames()) {
            this.removeCalendar(var15);
         }
      }
   }

   @Override
   public void storeJobAndTrigger(JobDetail var1, OperableTrigger var2) {
      this.storeJob(var1, false);
      this.storeTrigger(var2, false);
   }

   @Override
   public void storeJob(JobDetail var1, boolean var2) {
      JobWrapper var3 = new JobWrapper((JobDetail)var1.clone());
      boolean var4 = false;
      synchronized (this.lock) {
         if (this.jobsByKey.get(var3.key) != null) {
            if (!var2) {
               throw new ObjectAlreadyExistsException(var1);
            }

            var4 = true;
         }

         if (!var4) {
            HashMap var6 = this.jobsByGroup.get(var1.getKey().getGroup());
            if (var6 == null) {
               var6 = new HashMap(100);
               this.jobsByGroup.put(var1.getKey().getGroup(), var6);
            }

            var6.put(var1.getKey(), var3);
            this.jobsByKey.put(var3.key, var3);
         } else {
            JobWrapper var9 = this.jobsByKey.get(var3.key);
            var9.jobDetail = var3.jobDetail;
         }
      }
   }

   @Override
   public boolean removeJob(JobKey var1) {
      boolean var2 = false;
      synchronized (this.lock) {
         for (OperableTrigger var6 : this.getTriggersForJob(var1)) {
            this.removeTrigger(var6.getKey());
            var2 = true;
         }

         var2 = this.jobsByKey.remove(var1) != null | var2;
         if (var2) {
            HashMap var10 = this.jobsByGroup.get(var1.getGroup());
            if (var10 != null) {
               var10.remove(var1);
               if (var10.size() == 0) {
                  this.jobsByGroup.remove(var1.getGroup());
               }
            }
         }

         return var2;
      }
   }

   @Override
   public boolean removeJobs(List<JobKey> var1) {
      boolean var2 = true;
      synchronized (this.lock) {
         for (JobKey var5 : var1) {
            var2 = this.removeJob(var5) && var2;
         }

         return var2;
      }
   }

   @Override
   public boolean removeTriggers(List<TriggerKey> var1) {
      boolean var2 = true;
      synchronized (this.lock) {
         for (TriggerKey var5 : var1) {
            var2 = this.removeTrigger(var5) && var2;
         }

         return var2;
      }
   }

   @Override
   public void storeJobsAndTriggers(Map<JobDetail, Set<? extends Trigger>> var1, boolean var2) {
      synchronized (this.lock) {
         if (!var2) {
            for (Entry var5 : var1.entrySet()) {
               if (this.checkExists(((JobDetail)var5.getKey()).getKey())) {
                  throw new ObjectAlreadyExistsException((JobDetail)var5.getKey());
               }

               for (Trigger var7 : (Set<? extends Trigger>)var5.getValue()) {
                  if (this.checkExists(var7.getKey())) {
                     throw new ObjectAlreadyExistsException(var7);
                  }
               }
            }
         }

         for (Entry var11 : var1.entrySet()) {
            this.storeJob((JobDetail)var11.getKey(), true);

            for (Trigger var13 : (Set<? extends Trigger>)var11.getValue()) {
               this.storeTrigger((OperableTrigger)var13, true);
            }
         }
      }
   }

   @Override
   public void storeTrigger(OperableTrigger var1, boolean var2) {
      TriggerWrapper var3 = new TriggerWrapper((OperableTrigger)var1.clone());
      synchronized (this.lock) {
         if (this.triggersByKey.get(var3.key) != null) {
            if (!var2) {
               throw new ObjectAlreadyExistsException(var1);
            }

            this.removeTrigger(var1.getKey(), false);
         }

         if (this.retrieveJob(var1.getJobKey()) == null) {
            throw new JobPersistenceException("The job (" + var1.getJobKey() + ") referenced by the trigger does not exist.");
         } else {
            List<TriggerWrapper> var5 = this.triggersByJob.get(var3.jobKey);
            if (var5 == null) {
               var5 = new ArrayList<TriggerWrapper>(1);
               this.triggersByJob.put(var3.jobKey, (List<TriggerWrapper>)var5);
            }

            var5.add(var3);
            HashMap var6 = this.triggersByGroup.get(var1.getKey().getGroup());
            if (var6 == null) {
               var6 = new HashMap(100);
               this.triggersByGroup.put(var1.getKey().getGroup(), var6);
            }

            var6.put(var1.getKey(), var3);
            this.triggersByKey.put(var3.key, var3);
            if (this.pausedTriggerGroups.contains(var1.getKey().getGroup()) || this.pausedJobGroups.contains(var1.getJobKey().getGroup())) {
               var3.state = 4;
               if (this.blockedJobs.contains(var3.jobKey)) {
                  var3.state = 6;
               }
            } else if (this.blockedJobs.contains(var3.jobKey)) {
               var3.state = 5;
            } else {
               this.timeTriggers.add(var3);
            }
         }
      }
   }

   @Override
   public boolean removeTrigger(TriggerKey var1) {
      return this.removeTrigger(var1, true);
   }

   private boolean removeTrigger(TriggerKey var1, boolean var2) {
      synchronized (this.lock) {
         TriggerWrapper var5 = this.triggersByKey.remove(var1);
         boolean var3 = var5 != null;
         if (var3) {
            HashMap var6 = this.triggersByGroup.get(var1.getGroup());
            if (var6 != null) {
               var6.remove(var1);
               if (var6.size() == 0) {
                  this.triggersByGroup.remove(var1.getGroup());
               }
            }

            List var7 = this.triggersByJob.get(var5.jobKey);
            if (var7 != null) {
               var7.remove(var5);
               if (var7.isEmpty()) {
                  this.triggersByJob.remove(var5.jobKey);
               }
            }

            this.timeTriggers.remove(var5);
            if (var2) {
               JobWrapper var8 = this.jobsByKey.get(var5.jobKey);
               List var9 = this.getTriggersForJob(var5.jobKey);
               if ((var9 == null || var9.size() == 0) && !var8.jobDetail.isDurable() && this.removeJob(var8.key)) {
                  this.signaler.notifySchedulerListenersJobDeleted(var8.key);
               }
            }
         }

         return var3;
      }
   }

   @Override
   public boolean replaceTrigger(TriggerKey var1, OperableTrigger var2) {
      synchronized (this.lock) {
         TriggerWrapper var5 = this.triggersByKey.remove(var1);
         boolean var3 = var5 != null;
         if (!var3) {
            return var3;
         } else if (!var5.getTrigger().getJobKey().equals(var2.getJobKey())) {
            throw new JobPersistenceException("New trigger is not related to the same job as the old trigger.");
         } else {
            HashMap var6 = this.triggersByGroup.get(var1.getGroup());
            if (var6 != null) {
               var6.remove(var1);
               if (var6.size() == 0) {
                  this.triggersByGroup.remove(var1.getGroup());
               }
            }

            List var7 = this.triggersByJob.get(var5.jobKey);
            if (var7 != null) {
               var7.remove(var5);
               if (var7.isEmpty()) {
                  this.triggersByJob.remove(var5.jobKey);
               }
            }

            this.timeTriggers.remove(var5);

            try {
               this.storeTrigger(var2, false);
               return var3;
            } catch (JobPersistenceException var10) {
               this.storeTrigger(var5.getTrigger(), false);
               throw var10;
            }
         }
      }
   }

   @Override
   public JobDetail retrieveJob(JobKey var1) {
      synchronized (this.lock) {
         JobWrapper var3 = this.jobsByKey.get(var1);
         return var3 != null ? (JobDetail)var3.jobDetail.clone() : null;
      }
   }

   @Override
   public OperableTrigger retrieveTrigger(TriggerKey var1) {
      synchronized (this.lock) {
         TriggerWrapper var3 = this.triggersByKey.get(var1);
         return var3 != null ? (OperableTrigger)var3.getTrigger().clone() : null;
      }
   }

   @Override
   public boolean checkExists(JobKey var1) {
      synchronized (this.lock) {
         JobWrapper var3 = this.jobsByKey.get(var1);
         return var3 != null;
      }
   }

   @Override
   public boolean checkExists(TriggerKey var1) {
      synchronized (this.lock) {
         TriggerWrapper var3 = this.triggersByKey.get(var1);
         return var3 != null;
      }
   }

   @Override
   public Trigger.TriggerState getTriggerState(TriggerKey var1) {
      synchronized (this.lock) {
         TriggerWrapper var3 = this.triggersByKey.get(var1);
         if (var3 == null) {
            return Trigger.TriggerState.NONE;
         } else if (var3.state == 3) {
            return Trigger.TriggerState.COMPLETE;
         } else if (var3.state == 4) {
            return Trigger.TriggerState.PAUSED;
         } else if (var3.state == 6) {
            return Trigger.TriggerState.PAUSED;
         } else if (var3.state == 5) {
            return Trigger.TriggerState.BLOCKED;
         } else {
            return var3.state == 7 ? Trigger.TriggerState.ERROR : Trigger.TriggerState.NORMAL;
         }
      }
   }

   @Override
   public void resetTriggerFromErrorState(TriggerKey var1) {
      synchronized (this.lock) {
         TriggerWrapper var3 = this.triggersByKey.get(var1);
         if (var3 != null && var3.trigger != null) {
            if (var3.state == 7) {
               if (this.pausedTriggerGroups.contains(var1.getGroup())) {
                  var3.state = 4;
               } else {
                  var3.state = 0;
                  this.timeTriggers.add(var3);
               }
            }
         }
      }
   }

   @Override
   public void storeCalendar(String var1, Calendar var2, boolean var3, boolean var4) {
      var2 = (Calendar)var2.clone();
      synchronized (this.lock) {
         Object var6 = this.calendarsByName.get(var1);
         if (var6 != null && !var3) {
            throw new ObjectAlreadyExistsException("Calendar with name '" + var1 + "' already exists.");
         } else {
            if (var6 != null) {
               this.calendarsByName.remove(var1);
            }

            this.calendarsByName.put(var1, var2);
            if (var6 != null && var4) {
               for (TriggerWrapper var8 : this.getTriggerWrappersForCalendar(var1)) {
                  OperableTrigger var9 = var8.getTrigger();
                  boolean var10 = this.timeTriggers.remove(var8);
                  var9.updateWithNewCalendar(var2, this.getMisfireThreshold());
                  if (var10) {
                     this.timeTriggers.add(var8);
                  }
               }
            }
         }
      }
   }

   @Override
   public boolean removeCalendar(String var1) {
      int var2 = 0;
      synchronized (this.lock) {
         for (TriggerWrapper var5 : this.triggersByKey.values()) {
            OperableTrigger var6 = var5.trigger;
            if (var6.getCalendarName() != null && var6.getCalendarName().equals(var1)) {
               var2++;
            }
         }
      }

      if (var2 > 0) {
         throw new JobPersistenceException("Calender cannot be removed if it referenced by a Trigger!");
      } else {
         return this.calendarsByName.remove(var1) != null;
      }
   }

   @Override
   public Calendar retrieveCalendar(String var1) {
      synchronized (this.lock) {
         Calendar var3 = this.calendarsByName.get(var1);
         return var3 != null ? (Calendar)var3.clone() : null;
      }
   }

   @Override
   public int getNumberOfJobs() {
      synchronized (this.lock) {
         return this.jobsByKey.size();
      }
   }

   @Override
   public int getNumberOfTriggers() {
      synchronized (this.lock) {
         return this.triggersByKey.size();
      }
   }

   @Override
   public int getNumberOfCalendars() {
      synchronized (this.lock) {
         return this.calendarsByName.size();
      }
   }

   @Override
   public Set<JobKey> getJobKeys(GroupMatcher<JobKey> var1) {
      Set<JobKey> var2 = null;
      synchronized (this.lock) {
         StringMatcher.StringOperatorName var4 = var1.getCompareWithOperator();
         String var5 = var1.getCompareToValue();
         Iterator var7;
         switch (var4) {
            case EQUALS:
               HashMap<JobKey, JobWrapper> var6 = this.jobsByGroup.get(var5);
               if (var6 == null) {
                  return (Set<JobKey>)(var2 == null ? Collections.emptySet() : var2);
               }

               var2 = new HashSet<JobKey>();

               for (JobWrapper var14 : var6.values()) {
                  if (var14 != null) {
                     var2.add(var14.jobDetail.getKey());
                  }
               }

               return (Set<JobKey>)(var2 == null ? Collections.emptySet() : var2);
            default:
               var7 = this.jobsByGroup.entrySet().iterator();
         }

         while (var7.hasNext()) {
            Entry var8 = (Entry)var7.next();
            if (var4.evaluate((String)var8.getKey(), var5) && var8.getValue() != null) {
               if (var2 == null) {
                  var2 = new HashSet<JobKey>();
               }

               for (JobWrapper var10 : ((HashMap<JobKey, JobWrapper>)var8.getValue()).values()) {
                  if (var10 != null) {
                     var2.add(var10.jobDetail.getKey());
                  }
               }
            }
         }
      }

      return (Set<JobKey>)(var2 == null ? Collections.emptySet() : var2);
   }

   @Override
   public List<String> getCalendarNames() {
      synchronized (this.lock) {
         return new LinkedList<>(this.calendarsByName.keySet());
      }
   }

   @Override
   public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> var1) {
      Set<TriggerKey> var2 = null;
      synchronized (this.lock) {
         StringMatcher.StringOperatorName var4 = var1.getCompareWithOperator();
         String var5 = var1.getCompareToValue();
         Iterator var7;
         switch (var4) {
            case EQUALS:
               HashMap<TriggerKey, TriggerWrapper> var6 = this.triggersByGroup.get(var5);
               if (var6 == null) {
                  return (Set<TriggerKey>)(var2 == null ? Collections.emptySet() : var2);
               }

               var2 = new HashSet<TriggerKey>();

               for (TriggerWrapper var14 : var6.values()) {
                  if (var14 != null) {
                     var2.add(var14.trigger.getKey());
                  }
               }

               return (Set<TriggerKey>)(var2 == null ? Collections.emptySet() : var2);
            default:
               var7 = this.triggersByGroup.entrySet().iterator();
         }

         while (var7.hasNext()) {
            Entry var8 = (Entry)var7.next();
            if (var4.evaluate((String)var8.getKey(), var5) && var8.getValue() != null) {
               if (var2 == null) {
                  var2 = new HashSet<TriggerKey>();
               }

               for (TriggerWrapper var10 : ((HashMap<TriggerKey, TriggerWrapper>)var8.getValue()).values()) {
                  if (var10 != null) {
                     var2.add(var10.trigger.getKey());
                  }
               }
            }
         }
      }

      return (Set<TriggerKey>)(var2 == null ? Collections.emptySet() : var2);
   }

   @Override
   public List<String> getJobGroupNames() {
      synchronized (this.lock) {
         return new LinkedList<>(this.jobsByGroup.keySet());
      }
   }

   @Override
   public List<String> getTriggerGroupNames() {
      synchronized (this.lock) {
         return new LinkedList<>(this.triggersByGroup.keySet());
      }
   }

   @Override
   public List<OperableTrigger> getTriggersForJob(JobKey var1) {
      ArrayList var2 = new ArrayList();
      synchronized (this.lock) {
         List<TriggerWrapper> var4 = this.triggersByJob.get(var1);
         if (var4 != null) {
            for (TriggerWrapper var6 : var4) {
               var2.add((OperableTrigger)var6.trigger.clone());
            }
         }

         return var2;
      }
   }

   protected ArrayList<TriggerWrapper> getTriggerWrappersForJob(JobKey var1) {
      ArrayList var2 = new ArrayList();
      synchronized (this.lock) {
         List<TriggerWrapper> var4 = this.triggersByJob.get(var1);
         if (var4 != null) {
            for (TriggerWrapper var6 : var4) {
               var2.add(var6);
            }
         }

         return var2;
      }
   }

   protected ArrayList<TriggerWrapper> getTriggerWrappersForCalendar(String var1) {
      ArrayList var2 = new ArrayList();
      synchronized (this.lock) {
         for (TriggerWrapper var5 : this.triggersByKey.values()) {
            String var6 = var5.getTrigger().getCalendarName();
            if (var6 != null && var6.equals(var1)) {
               var2.add(var5);
            }
         }

         return var2;
      }
   }

   @Override
   public void pauseTrigger(TriggerKey var1) {
      synchronized (this.lock) {
         TriggerWrapper var3 = this.triggersByKey.get(var1);
         if (var3 != null && var3.trigger != null) {
            if (var3.state != 3) {
               if (var3.state == 5) {
                  var3.state = 6;
               } else {
                  var3.state = 4;
               }

               this.timeTriggers.remove(var3);
            }
         }
      }
   }

   public List<String> pauseTriggers(GroupMatcher<TriggerKey> var1) {
      synchronized (this.lock) {
         LinkedList<String> var2 = new LinkedList<String>();
         StringMatcher.StringOperatorName var4 = var1.getCompareWithOperator();
         switch (var4) {
            case EQUALS:
               if (this.pausedTriggerGroups.add(var1.getCompareToValue())) {
                  var2.add(var1.getCompareToValue());
               }
               break;
            default:
               for (String var6 : this.triggersByGroup.keySet()) {
                  if (var4.evaluate(var6, var1.getCompareToValue()) && this.pausedTriggerGroups.add(var1.getCompareToValue())) {
                     var2.add(var6);
                  }
               }
         }

         for (String var13 : var2) {
            for (TriggerKey var9 : this.getTriggerKeys(GroupMatcher.triggerGroupEquals(var13))) {
               this.pauseTrigger(var9);
            }
         }

         return var2;
      }
   }

   @Override
   public void pauseJob(JobKey var1) {
      synchronized (this.lock) {
         for (OperableTrigger var5 : this.getTriggersForJob(var1)) {
            this.pauseTrigger(var5.getKey());
         }
      }
   }

   public List<String> pauseJobs(GroupMatcher<JobKey> var1) {
      LinkedList<String> var2 = new LinkedList<String>();
      synchronized (this.lock) {
         StringMatcher.StringOperatorName var4 = var1.getCompareWithOperator();
         switch (var4) {
            case EQUALS:
               if (this.pausedJobGroups.add(var1.getCompareToValue())) {
                  var2.add(var1.getCompareToValue());
               }
               break;
            default:
               for (String var6 : this.jobsByGroup.keySet()) {
                  if (var4.evaluate(var6, var1.getCompareToValue()) && this.pausedJobGroups.add(var6)) {
                     var2.add(var6);
                  }
               }
         }

         for (String var15 : var2) {
            for (JobKey var8 : this.getJobKeys(GroupMatcher.jobGroupEquals(var15))) {
               for (OperableTrigger var11 : this.getTriggersForJob(var8)) {
                  this.pauseTrigger(var11.getKey());
               }
            }
         }

         return var2;
      }
   }

   @Override
   public void resumeTrigger(TriggerKey var1) {
      synchronized (this.lock) {
         TriggerWrapper var3 = this.triggersByKey.get(var1);
         if (var3 != null && var3.trigger != null) {
            OperableTrigger var4 = var3.getTrigger();
            if (var3.state == 4 || var3.state == 6) {
               if (this.blockedJobs.contains(var4.getJobKey())) {
                  var3.state = 5;
               } else {
                  var3.state = 0;
               }

               this.applyMisfire(var3);
               if (var3.state == 0) {
                  this.timeTriggers.add(var3);
               }
            }
         }
      }
   }

   public List<String> resumeTriggers(GroupMatcher<TriggerKey> var1) {
      HashSet var2 = new HashSet();
      synchronized (this.lock) {
         for (TriggerKey var6 : this.getTriggerKeys(var1)) {
            var2.add(var6.getGroup());
            if (this.triggersByKey.get(var6) != null) {
               String var7 = this.triggersByKey.get(var6).jobKey.getGroup();
               if (this.pausedJobGroups.contains(var7)) {
                  continue;
               }
            }

            this.resumeTrigger(var6);
         }

         StringMatcher.StringOperatorName var12 = var1.getCompareWithOperator();
         LinkedList<String> var13 = new LinkedList<String>();
         String var14 = var1.getCompareToValue();
         switch (var12) {
            case EQUALS:
               if (this.pausedTriggerGroups.contains(var14)) {
                  var13.add(var1.getCompareToValue());
               }
               break;
            default:
               for (String var9 : this.pausedTriggerGroups) {
                  if (var12.evaluate(var9, var14)) {
                     var13.add(var9);
                  }
               }
         }

         for (String var16 : var13) {
            this.pausedTriggerGroups.remove(var16);
         }
      }

      return new ArrayList<>(var2);
   }

   @Override
   public void resumeJob(JobKey var1) {
      synchronized (this.lock) {
         for (OperableTrigger var5 : this.getTriggersForJob(var1)) {
            this.resumeTrigger(var5.getKey());
         }
      }
   }

   @Override
   public Collection<String> resumeJobs(GroupMatcher<JobKey> var1) {
      HashSet<String> var2 = new HashSet<String>();
      synchronized (this.lock) {
         Set<JobKey> var4 = this.getJobKeys(var1);

         for (String var6 : this.pausedJobGroups) {
            if (var1.getCompareWithOperator().evaluate(var6, var1.getCompareToValue())) {
               var2.add(var6);
            }
         }

         for (String var14 : var2) {
            this.pausedJobGroups.remove(var14);
         }

         for (JobKey var15 : var4) {
            for (OperableTrigger var9 : this.getTriggersForJob(var15)) {
               this.resumeTrigger(var9.getKey());
            }
         }

         return var2;
      }
   }

   @Override
   public void pauseAll() {
      synchronized (this.lock) {
         for (String var4 : this.getTriggerGroupNames()) {
            this.pauseTriggers(GroupMatcher.triggerGroupEquals(var4));
         }
      }
   }

   @Override
   public void resumeAll() {
      synchronized (this.lock) {
         this.pausedJobGroups.clear();
         this.resumeTriggers(GroupMatcher.anyTriggerGroup());
      }
   }

   protected boolean applyMisfire(TriggerWrapper var1) {
      long var2 = System.currentTimeMillis();
      if (this.getMisfireThreshold() > 0L) {
         var2 -= this.getMisfireThreshold();
      }

      Date var4 = var1.trigger.getNextFireTime();
      if (var4 != null && var4.getTime() <= var2 && var1.trigger.getMisfireInstruction() != -1) {
         Calendar var5 = null;
         if (var1.trigger.getCalendarName() != null) {
            var5 = this.retrieveCalendar(var1.trigger.getCalendarName());
         }

         this.signaler.notifyTriggerListenersMisfired((OperableTrigger)var1.trigger.clone());
         var1.trigger.updateAfterMisfire(var5);
         if (var1.trigger.getNextFireTime() == null) {
            var1.state = 3;
            this.signaler.notifySchedulerListenersFinalized(var1.trigger);
            synchronized (this.lock) {
               this.timeTriggers.remove(var1);
            }
         } else if (var4.equals(var1.trigger.getNextFireTime())) {
            return false;
         }

         return true;
      } else {
         return false;
      }
   }

   protected String getFiredTriggerRecordId() {
      return String.valueOf(ftrCtr.incrementAndGet());
   }

   @Override
   public List<OperableTrigger> acquireNextTriggers(long var1, int var3, long var4) {
      synchronized (this.lock) {
         ArrayList var7 = new ArrayList();
         HashSet var8 = new HashSet();
         HashSet var9 = new HashSet();
         long var10 = var1;
         if (this.timeTriggers.size() == 0) {
            return var7;
         } else {
            while (true) {
               TriggerWrapper var12;
               try {
                  var12 = this.timeTriggers.first();
                  if (var12 == null) {
                     break;
                  }

                  this.timeTriggers.remove(var12);
               } catch (NoSuchElementException var17) {
                  break;
               }

               if (var12.trigger.getNextFireTime() != null) {
                  if (this.applyMisfire(var12)) {
                     if (var12.trigger.getNextFireTime() != null) {
                        this.timeTriggers.add(var12);
                     }
                  } else {
                     if (var12.getTrigger().getNextFireTime().getTime() > var10) {
                        this.timeTriggers.add(var12);
                        break;
                     }

                     JobKey var13 = var12.trigger.getJobKey();
                     JobDetail var14 = this.jobsByKey.get(var12.trigger.getJobKey()).jobDetail;
                     if (var14.isConcurrentExectionDisallowed()) {
                        if (var8.contains(var13)) {
                           var9.add(var12);
                           continue;
                        }

                        var8.add(var13);
                     }

                     var12.state = 1;
                     var12.trigger.setFireInstanceId(this.getFiredTriggerRecordId());
                     OperableTrigger var15 = (OperableTrigger)var12.trigger.clone();
                     if (var7.isEmpty()) {
                        var10 = Math.max(var12.trigger.getNextFireTime().getTime(), System.currentTimeMillis()) + var4;
                     }

                     var7.add(var15);
                     if (var7.size() == var3) {
                        break;
                     }
                  }
               }
            }

            if (var9.size() > 0) {
               this.timeTriggers.addAll(var9);
            }

            return var7;
         }
      }
   }

   @Override
   public void releaseAcquiredTrigger(OperableTrigger var1) {
      synchronized (this.lock) {
         TriggerWrapper var3 = this.triggersByKey.get(var1.getKey());
         if (var3 != null && var3.state == 1) {
            var3.state = 0;
            this.timeTriggers.add(var3);
         }
      }
   }

   @Override
   public List<TriggerFiredResult> triggersFired(List<OperableTrigger> var1) {
      synchronized (this.lock) {
         ArrayList var3 = new ArrayList();
         Iterator var4 = var1.iterator();

         while (true) {
            OperableTrigger var5;
            TriggerWrapper var6;
            Calendar var7;
            while (true) {
               if (!var4.hasNext()) {
                  return var3;
               }

               var5 = (OperableTrigger)var4.next();
               var6 = this.triggersByKey.get(var5.getKey());
               if (var6 != null && var6.trigger != null && var6.state == 1) {
                  var7 = null;
                  if (var6.trigger.getCalendarName() == null) {
                     break;
                  }

                  var7 = this.retrieveCalendar(var6.trigger.getCalendarName());
                  if (var7 != null) {
                     break;
                  }
               }
            }

            Date var8 = var5.getPreviousFireTime();
            this.timeTriggers.remove(var6);
            var6.trigger.triggered(var7);
            var5.triggered(var7);
            var6.state = 0;
            TriggerFiredBundle var9 = new TriggerFiredBundle(
               this.retrieveJob(var6.jobKey), var5, var7, false, new Date(), var5.getPreviousFireTime(), var8, var5.getNextFireTime()
            );
            JobDetail var10 = var9.getJobDetail();
            if (!var10.isConcurrentExectionDisallowed()) {
               if (var6.trigger.getNextFireTime() != null) {
                  synchronized (this.lock) {
                     this.timeTriggers.add(var6);
                  }
               }
            } else {
               for (TriggerWrapper var13 : this.getTriggerWrappersForJob(var10.getKey())) {
                  if (var13.state == 0) {
                     var13.state = 5;
                  }

                  if (var13.state == 4) {
                     var13.state = 6;
                  }

                  this.timeTriggers.remove(var13);
               }

               this.blockedJobs.add(var10.getKey());
            }

            var3.add(new TriggerFiredResult(var9));
         }
      }
   }

   @Override
   public void triggeredJobComplete(OperableTrigger var1, JobDetail var2, Trigger.CompletedExecutionInstruction var3) {
      synchronized (this.lock) {
         JobWrapper var5 = this.jobsByKey.get(var2.getKey());
         TriggerWrapper var6 = this.triggersByKey.get(var1.getKey());
         if (var5 != null) {
            JobDetail var7 = var5.jobDetail;
            if (var7.isPersistJobDataAfterExecution()) {
               JobDataMap var8 = var2.getJobDataMap();
               if (var8 != null) {
                  var8 = (JobDataMap)var8.clone();
                  var8.clearDirtyFlag();
               }

               var7 = var7.getJobBuilder().setJobData(var8).build();
               var5.jobDetail = var7;
            }

            if (var7.isConcurrentExectionDisallowed()) {
               this.blockedJobs.remove(var7.getKey());

               for (TriggerWrapper var10 : this.getTriggerWrappersForJob(var7.getKey())) {
                  if (var10.state == 5) {
                     var10.state = 0;
                     this.timeTriggers.add(var10);
                  }

                  if (var10.state == 6) {
                     var10.state = 4;
                  }
               }

               this.signaler.signalSchedulingChange(0L);
            }
         } else {
            this.blockedJobs.remove(var2.getKey());
         }

         if (var6 != null) {
            if (var3 == Trigger.CompletedExecutionInstruction.DELETE_TRIGGER) {
               if (var1.getNextFireTime() == null) {
                  if (var6.getTrigger().getNextFireTime() == null) {
                     this.removeTrigger(var1.getKey());
                  }
               } else {
                  this.removeTrigger(var1.getKey());
                  this.signaler.signalSchedulingChange(0L);
               }
            } else if (var3 == Trigger.CompletedExecutionInstruction.SET_TRIGGER_COMPLETE) {
               var6.state = 3;
               this.timeTriggers.remove(var6);
               this.signaler.signalSchedulingChange(0L);
            } else if (var3 == Trigger.CompletedExecutionInstruction.SET_TRIGGER_ERROR) {
               this.getLog().info("Trigger " + var1.getKey() + " set to ERROR state.");
               var6.state = 7;
               this.signaler.signalSchedulingChange(0L);
            } else if (var3 == Trigger.CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR) {
               this.getLog().info("All triggers of Job " + var1.getJobKey() + " set to ERROR state.");
               this.setAllTriggersOfJobToState(var1.getJobKey(), 7);
               this.signaler.signalSchedulingChange(0L);
            } else if (var3 == Trigger.CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE) {
               this.setAllTriggersOfJobToState(var1.getJobKey(), 3);
               this.signaler.signalSchedulingChange(0L);
            }
         }
      }
   }

   @Override
   public long getAcquireRetryDelay(int var1) {
      return 20L;
   }

   protected void setAllTriggersOfJobToState(JobKey var1, int var2) {
      for (TriggerWrapper var5 : this.getTriggerWrappersForJob(var1)) {
         var5.state = var2;
         if (var2 != 0) {
            this.timeTriggers.remove(var5);
         }
      }
   }

   protected String peekTriggers() {
      StringBuilder var1 = new StringBuilder();
      synchronized (this.lock) {
         for (TriggerWrapper var4 : this.triggersByKey.values()) {
            var1.append(var4.trigger.getKey().getName());
            var1.append("/");
         }
      }

      var1.append(" | ");
      synchronized (this.lock) {
         for (TriggerWrapper var11 : this.timeTriggers) {
            var1.append(var11.trigger.getKey().getName());
            var1.append("->");
         }
      }

      return var1.toString();
   }

   @Override
   public Set<String> getPausedTriggerGroups() {
      HashSet var1 = new HashSet();
      var1.addAll(this.pausedTriggerGroups);
      return var1;
   }

   @Override
   public void setInstanceId(String var1) {
   }

   @Override
   public void setInstanceName(String var1) {
   }

   @Override
   public void setThreadPoolSize(int var1) {
   }

   @Override
   public long getEstimatedTimeToReleaseAndAcquireTrigger() {
      return 5L;
   }

   @Override
   public boolean isClustered() {
      return false;
   }
}
