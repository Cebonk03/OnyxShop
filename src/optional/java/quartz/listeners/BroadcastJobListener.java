package org.quartz.listeners;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

public class BroadcastJobListener implements JobListener {
   private String name;
   private List<JobListener> listeners;

   public BroadcastJobListener(String var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("Listener name cannot be null!");
      } else {
         this.name = var1;
         this.listeners = new LinkedList<>();
      }
   }

   public BroadcastJobListener(String var1, List<JobListener> var2) {
      this(var1);
      this.listeners.addAll(var2);
   }

   @Override
   public String getName() {
      return this.name;
   }

   public void addListener(JobListener var1) {
      this.listeners.add(var1);
   }

   public boolean removeListener(JobListener var1) {
      return this.listeners.remove(var1);
   }

   public boolean removeListener(String var1) {
      Iterator var2 = this.listeners.iterator();

      while (var2.hasNext()) {
         JobListener var3 = (JobListener)var2.next();
         if (var3.getName().equals(var1)) {
            var2.remove();
            return true;
         }
      }

      return false;
   }

   public List<JobListener> getListeners() {
      return Collections.unmodifiableList(this.listeners);
   }

   @Override
   public void jobToBeExecuted(JobExecutionContext var1) {
      for (JobListener var3 : this.listeners) {
         var3.jobToBeExecuted(var1);
      }
   }

   @Override
   public void jobExecutionVetoed(JobExecutionContext var1) {
      for (JobListener var3 : this.listeners) {
         var3.jobExecutionVetoed(var1);
      }
   }

   @Override
   public void jobWasExecuted(JobExecutionContext var1, JobExecutionException var2) {
      for (JobListener var4 : this.listeners) {
         var4.jobWasExecuted(var1, var2);
      }
   }
}
