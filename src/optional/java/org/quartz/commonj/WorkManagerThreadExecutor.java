package org.quartz.commonj;

import commonj.work.WorkManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.quartz.spi.ThreadExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkManagerThreadExecutor implements ThreadExecutor {
   private final Logger log = LoggerFactory.getLogger(this.getClass());
   private String workManagerName;
   private WorkManager workManager;

   public WorkManagerThreadExecutor() {
   }

   @Override
   public void execute(Thread var1) {
      DelegatingWork var2 = new DelegatingWork(var1);

      try {
         this.workManager.schedule(var2);
      } catch (Exception var4) {
         this.log.error("Error attempting to schedule QuartzSchedulerThread: " + var4.getMessage(), var4);
      }
   }

   @Override
   public void initialize() {
      try {
         this.workManager = (WorkManager)new InitialContext().lookup(this.workManagerName);
      } catch (NamingException var2) {
         throw new IllegalStateException("Could not locate WorkManager: " + var2.getMessage(), var2);
      }
   }

   public void setWorkManagerName(String var1) {
      this.workManagerName = var1;
   }
}
