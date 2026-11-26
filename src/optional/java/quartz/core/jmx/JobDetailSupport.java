package org.quartz.core.jmx;

import java.util.ArrayList;
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
import org.quartz.JobDetail;
import org.quartz.impl.JobDetailImpl;

public class JobDetailSupport {
   private static final String COMPOSITE_TYPE_NAME = "JobDetail";
   private static final String COMPOSITE_TYPE_DESCRIPTION = "Job Execution Details";
   private static final String[] ITEM_NAMES = new String[]{"name", "group", "description", "jobClass", "jobDataMap", "durability", "shouldRecover"};
   private static final String[] ITEM_DESCRIPTIONS = new String[]{"name", "group", "description", "jobClass", "jobDataMap", "durability", "shouldRecover"};
   private static final OpenType[] ITEM_TYPES = new OpenType[]{
      SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, JobDataMapSupport.TABULAR_TYPE, SimpleType.BOOLEAN, SimpleType.BOOLEAN
   };
   private static final CompositeType COMPOSITE_TYPE;
   private static final String TABULAR_TYPE_NAME = "JobDetail collection";
   private static final String TABULAR_TYPE_DESCRIPTION = "JobDetail collection";
   private static final String[] INDEX_NAMES = new String[]{"name", "group"};
   private static final TabularType TABULAR_TYPE;

   public JobDetailSupport() {
   }

   public static JobDetail newJobDetail(CompositeData var0) throws ClassNotFoundException {
      JobDetailImpl var1 = new JobDetailImpl();
      int var2 = 0;
      var1.setName((String)var0.get(ITEM_NAMES[var2++]));
      var1.setGroup((String)var0.get(ITEM_NAMES[var2++]));
      var1.setDescription((String)var0.get(ITEM_NAMES[var2++]));
      Class var3 = Class.forName((String)var0.get(ITEM_NAMES[var2++]));
      var1.setJobClass(var3);
      var1.setJobDataMap(JobDataMapSupport.newJobDataMap((TabularData)var0.get(ITEM_NAMES[var2++])));
      var1.setDurability((Boolean)var0.get(ITEM_NAMES[var2++]));
      var1.setRequestsRecovery((Boolean)var0.get(ITEM_NAMES[var2++]));
      return var1;
   }

   public static JobDetail newJobDetail(Map<String, Object> var0) throws ClassNotFoundException {
      JobDetailImpl var1 = new JobDetailImpl();
      int var2 = 0;
      var1.setName((String)var0.get(ITEM_NAMES[var2++]));
      var1.setGroup((String)var0.get(ITEM_NAMES[var2++]));
      var1.setDescription((String)var0.get(ITEM_NAMES[var2++]));
      Class var3 = Class.forName((String)var0.get(ITEM_NAMES[var2++]));
      var1.setJobClass(var3);
      if (var0.containsKey(ITEM_NAMES[var2])) {
         Map var5 = (Map)var0.get(ITEM_NAMES[var2]);
         var1.setJobDataMap(JobDataMapSupport.newJobDataMap(var5));
      }

      if (var0.containsKey(ITEM_NAMES[++var2])) {
         var1.setDurability((Boolean)var0.get(ITEM_NAMES[var2]));
      }

      if (var0.containsKey(ITEM_NAMES[++var2])) {
         var1.setRequestsRecovery((Boolean)var0.get(ITEM_NAMES[var2]));
      }

      var2++;
      return var1;
   }

   public static CompositeData toCompositeData(JobDetail var0) {
      try {
         return new CompositeDataSupport(
            COMPOSITE_TYPE,
            ITEM_NAMES,
            new Object[]{
               var0.getKey().getName(),
               var0.getKey().getGroup(),
               var0.getDescription(),
               var0.getJobClass().getName(),
               JobDataMapSupport.toTabularData(var0.getJobDataMap()),
               var0.isDurable(),
               var0.requestsRecovery()
            }
         );
      } catch (OpenDataException var2) {
         throw new RuntimeException(var2);
      }
   }

   public static TabularData toTabularData(JobDetail[] var0) {
      TabularDataSupport var1 = new TabularDataSupport(TABULAR_TYPE);
      if (var0 != null) {
         ArrayList<CompositeData> var2 = new ArrayList();

         for (JobDetail var6 : var0) {
            var2.add(toCompositeData(var6));
         }

         var1.putAll(var2.toArray(new CompositeData[var2.size()]));
      }

      return var1;
   }

   static {
      try {
         COMPOSITE_TYPE = new CompositeType("JobDetail", "Job Execution Details", ITEM_NAMES, ITEM_DESCRIPTIONS, ITEM_TYPES);
         TABULAR_TYPE = new TabularType("JobDetail collection", "JobDetail collection", COMPOSITE_TYPE, INDEX_NAMES);
      } catch (OpenDataException var1) {
         throw new RuntimeException(var1);
      }
   }
}
