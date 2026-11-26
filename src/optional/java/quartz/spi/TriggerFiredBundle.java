package org.quartz.spi;

import java.io.Serializable;
import java.util.Date;
import org.quartz.Calendar;
import org.quartz.JobDetail;

public class TriggerFiredBundle implements Serializable {
   private static final long serialVersionUID = -6414106108306999265L;
   private JobDetail job;
   private OperableTrigger trigger;
   private Calendar cal;
   private boolean jobIsRecovering;
   private Date fireTime;
   private Date scheduledFireTime;
   private Date prevFireTime;
   private Date nextFireTime;

   public TriggerFiredBundle(JobDetail var1, OperableTrigger var2, Calendar var3, boolean var4, Date var5, Date var6, Date var7, Date var8) {
      this.job = var1;
      this.trigger = var2;
      this.cal = var3;
      this.jobIsRecovering = var4;
      this.fireTime = var5;
      this.scheduledFireTime = var6;
      this.prevFireTime = var7;
      this.nextFireTime = var8;
   }

   public JobDetail getJobDetail() {
      return this.job;
   }

   public OperableTrigger getTrigger() {
      return this.trigger;
   }

   public Calendar getCalendar() {
      return this.cal;
   }

   public boolean isRecovering() {
      return this.jobIsRecovering;
   }

   public Date getFireTime() {
      return this.fireTime;
   }

   public Date getNextFireTime() {
      return this.nextFireTime;
   }

   public Date getPrevFireTime() {
      return this.prevFireTime;
   }

   public Date getScheduledFireTime() {
      return this.scheduledFireTime;
   }
}
