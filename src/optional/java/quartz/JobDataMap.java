package org.quartz;

import java.io.Serializable;
import java.util.Map;
import org.quartz.utils.StringKeyDirtyFlagMap;

public class JobDataMap extends StringKeyDirtyFlagMap implements Serializable {
   private static final long serialVersionUID = -6939901990106713909L;

   public JobDataMap() {
      super(15);
   }

   public JobDataMap(Map<?, ?> var1) {
      this();
      this.putAll((Map<String, Object>)var1);
      this.clearDirtyFlag();
   }

   public void putAsString(String var1, boolean var2) {
      String var3 = Boolean.valueOf(var2).toString();
      super.put(var1, var3);
   }

   public void putAsString(String var1, Boolean var2) {
      String var3 = var2.toString();
      super.put(var1, var3);
   }

   public void putAsString(String var1, char var2) {
      String var3 = Character.valueOf(var2).toString();
      super.put(var1, var3);
   }

   public void putAsString(String var1, Character var2) {
      String var3 = var2.toString();
      super.put(var1, var3);
   }

   public void putAsString(String var1, double var2) {
      String var4 = Double.toString(var2);
      super.put(var1, var4);
   }

   public void putAsString(String var1, Double var2) {
      String var3 = var2.toString();
      super.put(var1, var3);
   }

   public void putAsString(String var1, float var2) {
      String var3 = Float.toString(var2);
      super.put(var1, var3);
   }

   public void putAsString(String var1, Float var2) {
      String var3 = var2.toString();
      super.put(var1, var3);
   }

   public void putAsString(String var1, int var2) {
      String var3 = Integer.valueOf(var2).toString();
      super.put(var1, var3);
   }

   public void putAsString(String var1, Integer var2) {
      String var3 = var2.toString();
      super.put(var1, var3);
   }

   public void putAsString(String var1, long var2) {
      String var4 = Long.valueOf(var2).toString();
      super.put(var1, var4);
   }

   public void putAsString(String var1, Long var2) {
      String var3 = var2.toString();
      super.put(var1, var3);
   }

   public int getIntFromString(String var1) {
      Object var2 = this.get(var1);
      return new Integer((String)var2);
   }

   public int getIntValue(String var1) {
      Object var2 = this.get(var1);
      return var2 instanceof String ? this.getIntFromString(var1) : this.getInt(var1);
   }

   public Integer getIntegerFromString(String var1) {
      Object var2 = this.get(var1);
      return new Integer((String)var2);
   }

   public boolean getBooleanValueFromString(String var1) {
      Object var2 = this.get(var1);
      return Boolean.valueOf((String)var2);
   }

   public boolean getBooleanValue(String var1) {
      Object var2 = this.get(var1);
      return var2 instanceof String ? this.getBooleanValueFromString(var1) : this.getBoolean(var1);
   }

   public Boolean getBooleanFromString(String var1) {
      Object var2 = this.get(var1);
      return Boolean.valueOf((String)var2);
   }

   public char getCharFromString(String var1) {
      Object var2 = this.get(var1);
      return ((String)var2).charAt(0);
   }

   public Character getCharacterFromString(String var1) {
      Object var2 = this.get(var1);
      return ((String)var2).charAt(0);
   }

   public double getDoubleValueFromString(String var1) {
      Object var2 = this.get(var1);
      return Double.valueOf((String)var2);
   }

   public double getDoubleValue(String var1) {
      Object var2 = this.get(var1);
      return var2 instanceof String ? this.getDoubleValueFromString(var1) : this.getDouble(var1);
   }

   public Double getDoubleFromString(String var1) {
      Object var2 = this.get(var1);
      return new Double((String)var2);
   }

   public float getFloatValueFromString(String var1) {
      Object var2 = this.get(var1);
      return new Float((String)var2);
   }

   public float getFloatValue(String var1) {
      Object var2 = this.get(var1);
      return var2 instanceof String ? this.getFloatValueFromString(var1) : this.getFloat(var1);
   }

   public Float getFloatFromString(String var1) {
      Object var2 = this.get(var1);
      return new Float((String)var2);
   }

   public long getLongValueFromString(String var1) {
      Object var2 = this.get(var1);
      return new Long((String)var2);
   }

   public long getLongValue(String var1) {
      Object var2 = this.get(var1);
      return var2 instanceof String ? this.getLongValueFromString(var1) : this.getLong(var1);
   }

   public Long getLongFromString(String var1) {
      Object var2 = this.get(var1);
      return new Long((String)var2);
   }
}
