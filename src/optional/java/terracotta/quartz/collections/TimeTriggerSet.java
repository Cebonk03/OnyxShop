package org.terracotta.quartz.collections;

import java.util.Iterator;
import org.quartz.TriggerKey;
import org.terracotta.quartz.wrappers.TriggerWrapper;
import org.terracotta.toolkit.collections.ToolkitSortedSet;

public class TimeTriggerSet {
   private final ToolkitSortedSet<TimeTrigger> timeTriggers;

   public TimeTriggerSet(ToolkitSortedSet<TimeTrigger> var1) {
      this.timeTriggers = var1;
   }

   public boolean add(TriggerWrapper var1) {
      TimeTrigger var2 = new TimeTrigger(var1.getKey(), var1.getNextFireTime(), var1.getPriority());
      return this.timeTriggers.add(var2);
   }

   public boolean remove(TriggerWrapper var1) {
      TimeTrigger var2 = new TimeTrigger(var1.getKey(), var1.getNextFireTime(), var1.getPriority());
      return this.timeTriggers.remove(var2);
   }

   public TriggerKey removeFirst() {
      Iterator var1 = this.timeTriggers.iterator();
      TimeTrigger var2 = null;
      if (var1.hasNext()) {
         var2 = (TimeTrigger)var1.next();
         var1.remove();
      }

      return var2 == null ? null : var2.getTriggerKey();
   }

   public boolean isDestroyed() {
      return this.timeTriggers.isDestroyed();
   }

   public void destroy() {
      this.timeTriggers.destroy();
   }

   public int size() {
      return this.timeTriggers.size();
   }
}
