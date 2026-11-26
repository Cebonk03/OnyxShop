package org.quartz.listeners;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;

public class BroadcastTriggerListener implements TriggerListener {
   private String name;
   private List<TriggerListener> listeners;

   public BroadcastTriggerListener(String var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("Listener name cannot be null!");
      } else {
         this.name = var1;
         this.listeners = new LinkedList<>();
      }
   }

   public BroadcastTriggerListener(String var1, List<TriggerListener> var2) {
      this(var1);
      this.listeners.addAll(var2);
   }

   @Override
   public String getName() {
      return this.name;
   }

   public void addListener(TriggerListener var1) {
      this.listeners.add(var1);
   }

   public boolean removeListener(TriggerListener var1) {
      return this.listeners.remove(var1);
   }

   public boolean removeListener(String var1) {
      Iterator var2 = this.listeners.iterator();

      while (var2.hasNext()) {
         TriggerListener var3 = (TriggerListener)var2.next();
         if (var3.getName().equals(var1)) {
            var2.remove();
            return true;
         }
      }

      return false;
   }

   public List<TriggerListener> getListeners() {
      return Collections.unmodifiableList(this.listeners);
   }

   @Override
   public void triggerFired(Trigger var1, JobExecutionContext var2) {
      for (TriggerListener var4 : this.listeners) {
         var4.triggerFired(var1, var2);
      }
   }

   @Override
   public boolean vetoJobExecution(Trigger var1, JobExecutionContext var2) {
      for (TriggerListener var4 : this.listeners) {
         if (var4.vetoJobExecution(var1, var2)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void triggerMisfired(Trigger var1) {
      for (TriggerListener var3 : this.listeners) {
         var3.triggerMisfired(var1);
      }
   }

   @Override
   public void triggerComplete(Trigger var1, JobExecutionContext var2, Trigger.CompletedExecutionInstruction var3) {
      for (TriggerListener var5 : this.listeners) {
         var5.triggerComplete(var1, var2, var3);
      }
   }
}
