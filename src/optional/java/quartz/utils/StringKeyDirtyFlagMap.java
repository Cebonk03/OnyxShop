package org.quartz.utils;

import java.io.Serializable;

public class StringKeyDirtyFlagMap extends DirtyFlagMap<String, Object> {
   static final long serialVersionUID = -9076749120524952280L;
   /** @deprecated */
   private boolean allowsTransientData = false;

   public StringKeyDirtyFlagMap() {
   }

   public StringKeyDirtyFlagMap(int var1) {
      super(var1);
   }

   public StringKeyDirtyFlagMap(int var1, float var2) {
      super(var1, var2);
   }

   @Override
   public boolean equals(Object var1) {
      return super.equals(var1);
   }

   @Override
   public int hashCode() {
      return this.getWrappedMap().hashCode();
   }

   public String[] getKeys() {
      return this.keySet().toArray(new String[this.size()]);
   }

   /** @deprecated */
   public void setAllowsTransientData(boolean var1) {
      if (this.containsTransientData() && !var1) {
         throw new IllegalStateException("Cannot set property 'allowsTransientData' to 'false' when data map contains non-serializable objects.");
      } else {
         this.allowsTransientData = var1;
      }
   }

   /** @deprecated */
   public boolean getAllowsTransientData() {
      return this.allowsTransientData;
   }

   /** @deprecated */
   public boolean containsTransientData() {
      if (!this.getAllowsTransientData()) {
         return false;
      } else {
         String[] var1 = this.getKeys();

         for (int var2 = 0; var2 < var1.length; var2++) {
            Object var3 = super.get(var1[var2]);
            if (!(var3 instanceof Serializable)) {
               return true;
            }
         }

         return false;
      }
   }

   /** @deprecated */
   public void removeTransientData() {
      if (this.getAllowsTransientData()) {
         String[] var1 = this.getKeys();

         for (int var2 = 0; var2 < var1.length; var2++) {
            Object var3 = super.get(var1[var2]);
            if (!(var3 instanceof Serializable)) {
               this.remove(var1[var2]);
            }
         }
      }
   }

   public void put(String var1, int var2) {
      super.put(var1, var2);
   }

   public void put(String var1, long var2) {
      super.put(var1, var2);
   }

   public void put(String var1, float var2) {
      super.put(var1, var2);
   }

   public void put(String var1, double var2) {
      super.put(var1, var2);
   }

   public void put(String var1, boolean var2) {
      super.put(var1, var2);
   }

   public void put(String var1, char var2) {
      super.put(var1, var2);
   }

   public void put(String var1, String var2) {
      super.put(var1, var2);
   }

   public Object put(String var1, Object var2) {
      return super.put(var1, var2);
   }

   public int getInt(String var1) {
      Object var2 = this.get(var1);

      try {
         return var2 instanceof Integer ? (Integer)var2 : Integer.parseInt((String)var2);
      } catch (Exception var4) {
         throw new ClassCastException("Identified object is not an Integer.");
      }
   }

   public long getLong(String var1) {
      Object var2 = this.get(var1);

      try {
         return var2 instanceof Long ? (Long)var2 : Long.parseLong((String)var2);
      } catch (Exception var4) {
         throw new ClassCastException("Identified object is not a Long.");
      }
   }

   public float getFloat(String var1) {
      Object var2 = this.get(var1);

      try {
         return var2 instanceof Float ? (Float)var2 : Float.parseFloat((String)var2);
      } catch (Exception var4) {
         throw new ClassCastException("Identified object is not a Float.");
      }
   }

   public double getDouble(String var1) {
      Object var2 = this.get(var1);

      try {
         return var2 instanceof Double ? (Double)var2 : Double.parseDouble((String)var2);
      } catch (Exception var4) {
         throw new ClassCastException("Identified object is not a Double.");
      }
   }

   public boolean getBoolean(String var1) {
      Object var2 = this.get(var1);

      try {
         return var2 instanceof Boolean ? (Boolean)var2 : Boolean.parseBoolean((String)var2);
      } catch (Exception var4) {
         throw new ClassCastException("Identified object is not a Boolean.");
      }
   }

   public char getChar(String var1) {
      Object var2 = this.get(var1);

      try {
         return var2 instanceof Character ? (Character)var2 : ((String)var2).charAt(0);
      } catch (Exception var4) {
         throw new ClassCastException("Identified object is not a Character.");
      }
   }

   public String getString(String var1) {
      Object var2 = this.get(var1);

      try {
         return (String)var2;
      } catch (Exception var4) {
         throw new ClassCastException("Identified object is not a String.");
      }
   }
}
