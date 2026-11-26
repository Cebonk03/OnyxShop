package org.terracotta.quartz.collections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class SerializationHelper {
   private static final char MARKER = '\ufffe';

   public SerializationHelper() {
   }

   public static byte[] serialize(Object var0) {
      try {
         ByteArrayOutputStream var1 = new ByteArrayOutputStream();
         ObjectOutputStream var2 = new ObjectOutputStream(var1);
         var2.writeObject(var0);
         var2.close();
         return var1.toByteArray();
      } catch (IOException var3) {
         throw new RuntimeException("error serializing " + var0, var3);
      }
   }

   public static Object deserialize(byte[] var0) {
      try {
         ByteArrayInputStream var1 = new ByteArrayInputStream(var0);
         ObjectInputStream var2 = new ObjectInputStream(var1);
         Object var3 = var2.readObject();
         var2.close();
         return var3;
      } catch (Exception var4) {
         throw new RuntimeException("error deserializing " + var0, var4);
      }
   }

   public static Object deserializeFromString(String var0) {
      if (var0.length() >= 1 && var0.charAt(0) == '\ufffe') {
         ObjectInputStream var1 = new ObjectInputStream(new SerializationHelper.StringSerializedObjectInputStream(var0));
         return var1.readObject();
      } else {
         return var0;
      }
   }

   public static String serializeToString(Object var0) {
      if (var0 instanceof String) {
         String var3 = (String)var0;
         if (var3.length() >= 1 && var3.charAt(0) == '\ufffe') {
            throw new IOException("Illegal string key: " + var3);
         } else {
            return var3;
         }
      } else {
         SerializationHelper.StringSerializedObjectOutputStream var1 = new SerializationHelper.StringSerializedObjectOutputStream();
         ObjectOutputStream var2 = new ObjectOutputStream(var1);
         writeStringKey(var0, var2);
         var2.close();
         return var1.toString();
      }
   }

   private static void writeStringKey(Object var0, ObjectOutputStream var1) {
      var1.writeObject(var0);
   }

   private static class StringSerializedObjectInputStream extends InputStream {
      private final String source;
      private final int length;
      private int index;

      StringSerializedObjectInputStream(String var1) {
         this.source = var1;
         this.length = var1.length();
         this.read();
      }

      @Override
      public int read() {
         return this.index == this.length ? -1 : this.source.charAt(this.index++) & 0xFF;
      }
   }

   private static class StringSerializedObjectOutputStream extends OutputStream {
      private int count;
      private char[] buf;

      StringSerializedObjectOutputStream() {
         this(16);
      }

      StringSerializedObjectOutputStream(int var1) {
         var1 = Math.max(1, var1);
         this.buf = new char[var1];
         this.buf[this.count++] = '\ufffe';
      }

      @Override
      public void write(int var1) {
         if (this.count + 1 > this.buf.length) {
            char[] var2 = new char[this.buf.length << 1];
            System.arraycopy(this.buf, 0, var2, 0, this.count);
            this.buf = var2;
         }

         this.writeChar(var1);
      }

      private void writeChar(int var1) {
         this.buf[this.count++] = (char)(var1 & 0xFF);
      }

      @Override
      public void write(byte[] var1, int var2, int var3) {
         if (var2 < 0 || var2 > var1.length || var3 < 0 || var2 + var3 > var1.length || var2 + var3 < 0) {
            throw new IndexOutOfBoundsException();
         } else if (var3 != 0) {
            int var4 = this.count + var3;
            if (var4 > this.buf.length) {
               char[] var5 = new char[Math.max(this.buf.length << 1, var4)];
               System.arraycopy(this.buf, 0, var5, 0, this.count);
               this.buf = var5;
            }

            for (int var6 = 0; var6 < var3; var6++) {
               this.writeChar(var1[var2 + var6]);
            }
         }
      }

      @Override
      public String toString() {
         return new String(this.buf, 0, this.count);
      }
   }
}
