package org.quartz.simpl;

import org.quartz.spi.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZeroSizeThreadPool implements ThreadPool {
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public ZeroSizeThreadPool() {
   }

   public Logger getLog() {
      return this.log;
   }

   @Override
   public int getPoolSize() {
      return 0;
   }

   @Override
   public void initialize() {
   }

   public void shutdown() {
      this.shutdown(true);
   }

   @Override
   public void shutdown(boolean var1) {
      this.getLog().debug("shutdown complete");
   }

   @Override
   public boolean runInThread(Runnable var1) {
      throw new UnsupportedOperationException("This ThreadPool should not be used on Scheduler instances that are start()ed.");
   }

   @Override
   public int blockForAvailableThreads() {
      throw new UnsupportedOperationException("This ThreadPool should not be used on Scheduler instances that are start()ed.");
   }

   @Override
   public void setInstanceId(String var1) {
   }

   @Override
   public void setInstanceName(String var1) {
   }
}
