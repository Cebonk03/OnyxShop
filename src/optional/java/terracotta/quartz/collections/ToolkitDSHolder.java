package org.terracotta.quartz.collections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.quartz.Calendar;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.terracotta.quartz.wrappers.FiredTrigger;
import org.terracotta.quartz.wrappers.JobWrapper;
import org.terracotta.quartz.wrappers.TriggerWrapper;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.builder.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitConfigFields.Consistency;

public class ToolkitDSHolder {
   private static final String JOBS_MAP_PREFIX = "_tc_quartz_jobs";
   private static final String ALL_JOBS_GROUP_NAMES_SET_PREFIX = "_tc_quartz_grp_names";
   private static final String PAUSED_GROUPS_SET_PREFIX = "_tc_quartz_grp_paused_names";
   private static final String BLOCKED_JOBS_SET_PREFIX = "_tc_quartz_blocked_jobs";
   private static final String JOBS_GROUP_MAP_PREFIX = "_tc_quartz_grp_jobs_";
   private static final String TRIGGERS_MAP_PREFIX = "_tc_quartz_triggers";
   private static final String TRIGGERS_GROUP_MAP_PREFIX = "_tc_quartz_grp_triggers_";
   private static final String ALL_TRIGGERS_GROUP_NAMES_SET_PREFIX = "_tc_quartz_grp_names_triggers";
   private static final String PAUSED_TRIGGER_GROUPS_SET_PREFIX = "_tc_quartz_grp_paused_trogger_names";
   private static final String TIME_TRIGGER_SORTED_SET_PREFIX = "_tc_time_trigger_sorted_set";
   private static final String FIRED_TRIGGER_MAP_PREFIX = "_tc_quartz_fired_trigger";
   private static final String CALENDAR_WRAPPER_MAP_PREFIX = "_tc_quartz_calendar_wrapper";
   private static final String SINGLE_LOCK_NAME_PREFIX = "_tc_quartz_single_lock";
   private static final String DELIMETER = "|";
   private final String jobStoreName;
   protected final Toolkit toolkit;
   private final AtomicReference<SerializedToolkitStore<JobKey, JobWrapper>> jobsMapReference = new AtomicReference<>();
   private final AtomicReference<SerializedToolkitStore<TriggerKey, TriggerWrapper>> triggersMapReference = new AtomicReference<>();
   private final AtomicReference<ToolkitSet<String>> allGroupsReference = new AtomicReference<>();
   private final AtomicReference<ToolkitSet<String>> allTriggersGroupsReference = new AtomicReference<>();
   private final AtomicReference<ToolkitSet<String>> pausedGroupsReference = new AtomicReference<>();
   private final AtomicReference<ToolkitSet<JobKey>> blockedJobsReference = new AtomicReference<>();
   private final Map<String, ToolkitSet<String>> jobsGroupSet = new HashMap<>();
   private final Map<String, ToolkitSet<String>> triggersGroupSet = new HashMap<>();
   private final AtomicReference<ToolkitSet<String>> pausedTriggerGroupsReference = new AtomicReference<>();
   private final AtomicReference<ToolkitStore<String, FiredTrigger>> firedTriggersMapReference = new AtomicReference<>();
   private final AtomicReference<ToolkitStore<String, Calendar>> calendarWrapperMapReference = new AtomicReference<>();
   private final AtomicReference<TimeTriggerSet> timeTriggerSetReference = new AtomicReference<>();
   private final Map<String, ToolkitStore<?, ?>> toolkitMaps = new HashMap<>();

   public ToolkitDSHolder(String var1, Toolkit var2) {
      this.jobStoreName = var1;
      this.toolkit = var2;
   }

   protected final String generateName(String var1) {
      return var1 + "|" + this.jobStoreName;
   }

   public SerializedToolkitStore<JobKey, JobWrapper> getOrCreateJobsMap() {
      String var1 = this.generateName("_tc_quartz_jobs");
      SerializedToolkitStore var2 = new SerializedToolkitStore(this.createStore(var1));
      this.jobsMapReference.compareAndSet(null, var2);
      return this.jobsMapReference.get();
   }

   protected ToolkitStore<?, ?> toolkitMap(String var1) {
      ToolkitStore var2 = this.toolkitMaps.get(var1);
      if (var2 != null && !var2.isDestroyed()) {
         return var2;
      } else {
         var2 = this.createStore(var1);
         this.toolkitMaps.put(var1, var2);
         return var2;
      }
   }

   private ToolkitStore createStore(String var1) {
      ToolkitStoreConfigBuilder var2 = new ToolkitStoreConfigBuilder();
      return this.toolkit.getStore(var1, var2.consistency(Consistency.STRONG).concurrency(1).build(), null);
   }

   public SerializedToolkitStore<TriggerKey, TriggerWrapper> getOrCreateTriggersMap() {
      String var1 = this.generateName("_tc_quartz_triggers");
      SerializedToolkitStore var2 = new SerializedToolkitStore(this.createStore(var1));
      this.triggersMapReference.compareAndSet(null, var2);
      return this.triggersMapReference.get();
   }

   public ToolkitStore<String, FiredTrigger> getOrCreateFiredTriggersMap() {
      String var1 = this.generateName("_tc_quartz_fired_trigger");
      ToolkitStore var2 = this.createStore(var1);
      this.firedTriggersMapReference.compareAndSet(null, var2);
      return this.firedTriggersMapReference.get();
   }

   public ToolkitStore<String, Calendar> getOrCreateCalendarWrapperMap() {
      String var1 = this.generateName("_tc_quartz_calendar_wrapper");
      ToolkitStore var2 = this.createStore(var1);
      this.calendarWrapperMapReference.compareAndSet(null, var2);
      return this.calendarWrapperMapReference.get();
   }

   public Set<String> getOrCreateAllGroupsSet() {
      String var1 = this.generateName("_tc_quartz_grp_names");
      ToolkitSet var2 = this.toolkit.getSet(var1, String.class);
      this.allGroupsReference.compareAndSet(null, var2);
      return (Set<String>)this.allGroupsReference.get();
   }

   public Set<JobKey> getOrCreateBlockedJobsSet() {
      String var1 = this.generateName("_tc_quartz_blocked_jobs");
      ToolkitSet var2 = this.toolkit.getSet(var1, JobKey.class);
      this.blockedJobsReference.compareAndSet(null, var2);
      return (Set<JobKey>)this.blockedJobsReference.get();
   }

   public Set<String> getOrCreatePausedGroupsSet() {
      String var1 = this.generateName("_tc_quartz_grp_paused_names");
      ToolkitSet var2 = this.toolkit.getSet(var1, String.class);
      this.pausedGroupsReference.compareAndSet(null, var2);
      return (Set<String>)this.pausedGroupsReference.get();
   }

   public Set<String> getOrCreatePausedTriggerGroupsSet() {
      String var1 = this.generateName("_tc_quartz_grp_paused_trogger_names");
      ToolkitSet var2 = this.toolkit.getSet(var1, String.class);
      this.pausedTriggerGroupsReference.compareAndSet(null, var2);
      return (Set<String>)this.pausedTriggerGroupsReference.get();
   }

   public Set<String> getOrCreateJobsGroupMap(String var1) {
      ToolkitSet var2 = this.jobsGroupSet.get(var1);
      if (var2 != null && !var2.isDestroyed()) {
         return var2;
      } else {
         String var3 = this.generateName("_tc_quartz_grp_jobs_" + var1);
         var2 = this.toolkit.getSet(var3, String.class);
         this.jobsGroupSet.put(var1, var2);
         return var2;
      }
   }

   public void removeJobsGroupMap(String var1) {
      ToolkitSet var2 = this.jobsGroupSet.remove(var1);
      if (var2 != null) {
         var2.destroy();
      }
   }

   public Set<String> getOrCreateTriggersGroupMap(String var1) {
      ToolkitSet var2 = this.triggersGroupSet.get(var1);
      if (var2 != null && !var2.isDestroyed()) {
         return var2;
      } else {
         String var3 = this.generateName("_tc_quartz_grp_triggers_" + var1);
         var2 = this.toolkit.getSet(var3, String.class);
         this.triggersGroupSet.put(var1, var2);
         return var2;
      }
   }

   public void removeTriggersGroupMap(String var1) {
      ToolkitSet var2 = this.triggersGroupSet.remove(var1);
      if (var2 != null) {
         var2.destroy();
      }
   }

   public Set<String> getOrCreateAllTriggersGroupsSet() {
      String var1 = this.generateName("_tc_quartz_grp_names_triggers");
      ToolkitSet var2 = this.toolkit.getSet(var1, String.class);
      this.allTriggersGroupsReference.compareAndSet(null, var2);
      return (Set<String>)this.allTriggersGroupsReference.get();
   }

   public TimeTriggerSet getOrCreateTimeTriggerSet() {
      String var1 = this.generateName("_tc_time_trigger_sorted_set");
      TimeTriggerSet var2 = new TimeTriggerSet(this.toolkit.getSortedSet(var1, TimeTrigger.class));
      this.timeTriggerSetReference.compareAndSet(null, var2);
      return this.timeTriggerSetReference.get();
   }

   public ToolkitLock getLock(ToolkitLockTypeInternal var1) {
      String var2 = this.generateName("_tc_quartz_single_lock");
      return ((ToolkitInternal)this.toolkit).getLock(var2, var1);
   }
}
