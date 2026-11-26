package org.quartz.impl;

import org.quartz.spi.ThreadExecutor;

public class DefaultThreadExecutor implements ThreadExecutor {
   public DefaultThreadExecutor() {
   }

   @Override
   public void initialize() {
   }

   @Override
   public void execute(Thread var1) {
      var1.start();
   }
}
