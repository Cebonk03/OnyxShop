package org.quartz.ee.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuartzInitializerListener implements ServletContextListener {
   public static final String QUARTZ_FACTORY_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";
   private boolean performShutdown = true;
   private boolean waitOnShutdown = false;
   private Scheduler scheduler = null;
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public QuartzInitializerListener() {
   }

   public void contextInitialized(ServletContextEvent var1) {
      this.log.info("Quartz Initializer Servlet loaded, initializing Scheduler...");
      ServletContext var2 = var1.getServletContext();

      try {
         String var4 = var2.getInitParameter("quartz:config-file");
         if (var4 == null) {
            var4 = var2.getInitParameter("config-file");
         }

         String var5 = var2.getInitParameter("quartz:shutdown-on-unload");
         if (var5 == null) {
            var5 = var2.getInitParameter("shutdown-on-unload");
         }

         if (var5 != null) {
            this.performShutdown = Boolean.valueOf(var5);
         }

         String var6 = var2.getInitParameter("quartz:wait-on-shutdown");
         if (var5 != null) {
            this.waitOnShutdown = Boolean.valueOf(var6);
         }

         StdSchedulerFactory var3 = this.getSchedulerFactory(var4);
         this.scheduler = var3.getScheduler();
         String var7 = var2.getInitParameter("quartz:start-on-load");
         if (var7 == null) {
            var7 = var2.getInitParameter("start-scheduler-on-load");
         }

         int var8 = 0;
         String var9 = var2.getInitParameter("quartz:start-delay-seconds");
         if (var9 == null) {
            var9 = var2.getInitParameter("start-delay-seconds");
         }

         try {
            if (var9 != null && var9.trim().length() > 0) {
               var8 = Integer.parseInt(var9);
            }
         } catch (Exception var12) {
            this.log.error("Cannot parse value of 'start-delay-seconds' to an integer: " + var9 + ", defaulting to 5 seconds.");
            var8 = 5;
         }

         if (var7 != null && !Boolean.valueOf(var7)) {
            this.log.info("Scheduler has not been started. Use scheduler.start()");
         } else if (var8 <= 0) {
            this.scheduler.start();
            this.log.info("Scheduler has been started...");
         } else {
            this.scheduler.startDelayed(var8);
            this.log.info("Scheduler will start in " + var8 + " seconds.");
         }

         String var10 = var2.getInitParameter("quartz:servlet-context-factory-key");
         if (var10 == null) {
            var10 = var2.getInitParameter("servlet-context-factory-key");
         }

         if (var10 == null) {
            var10 = "org.quartz.impl.StdSchedulerFactory.KEY";
         }

         this.log.info("Storing the Quartz Scheduler Factory in the servlet context at key: " + var10);
         var2.setAttribute(var10, var3);
         String var11 = var2.getInitParameter("quartz:scheduler-context-servlet-context-key");
         if (var11 == null) {
            var11 = var2.getInitParameter("scheduler-context-servlet-context-key");
         }

         if (var11 != null) {
            this.log.info("Storing the ServletContext in the scheduler context at key: " + var11);
            this.scheduler.getContext().put(var11, var2);
         }
      } catch (Exception var13) {
         this.log.error("Quartz Scheduler failed to initialize: " + var13.toString());
         var13.printStackTrace();
      }
   }

   protected StdSchedulerFactory getSchedulerFactory(String var1) {
      StdSchedulerFactory var2;
      if (var1 != null) {
         var2 = new StdSchedulerFactory(var1);
      } else {
         var2 = new StdSchedulerFactory();
      }

      return var2;
   }

   public void contextDestroyed(ServletContextEvent var1) {
      if (this.performShutdown) {
         try {
            if (this.scheduler != null) {
               this.scheduler.shutdown(this.waitOnShutdown);
            }
         } catch (Exception var3) {
            this.log.error("Quartz Scheduler failed to shutdown cleanly: " + var3.toString());
            var3.printStackTrace();
         }

         this.log.info("Quartz Scheduler successful shutdown.");
      }
   }
}
