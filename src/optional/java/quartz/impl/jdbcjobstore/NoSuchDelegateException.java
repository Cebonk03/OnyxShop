package org.quartz.impl.jdbcjobstore;

import org.quartz.JobPersistenceException;

public class NoSuchDelegateException extends JobPersistenceException {
   private static final long serialVersionUID = -4255865028975822979L;

   public NoSuchDelegateException(String var1) {
      super(var1);
   }

   public NoSuchDelegateException(String var1, Throwable var2) {
      super(var1, var2);
   }
}
