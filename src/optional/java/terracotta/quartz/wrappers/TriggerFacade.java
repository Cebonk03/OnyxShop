package org.terracotta.quartz.wrappers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.terracotta.quartz.collections.ToolkitDSHolder;
import org.terracotta.toolkit.store.ToolkitStore;

public class TriggerFacade {
   private final ToolkitStore<TriggerKey, TriggerWrapper> triggersByFQN;
   private final Set<String> allTriggersGroupNames;
   private final Set<String> pausedTriggerGroupNames;
   private final ToolkitStore<String, FiredTrigger> firedTriggers;

   public TriggerFacade(ToolkitDSHolder var1) {
      this.triggersByFQN = var1.getOrCreateTriggersMap();
      this.allTriggersGroupNames = var1.getOrCreateAllTriggersGroupsSet();
      this.pausedTriggerGroupNames = var1.getOrCreatePausedTriggerGroupsSet();
      this.firedTriggers = var1.getOrCreateFiredTriggersMap();
   }

   public TriggerWrapper get(TriggerKey var1) {
      return (TriggerWrapper)this.triggersByFQN.get(var1);
   }

   public boolean containsKey(TriggerKey var1) {
      return this.triggersByFQN.containsKey(var1);
   }

   public void put(TriggerKey var1, TriggerWrapper var2) {
      this.triggersByFQN.putNoReturn(var1, var2);
   }

   public TriggerWrapper remove(TriggerKey var1) {
      return (TriggerWrapper)this.triggersByFQN.remove(var1);
   }

   public FiredTrigger getFiredTrigger(String var1) {
      return (FiredTrigger)this.firedTriggers.get(var1);
   }

   public boolean containsFiredTrigger(String var1) {
      return this.firedTriggers.containsKey(var1);
   }

   public void putFiredTrigger(String var1, FiredTrigger var2) {
      this.firedTriggers.putNoReturn(var1, var2);
   }

   public FiredTrigger removeFiredTrigger(String var1) {
      return (FiredTrigger)this.firedTriggers.remove(var1);
   }

   public boolean addGroup(String var1) {
      return this.allTriggersGroupNames.add(var1);
   }

   public boolean hasGroup(String var1) {
      return this.allTriggersGroupNames.contains(var1);
   }

   public boolean removeGroup(String var1) {
      return this.allTriggersGroupNames.remove(var1);
   }

   public boolean addPausedGroup(String var1) {
      return this.pausedTriggerGroupNames.add(var1);
   }

   public boolean pausedGroupsContain(String var1) {
      return this.pausedTriggerGroupNames.contains(var1);
   }

   public boolean removePausedGroup(String var1) {
      return this.pausedTriggerGroupNames.remove(var1);
   }

   public Set<String> allTriggersGroupNames() {
      return this.allTriggersGroupNames;
   }

   public Set<String> allPausedTriggersGroupNames() {
      return this.pausedTriggerGroupNames;
   }

   public Set<TriggerKey> allTriggerKeys() {
      return this.triggersByFQN.keySet();
   }

   public Collection<FiredTrigger> allFiredTriggers() {
      return this.firedTriggers.values();
   }

   public int numberOfTriggers() {
      return this.triggersByFQN.size();
   }

   public List<TriggerWrapper> getTriggerWrappersForJob(JobKey var1) {
      ArrayList var2 = new ArrayList();

      for (TriggerKey var4 : this.triggersByFQN.keySet()) {
         TriggerWrapper var5 = (TriggerWrapper)this.triggersByFQN.get(var4);
         if (var5.getJobKey().equals(var1)) {
            var2.add(var5);
         }
      }

      return var2;
   }

   public List<TriggerWrapper> getTriggerWrappersForCalendar(String var1) {
      ArrayList var2 = new ArrayList();

      for (TriggerKey var4 : this.triggersByFQN.keySet()) {
         TriggerWrapper var5 = (TriggerWrapper)this.triggersByFQN.get(var4);
         String var6 = var5.getCalendarName();
         if (var6 != null && var6.equals(var1)) {
            var2.add(var5);
         }
      }

      return var2;
   }

   public void removeAllPausedGroups(Collection<String> var1) {
      this.pausedTriggerGroupNames.removeAll(var1);
   }
}
