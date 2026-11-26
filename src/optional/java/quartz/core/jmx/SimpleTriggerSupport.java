package org.quartz.core.jmx;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.quartz.SimpleTrigger;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.OperableTrigger;

public class SimpleTriggerSupport {
   private static final String COMPOSITE_TYPE_NAME = "SimpleTrigger";
   private static final String COMPOSITE_TYPE_DESCRIPTION = "SimpleTrigger Details";
   private static final String[] ITEM_NAMES = new String[]{"repeatCount", "repeatInterval", "timesTriggered"};
   private static final String[] ITEM_DESCRIPTIONS = new String[]{"repeatCount", "repeatInterval", "timesTriggered"};
   private static final OpenType[] ITEM_TYPES = new OpenType[]{SimpleType.INTEGER, SimpleType.LONG, SimpleType.INTEGER};
   private static final CompositeType COMPOSITE_TYPE;
   private static final String TABULAR_TYPE_NAME = "SimpleTrigger collection";
   private static final String TABULAR_TYPE_DESCRIPTION = "SimpleTrigger collection";
   private static final TabularType TABULAR_TYPE;

   public SimpleTriggerSupport() {
   }

   public static String[] getItemNames() {
      ArrayList<String> var0 = new ArrayList<>(Arrays.asList(ITEM_NAMES));
      var0.addAll(Arrays.asList(TriggerSupport.getItemNames()));
      return var0.toArray(new String[var0.size()]);
   }

   public static String[] getItemDescriptions() {
      ArrayList<String> var0 = new ArrayList<>(Arrays.asList(ITEM_DESCRIPTIONS));
      var0.addAll(Arrays.asList(TriggerSupport.getItemDescriptions()));
      return var0.toArray(new String[var0.size()]);
   }

   public static OpenType[] getItemTypes() {
      ArrayList<OpenType> var0 = new ArrayList<>(Arrays.asList(ITEM_TYPES));
      var0.addAll(Arrays.asList(TriggerSupport.getItemTypes()));
      return var0.toArray(new OpenType[var0.size()]);
   }

   public static CompositeData toCompositeData(SimpleTrigger var0) {
      try {
         return new CompositeDataSupport(
            COMPOSITE_TYPE,
            ITEM_NAMES,
            new Object[]{
               var0.getRepeatCount(),
               var0.getRepeatInterval(),
               var0.getTimesTriggered(),
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

   public static TabularData toTabularData(List<? extends SimpleTrigger> var0) {
      TabularDataSupport var1 = new TabularDataSupport(TABULAR_TYPE);
      if (var0 != null) {
         ArrayList<CompositeData> var2 = new ArrayList();

         for (SimpleTrigger var4 : var0) {
            var2.add(toCompositeData(var4));
         }

         var1.putAll(var2.toArray(new CompositeData[var2.size()]));
      }

      return var1;
   }

   public static OperableTrigger newTrigger(CompositeData var0) {
      SimpleTriggerImpl var1 = new SimpleTriggerImpl();
      var1.setRepeatCount((Integer)var0.get("repeatCount"));
      var1.setRepeatInterval((Long)var0.get("repeatInterval"));
      var1.setTimesTriggered((Integer)var0.get("timesTriggered"));
      TriggerSupport.initializeTrigger(var1, var0);
      return var1;
   }

   public static OperableTrigger newTrigger(Map<String, Object> var0) {
      SimpleTriggerImpl var1 = new SimpleTriggerImpl();
      if (var0.containsKey("repeatCount")) {
         var1.setRepeatCount((Integer)var0.get("repeatCount"));
      }

      if (var0.containsKey("repeatInterval")) {
         var1.setRepeatInterval((Long)var0.get("repeatInterval"));
      }

      if (var0.containsKey("timesTriggered")) {
         var1.setTimesTriggered((Integer)var0.get("timesTriggered"));
      }

      TriggerSupport.initializeTrigger(var1, var0);
      return var1;
   }

   static {
      try {
         COMPOSITE_TYPE = new CompositeType("SimpleTrigger", "SimpleTrigger Details", getItemNames(), getItemDescriptions(), getItemTypes());
         TABULAR_TYPE = new TabularType("SimpleTrigger collection", "SimpleTrigger collection", COMPOSITE_TYPE, getItemNames());
      } catch (OpenDataException var1) {
         throw new RuntimeException(var1);
      }
   }
}
