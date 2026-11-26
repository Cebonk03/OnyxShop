package org.terracotta.quartz.wrappers;

import java.util.Set;
import org.quartz.JobKey;
import org.terracotta.quartz.collections.ToolkitDSHolder;
import org.terracotta.toolkit.store.ToolkitStore;

public class JobFacade {
   private final ToolkitStore<JobKey, JobWrapper> jobsByFQN;
   private final Set<String> allJobsGroupNames;
   private final Set<String> pausedJobGroups;
   private final Set<JobKey> blockedJobs;

   public JobFacade(ToolkitDSHolder var1) {
      this.jobsByFQN = var1.getOrCreateJobsMap();
      this.allJobsGroupNames = var1.getOrCreateAllGroupsSet();
      this.pausedJobGroups = var1.getOrCreatePausedGroupsSet();
      this.blockedJobs = var1.getOrCreateBlockedJobsSet();
   }

   public JobWrapper get(JobKey var1) {
      return (JobWrapper)this.jobsByFQN.get(var1);
   }

   public void put(JobKey var1, JobWrapper var2) {
      this.jobsByFQN.putNoReturn(var1, var2);
   }

   public boolean containsKey(JobKey var1) {
      return this.jobsByFQN.containsKey(var1);
   }

   public boolean hasGroup(String var1) {
      return this.allJobsGroupNames.contains(var1);
   }

   public boolean addGroup(String var1) {
      return this.allJobsGroupNames.add(var1);
   }

   public boolean addPausedGroup(String var1) {
      return this.pausedJobGroups.add(var1);
   }

   public JobWrapper remove(JobKey var1) {
      return (JobWrapper)this.jobsByFQN.remove(var1);
   }

   public boolean removeGroup(String var1) {
      return this.allJobsGroupNames.remove(var1);
   }

   public boolean pausedGroupsContain(String var1) {
      return this.pausedJobGroups.contains(var1);
   }

   public boolean blockedJobsContain(JobKey var1) {
      return this.blockedJobs.contains(var1);
   }

   public int numberOfJobs() {
      return this.jobsByFQN.size();
   }

   public Set<String> getAllGroupNames() {
      return this.allJobsGroupNames;
   }

   public boolean removePausedJobGroup(String var1) {
      return this.pausedJobGroups.remove(var1);
   }

   public void clearPausedJobGroups() {
      this.pausedJobGroups.clear();
   }

   public void addBlockedJob(JobKey var1) {
      this.blockedJobs.add(var1);
   }

   public boolean removeBlockedJob(JobKey var1) {
      return this.blockedJobs.remove(var1);
   }
}
