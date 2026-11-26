package org.quartz.impl;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerListener;
import org.quartz.core.JobRunShellFactory;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.ee.jta.JTAAnnotationAwareJobRunShellFactory;
import org.quartz.ee.jta.JTAJobRunShellFactory;
import org.quartz.ee.jta.UserTransactionHelper;
import org.quartz.impl.jdbcjobstore.JobStoreSupport;
import org.quartz.impl.jdbcjobstore.Semaphore;
import org.quartz.impl.jdbcjobstore.TablePrefixAware;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.management.ManagementRESTServiceConfiguration;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.InstanceIdGenerator;
import org.quartz.spi.JobFactory;
import org.quartz.spi.JobStore;
import org.quartz.spi.SchedulerPlugin;
import org.quartz.spi.ThreadExecutor;
import org.quartz.spi.ThreadPool;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;
import org.quartz.utils.JNDIConnectionProvider;
import org.quartz.utils.PoolingConnectionProvider;
import org.quartz.utils.PropertiesParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StdSchedulerFactory implements SchedulerFactory {
   public static final String PROPERTIES_FILE = "org.quartz.properties";
   public static final String PROP_SCHED_INSTANCE_NAME = "org.quartz.scheduler.instanceName";
   public static final String PROP_SCHED_INSTANCE_ID = "org.quartz.scheduler.instanceId";
   public static final String PROP_SCHED_INSTANCE_ID_GENERATOR_PREFIX = "org.quartz.scheduler.instanceIdGenerator";
   public static final String PROP_SCHED_INSTANCE_ID_GENERATOR_CLASS = "org.quartz.scheduler.instanceIdGenerator.class";
   public static final String PROP_SCHED_THREAD_NAME = "org.quartz.scheduler.threadName";
   public static final String PROP_SCHED_BATCH_TIME_WINDOW = "org.quartz.scheduler.batchTriggerAcquisitionFireAheadTimeWindow";
   public static final String PROP_SCHED_MAX_BATCH_SIZE = "org.quartz.scheduler.batchTriggerAcquisitionMaxCount";
   public static final String PROP_SCHED_JMX_EXPORT = "org.quartz.scheduler.jmx.export";
   public static final String PROP_SCHED_JMX_OBJECT_NAME = "org.quartz.scheduler.jmx.objectName";
   public static final String PROP_SCHED_JMX_PROXY = "org.quartz.scheduler.jmx.proxy";
   public static final String PROP_SCHED_JMX_PROXY_CLASS = "org.quartz.scheduler.jmx.proxy.class";
   public static final String PROP_SCHED_RMI_EXPORT = "org.quartz.scheduler.rmi.export";
   public static final String PROP_SCHED_RMI_PROXY = "org.quartz.scheduler.rmi.proxy";
   public static final String PROP_SCHED_RMI_HOST = "org.quartz.scheduler.rmi.registryHost";
   public static final String PROP_SCHED_RMI_PORT = "org.quartz.scheduler.rmi.registryPort";
   public static final String PROP_SCHED_RMI_SERVER_PORT = "org.quartz.scheduler.rmi.serverPort";
   public static final String PROP_SCHED_RMI_CREATE_REGISTRY = "org.quartz.scheduler.rmi.createRegistry";
   public static final String PROP_SCHED_RMI_BIND_NAME = "org.quartz.scheduler.rmi.bindName";
   public static final String PROP_SCHED_WRAP_JOB_IN_USER_TX = "org.quartz.scheduler.wrapJobExecutionInUserTransaction";
   public static final String PROP_SCHED_USER_TX_URL = "org.quartz.scheduler.userTransactionURL";
   public static final String PROP_SCHED_IDLE_WAIT_TIME = "org.quartz.scheduler.idleWaitTime";
   public static final String PROP_SCHED_DB_FAILURE_RETRY_INTERVAL = "org.quartz.scheduler.dbFailureRetryInterval";
   public static final String PROP_SCHED_MAKE_SCHEDULER_THREAD_DAEMON = "org.quartz.scheduler.makeSchedulerThreadDaemon";
   public static final String PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD = "org.quartz.scheduler.threadsInheritContextClassLoaderOfInitializer";
   public static final String PROP_SCHED_CLASS_LOAD_HELPER_CLASS = "org.quartz.scheduler.classLoadHelper.class";
   public static final String PROP_SCHED_JOB_FACTORY_CLASS = "org.quartz.scheduler.jobFactory.class";
   public static final String PROP_SCHED_JOB_FACTORY_PREFIX = "org.quartz.scheduler.jobFactory";
   public static final String PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN = "org.quartz.scheduler.interruptJobsOnShutdown";
   public static final String PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN_WITH_WAIT = "org.quartz.scheduler.interruptJobsOnShutdownWithWait";
   public static final String PROP_SCHED_CONTEXT_PREFIX = "org.quartz.context.key";
   public static final String PROP_THREAD_POOL_PREFIX = "org.quartz.threadPool";
   public static final String PROP_THREAD_POOL_CLASS = "org.quartz.threadPool.class";
   public static final String PROP_JOB_STORE_PREFIX = "org.quartz.jobStore";
   public static final String PROP_JOB_STORE_LOCK_HANDLER_PREFIX = "org.quartz.jobStore.lockHandler";
   public static final String PROP_JOB_STORE_LOCK_HANDLER_CLASS = "org.quartz.jobStore.lockHandler.class";
   public static final String PROP_TABLE_PREFIX = "tablePrefix";
   public static final String PROP_SCHED_NAME = "schedName";
   public static final String PROP_JOB_STORE_CLASS = "org.quartz.jobStore.class";
   public static final String PROP_JOB_STORE_USE_PROP = "org.quartz.jobStore.useProperties";
   public static final String PROP_DATASOURCE_PREFIX = "org.quartz.dataSource";
   public static final String PROP_CONNECTION_PROVIDER_CLASS = "connectionProvider.class";
   @Deprecated
   public static final String PROP_DATASOURCE_DRIVER = "driver";
   @Deprecated
   public static final String PROP_DATASOURCE_URL = "URL";
   @Deprecated
   public static final String PROP_DATASOURCE_USER = "user";
   @Deprecated
   public static final String PROP_DATASOURCE_PASSWORD = "password";
   @Deprecated
   public static final String PROP_DATASOURCE_MAX_CONNECTIONS = "maxConnections";
   @Deprecated
   public static final String PROP_DATASOURCE_VALIDATION_QUERY = "validationQuery";
   public static final String PROP_DATASOURCE_JNDI_URL = "jndiURL";
   public static final String PROP_DATASOURCE_JNDI_ALWAYS_LOOKUP = "jndiAlwaysLookup";
   public static final String PROP_DATASOURCE_JNDI_INITIAL = "java.naming.factory.initial";
   public static final String PROP_DATASOURCE_JNDI_PROVDER = "java.naming.provider.url";
   public static final String PROP_DATASOURCE_JNDI_PRINCIPAL = "java.naming.security.principal";
   public static final String PROP_DATASOURCE_JNDI_CREDENTIALS = "java.naming.security.credentials";
   public static final String PROP_PLUGIN_PREFIX = "org.quartz.plugin";
   public static final String PROP_PLUGIN_CLASS = "class";
   public static final String PROP_JOB_LISTENER_PREFIX = "org.quartz.jobListener";
   public static final String PROP_TRIGGER_LISTENER_PREFIX = "org.quartz.triggerListener";
   public static final String PROP_LISTENER_CLASS = "class";
   public static final String DEFAULT_INSTANCE_ID = "NON_CLUSTERED";
   public static final String AUTO_GENERATE_INSTANCE_ID = "AUTO";
   public static final String PROP_THREAD_EXECUTOR = "org.quartz.threadExecutor";
   public static final String PROP_THREAD_EXECUTOR_CLASS = "org.quartz.threadExecutor.class";
   public static final String SYSTEM_PROPERTY_AS_INSTANCE_ID = "SYS_PROP";
   public static final String MANAGEMENT_REST_SERVICE_ENABLED = "org.quartz.managementRESTService.enabled";
   public static final String MANAGEMENT_REST_SERVICE_HOST_PORT = "org.quartz.managementRESTService.bind";
   private SchedulerException initException = null;
   private String propSrc = null;
   private PropertiesParser cfg;
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public StdSchedulerFactory() {
   }

   public StdSchedulerFactory(Properties var1) {
      this.initialize(var1);
   }

   public StdSchedulerFactory(String var1) {
      this.initialize(var1);
   }

   public Logger getLog() {
      return this.log;
   }

   public void initialize() throws SchedulerConfigException {
      if (this.cfg == null) {
         if (this.initException != null) {
            throw this.initException;
         } else {
            String var1 = System.getProperty("org.quartz.properties");
            String var2 = var1 != null ? var1 : "quartz.properties";
            File var3 = new File(var2);
            Properties var4 = new Properties();
            InputStream var5 = null;

            try {
               if (var3.exists()) {
                  try {
                     if (var1 != null) {
                        this.propSrc = "specified file: '" + var1 + "'";
                     } else {
                        this.propSrc = "default file in current working dir: 'quartz.properties'";
                     }

                     var5 = new BufferedInputStream(new FileInputStream(var2));
                     var4.load((InputStream)var5);
                  } catch (IOException var19) {
                     this.initException = new SchedulerException("Properties file: '" + var2 + "' could not be read.", var19);
                     throw this.initException;
                  }
               } else if (var1 != null) {
                  var5 = Thread.currentThread().getContextClassLoader().getResourceAsStream(var1);
                  if (var5 == null) {
                     this.initException = new SchedulerException("Properties file: '" + var1 + "' could not be found.");
                     throw this.initException;
                  }

                  this.propSrc = "specified file: '" + var1 + "' in the class resource path.";
                  var5 = new BufferedInputStream((InputStream)var5);

                  try {
                     var4.load((InputStream)var5);
                  } catch (IOException var18) {
                     this.initException = new SchedulerException("Properties file: '" + var1 + "' could not be read.", var18);
                     throw this.initException;
                  }
               } else {
                  this.propSrc = "default resource file in Quartz package: 'quartz.properties'";
                  ClassLoader var6 = this.getClass().getClassLoader();
                  if (var6 == null) {
                     var6 = this.findClassloader();
                  }

                  if (var6 == null) {
                     throw new SchedulerConfigException("Unable to find a class loader on the current thread or class.");
                  }

                  var5 = var6.getResourceAsStream("quartz.properties");
                  if (var5 == null) {
                     var5 = var6.getResourceAsStream("/quartz.properties");
                  }

                  if (var5 == null) {
                     var5 = var6.getResourceAsStream("org/quartz/quartz.properties");
                  }

                  if (var5 == null) {
                     this.initException = new SchedulerException("Default quartz.properties not found in class path");
                     throw this.initException;
                  }

                  try {
                     var4.load((InputStream)var5);
                  } catch (IOException var17) {
                     this.initException = new SchedulerException(
                        "Resource properties file: 'org/quartz/quartz.properties' could not be read from the classpath.", var17
                     );
                     throw this.initException;
                  }
               }
            } finally {
               if (var5 != null) {
                  try {
                     var5.close();
                  } catch (IOException var16) {
                  }
               }
            }

            this.initialize(this.overrideWithSysProps(var4));
         }
      }
   }

   private Properties overrideWithSysProps(Properties var1) {
      Properties var2 = null;

      try {
         var2 = System.getProperties();
      } catch (AccessControlException var4) {
         this.getLog()
            .warn(
               "Skipping overriding quartz properties with System properties during initialization because of an AccessControlException.  This is likely due to not having read/write access for java.util.PropertyPermission as required by java.lang.System.getProperties().  To resolve this warning, either add this permission to your policy file or use a non-default version of initialize().",
               var4
            );
      }

      if (var2 != null) {
         var1.putAll(var2);
      }

      return var1;
   }

   public void initialize(String var1) {
      if (this.cfg == null) {
         if (this.initException != null) {
            throw this.initException;
         } else {
            InputStream var2 = null;
            Properties var3 = new Properties();
            var2 = Thread.currentThread().getContextClassLoader().getResourceAsStream(var1);

            try {
               if (var2 != null) {
                  var2 = new BufferedInputStream((InputStream)var2);
                  this.propSrc = "the specified file : '" + var1 + "' from the class resource path.";
               } else {
                  var2 = new BufferedInputStream(new FileInputStream(var1));
                  this.propSrc = "the specified file : '" + var1 + "'";
               }

               var3.load((InputStream)var2);
            } catch (IOException var12) {
               this.initException = new SchedulerException("Properties file: '" + var1 + "' could not be read.", var12);
               throw this.initException;
            } finally {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (IOException var11) {
                  }
               }
            }

            this.initialize(var3);
         }
      }
   }

   public void initialize(InputStream var1) {
      if (this.cfg == null) {
         if (this.initException != null) {
            throw this.initException;
         } else {
            Properties var2 = new Properties();
            if (var1 != null) {
               try {
                  var2.load(var1);
                  this.propSrc = "an externally opened InputStream.";
               } catch (IOException var4) {
                  this.initException = new SchedulerException("Error loading property data from InputStream", var4);
                  throw this.initException;
               }

               this.initialize(var2);
            } else {
               this.initException = new SchedulerException("Error loading property data from InputStream - InputStream is null.");
               throw this.initException;
            }
         }
      }
   }

   public void initialize(Properties var1) {
      if (this.propSrc == null) {
         this.propSrc = "an externally provided properties instance.";
      }

      this.cfg = new PropertiesParser(var1);
   }

   private Scheduler instantiate() throws SchedulerException, SchedulerConfigException {
      if (this.cfg == null) {
         this.initialize();
      }

      if (this.initException != null) {
         throw this.initException;
      } else {
         JobStore var1 = null;
         ThreadPool var2 = null;
         QuartzScheduler var3 = null;
         DBConnectionManager var4 = null;
         String var5 = null;
         Properties var6 = null;
         String var7 = null;
         boolean var8 = false;
         boolean var9 = false;
         long var10 = -1L;
         long var12 = 15000L;
         SchedulerRepository var17 = SchedulerRepository.getInstance();
         String var18 = this.cfg.getStringProperty("org.quartz.scheduler.instanceName", "QuartzScheduler");
         String var19 = this.cfg.getStringProperty("org.quartz.scheduler.threadName", var18 + "_QuartzSchedulerThread");
         String var20 = this.cfg.getStringProperty("org.quartz.scheduler.instanceId", "NON_CLUSTERED");
         if (var20.equals("AUTO")) {
            var9 = true;
            var5 = this.cfg.getStringProperty("org.quartz.scheduler.instanceIdGenerator.class", "org.quartz.simpl.SimpleInstanceIdGenerator");
         } else if (var20.equals("SYS_PROP")) {
            var9 = true;
            var5 = "org.quartz.simpl.SystemPropertyInstanceIdGenerator";
         }

         var7 = this.cfg.getStringProperty("org.quartz.scheduler.userTransactionURL", (String)var7);
         if (var7 != null && var7.trim().length() == 0) {
            var7 = null;
         }

         String var14 = this.cfg.getStringProperty("org.quartz.scheduler.classLoadHelper.class", "org.quartz.simpl.CascadingClassLoadHelper");
         var8 = this.cfg.getBooleanProperty("org.quartz.scheduler.wrapJobExecutionInUserTransaction", var8);
         String var15 = this.cfg.getStringProperty("org.quartz.scheduler.jobFactory.class", null);
         var10 = this.cfg.getLongProperty("org.quartz.scheduler.idleWaitTime", var10);
         if (var10 > -1L && var10 < 1000L) {
            throw new SchedulerException("org.quartz.scheduler.idleWaitTime of less than 1000ms is not legal.");
         } else {
            var12 = this.cfg.getLongProperty("org.quartz.scheduler.dbFailureRetryInterval", var12);
            if (var12 < 0L) {
               throw new SchedulerException("org.quartz.scheduler.dbFailureRetryInterval of less than 0 ms is not legal.");
            } else {
               boolean var21 = this.cfg.getBooleanProperty("org.quartz.scheduler.makeSchedulerThreadDaemon");
               boolean var22 = this.cfg.getBooleanProperty("org.quartz.scheduler.threadsInheritContextClassLoaderOfInitializer");
               long var23 = this.cfg.getLongProperty("org.quartz.scheduler.batchTriggerAcquisitionFireAheadTimeWindow", 0L);
               int var25 = this.cfg.getIntProperty("org.quartz.scheduler.batchTriggerAcquisitionMaxCount", 1);
               boolean var26 = this.cfg.getBooleanProperty("org.quartz.scheduler.interruptJobsOnShutdown", false);
               boolean var27 = this.cfg.getBooleanProperty("org.quartz.scheduler.interruptJobsOnShutdownWithWait", false);
               boolean var28 = this.cfg.getBooleanProperty("org.quartz.scheduler.jmx.export");
               String var29 = this.cfg.getStringProperty("org.quartz.scheduler.jmx.objectName");
               boolean var30 = this.cfg.getBooleanProperty("org.quartz.scheduler.jmx.proxy");
               String var31 = this.cfg.getStringProperty("org.quartz.scheduler.jmx.proxy.class");
               boolean var32 = this.cfg.getBooleanProperty("org.quartz.scheduler.rmi.export", false);
               boolean var33 = this.cfg.getBooleanProperty("org.quartz.scheduler.rmi.proxy", false);
               String var34 = this.cfg.getStringProperty("org.quartz.scheduler.rmi.registryHost", "localhost");
               int var35 = this.cfg.getIntProperty("org.quartz.scheduler.rmi.registryPort", 1099);
               int var36 = this.cfg.getIntProperty("org.quartz.scheduler.rmi.serverPort", -1);
               String var37 = this.cfg.getStringProperty("org.quartz.scheduler.rmi.createRegistry", "never");
               String var38 = this.cfg.getStringProperty("org.quartz.scheduler.rmi.bindName");
               if (var30 && var33) {
                  throw new SchedulerConfigException("Cannot proxy both RMI and JMX.");
               } else {
                  boolean var39 = this.cfg.getBooleanProperty("org.quartz.managementRESTService.enabled", false);
                  String var40 = this.cfg.getStringProperty("org.quartz.managementRESTService.bind", "0.0.0.0:9889");
                  Properties var41 = this.cfg.getPropertyGroup("org.quartz.context.key", true);
                  if (var33) {
                     if (var9) {
                        var20 = "NON_CLUSTERED";
                     }

                     String var109 = var38 == null ? QuartzSchedulerResources.getUniqueIdentifier(var18, var20) : var38;
                     RemoteScheduler var112 = new RemoteScheduler(var109, var34, var35);
                     var17.bind(var112);
                     return var112;
                  } else {
                     ClassLoadHelper var42 = null;

                     try {
                        var42 = (ClassLoadHelper)this.loadClass(var14).newInstance();
                     } catch (Exception var89) {
                        throw new SchedulerConfigException("Unable to instantiate class load helper class: " + var89.getMessage(), var89);
                     }

                     var42.initialize();
                     if (var30) {
                        if (var9) {
                           var20 = "NON_CLUSTERED";
                        }

                        if (var31 == null) {
                           throw new SchedulerConfigException("No JMX Proxy Scheduler class provided");
                        } else {
                           RemoteMBeanScheduler var110 = null;

                           try {
                              var110 = (RemoteMBeanScheduler)var42.loadClass(var31).newInstance();
                           } catch (Exception var65) {
                              throw new SchedulerConfigException("Unable to instantiate RemoteMBeanScheduler class.", var65);
                           }

                           if (var29 == null) {
                              var29 = QuartzSchedulerResources.generateJMXObjectName(var18, var20);
                           }

                           var110.setSchedulerObjectName(var29);
                           var6 = this.cfg.getPropertyGroup("org.quartz.scheduler.jmx.proxy", true);

                           try {
                              this.setBeanProps(var110, (Properties)var6);
                           } catch (Exception var64) {
                              this.initException = new SchedulerException("RemoteMBeanScheduler class '" + var31 + "' props could not be configured.", var64);
                              throw this.initException;
                           }

                           var110.initialize();
                           var17.bind(var110);
                           return var110;
                        }
                     } else {
                        JobFactory var43 = null;
                        if (var15 != null) {
                           try {
                              var43 = (JobFactory)var42.loadClass(var15).newInstance();
                           } catch (Exception var88) {
                              throw new SchedulerConfigException("Unable to instantiate JobFactory class: " + var88.getMessage(), var88);
                           }

                           var6 = this.cfg.getPropertyGroup("org.quartz.scheduler.jobFactory", true);

                           try {
                              this.setBeanProps(var43, (Properties)var6);
                           } catch (Exception var87) {
                              this.initException = new SchedulerException("JobFactory class '" + var15 + "' props could not be configured.", var87);
                              throw this.initException;
                           }
                        }

                        InstanceIdGenerator var44 = null;
                        if (var5 != null) {
                           try {
                              var44 = (InstanceIdGenerator)var42.loadClass(var5).newInstance();
                           } catch (Exception var86) {
                              throw new SchedulerConfigException("Unable to instantiate InstanceIdGenerator class: " + var86.getMessage(), var86);
                           }

                           var6 = this.cfg.getPropertyGroup("org.quartz.scheduler.instanceIdGenerator", true);

                           try {
                              this.setBeanProps(var44, (Properties)var6);
                           } catch (Exception var85) {
                              this.initException = new SchedulerException("InstanceIdGenerator class '" + var5 + "' props could not be configured.", var85);
                              throw this.initException;
                           }
                        }

                        String var45 = this.cfg.getStringProperty("org.quartz.threadPool.class", SimpleThreadPool.class.getName());
                        if (var45 == null) {
                           this.initException = new SchedulerException("ThreadPool class not specified. ");
                           throw this.initException;
                        } else {
                           try {
                              var2 = (ThreadPool)var42.loadClass(var45).newInstance();
                           } catch (Exception var84) {
                              this.initException = new SchedulerException("ThreadPool class '" + var45 + "' could not be instantiated.", var84);
                              throw this.initException;
                           }

                           var6 = this.cfg.getPropertyGroup("org.quartz.threadPool", true);

                           try {
                              this.setBeanProps(var2, (Properties)var6);
                           } catch (Exception var83) {
                              this.initException = new SchedulerException("ThreadPool class '" + var45 + "' props could not be configured.", var83);
                              throw this.initException;
                           }

                           String var46 = this.cfg.getStringProperty("org.quartz.jobStore.class", RAMJobStore.class.getName());
                           if (var46 == null) {
                              this.initException = new SchedulerException("JobStore class not specified. ");
                              throw this.initException;
                           } else {
                              try {
                                 var1 = (JobStore)var42.loadClass(var46).newInstance();
                              } catch (Exception var82) {
                                 this.initException = new SchedulerException("JobStore class '" + var46 + "' could not be instantiated.", var82);
                                 throw this.initException;
                              }

                              SchedulerDetailsSetter.setDetails(var1, var18, var20);
                              var6 = this.cfg.getPropertyGroup("org.quartz.jobStore", true, new String[]{"org.quartz.jobStore.lockHandler"});

                              try {
                                 this.setBeanProps(var1, (Properties)var6);
                              } catch (Exception var81) {
                                 this.initException = new SchedulerException("JobStore class '" + var46 + "' props could not be configured.", var81);
                                 throw this.initException;
                              }

                              if (var1 instanceof JobStoreSupport) {
                                 String var47 = this.cfg.getStringProperty("org.quartz.jobStore.lockHandler.class");
                                 if (var47 != null) {
                                    try {
                                       Semaphore var48 = (Semaphore)var42.loadClass(var47).newInstance();
                                       var6 = this.cfg.getPropertyGroup("org.quartz.jobStore.lockHandler", true);
                                       if (var48 instanceof TablePrefixAware) {
                                          var6.setProperty("tablePrefix", ((JobStoreSupport)var1).getTablePrefix());
                                          var6.setProperty("schedName", var18);
                                       }

                                       try {
                                          this.setBeanProps(var48, var6);
                                       } catch (Exception var79) {
                                          this.initException = new SchedulerException(
                                             "JobStore LockHandler class '" + var47 + "' props could not be configured.", var79
                                          );
                                          throw this.initException;
                                       }

                                       ((JobStoreSupport)var1).setLockHandler(var48);
                                       this.getLog().info("Using custom data access locking (synchronization): " + var47);
                                    } catch (Exception var80) {
                                       this.initException = new SchedulerException(
                                          "JobStore LockHandler class '" + var47 + "' could not be instantiated.", var80
                                       );
                                       throw this.initException;
                                    }
                                 }
                              }

                              String[] var113 = this.cfg.getPropertyGroups("org.quartz.dataSource");

                              for (int var114 = 0; var114 < var113.length; var114++) {
                                 PropertiesParser var49 = new PropertiesParser(this.cfg.getPropertyGroup("org.quartz.dataSource." + var113[var114], true));
                                 String var50 = var49.getStringProperty("connectionProvider.class", null);
                                 if (var50 != null) {
                                    ConnectionProvider var51 = null;

                                    try {
                                       var51 = (ConnectionProvider)var42.loadClass(var50).newInstance();
                                    } catch (Exception var78) {
                                       this.initException = new SchedulerException("ConnectionProvider class '" + var50 + "' could not be instantiated.", var78);
                                       throw this.initException;
                                    }

                                    try {
                                       var49.getUnderlyingProperties().remove("connectionProvider.class");
                                       if (var51 instanceof PoolingConnectionProvider) {
                                          this.populateProviderWithExtraProps((PoolingConnectionProvider)var51, var49.getUnderlyingProperties());
                                       } else {
                                          this.setBeanProps(var51, var49.getUnderlyingProperties());
                                       }

                                       var51.initialize();
                                    } catch (Exception var77) {
                                       this.initException = new SchedulerException(
                                          "ConnectionProvider class '" + var50 + "' props could not be configured.", var77
                                       );
                                       throw this.initException;
                                    }

                                    var4 = DBConnectionManager.getInstance();
                                    var4.addConnectionProvider(var113[var114], var51);
                                 } else {
                                    String var121 = var49.getStringProperty("jndiURL", null);
                                    if (var121 != null) {
                                       boolean var52 = var49.getBooleanProperty("jndiAlwaysLookup");
                                       String var53 = var49.getStringProperty("java.naming.factory.initial");
                                       String var54 = var49.getStringProperty("java.naming.provider.url");
                                       String var55 = var49.getStringProperty("java.naming.security.principal");
                                       String var56 = var49.getStringProperty("java.naming.security.credentials");
                                       Properties var57 = null;
                                       if (null != var53 || null != var54 || null != var55 || null != var56) {
                                          var57 = new Properties();
                                          if (var53 != null) {
                                             var57.put("java.naming.factory.initial", var53);
                                          }

                                          if (var54 != null) {
                                             var57.put("java.naming.provider.url", var54);
                                          }

                                          if (var55 != null) {
                                             var57.put("java.naming.security.principal", var55);
                                          }

                                          if (var56 != null) {
                                             var57.put("java.naming.security.credentials", var56);
                                          }
                                       }

                                       JNDIConnectionProvider var58 = new JNDIConnectionProvider(var121, var57, var52);
                                       var4 = DBConnectionManager.getInstance();
                                       var4.addConnectionProvider(var113[var114], var58);
                                    } else {
                                       String var124 = var49.getStringProperty("provider");
                                       String var127 = var49.getStringProperty("driver");
                                       String var132 = var49.getStringProperty("URL");
                                       if (var127 == null) {
                                          this.initException = new SchedulerException("Driver not specified for DataSource: " + var113[var114]);
                                          throw this.initException;
                                       }

                                       if (var132 == null) {
                                          this.initException = new SchedulerException("DB URL not specified for DataSource: " + var113[var114]);
                                          throw this.initException;
                                       }

                                       if (var124 != null && var124.equals("hikaricp")) {
                                          var50 = "org.quartz.utils.HikariCpPoolingConnectionProvider";
                                       } else {
                                          var50 = "org.quartz.utils.C3p0PoolingConnectionProvider";
                                       }

                                       this.log.info("Using ConnectionProvider class '" + var50 + "' for data source '" + var113[var114] + "'");

                                       try {
                                          Object var135 = null;

                                          try {
                                             Constructor var140 = var42.loadClass(var50).getConstructor(Properties.class);
                                             var135 = (ConnectionProvider)var140.newInstance(var49.getUnderlyingProperties());
                                          } catch (Exception var75) {
                                             this.initException = new SchedulerException(
                                                "ConnectionProvider class '" + var50 + "' could not be instantiated.", var75
                                             );
                                             throw this.initException;
                                          }

                                          var4 = DBConnectionManager.getInstance();
                                          var4.addConnectionProvider(var113[var114], (ConnectionProvider)var135);
                                          this.populateProviderWithExtraProps((PoolingConnectionProvider)var135, var49.getUnderlyingProperties());
                                       } catch (Exception var76) {
                                          this.initException = new SchedulerException("Could not initialize DataSource: " + var113[var114], var76);
                                          throw this.initException;
                                       }
                                    }
                                 }
                              }

                              String[] var115 = this.cfg.getPropertyGroups("org.quartz.plugin");
                              SchedulerPlugin[] var116 = new SchedulerPlugin[var115.length];

                              for (int var118 = 0; var118 < var115.length; var118++) {
                                 Properties var122 = this.cfg.getPropertyGroup("org.quartz.plugin." + var115[var118], true);
                                 String var125 = var122.getProperty("class", null);
                                 if (var125 == null) {
                                    this.initException = new SchedulerException("SchedulerPlugin class not specified for plugin '" + var115[var118] + "'");
                                    throw this.initException;
                                 }

                                 Object var128 = null;

                                 try {
                                    var128 = (SchedulerPlugin)var42.loadClass(var125).newInstance();
                                 } catch (Exception var74) {
                                    this.initException = new SchedulerException("SchedulerPlugin class '" + var125 + "' could not be instantiated.", var74);
                                    throw this.initException;
                                 }

                                 try {
                                    this.setBeanProps(var128, var122);
                                 } catch (Exception var73) {
                                    this.initException = new SchedulerException(
                                       "JobStore SchedulerPlugin '" + var125 + "' props could not be configured.", var73
                                    );
                                    throw this.initException;
                                 }

                                 var116[var118] = (SchedulerPlugin)var128;
                              }

                              Class[] var119 = new Class[]{String.class};
                              String[] var123 = this.cfg.getPropertyGroups("org.quartz.jobListener");
                              JobListener[] var126 = new JobListener[var123.length];

                              for (int var130 = 0; var130 < var123.length; var130++) {
                                 Properties var133 = this.cfg.getPropertyGroup("org.quartz.jobListener." + var123[var130], true);
                                 String var137 = var133.getProperty("class", null);
                                 if (var137 == null) {
                                    this.initException = new SchedulerException("JobListener class not specified for listener '" + var123[var130] + "'");
                                    throw this.initException;
                                 }

                                 Object var141 = null;

                                 try {
                                    var141 = (JobListener)var42.loadClass(var137).newInstance();
                                 } catch (Exception var72) {
                                    this.initException = new SchedulerException("JobListener class '" + var137 + "' could not be instantiated.", var72);
                                    throw this.initException;
                                 }

                                 try {
                                    Method var145 = null;

                                    try {
                                       var145 = var141.getClass().getMethod("setName", var119);
                                    } catch (NoSuchMethodException var71) {
                                    }

                                    if (var145 != null) {
                                       var145.invoke(var141, var123[var130]);
                                    }

                                    this.setBeanProps(var141, var133);
                                 } catch (Exception var94) {
                                    this.initException = new SchedulerException("JobListener '" + var137 + "' props could not be configured.", var94);
                                    throw this.initException;
                                 }

                                 var126[var130] = (JobListener)var141;
                              }

                              String[] var131 = this.cfg.getPropertyGroups("org.quartz.triggerListener");
                              TriggerListener[] var134 = new TriggerListener[var131.length];

                              for (int var138 = 0; var138 < var131.length; var138++) {
                                 Properties var143 = this.cfg.getPropertyGroup("org.quartz.triggerListener." + var131[var138], true);
                                 String var146 = var143.getProperty("class", null);
                                 if (var146 == null) {
                                    this.initException = new SchedulerException("TriggerListener class not specified for listener '" + var131[var138] + "'");
                                    throw this.initException;
                                 }

                                 Object var148 = null;

                                 try {
                                    var148 = (TriggerListener)var42.loadClass(var146).newInstance();
                                 } catch (Exception var70) {
                                    this.initException = new SchedulerException("TriggerListener class '" + var146 + "' could not be instantiated.", var70);
                                    throw this.initException;
                                 }

                                 try {
                                    Method var59 = null;

                                    try {
                                       var59 = var148.getClass().getMethod("setName", var119);
                                    } catch (NoSuchMethodException var69) {
                                    }

                                    if (var59 != null) {
                                       var59.invoke(var148, var131[var138]);
                                    }

                                    this.setBeanProps(var148, var143);
                                 } catch (Exception var93) {
                                    this.initException = new SchedulerException("TriggerListener '" + var146 + "' props could not be configured.", var93);
                                    throw this.initException;
                                 }

                                 var134[var138] = (TriggerListener)var148;
                              }

                              boolean var139 = false;
                              boolean var144 = false;
                              String var147 = this.cfg.getStringProperty("org.quartz.threadExecutor.class");
                              ThreadExecutor var16;
                              if (var147 != null) {
                                 var6 = this.cfg.getPropertyGroup("org.quartz.threadExecutor", true);

                                 try {
                                    var16 = (ThreadExecutor)var42.loadClass(var147).newInstance();
                                    this.log.info("Using custom implementation for ThreadExecutor: " + var147);
                                    this.setBeanProps(var16, var6);
                                 } catch (Exception var68) {
                                    this.initException = new SchedulerException("ThreadExecutor class '" + var147 + "' could not be instantiated.", var68);
                                    throw this.initException;
                                 }
                              } else {
                                 this.log.info("Using default implementation for ThreadExecutor");
                                 var16 = new DefaultThreadExecutor();
                              }

                              try {
                                 JobRunShellFactory var150 = null;
                                 if (var7 != null) {
                                    UserTransactionHelper.setUserTxLocation((String)var7);
                                 }

                                 if (var8) {
                                    var150 = new JTAJobRunShellFactory();
                                 } else {
                                    var150 = new JTAAnnotationAwareJobRunShellFactory();
                                 }

                                 if (var9) {
                                    try {
                                       var20 = "NON_CLUSTERED";
                                       if (var1.isClustered()) {
                                          var20 = var44.generateInstanceId();
                                       }
                                    } catch (Exception var67) {
                                       this.getLog().error("Couldn't generate instance Id!", var67);
                                       throw new IllegalStateException("Cannot run without an instance id.");
                                    }
                                 }

                                 if (var1.getClass().getName().startsWith("org.terracotta.quartz")) {
                                    try {
                                       String var152 = (String)var1.getClass().getMethod("getUUID").invoke(var1);
                                       if (var20.equals("NON_CLUSTERED")) {
                                          var20 = "TERRACOTTA_CLUSTERED,node=" + var152;
                                          if (var29 == null) {
                                             var29 = QuartzSchedulerResources.generateJMXObjectName(var18, var20);
                                          }
                                       } else if (var29 == null) {
                                          var29 = QuartzSchedulerResources.generateJMXObjectName(var18, var20 + ",node=" + var152);
                                       }
                                    } catch (Exception var66) {
                                       throw new RuntimeException("Problem obtaining node id from TerracottaJobStore.", var66);
                                    }

                                    if (null == this.cfg.getStringProperty("org.quartz.scheduler.jmx.export")) {
                                       var28 = true;
                                    }
                                 }

                                 if (var1 instanceof JobStoreSupport) {
                                    JobStoreSupport var153 = (JobStoreSupport)var1;
                                    var153.setDbRetryInterval(var12);
                                    if (var22) {
                                       var153.setThreadsInheritInitializersClassLoadContext(var22);
                                    }

                                    var153.setThreadExecutor((ThreadExecutor)var16);
                                 }

                                 QuartzSchedulerResources var154 = new QuartzSchedulerResources();
                                 var154.setName(var18);
                                 var154.setThreadName(var19);
                                 var154.setInstanceId(var20);
                                 var154.setJobRunShellFactory((JobRunShellFactory)var150);
                                 var154.setMakeSchedulerThreadDaemon(var21);
                                 var154.setThreadsInheritInitializersClassLoadContext(var22);
                                 var154.setBatchTimeWindow(var23);
                                 var154.setMaxBatchSize(var25);
                                 var154.setInterruptJobsOnShutdown(var26);
                                 var154.setInterruptJobsOnShutdownWithWait(var27);
                                 var154.setJMXExport(var28);
                                 var154.setJMXObjectName(var29);
                                 if (var39) {
                                    ManagementRESTServiceConfiguration var60 = new ManagementRESTServiceConfiguration();
                                    var60.setBind(var40);
                                    var60.setEnabled(var39);
                                    var154.setManagementRESTServiceConfiguration(var60);
                                 }

                                 if (var32) {
                                    var154.setRMIRegistryHost(var34);
                                    var154.setRMIRegistryPort(var35);
                                    var154.setRMIServerPort(var36);
                                    var154.setRMICreateRegistryStrategy(var37);
                                    var154.setRMIBindName(var38);
                                 }

                                 SchedulerDetailsSetter.setDetails(var2, var18, var20);
                                 var154.setThreadExecutor((ThreadExecutor)var16);
                                 var16.initialize();
                                 var154.setThreadPool(var2);
                                 if (var2 instanceof SimpleThreadPool && var22) {
                                    ((SimpleThreadPool)var2).setThreadsInheritContextClassLoaderOfInitializingThread(var22);
                                 }

                                 var2.initialize();
                                 var139 = true;
                                 var154.setJobStore(var1);

                                 for (int var155 = 0; var155 < var116.length; var155++) {
                                    var154.addSchedulerPlugin(var116[var155]);
                                 }

                                 var3 = new QuartzScheduler(var154, var10, var12);
                                 var144 = true;
                                 Scheduler var156 = this.instantiate(var154, var3);
                                 if (var43 != null) {
                                    var3.setJobFactory(var43);
                                 }

                                 for (int var61 = 0; var61 < var116.length; var61++) {
                                    var116[var61].initialize(var115[var61], var156, var42);
                                 }

                                 for (int var157 = 0; var157 < var126.length; var157++) {
                                    var3.getListenerManager().addJobListener(var126[var157], EverythingMatcher.allJobs());
                                 }

                                 for (int var158 = 0; var158 < var134.length; var158++) {
                                    var3.getListenerManager().addTriggerListener(var134[var158], EverythingMatcher.allTriggers());
                                 }

                                 for (Object var62 : var41.keySet()) {
                                    String var63 = var41.getProperty((String)var62);
                                    var156.getContext().put((String)var62, var63);
                                 }

                                 var1.setInstanceId(var20);
                                 var1.setInstanceName(var18);
                                 var1.setThreadPoolSize(var2.getPoolSize());
                                 var1.initialize(var42, var3.getSchedulerSignaler());
                                 var150.initialize(var156);
                                 var3.initialize();
                                 this.getLog().info("Quartz scheduler '" + var156.getSchedulerName() + "' initialized from " + this.propSrc);
                                 this.getLog().info("Quartz scheduler version: " + var3.getVersion());
                                 var3.addNoGCObject(var17);
                                 if (var4 != null) {
                                    var3.addNoGCObject(var4);
                                 }

                                 var17.bind(var156);
                                 return var156;
                              } catch (SchedulerException var90) {
                                 this.shutdownFromInstantiateException(var2, var3, var139, var144);
                                 throw var90;
                              } catch (RuntimeException var91) {
                                 this.shutdownFromInstantiateException(var2, var3, var139, var144);
                                 throw var91;
                              } catch (Error var92) {
                                 this.shutdownFromInstantiateException(var2, var3, var139, var144);
                                 throw var92;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void populateProviderWithExtraProps(PoolingConnectionProvider var1, Properties var2) {
      Properties var3 = new Properties();
      var3.putAll(var2);
      var3.remove("driver");
      var3.remove("URL");
      var3.remove("user");
      var3.remove("password");
      var3.remove("maxConnections");
      var3.remove("validationQuery");
      var2.remove("provider");
      this.setBeanProps(var1.getDataSource(), var3);
   }

   private void shutdownFromInstantiateException(ThreadPool var1, QuartzScheduler var2, boolean var3, boolean var4) {
      try {
         if (var4) {
            var2.shutdown(false);
         } else if (var3) {
            var1.shutdown(false);
         }
      } catch (Exception var6) {
         this.getLog().error("Got another exception while shutting down after instantiation exception", var6);
      }
   }

   protected Scheduler instantiate(QuartzSchedulerResources var1, QuartzScheduler var2) {
      return new StdScheduler(var2);
   }

   private void setBeanProps(Object var1, Properties var2) {
      var2.remove("class");
      var2.remove("provider");
      BeanInfo var3 = Introspector.getBeanInfo(var1.getClass());
      PropertyDescriptor[] var4 = var3.getPropertyDescriptors();
      PropertiesParser var5 = new PropertiesParser(var2);
      Enumeration var6 = var2.keys();

      while (var6.hasMoreElements()) {
         String var7 = (String)var6.nextElement();
         String var8 = var7.substring(0, 1).toUpperCase(Locale.US);
         String var9 = "set" + var8 + var7.substring(1);
         Method var10 = this.getSetMethod(var9, var4);

         try {
            if (var10 == null) {
               throw new NoSuchMethodException("No setter for property '" + var7 + "'");
            }

            Class[] var11 = var10.getParameterTypes();
            if (var11.length != 1) {
               throw new NoSuchMethodException("No 1-argument setter for property '" + var7 + "'");
            }

            PropertiesParser var12 = var5;
            String var13 = var5.getStringProperty(var7);
            if (var13 != null && var13.startsWith("$@")) {
               var13 = var13.substring(2);
               var12 = this.cfg;
            } else {
               var13 = var7;
            }

            if (var11[0].equals(int.class)) {
               var10.invoke(var1, var12.getIntProperty(var13));
            } else if (var11[0].equals(long.class)) {
               var10.invoke(var1, var12.getLongProperty(var13));
            } else if (var11[0].equals(float.class)) {
               var10.invoke(var1, var12.getFloatProperty(var13));
            } else if (var11[0].equals(double.class)) {
               var10.invoke(var1, var12.getDoubleProperty(var13));
            } else if (var11[0].equals(boolean.class)) {
               var10.invoke(var1, var12.getBooleanProperty(var13));
            } else {
               if (!var11[0].equals(String.class)) {
                  throw new NoSuchMethodException("No primitive-type setter for property '" + var7 + "'");
               }

               var10.invoke(var1, var12.getStringProperty(var13));
            }
         } catch (NumberFormatException var14) {
            throw new SchedulerConfigException("Could not parse property '" + var7 + "' into correct data type: " + var14.toString());
         }
      }
   }

   private Method getSetMethod(String var1, PropertyDescriptor[] var2) {
      for (int var3 = 0; var3 < var2.length; var3++) {
         Method var4 = var2[var3].getWriteMethod();
         if (var4 != null && var4.getName().equals(var1)) {
            return var4;
         }
      }

      return null;
   }

   private Class<?> loadClass(String var1) {
      try {
         ClassLoader var2 = this.findClassloader();
         if (var2 != null) {
            return var2.loadClass(var1);
         } else {
            throw new SchedulerConfigException("Unable to find a class loader on the current thread or class.");
         }
      } catch (ClassNotFoundException var3) {
         if (this.getClass().getClassLoader() != null) {
            return this.getClass().getClassLoader().loadClass(var1);
         } else {
            throw var3;
         }
      }
   }

   private ClassLoader findClassloader() {
      if (Thread.currentThread().getContextClassLoader() == null && this.getClass().getClassLoader() != null) {
         Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
      }

      return Thread.currentThread().getContextClassLoader();
   }

   private String getSchedulerName() {
      return this.cfg.getStringProperty("org.quartz.scheduler.instanceName", "QuartzScheduler");
   }

   public Scheduler getScheduler() {
      if (this.cfg == null) {
         this.initialize();
      }

      SchedulerRepository var1 = SchedulerRepository.getInstance();
      Scheduler var2 = var1.lookup(this.getSchedulerName());
      if (var2 != null) {
         if (!var2.isShutdown()) {
            return var2;
         }

         var1.remove(this.getSchedulerName());
      }

      return this.instantiate();
   }

   public static Scheduler getDefaultScheduler() {
      StdSchedulerFactory var0 = new StdSchedulerFactory();
      return var0.getScheduler();
   }

   public Scheduler getScheduler(String var1) {
      return SchedulerRepository.getInstance().lookup(var1);
   }

   public Collection<Scheduler> getAllSchedulers() {
      return SchedulerRepository.getInstance().lookupAll();
   }
}
