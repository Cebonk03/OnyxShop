package org.quartz.impl.jdbcjobstore;

import java.io.Serializable;

public class SchedulerStateRecord implements Serializable {
   private static final long serialVersionUID = -715704959016191445L;
   private String schedulerInstanceId;
   private long checkinTimestamp;
   private long checkinInterval;

   public SchedulerStateRecord() {
   }

   public long getCheckinInterval() {
      return this.checkinInterval;
   }

   public long getCheckinTimestamp() {
      return this.checkinTimestamp;
   }

   public String getSchedulerInstanceId() {
      return this.schedulerInstanceId;
   }

   public void setCheckinInterval(long var1) {
      this.checkinInterval = var1;
   }

   public void setCheckinTimestamp(long var1) {
      this.checkinTimestamp = var1;
   }

   public void setSchedulerInstanceId(String var1) {
      this.schedulerInstanceId = var1;
   }
}
