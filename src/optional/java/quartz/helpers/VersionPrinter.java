package org.quartz.helpers;

import org.quartz.core.QuartzScheduler;

public class VersionPrinter {
   private VersionPrinter() {
   }

   public static void main(String[] var0) {
      System.out
         .println(
            "Quartz version: " + QuartzScheduler.getVersionMajor() + "." + QuartzScheduler.getVersionMinor() + "." + QuartzScheduler.getVersionIteration()
         );
   }
}
