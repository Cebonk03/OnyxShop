package org.quartz.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

public class SchedulerRepository {
   private HashMap<String, Scheduler> schedulers = new HashMap<>();
   private static SchedulerRepository inst;

   private SchedulerRepository() {
   }

   public static synchronized SchedulerRepository getInstance() {
      if (inst == null) {
         inst = new SchedulerRepository();
      }

      return inst;
   }

   public synchronized void bind(Scheduler var1) {
      if (this.schedulers.get(var1.getSchedulerName()) != null) {
         throw new SchedulerException("Scheduler with name '" + var1.getSchedulerName() + "' already exists.");
      } else {
         this.schedulers.put(var1.getSchedulerName(), var1);
      }
   }

   public synchronized boolean remove(String var1) {
      return this.schedulers.remove(var1) != null;
   }

   public synchronized Scheduler lookup(String var1) {
      return this.schedulers.get(var1);
   }

   public synchronized Collection<Scheduler> lookupAll() {
      return Collections.unmodifiableCollection(this.schedulers.values());
   }
}
