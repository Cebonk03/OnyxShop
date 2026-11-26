package org.quartz.impl.jdbcjobstore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CacheDelegate extends StdJDBCDelegate {
   public CacheDelegate() {
   }

   @Override
   protected void setBytes(PreparedStatement var1, int var2, ByteArrayOutputStream var3) {
      var1.setObject(var2, var3 == null ? null : var3.toByteArray(), 2004);
   }

   @Override
   protected Object getObjectFromBlob(ResultSet var1, String var2) {
      Blob var3 = var1.getBlob(var2);
      if (var3 == null) {
         return null;
      } else {
         ObjectInputStream var5;
         try {
            if (var3.length() == 0L) {
               return null;
            }

            InputStream var4 = var3.getBinaryStream();
            if (var4 == null) {
               return null;
            }

            if (!(var4 instanceof ByteArrayInputStream) || ((ByteArrayInputStream)var4).available() != 0) {
               var5 = new ObjectInputStream(var4);

               try {
                  return var5.readObject();
               } finally {
                  var5.close();
               }
            }

            var5 = null;
         } finally {
            var3.free();
         }

         return var5;
      }
   }

   @Override
   protected Object getJobDataFromBlob(ResultSet var1, String var2) {
      if (this.canUseProperties()) {
         Blob var3 = var1.getBlob(var2);
         return var3 == null ? null : new CacheDelegate.BlobFreeingStream(var3, var3.getBinaryStream());
      } else {
         return this.getObjectFromBlob(var1, var2);
      }
   }

   private static class BlobFreeingStream extends InputStream {
      private final Blob source;
      private final InputStream delegate;

      private BlobFreeingStream(Blob var1, InputStream var2) {
         this.source = var1;
         this.delegate = var2;
      }

      @Override
      public int read() {
         return this.delegate.read();
      }

      @Override
      public int read(byte[] var1) {
         return this.delegate.read(var1);
      }

      @Override
      public int read(byte[] var1, int var2, int var3) {
         return this.delegate.read(var1, var2, var3);
      }

      @Override
      public long skip(long var1) {
         return this.delegate.skip(var1);
      }

      @Override
      public int available() {
         return this.delegate.available();
      }

      @Override
      public void close() throws IOException {
         try {
            this.delegate.close();
         } finally {
            try {
               this.source.free();
            } catch (SQLException var7) {
               throw new IOException(var7);
            }
         }
      }

      @Override
      public synchronized void mark(int var1) {
         this.delegate.mark(var1);
      }

      @Override
      public synchronized void reset() {
         this.delegate.reset();
      }

      @Override
      public boolean markSupported() {
         return this.delegate.markSupported();
      }
   }
}
