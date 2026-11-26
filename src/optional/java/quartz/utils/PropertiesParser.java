package org.quartz.utils;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.StringTokenizer;

public class PropertiesParser {
   Properties props = null;

   public PropertiesParser(Properties var1) {
      this.props = var1;
   }

   public Properties getUnderlyingProperties() {
      return this.props;
   }

   public String getStringProperty(String var1) {
      return this.getStringProperty(var1, null);
   }

   public String getStringProperty(String var1, String var2) {
      String var3 = this.props.getProperty(var1, var2);
      if (var3 == null) {
         return var2;
      } else {
         var3 = var3.trim();
         return var3.length() == 0 ? var2 : var3;
      }
   }

   public String[] getStringArrayProperty(String var1) {
      return this.getStringArrayProperty(var1, null);
   }

   public String[] getStringArrayProperty(String var1, String[] var2) {
      String var3 = this.getStringProperty(var1);
      if (var3 == null) {
         return var2;
      } else {
         StringTokenizer var4 = new StringTokenizer(var3, ",");
         ArrayList<String> var5 = new ArrayList();

         try {
            while (var4.hasMoreTokens()) {
               var5.add(var4.nextToken().trim());
            }

            return var5.toArray(new String[var5.size()]);
         } catch (Exception var7) {
            return var2;
         }
      }
   }

   public boolean getBooleanProperty(String var1) {
      return this.getBooleanProperty(var1, false);
   }

   public boolean getBooleanProperty(String var1, boolean var2) {
      String var3 = this.getStringProperty(var1);
      return var3 == null ? var2 : Boolean.valueOf(var3);
   }

   public byte getByteProperty(String var1) {
      String var2 = this.getStringProperty(var1);
      if (var2 == null) {
         throw new NumberFormatException(" null string");
      } else {
         try {
            return Byte.parseByte(var2);
         } catch (NumberFormatException var4) {
            throw new NumberFormatException(" '" + var2 + "'");
         }
      }
   }

   public byte getByteProperty(String var1, byte var2) {
      String var3 = this.getStringProperty(var1);
      if (var3 == null) {
         return var2;
      } else {
         try {
            return Byte.parseByte(var3);
         } catch (NumberFormatException var5) {
            throw new NumberFormatException(" '" + var3 + "'");
         }
      }
   }

   public char getCharProperty(String var1) {
      return this.getCharProperty(var1, '\u0000');
   }

   public char getCharProperty(String var1, char var2) {
      String var3 = this.getStringProperty(var1);
      return var3 == null ? var2 : var3.charAt(0);
   }

   public double getDoubleProperty(String var1) {
      String var2 = this.getStringProperty(var1);
      if (var2 == null) {
         throw new NumberFormatException(" null string");
      } else {
         try {
            return Double.parseDouble(var2);
         } catch (NumberFormatException var4) {
            throw new NumberFormatException(" '" + var2 + "'");
         }
      }
   }

   public double getDoubleProperty(String var1, double var2) {
      String var4 = this.getStringProperty(var1);
      if (var4 == null) {
         return var2;
      } else {
         try {
            return Double.parseDouble(var4);
         } catch (NumberFormatException var6) {
            throw new NumberFormatException(" '" + var4 + "'");
         }
      }
   }

   public float getFloatProperty(String var1) {
      String var2 = this.getStringProperty(var1);
      if (var2 == null) {
         throw new NumberFormatException(" null string");
      } else {
         try {
            return Float.parseFloat(var2);
         } catch (NumberFormatException var4) {
            throw new NumberFormatException(" '" + var2 + "'");
         }
      }
   }

   public float getFloatProperty(String var1, float var2) {
      String var3 = this.getStringProperty(var1);
      if (var3 == null) {
         return var2;
      } else {
         try {
            return Float.parseFloat(var3);
         } catch (NumberFormatException var5) {
            throw new NumberFormatException(" '" + var3 + "'");
         }
      }
   }

   public int getIntProperty(String var1) {
      String var2 = this.getStringProperty(var1);
      if (var2 == null) {
         throw new NumberFormatException(" null string");
      } else {
         try {
            return Integer.parseInt(var2);
         } catch (NumberFormatException var4) {
            throw new NumberFormatException(" '" + var2 + "'");
         }
      }
   }

   public int getIntProperty(String var1, int var2) {
      String var3 = this.getStringProperty(var1);
      if (var3 == null) {
         return var2;
      } else {
         try {
            return Integer.parseInt(var3);
         } catch (NumberFormatException var5) {
            throw new NumberFormatException(" '" + var3 + "'");
         }
      }
   }

   public int[] getIntArrayProperty(String var1) {
      return this.getIntArrayProperty(var1, null);
   }

   public int[] getIntArrayProperty(String var1, int[] var2) {
      String var3 = this.getStringProperty(var1);
      if (var3 == null) {
         return var2;
      } else {
         StringTokenizer var4 = new StringTokenizer(var3, ",");
         ArrayList var5 = new ArrayList();

         try {
            while (var4.hasMoreTokens()) {
               try {
                  var5.add(new Integer(var4.nextToken().trim()));
               } catch (NumberFormatException var8) {
                  throw new NumberFormatException(" '" + var3 + "'");
               }
            }

            int[] var6 = new int[var5.size()];

            for (int var7 = 0; var7 < var5.size(); var7++) {
               var6[var7] = (Integer)var5.get(var7);
            }

            return var6;
         } catch (Exception var9) {
            return var2;
         }
      }
   }

   public long getLongProperty(String var1) {
      String var2 = this.getStringProperty(var1);
      if (var2 == null) {
         throw new NumberFormatException(" null string");
      } else {
         try {
            return Long.parseLong(var2);
         } catch (NumberFormatException var4) {
            throw new NumberFormatException(" '" + var2 + "'");
         }
      }
   }

   public long getLongProperty(String var1, long var2) {
      String var4 = this.getStringProperty(var1);
      if (var4 == null) {
         return var2;
      } else {
         try {
            return Long.parseLong(var4);
         } catch (NumberFormatException var6) {
            throw new NumberFormatException(" '" + var4 + "'");
         }
      }
   }

   public short getShortProperty(String var1) {
      String var2 = this.getStringProperty(var1);
      if (var2 == null) {
         throw new NumberFormatException(" null string");
      } else {
         try {
            return Short.parseShort(var2);
         } catch (NumberFormatException var4) {
            throw new NumberFormatException(" '" + var2 + "'");
         }
      }
   }

   public short getShortProperty(String var1, short var2) {
      String var3 = this.getStringProperty(var1);
      if (var3 == null) {
         return var2;
      } else {
         try {
            return Short.parseShort(var3);
         } catch (NumberFormatException var5) {
            throw new NumberFormatException(" '" + var3 + "'");
         }
      }
   }

   public String[] getPropertyGroups(String var1) {
      Enumeration var2 = this.props.propertyNames();
      HashSet<String> var3 = new HashSet<String>(10);
      if (!var1.endsWith(".")) {
         var1 = var1 + ".";
      }

      while (var2.hasMoreElements()) {
         String var4 = (String)var2.nextElement();
         if (var4.startsWith(var1)) {
            String var5 = var4.substring(var1.length(), var4.indexOf(46, var1.length()));
            var3.add(var5);
         }
      }

      return var3.toArray(new String[var3.size()]);
   }

   public Properties getPropertyGroup(String var1) {
      return this.getPropertyGroup(var1, false, null);
   }

   public Properties getPropertyGroup(String var1, boolean var2) {
      return this.getPropertyGroup(var1, var2, null);
   }

   public Properties getPropertyGroup(String var1, boolean var2, String[] var3) {
      Enumeration var4 = this.props.propertyNames();
      Properties var5 = new Properties();
      if (!var1.endsWith(".")) {
         var1 = var1 + ".";
      }

      while (var4.hasMoreElements()) {
         String var6 = (String)var4.nextElement();
         if (var6.startsWith(var1)) {
            boolean var7 = false;
            if (var3 != null) {
               for (int var8 = 0; var8 < var3.length && !var7; var8++) {
                  var7 = var6.startsWith(var3[var8]);
               }
            }

            if (!var7) {
               String var9 = this.getStringProperty(var6, "");
               if (var2) {
                  var5.put(var6.substring(var1.length()), var9);
               } else {
                  var5.put(var6, var9);
               }
            }
         }
      }

      return var5;
   }
}
