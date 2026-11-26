package org.quartz.core.jmx;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.spi.MutableTrigger;
import org.quartz.spi.OperableTrigger;

public class TriggerSupport {
   private static final String COMPOSITE_TYPE_NAME = "Trigger";
   private static final String COMPOSITE_TYPE_DESCRIPTION = "Trigger Details";
   private static final String[] ITEM_NAMES = new String[]{
      "name",
      "group",
      "jobName",
      "jobGroup",
      "description",
      "jobDataMap",
      "calendarName",
      "fireInstanceId",
      "misfireInstruction",
      "priority",
      "startTime",
      "endTime",
      "nextFireTime",
      "previousFireTime",
      "finalFireTime"
   };
   private static final String[] ITEM_DESCRIPTIONS = new String[]{
      "name",
      "group",
      "jobName",
      "jobGroup",
      "description",
      "jobDataMap",
      "calendarName",
      "fireInstanceId",
      "misfireInstruction",
      "priority",
      "startTime",
      "endTime",
      "nextFireTime",
      "previousFireTime",
      "finalFireTime"
   };
   private static final OpenType[] ITEM_TYPES = new OpenType[]{
      SimpleType.STRING,
      SimpleType.STRING,
      SimpleType.STRING,
      SimpleType.STRING,
      SimpleType.STRING,
      JobDataMapSupport.TABULAR_TYPE,
      SimpleType.STRING,
      SimpleType.STRING,
      SimpleType.INTEGER,
      SimpleType.INTEGER,
      SimpleType.DATE,
      SimpleType.DATE,
      SimpleType.DATE,
      SimpleType.DATE,
      SimpleType.DATE
   };
   private static final CompositeType COMPOSITE_TYPE;
   private static final String TABULAR_TYPE_NAME = "Trigger collection";
   private static final String TABULAR_TYPE_DESCRIPTION = "Trigger collection";
   private static final String[] INDEX_NAMES = new String[]{"name", "group"};
   private static final TabularType TABULAR_TYPE;

   public TriggerSupport() {
   }

   public static String[] getItemNames() {
      return ITEM_NAMES;
   }

   public static String[] getItemDescriptions() {
      return ITEM_DESCRIPTIONS;
   }

   public static OpenType[] getItemTypes() {
      return ITEM_TYPES;
   }

   public String[] getIndexNames() {
      return INDEX_NAMES;
   }

   public static CompositeData toCompositeData(Trigger var0) {
      try {
         return new CompositeDataSupport(
            COMPOSITE_TYPE,
            ITEM_NAMES,
            new Object[]{
               var0.getKey().getName(),
               var0.getKey().getGroup(),
               var0.getJobKey().getName(),
               var0.getJobKey().getGroup(),
               var0.getDescription(),
               JobDataMapSupport.toTabularData(var0.getJobDataMap()),
               var0.getCalendarName(),
               ((OperableTrigger)var0).getFireInstanceId(),
               var0.getMisfireInstruction(),
               var0.getPriority(),
               var0.getStartTime(),
               var0.getEndTime(),
               var0.getNextFireTime(),
               var0.getPreviousFireTime(),
               var0.getFinalFireTime()
            }
         );
      } catch (OpenDataException var2) {
         throw new RuntimeException(var2);
      }
   }

   public static TabularData toTabularData(List<? extends Trigger> var0) {
      TabularDataSupport var1 = new TabularDataSupport(TABULAR_TYPE);
      if (var0 != null) {
         ArrayList<CompositeData> var2 = new ArrayList();

         for (Trigger var4 : var0) {
            var2.add(toCompositeData(var4));
         }

         var1.putAll(var2.toArray(new CompositeData[var2.size()]));
      }

      return var1;
   }

   public static List<CompositeData> toCompositeList(List<? extends Trigger> var0) {
      ArrayList var1 = new ArrayList();

      for (Trigger var3 : var0) {
         CompositeData var4 = toCompositeData(var3);
         if (var4 != null) {
            var1.add(var4);
         }
      }

      return var1;
   }

   public static void initializeTrigger(MutableTrigger var0, CompositeData var1) {
      var0.setDescription((String)var1.get("description"));
      var0.setCalendarName((String)var1.get("calendarName"));
      if (var1.containsKey("priority")) {
         var0.setPriority((Integer)var1.get("priority"));
      }

      if (var1.containsKey("jobDataMap")) {
         var0.setJobDataMap(JobDataMapSupport.newJobDataMap((TabularData)var1.get("jobDataMap")));
      }

      Date var2;
      if (var1.containsKey("startTime")) {
         var2 = (Date)var1.get("startTime");
      } else {
         var2 = new Date();
      }

      var0.setStartTime(var2);
      var0.setEndTime((Date)var1.get("endTime"));
      if (var1.containsKey("misfireInstruction")) {
         var0.setMisfireInstruction((Integer)var1.get("misfireInstruction"));
      }

      var0.setKey(new TriggerKey((String)var1.get("name"), (String)var1.get("group")));
      var0.setJobKey(new JobKey((String)var1.get("jobName"), (String)var1.get("jobGroup")));
   }

   public static void initializeTrigger(MutableTrigger var0, Map<String, Object> var1) {
      var0.setDescription((String)var1.get("description"));
      var0.setCalendarName((String)var1.get("calendarName"));
      if (var1.containsKey("priority")) {
         var0.setPriority((Integer)var1.get("priority"));
      }

      if (var1.containsKey("jobDataMap")) {
         Map var2 = (Map)var1.get("jobDataMap");
         var0.setJobDataMap(JobDataMapSupport.newJobDataMap(var2));
      }

      Date var3;
      if (var1.containsKey("startTime")) {
         var3 = (Date)var1.get("startTime");
      } else {
         var3 = new Date();
      }

      var0.setStartTime(var3);
      if (var1.containsKey("endTime")) {
         var0.setEndTime((Date)var1.get("endTime"));
      }

      if (var1.containsKey("misfireInstruction")) {
         var0.setMisfireInstruction((Integer)var1.get("misfireInstruction"));
      }

      var0.setKey(new TriggerKey((String)var1.get("name"), (String)var1.get("group")));
      var0.setJobKey(new JobKey((String)var1.get("jobName"), (String)var1.get("jobGroup")));
   }

   public static OperableTrigger newTrigger(CompositeData var0) {
      OperableTrigger var1 = null;
      if (var0.containsKey("cronExpression")) {
         var1 = CronTriggerSupport.newTrigger(var0);
      } else {
         var1 = SimpleTriggerSupport.newTrigger(var0);
      }

      return var1;
   }

   public static OperableTrigger newTrigger(Map<String, Object> var0) throws java.text.ParseException {
      OperableTrigger var1 = null;
      if (var0.containsKey("cronExpression")) {
         var1 = CronTriggerSupport.newTrigger(var0);
      } else {
         var1 = SimpleTriggerSupport.newTrigger(var0);
      }

      return var1;
   }

   static {
      try {
         COMPOSITE_TYPE = new CompositeType("Trigger", "Trigger Details", ITEM_NAMES, ITEM_DESCRIPTIONS, ITEM_TYPES);
         TABULAR_TYPE = new TabularType("Trigger collection", "Trigger collection", COMPOSITE_TYPE, INDEX_NAMES);
      } catch (OpenDataException var1) {
         throw new RuntimeException(var1);
      }
   }
}
