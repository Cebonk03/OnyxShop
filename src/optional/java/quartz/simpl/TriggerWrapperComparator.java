package org.quartz.simpl;

import java.io.Serializable;
import java.util.Comparator;
import org.quartz.Trigger;

class TriggerWrapperComparator implements Comparator<TriggerWrapper>, Serializable {
   private static final long serialVersionUID = 8809557142191514261L;
   Trigger.TriggerTimeComparator ttc = new Trigger.TriggerTimeComparator();

   TriggerWrapperComparator() {
   }

   public int compare(TriggerWrapper var1, TriggerWrapper var2) {
      return this.ttc.compare((Trigger)var1.trigger, (Trigger)var2.trigger);
   }

   @Override
   public boolean equals(Object var1) {
      return var1 instanceof TriggerWrapperComparator;
   }

   @Override
   public int hashCode() {
      return super.hashCode();
   }
}
