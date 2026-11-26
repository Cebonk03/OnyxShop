package org.quartz.commonj;

import commonj.work.Work;

class DelegatingWork implements Work {
   private final Runnable delegate;

   public DelegatingWork(Runnable var1) {
      this.delegate = var1;
   }

   public final Runnable getDelegate() {
      return this.delegate;
   }

   public void run() {
      this.delegate.run();
   }

   public boolean isDaemon() {
      return false;
   }

   public void release() {
   }
}
