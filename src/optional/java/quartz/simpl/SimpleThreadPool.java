package org.quartz.simpl;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.quartz.SchedulerConfigException;
import org.quartz.spi.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleThreadPool implements ThreadPool {
   private int count = -1;
   private int prio = 5;
   private boolean isShutdown = false;
   private boolean handoffPending = false;
   private boolean inheritLoader = false;
   private boolean inheritGroup = true;
   private boolean makeThreadsDaemons = false;
   private ThreadGroup threadGroup;
   private final Object nextRunnableLock = new Object();
   private List<SimpleThreadPool.WorkerThread> workers;
   private LinkedList<SimpleThreadPool.WorkerThread> availWorkers = new LinkedList<>();
   private LinkedList<SimpleThreadPool.WorkerThread> busyWorkers = new LinkedList<>();
   private String threadNamePrefix;
   private final Logger log = LoggerFactory.getLogger(this.getClass());
   private String schedulerInstanceName;

   public SimpleThreadPool() {
   }

   public SimpleThreadPool(int var1, int var2) {
      this.setThreadCount(var1);
      this.setThreadPriority(var2);
   }

   public Logger getLog() {
      return this.log;
   }

   @Override
   public int getPoolSize() {
      return this.getThreadCount();
   }

   public void setThreadCount(int var1) {
      this.count = var1;
   }

   public int getThreadCount() {
      return this.count;
   }

   public void setThreadPriority(int var1) {
      this.prio = var1;
   }

   public int getThreadPriority() {
      return this.prio;
   }

   public void setThreadNamePrefix(String var1) {
      this.threadNamePrefix = var1;
   }

   public String getThreadNamePrefix() {
      return this.threadNamePrefix;
   }

   public boolean isThreadsInheritContextClassLoaderOfInitializingThread() {
      return this.inheritLoader;
   }

   public void setThreadsInheritContextClassLoaderOfInitializingThread(boolean var1) {
      this.inheritLoader = var1;
   }

   public boolean isThreadsInheritGroupOfInitializingThread() {
      return this.inheritGroup;
   }

   public void setThreadsInheritGroupOfInitializingThread(boolean var1) {
      this.inheritGroup = var1;
   }

   public boolean isMakeThreadsDaemons() {
      return this.makeThreadsDaemons;
   }

   public void setMakeThreadsDaemons(boolean var1) {
      this.makeThreadsDaemons = var1;
   }

   @Override
   public void setInstanceId(String var1) {
   }

   @Override
   public void setInstanceName(String var1) {
      this.schedulerInstanceName = var1;
   }

   @Override
   public void initialize() throws SchedulerConfigException {
      if (this.workers == null || this.workers.size() <= 0) {
         if (this.count <= 0) {
            throw new SchedulerConfigException("Thread count must be > 0");
         } else if (this.prio > 0 && this.prio <= 9) {
            if (this.isThreadsInheritGroupOfInitializingThread()) {
               this.threadGroup = Thread.currentThread().getThreadGroup();
            } else {
               this.threadGroup = Thread.currentThread().getThreadGroup();

               ThreadGroup var1;
               for (var1 = this.threadGroup; !var1.getName().equals("main"); var1 = this.threadGroup.getParent()) {
                  this.threadGroup = var1;
               }

               this.threadGroup = new ThreadGroup(var1, this.schedulerInstanceName + "-SimpleThreadPool");
               if (this.isMakeThreadsDaemons()) {
                  this.threadGroup.setDaemon(true);
               }
            }

            if (this.isThreadsInheritContextClassLoaderOfInitializingThread()) {
               this.getLog().info("Job execution threads will use class loader of thread: " + Thread.currentThread().getName());
            }

            for (SimpleThreadPool.WorkerThread var2 : this.createWorkerThreads(this.count)) {
               var2.start();
               this.availWorkers.add(var2);
            }
         } else {
            throw new SchedulerConfigException("Thread priority must be > 0 and <= 9");
         }
      }
   }

   protected List<SimpleThreadPool.WorkerThread> createWorkerThreads(int var1) {
      this.workers = new LinkedList<>();

      for (int var2 = 1; var2 <= var1; var2++) {
         String var3 = this.getThreadNamePrefix();
         if (var3 == null) {
            var3 = this.schedulerInstanceName + "_Worker";
         }

         SimpleThreadPool.WorkerThread var4 = new SimpleThreadPool.WorkerThread(
            this, this.threadGroup, var3 + "-" + var2, this.getThreadPriority(), this.isMakeThreadsDaemons()
         );
         if (this.isThreadsInheritContextClassLoaderOfInitializingThread()) {
            var4.setContextClassLoader(Thread.currentThread().getContextClassLoader());
         }

         this.workers.add(var4);
      }

      return this.workers;
   }

   public void shutdown() {
      this.shutdown(true);
   }

   @Override
   public void shutdown(boolean var1) {
      synchronized (this.nextRunnableLock) {
         this.getLog().debug("Shutting down threadpool...");
         this.isShutdown = true;
         if (this.workers != null) {
            for (SimpleThreadPool.WorkerThread var4 : this.workers) {
               var4.shutdown();
               this.availWorkers.remove(var4);
            }

            this.nextRunnableLock.notifyAll();
            if (var1) {
               boolean var20 = false;

               try {
                  while (this.handoffPending) {
                     try {
                        this.nextRunnableLock.wait(100L);
                     } catch (InterruptedException var16) {
                        var20 = true;
                     }
                  }

                  while (this.busyWorkers.size() > 0) {
                     SimpleThreadPool.WorkerThread var5 = this.busyWorkers.getFirst();

                     try {
                        this.getLog().debug("Waiting for thread " + var5.getName() + " to shut down");
                        this.nextRunnableLock.wait(2000L);
                     } catch (InterruptedException var15) {
                        var20 = true;
                     }
                  }

                  Iterator var19 = this.workers.iterator();

                  while (var19.hasNext()) {
                     SimpleThreadPool.WorkerThread var21 = (SimpleThreadPool.WorkerThread)var19.next();

                     try {
                        var21.join();
                        var19.remove();
                     } catch (InterruptedException var14) {
                        var20 = true;
                     }
                  }
               } finally {
                  if (var20) {
                     Thread.currentThread().interrupt();
                  }
               }

               this.getLog().debug("No executing jobs remaining, all threads stopped.");
            }

            this.getLog().debug("Shutdown of threadpool complete.");
         }
      }
   }

   @Override
   public boolean runInThread(Runnable var1) {
      if (var1 == null) {
         return false;
      } else {
         synchronized (this.nextRunnableLock) {
            this.handoffPending = true;

            while (this.availWorkers.size() < 1 && !this.isShutdown) {
               try {
                  this.nextRunnableLock.wait(500L);
               } catch (InterruptedException var5) {
               }
            }

            if (!this.isShutdown) {
               SimpleThreadPool.WorkerThread var3 = this.availWorkers.removeFirst();
               this.busyWorkers.add(var3);
               var3.run(var1);
            } else {
               SimpleThreadPool.WorkerThread var7 = new SimpleThreadPool.WorkerThread(
                  this, this.threadGroup, "WorkerThread-LastJob", this.prio, this.isMakeThreadsDaemons(), var1
               );
               this.busyWorkers.add(var7);
               this.workers.add(var7);
               var7.start();
            }

            this.nextRunnableLock.notifyAll();
            this.handoffPending = false;
            return true;
         }
      }
   }

   @Override
   public int blockForAvailableThreads() {
      synchronized (this.nextRunnableLock) {
         while ((this.availWorkers.size() < 1 || this.handoffPending) && !this.isShutdown) {
            try {
               this.nextRunnableLock.wait(500L);
            } catch (InterruptedException var4) {
            }
         }

         return this.availWorkers.size();
      }
   }

   protected void makeAvailable(SimpleThreadPool.WorkerThread var1) {
      synchronized (this.nextRunnableLock) {
         if (!this.isShutdown) {
            this.availWorkers.add(var1);
         }

         this.busyWorkers.remove(var1);
         this.nextRunnableLock.notifyAll();
      }
   }

   protected void clearFromBusyWorkersList(SimpleThreadPool.WorkerThread var1) {
      synchronized (this.nextRunnableLock) {
         this.busyWorkers.remove(var1);
         this.nextRunnableLock.notifyAll();
      }
   }

   class WorkerThread extends Thread {
      private final Object lock = new Object();
      private AtomicBoolean run = new AtomicBoolean(true);
      private SimpleThreadPool tp;
      private Runnable runnable = null;
      private boolean runOnce = false;

      WorkerThread(SimpleThreadPool var2, ThreadGroup var3, String var4, int var5, boolean var6) {
         this(var2, var3, var4, var5, var6, null);
      }

      WorkerThread(SimpleThreadPool var2, ThreadGroup var3, String var4, int var5, boolean var6, Runnable var7) {
         super(var3, var4);
         this.tp = var2;
         this.runnable = var7;
         if (var7 != null) {
            this.runOnce = true;
         }

         this.setPriority(var5);
         this.setDaemon(var6);
      }

      void shutdown() {
         this.run.set(false);
      }

      public void run(Runnable var1) {
         synchronized (this.lock) {
            if (this.runnable != null) {
               throw new IllegalStateException("Already running a Runnable!");
            } else {
               this.runnable = var1;
               this.lock.notifyAll();
            }
         }
      }

      @Override
      public void run() {
         boolean var1 = false;

         while (this.run.get()) {
            try {
               synchronized (this.lock) {
                  while (this.runnable == null && this.run.get()) {
                     this.lock.wait(500L);
                  }

                  if (this.runnable != null) {
                     var1 = true;
                     this.runnable.run();
                  }
               }
            } catch (InterruptedException var26) {
               InterruptedException var30 = var26;

               try {
                  SimpleThreadPool.this.getLog().error("Worker thread was interrupt()'ed.", var30);
               } catch (Exception var24) {
               }
            } catch (Throwable var27) {
               Throwable var2 = var27;

               try {
                  SimpleThreadPool.this.getLog().error("Error while executing the Runnable: ", var2);
               } catch (Exception var23) {
               }
            } finally {
               synchronized (this.lock) {
                  this.runnable = null;
               }

               if (this.getPriority() != this.tp.getThreadPriority()) {
                  this.setPriority(this.tp.getThreadPriority());
               }

               if (this.runOnce) {
                  this.run.set(false);
                  SimpleThreadPool.this.clearFromBusyWorkersList(this);
               } else if (var1) {
                  var1 = false;
                  SimpleThreadPool.this.makeAvailable(this);
               }
            }
         }

         try {
            SimpleThreadPool.this.getLog().debug("WorkerThread is shut down.");
         } catch (Exception var22) {
         }
      }
   }
}
