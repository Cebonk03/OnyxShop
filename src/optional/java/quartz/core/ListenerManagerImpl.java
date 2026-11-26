package org.quartz.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.Matcher;
import org.quartz.SchedulerListener;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.quartz.impl.matchers.EverythingMatcher;

public class ListenerManagerImpl implements ListenerManager {
   private Map<String, JobListener> globalJobListeners = new LinkedHashMap<>(10);
   private Map<String, TriggerListener> globalTriggerListeners = new LinkedHashMap<>(10);
   private Map<String, List<Matcher<JobKey>>> globalJobListenersMatchers = new LinkedHashMap<>(10);
   private Map<String, List<Matcher<TriggerKey>>> globalTriggerListenersMatchers = new LinkedHashMap<>(10);
   private ArrayList<SchedulerListener> schedulerListeners = new ArrayList<>(10);

   public ListenerManagerImpl() {
   }

   @Override
   public void addJobListener(JobListener var1, Matcher<JobKey>... var2) {
      this.addJobListener(var1, Arrays.asList(var2));
   }

   @Override
   public void addJobListener(JobListener var1, List<Matcher<JobKey>> var2) {
      if (var1.getName() != null && var1.getName().length() != 0) {
         synchronized (this.globalJobListeners) {
            this.globalJobListeners.put(var1.getName(), var1);
            LinkedList var4 = new LinkedList();
            if (var2 != null && var2.size() > 0) {
               var4.addAll(var2);
            } else {
               var4.add(EverythingMatcher.allJobs());
            }

            this.globalJobListenersMatchers.put(var1.getName(), var4);
         }
      } else {
         throw new IllegalArgumentException("JobListener name cannot be empty.");
      }
   }

   @Override
   public void addJobListener(JobListener var1) {
      this.addJobListener(var1, EverythingMatcher.allJobs());
   }

   @Override
   public void addJobListener(JobListener var1, Matcher<JobKey> var2) {
      if (var1.getName() != null && var1.getName().length() != 0) {
         synchronized (this.globalJobListeners) {
            this.globalJobListeners.put(var1.getName(), var1);
            LinkedList var4 = new LinkedList();
            if (var2 != null) {
               var4.add(var2);
            } else {
               var4.add(EverythingMatcher.allJobs());
            }

            this.globalJobListenersMatchers.put(var1.getName(), var4);
         }
      } else {
         throw new IllegalArgumentException("JobListener name cannot be empty.");
      }
   }

   @Override
   public boolean addJobListenerMatcher(String var1, Matcher<JobKey> var2) {
      if (var2 == null) {
         throw new IllegalArgumentException("Null value not acceptable.");
      } else {
         synchronized (this.globalJobListeners) {
            List var4 = this.globalJobListenersMatchers.get(var1);
            if (var4 == null) {
               return false;
            } else {
               var4.add(var2);
               return true;
            }
         }
      }
   }

   @Override
   public boolean removeJobListenerMatcher(String var1, Matcher<JobKey> var2) {
      if (var2 == null) {
         throw new IllegalArgumentException("Non-null value not acceptable.");
      } else {
         synchronized (this.globalJobListeners) {
            List var4 = this.globalJobListenersMatchers.get(var1);
            return var4 == null ? false : var4.remove(var2);
         }
      }
   }

   @Override
   public List<Matcher<JobKey>> getJobListenerMatchers(String var1) {
      synchronized (this.globalJobListeners) {
         List var3 = this.globalJobListenersMatchers.get(var1);
         return var3 == null ? null : Collections.unmodifiableList(var3);
      }
   }

   @Override
   public boolean setJobListenerMatchers(String var1, List<Matcher<JobKey>> var2) {
      if (var2 == null) {
         throw new IllegalArgumentException("Non-null value not acceptable.");
      } else {
         synchronized (this.globalJobListeners) {
            List var4 = this.globalJobListenersMatchers.get(var1);
            if (var4 == null) {
               return false;
            } else {
               this.globalJobListenersMatchers.put(var1, var2);
               return true;
            }
         }
      }
   }

   @Override
   public boolean removeJobListener(String var1) {
      synchronized (this.globalJobListeners) {
         return this.globalJobListeners.remove(var1) != null;
      }
   }

   @Override
   public List<JobListener> getJobListeners() {
      synchronized (this.globalJobListeners) {
         return Collections.unmodifiableList(new LinkedList<>(this.globalJobListeners.values()));
      }
   }

   @Override
   public JobListener getJobListener(String var1) {
      synchronized (this.globalJobListeners) {
         return this.globalJobListeners.get(var1);
      }
   }

   @Override
   public void addTriggerListener(TriggerListener var1, Matcher<TriggerKey>... var2) {
      this.addTriggerListener(var1, Arrays.asList(var2));
   }

   @Override
   public void addTriggerListener(TriggerListener var1, List<Matcher<TriggerKey>> var2) {
      if (var1.getName() != null && var1.getName().length() != 0) {
         synchronized (this.globalTriggerListeners) {
            this.globalTriggerListeners.put(var1.getName(), var1);
            LinkedList var4 = new LinkedList();
            if (var2 != null && var2.size() > 0) {
               var4.addAll(var2);
            } else {
               var4.add(EverythingMatcher.allTriggers());
            }

            this.globalTriggerListenersMatchers.put(var1.getName(), var4);
         }
      } else {
         throw new IllegalArgumentException("TriggerListener name cannot be empty.");
      }
   }

   @Override
   public void addTriggerListener(TriggerListener var1) {
      this.addTriggerListener(var1, EverythingMatcher.allTriggers());
   }

   @Override
   public void addTriggerListener(TriggerListener var1, Matcher<TriggerKey> var2) {
      if (var2 == null) {
         throw new IllegalArgumentException("Null value not acceptable for matcher.");
      } else if (var1.getName() != null && var1.getName().length() != 0) {
         synchronized (this.globalTriggerListeners) {
            this.globalTriggerListeners.put(var1.getName(), var1);
            LinkedList var4 = new LinkedList();
            var4.add(var2);
            this.globalTriggerListenersMatchers.put(var1.getName(), var4);
         }
      } else {
         throw new IllegalArgumentException("TriggerListener name cannot be empty.");
      }
   }

   @Override
   public boolean addTriggerListenerMatcher(String var1, Matcher<TriggerKey> var2) {
      if (var2 == null) {
         throw new IllegalArgumentException("Non-null value not acceptable.");
      } else {
         synchronized (this.globalTriggerListeners) {
            List var4 = this.globalTriggerListenersMatchers.get(var1);
            if (var4 == null) {
               return false;
            } else {
               var4.add(var2);
               return true;
            }
         }
      }
   }

   @Override
   public boolean removeTriggerListenerMatcher(String var1, Matcher<TriggerKey> var2) {
      if (var2 == null) {
         throw new IllegalArgumentException("Non-null value not acceptable.");
      } else {
         synchronized (this.globalTriggerListeners) {
            List var4 = this.globalTriggerListenersMatchers.get(var1);
            return var4 == null ? false : var4.remove(var2);
         }
      }
   }

   @Override
   public List<Matcher<TriggerKey>> getTriggerListenerMatchers(String var1) {
      synchronized (this.globalTriggerListeners) {
         List var3 = this.globalTriggerListenersMatchers.get(var1);
         return var3 == null ? null : Collections.unmodifiableList(var3);
      }
   }

   @Override
   public boolean setTriggerListenerMatchers(String var1, List<Matcher<TriggerKey>> var2) {
      if (var2 == null) {
         throw new IllegalArgumentException("Non-null value not acceptable.");
      } else {
         synchronized (this.globalTriggerListeners) {
            List var4 = this.globalTriggerListenersMatchers.get(var1);
            if (var4 == null) {
               return false;
            } else {
               this.globalTriggerListenersMatchers.put(var1, var2);
               return true;
            }
         }
      }
   }

   @Override
   public boolean removeTriggerListener(String var1) {
      synchronized (this.globalTriggerListeners) {
         return this.globalTriggerListeners.remove(var1) != null;
      }
   }

   @Override
   public List<TriggerListener> getTriggerListeners() {
      synchronized (this.globalTriggerListeners) {
         return Collections.unmodifiableList(new LinkedList<>(this.globalTriggerListeners.values()));
      }
   }

   @Override
   public TriggerListener getTriggerListener(String var1) {
      synchronized (this.globalTriggerListeners) {
         return this.globalTriggerListeners.get(var1);
      }
   }

   @Override
   public void addSchedulerListener(SchedulerListener var1) {
      synchronized (this.schedulerListeners) {
         this.schedulerListeners.add(var1);
      }
   }

   @Override
   public boolean removeSchedulerListener(SchedulerListener var1) {
      synchronized (this.schedulerListeners) {
         return this.schedulerListeners.remove(var1);
      }
   }

   @Override
   public List<SchedulerListener> getSchedulerListeners() {
      synchronized (this.schedulerListeners) {
         return Collections.unmodifiableList(new ArrayList<>(this.schedulerListeners));
      }
   }
}
