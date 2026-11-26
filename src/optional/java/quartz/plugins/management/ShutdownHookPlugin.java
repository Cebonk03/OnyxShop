package org.quartz.plugins.management;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownHookPlugin implements SchedulerPlugin {
   private boolean cleanShutdown = true;
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public ShutdownHookPlugin() {
   }

   public boolean isCleanShutdown() {
      return this.cleanShutdown;
   }

   public void setCleanShutdown(boolean var1) {
      this.cleanShutdown = var1;
   }

   protected Logger getLog() {
      return this.log;
   }

   @Override
   public void initialize(String var1, final Scheduler var2, ClassLoadHelper var3) {
      this.getLog().info("Registering Quartz shutdown hook.");
      Thread var4 = new Thread("Quartz Shutdown-Hook " + var2.getSchedulerName()) {
         @Override
         public void run() {
            ShutdownHookPlugin.this.getLog().info("Shutting down Quartz...");

            try {
               var2.shutdown(ShutdownHookPlugin.this.isCleanShutdown());
            } catch (SchedulerException var2x) {
               ShutdownHookPlugin.this.getLog().info("Error shutting down Quartz: " + var2x.getMessage(), var2x);
            }
         }
      };
      Runtime.getRuntime().addShutdownHook(var4);
   }

   @Override
   public void start() {
   }

   @Override
   public void shutdown() {
   }
}
