package org.quartz.core.jmx;

import java.util.ArrayList;
import java.util.Collection;
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
import org.quartz.JobDataMap;

public class JobDataMapSupport {
   private static final String typeName = "JobDataMap";
   private static final String[] keyValue = new String[]{"key", "value"};
   private static final OpenType[] openTypes = new OpenType[]{SimpleType.STRING, SimpleType.STRING};
   private static final CompositeType rowType;
   public static final TabularType TABULAR_TYPE;

   public JobDataMapSupport() {
   }

   public static JobDataMap newJobDataMap(TabularData var0) {
      JobDataMap var1 = new JobDataMap();
      if (var0 != null) {
         for (CompositeData var3 : (Collection<CompositeData>)var0.values()) {
            var1.put((String)var3.get("key"), (String)var3.get("value"));
         }
      }

      return var1;
   }

   public static JobDataMap newJobDataMap(Map<String, Object> var0) {
      JobDataMap var1 = new JobDataMap();
      if (var0 != null) {
         for (String var3 : var0.keySet()) {
            var1.put(var3, var0.get(var3));
         }
      }

      return var1;
   }

   public static CompositeData toCompositeData(String var0, String var1) {
      try {
         return new CompositeDataSupport(rowType, keyValue, new Object[]{var0, var1});
      } catch (OpenDataException var3) {
         throw new RuntimeException(var3);
      }
   }

   public static TabularData toTabularData(JobDataMap var0) {
      TabularDataSupport var1 = new TabularDataSupport(TABULAR_TYPE);
      ArrayList<CompositeData> var2 = new ArrayList<CompositeData>();

      for (String var4 : var0.keySet()) {
         var2.add(toCompositeData(var4, String.valueOf(var0.get(var4))));
      }

      var1.putAll(var2.toArray(new CompositeData[var2.size()]));
      return var1;
   }

   static {
      try {
         rowType = new CompositeType("JobDataMap", "JobDataMap", keyValue, keyValue, openTypes);
         TABULAR_TYPE = new TabularType("JobDataMap", "JobDataMap", rowType, new String[]{"key"});
      } catch (OpenDataException var1) {
         throw new RuntimeException(var1);
      }
   }
}
