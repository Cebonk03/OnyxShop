package org.terracotta.quartz.wrappers;

import java.io.Serializable;
import java.util.Date;
import org.quartz.Calendar;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.quartz.spi.OperableTrigger;

public class TriggerWrapper implements Serializable {
   private final boolean jobDisallowsConcurrence;
   private volatile String lastTerracotaClientId = null;
   private volatile TriggerWrapper.TriggerState state = TriggerWrapper.TriggerState.WAITING;
   protected final OperableTrigger trigger;

   protected TriggerWrapper(OperableTrigger var1, boolean var2) {
      this.trigger = var1;
      this.jobDisallowsConcurrence = var2;
   }

   public boolean jobDisallowsConcurrence() {
      return this.jobDisallowsConcurrence;
   }

   public String getLastTerracotaClientId() {
      return this.lastTerracotaClientId;
   }

   @Override
   public int hashCode() {
      return this.getKey().hashCode();
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName() + "( " + this.state + ", lastTC=" + this.lastTerracotaClientId + ", " + this.trigger + ")";
   }

   public TriggerKey getKey() {
      return this.trigger.getKey();
   }

   public JobKey getJobKey() {
      return this.trigger.getJobKey();
   }

   public void setState(TriggerWrapper.TriggerState var1, String var2, TriggerFacade var3) {
      if (var2 == null) {
         throw new NullPointerException();
      } else {
         this.state = var1;
         this.lastTerracotaClientId = var2;
         this.rePut(var3);
      }
   }

   public TriggerWrapper.TriggerState getState() {
      return this.state;
   }

   public Date getNextFireTime() {
      return this.trigger.getNextFireTime();
   }

   public int getPriority() {
      return this.trigger.getPriority();
   }

   public boolean mayFireAgain() {
      return this.trigger.mayFireAgain();
   }

   public OperableTrigger getTriggerClone() {
      return (OperableTrigger)this.trigger.clone();
   }

   public void updateWithNewCalendar(Calendar var1, long var2, TriggerFacade var4) {
      this.trigger.updateWithNewCalendar(var1, var2);
      this.rePut(var4);
   }

   public String getCalendarName() {
      return this.trigger.getCalendarName();
   }

   public int getMisfireInstruction() {
      return this.trigger.getMisfireInstruction();
   }

   public void updateAfterMisfire(Calendar var1, TriggerFacade var2) {
      this.trigger.updateAfterMisfire(var1);
      this.rePut(var2);
   }

   public void setFireInstanceId(String var1, TriggerFacade var2) {
      this.trigger.setFireInstanceId(var1);
      this.rePut(var2);
   }

   public void triggered(Calendar var1, TriggerFacade var2) {
      this.trigger.triggered(var1);
      this.rePut(var2);
   }

   private void rePut(TriggerFacade var1) {
      var1.put(this.trigger.getKey(), this);
   }

   public boolean isInstanceof(Class var1) {
      return var1.isInstance(this.trigger);
   }

   public static enum TriggerState {
      WAITING,
      ACQUIRED,
      COMPLETE,
      PAUSED,
      BLOCKED,
      PAUSED_BLOCKED,
      ERROR;

      private TriggerState() {
      }
   }
}
