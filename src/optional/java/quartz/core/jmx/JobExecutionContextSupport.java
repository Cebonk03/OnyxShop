package org.quartz.core.jmx;

import java.util.ArrayList;
import java.util.List;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.quartz.SchedulerException;
import org.quartz.JobExecutionContext;

public class JobExecutionContextSupport {
   private static final String COMPOSITE_TYPE_NAME = "JobExecutionContext";
   private static final String COMPOSITE_TYPE_DESCRIPTION = "Job Execution Instance Details";
   private static final String[] ITEM_NAMES = new String[]{
      "schedulerName",
      "triggerName",
      "triggerGroup",
      "jobName",
      "jobGroup",
      "jobDataMap",
      "calendarName",
      "recovering",
      "refireCount",
      "fireTime",
      "scheduledFireTime",
      "previousFireTime",
      "nextFireTime",
      "jobRunTime",
      "fireInstanceId"
   };
   private static final String[] ITEM_DESCRIPTIONS = new String[]{
      "schedulerName",
      "triggerName",
      "triggerGroup",
      "jobName",
      "jobGroup",
      "jobDataMap",
      "calendarName",
      "recovering",
      "refireCount",
      "fireTime",
      "scheduledFireTime",
      "previousFireTime",
      "nextFireTime",
      "jobRunTime",
      "fireInstanceId"
   };
   private static final OpenType[] ITEM_TYPES = new OpenType[]{
      SimpleType.STRING,
      SimpleType.STRING,
      SimpleType.STRING,
      SimpleType.STRING,
      SimpleType.STRING,
      JobDataMapSupport.TABULAR_TYPE,
      SimpleType.STRING,
      SimpleType.BOOLEAN,
      SimpleType.INTEGER,
      SimpleType.DATE,
      SimpleType.DATE,
      SimpleType.DATE,
      SimpleType.DATE,
      SimpleType.LONG,
      SimpleType.STRING
   };
   private static final CompositeType COMPOSITE_TYPE;
   private static final String TABULAR_TYPE_NAME = "JobExecutionContextArray";
   private static final String TABULAR_TYPE_DESCRIPTION = "Array of composite JobExecutionContext";
   private static final String[] INDEX_NAMES = new String[]{"schedulerName", "triggerName", "triggerGroup", "jobName", "jobGroup", "fireTime"};
   private static final TabularType TABULAR_TYPE;

   public JobExecutionContextSupport() {
   }

   public static CompositeData toCompositeData(JobExecutionContext var0) throws SchedulerException {
      try {
         return new CompositeDataSupport(
            COMPOSITE_TYPE,
            ITEM_NAMES,
            new Object[]{
               var0.getScheduler().getSchedulerName(),
               var0.getTrigger().getKey().getName(),
               var0.getTrigger().getKey().getGroup(),
               var0.getJobDetail().getKey().getName(),
               var0.getJobDetail().getKey().getGroup(),
               JobDataMapSupport.toTabularData(var0.getMergedJobDataMap()),
               var0.getTrigger().getCalendarName(),
               var0.isRecovering(),
               var0.getRefireCount(),
               var0.getFireTime(),
               var0.getScheduledFireTime(),
               var0.getPreviousFireTime(),
               var0.getNextFireTime(),
               var0.getJobRunTime(),
               var0.getFireInstanceId()
            }
         );
      } catch (OpenDataException var2) {
         throw new RuntimeException(var2);
      }
   }

   public static TabularData toTabularData(List<JobExecutionContext> var0) throws SchedulerException {
      ArrayList<CompositeData> var1 = new ArrayList();

      for (JobExecutionContext var3 : var0) {
         var1.add(toCompositeData(var3));
      }

      TabularDataSupport var4 = new TabularDataSupport(TABULAR_TYPE);
      var4.putAll(var1.toArray(new CompositeData[var1.size()]));
      return var4;
   }

   static {
      try {
         COMPOSITE_TYPE = new CompositeType("JobExecutionContext", "Job Execution Instance Details", ITEM_NAMES, ITEM_DESCRIPTIONS, ITEM_TYPES);
         TABULAR_TYPE = new TabularType("JobExecutionContextArray", "Array of composite JobExecutionContext", COMPOSITE_TYPE, INDEX_NAMES);
      } catch (OpenDataException var1) {
         throw new RuntimeException(var1);
      }
   }
}
