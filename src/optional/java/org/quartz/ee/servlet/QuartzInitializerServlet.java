package org.quartz.ee.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;

public class QuartzInitializerServlet extends HttpServlet {
   private static final long serialVersionUID = 1L;
   public static final String QUARTZ_FACTORY_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";
   private boolean performShutdown = true;
   private boolean waitOnShutdown = false;
   private transient Scheduler scheduler = null;

   public QuartzInitializerServlet() {
   }

   public void init(ServletConfig var1) {
      super.init(var1);
      this.log("Quartz Initializer Servlet loaded, initializing Scheduler...");

      try {
         String var3 = var1.getInitParameter("config-file");
         String var4 = var1.getInitParameter("shutdown-on-unload");
         if (var4 != null) {
            this.performShutdown = Boolean.valueOf(var4);
         }

         String var5 = var1.getInitParameter("wait-on-shutdown");
         if (var4 != null) {
            this.waitOnShutdown = Boolean.valueOf(var5);
         }

         StdSchedulerFactory var2 = this.getSchedulerFactory(var3);
         this.scheduler = var2.getScheduler();
         String var6 = var1.getInitParameter("start-scheduler-on-load");
         int var7 = 0;
         String var8 = var1.getInitParameter("start-delay-seconds");

         try {
            if (var8 != null && var8.trim().length() > 0) {
               var7 = Integer.parseInt(var8);
            }
         } catch (Exception var11) {
            this.log("Cannot parse value of 'start-delay-seconds' to an integer: " + var8 + ", defaulting to 5 seconds.", var11);
            var7 = 5;
         }

         if (var6 != null && !Boolean.valueOf(var6)) {
            this.log("Scheduler has not been started. Use scheduler.start()");
         } else if (var7 <= 0) {
            this.scheduler.start();
            this.log("Scheduler has been started...");
         } else {
            this.scheduler.startDelayed(var7);
            this.log("Scheduler will start in " + var7 + " seconds.");
         }

         String var9 = var1.getInitParameter("servlet-context-factory-key");
         if (var9 == null) {
            var9 = "org.quartz.impl.StdSchedulerFactory.KEY";
         }

         this.log("Storing the Quartz Scheduler Factory in the servlet context at key: " + var9);
         var1.getServletContext().setAttribute(var9, var2);
         String var10 = var1.getInitParameter("scheduler-context-servlet-context-key");
         if (var10 != null) {
            this.log("Storing the ServletContext in the scheduler context at key: " + var10);
            this.scheduler.getContext().put(var10, var1.getServletContext());
         }
      } catch (Exception var12) {
         this.log("Quartz Scheduler failed to initialize: " + var12.toString());
         throw new ServletException(var12);
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

   public void destroy() {
      if (this.performShutdown) {
         try {
            if (this.scheduler != null) {
               this.scheduler.shutdown(this.waitOnShutdown);
            }
         } catch (Exception var2) {
            this.log("Quartz Scheduler failed to shutdown cleanly: " + var2.toString());
            var2.printStackTrace();
         }

         this.log("Quartz Scheduler successful shutdown.");
      }
   }

   public void doPost(HttpServletRequest var1, HttpServletResponse var2) {
      var2.sendError(403);
   }

   public void doGet(HttpServletRequest var1, HttpServletResponse var2) {
      var2.sendError(403);
   }
}
