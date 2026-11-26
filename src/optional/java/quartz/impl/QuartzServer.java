package org.quartz.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.RMISecurityManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.listeners.SchedulerListenerSupport;

public class QuartzServer extends SchedulerListenerSupport {
   private Scheduler sched = null;

   QuartzServer() {
   }

   public void serve(SchedulerFactory var1, boolean var2) {
      this.sched = var1.getScheduler();
      this.sched.start();

      try {
         Thread.sleep(3000L);
      } catch (Exception var4) {
      }

      System.out.println("\n*** The scheduler successfully started.");
      if (var2) {
         System.out.println("\n");
         System.out.println("The scheduler will now run until you type \"exit\"");
         System.out.println("   If it was configured to export itself via RMI,");
         System.out.println("   then other process may now use it.");
         BufferedReader var3 = new BufferedReader(new InputStreamReader(System.in));

         do {
            System.out.print("Type 'exit' to shutdown the server: ");
         } while (!"exit".equals(var3.readLine()));

         System.out.println("\n...Shutting down server...");
         this.sched.shutdown(true);
      }
   }

   @Override
   public void schedulerError(String var1, SchedulerException var2) {
      System.err.println("*** " + var1);
      var2.printStackTrace();
   }

   @Override
   public void schedulerShutdown() {
      System.out.println("\n*** The scheduler is now shutdown.");
      this.sched = null;
   }

   public static void main(String[] var0) {
      if (System.getSecurityManager() == null) {
         System.setSecurityManager(new RMISecurityManager());
      }

      try {
         QuartzServer var1 = new QuartzServer();
         if (var0.length == 0) {
            var1.serve(new StdSchedulerFactory(), false);
         } else if (var0.length == 1 && var0[0].equalsIgnoreCase("console")) {
            var1.serve(new StdSchedulerFactory(), true);
         } else {
            System.err.println("\nUsage: QuartzServer [console]");
         }
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }
}
