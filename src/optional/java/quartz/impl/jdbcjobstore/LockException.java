package org.quartz.impl.jdbcjobstore;

import org.quartz.JobPersistenceException;

public class LockException extends JobPersistenceException {
   private static final long serialVersionUID = 3993800462589137228L;

   public LockException(String var1) {
      super(var1);
   }

   public LockException(String var1, Throwable var2) {
      super(var1, var2);
   }
}
