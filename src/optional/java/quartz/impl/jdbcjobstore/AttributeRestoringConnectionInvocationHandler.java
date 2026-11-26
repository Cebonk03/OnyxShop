package org.quartz.impl.jdbcjobstore;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeRestoringConnectionInvocationHandler implements InvocationHandler {
   private Connection conn;
   private boolean overwroteOriginalAutoCommitValue;
   private boolean overwroteOriginalTxIsolationValue;
   private boolean originalAutoCommitValue;
   private int originalTxIsolationValue;

   public AttributeRestoringConnectionInvocationHandler(Connection var1) {
      this.conn = var1;
   }

   protected Logger getLog() {
      return LoggerFactory.getLogger(this.getClass());
   }

   @Override
   public Object invoke(Object var1, Method var2, Object[] var3) {
      if (var2.getName().equals("setAutoCommit")) {
         this.setAutoCommit((Boolean)var3[0]);
      } else if (var2.getName().equals("setTransactionIsolation")) {
         this.setTransactionIsolation((Integer)var3[0]);
      } else {
         if (!var2.getName().equals("close")) {
            try {
               return var2.invoke(this.conn, var3);
            } catch (InvocationTargetException var5) {
               throw var5.getCause() != null ? var5.getCause() : var5;
            }
         }

         this.close();
      }

      return null;
   }

   public void setAutoCommit(boolean var1) {
      boolean var2 = this.conn.getAutoCommit();
      if (var1 != var2) {
         if (!this.overwroteOriginalAutoCommitValue) {
            this.overwroteOriginalAutoCommitValue = true;
            this.originalAutoCommitValue = var2;
         }

         this.conn.setAutoCommit(var1);
      }
   }

   public void setTransactionIsolation(int var1) {
      int var2 = this.conn.getTransactionIsolation();
      if (var1 != var2) {
         if (!this.overwroteOriginalTxIsolationValue) {
            this.overwroteOriginalTxIsolationValue = true;
            this.originalTxIsolationValue = var2;
         }

         this.conn.setTransactionIsolation(var1);
      }
   }

   public Connection getWrappedConnection() {
      return this.conn;
   }

   public void restoreOriginalAtributes() {
      try {
         if (this.overwroteOriginalAutoCommitValue) {
            this.conn.setAutoCommit(this.originalAutoCommitValue);
         }
      } catch (Throwable var3) {
         this.getLog().warn("Failed restore connection's original auto commit setting.", var3);
      }

      try {
         if (this.overwroteOriginalTxIsolationValue) {
            this.conn.setTransactionIsolation(this.originalTxIsolationValue);
         }
      } catch (Throwable var2) {
         this.getLog().warn("Failed restore connection's original transaction isolation setting.", var2);
      }
   }

   public void close() {
      this.restoreOriginalAtributes();
      this.conn.close();
   }
}
