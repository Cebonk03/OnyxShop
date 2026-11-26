package org.quartz.ee.jmx.jboss;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import org.jboss.naming.NonSerializableFactory;
import org.jboss.system.ServiceMBeanSupport;
import org.quartz.Scheduler;
import org.quartz.SchedulerConfigException;
import org.quartz.impl.StdSchedulerFactory;

public class QuartzService extends ServiceMBeanSupport implements QuartzServiceMBean {
   private Properties properties;
   private StdSchedulerFactory schedulerFactory;
   private String jndiName;
   private String propertiesFile;
   private boolean error;
   private boolean useProperties;
   private boolean usePropertiesFile;
   private boolean startScheduler = true;

   public QuartzService() {
      this.error = false;
      this.usePropertiesFile = false;
      this.propertiesFile = "";
      this.useProperties = false;
      this.properties = new Properties();
      this.jndiName = "Quartz";
   }

   @Override
   public void setJndiName(String var1) {
      String var2 = this.jndiName;
      this.jndiName = var1;
      if (super.getState() == 3) {
         this.unbind(var2);

         try {
            this.rebind();
         } catch (NamingException var4) {
            this.log.error("Failed to rebind Scheduler", var4);
            throw new SchedulerConfigException("Failed to rebind Scheduler - ", var4);
         }
      }
   }

   @Override
   public String getJndiName() {
      return this.jndiName;
   }

   public String getName() {
      return "QuartzService(" + this.jndiName + ")";
   }

   @Override
   public void setProperties(String var1) {
      if (this.usePropertiesFile) {
         this.log.error("Must specify only one of 'Properties' or 'PropertiesFile'");
         this.error = true;
      } else {
         this.useProperties = true;

         try {
            var1 = var1.replace(File.separator, "/");
            ByteArrayInputStream var2 = new ByteArrayInputStream(var1.getBytes());
            this.properties = new Properties();
            this.properties.load(var2);
         } catch (IOException var3) {
         }
      }
   }

   public String getProperties() {
      try {
         ByteArrayOutputStream var1 = new ByteArrayOutputStream();
         this.properties.store(var1, "");
         return new String(var1.toByteArray());
      } catch (IOException var2) {
         return "";
      }
   }

   @Override
   public void setPropertiesFile(String var1) {
      if (this.useProperties) {
         this.log.error("Must specify only one of 'Properties' or 'PropertiesFile'");
         this.error = true;
      } else {
         this.usePropertiesFile = true;
         this.propertiesFile = var1;
      }
   }

   public String getPropertiesFile() {
      return this.propertiesFile;
   }

   @Override
   public void setStartScheduler(boolean var1) {
      this.startScheduler = var1;
   }

   public boolean getStartScheduler() {
      return this.startScheduler;
   }

   public void createService() {
      this.log.info("Create QuartzService(" + this.jndiName + ")...");
      if (this.error) {
         this.log.error("Must specify only one of 'Properties' or 'PropertiesFile'");
         throw new Exception("Must specify only one of 'Properties' or 'PropertiesFile'");
      } else {
         this.schedulerFactory = new StdSchedulerFactory();

         try {
            if (this.useProperties) {
               this.schedulerFactory.initialize(this.properties);
            }

            if (this.usePropertiesFile) {
               this.schedulerFactory.initialize(this.propertiesFile);
            }
         } catch (Exception var2) {
            this.log.error("Failed to initialize Scheduler", var2);
            throw new SchedulerConfigException("Failed to initialize Scheduler - ", var2);
         }

         this.log.info("QuartzService(" + this.jndiName + ") created.");
      }
   }

   public void destroyService() {
      this.log.info("Destroy QuartzService(" + this.jndiName + ")...");
      this.schedulerFactory = null;
      this.log.info("QuartzService(" + this.jndiName + ") destroyed.");
   }

   public void startService() {
      this.log.info("Start QuartzService(" + this.jndiName + ")...");

      try {
         this.rebind();
      } catch (NamingException var3) {
         this.log.error("Failed to rebind Scheduler", var3);
         throw new SchedulerConfigException("Failed to rebind Scheduler - ", var3);
      }

      try {
         Scheduler var1 = this.schedulerFactory.getScheduler();
         if (this.startScheduler) {
            var1.start();
         } else {
            this.log.info("Skipping starting the scheduler (will not run jobs).");
         }
      } catch (Exception var2) {
         this.log.error("Failed to start Scheduler", var2);
         throw new SchedulerConfigException("Failed to start Scheduler - ", var2);
      }

      this.log.info("QuartzService(" + this.jndiName + ") started.");
   }

   public void stopService() {
      this.log.info("Stop QuartzService(" + this.jndiName + ")...");

      try {
         Scheduler var1 = this.schedulerFactory.getScheduler();
         var1.shutdown();
      } catch (Exception var2) {
         this.log.error("Failed to shutdown Scheduler", var2);
         throw new SchedulerConfigException("Failed to shutdown Scheduler - ", var2);
      }

      this.unbind(this.jndiName);
      this.log.info("QuartzService(" + this.jndiName + ") stopped.");
   }

   private void rebind() {
      InitialContext var1 = null;

      try {
         var1 = new InitialContext();
         Name var2 = var1.getNameParser("").parse(this.jndiName);
         Scheduler var3 = this.schedulerFactory.getScheduler();
         NonSerializableFactory.rebind(var2, var3, true);
      } finally {
         if (var1 != null) {
            try {
               var1.close();
            } catch (NamingException var9) {
            }
         }
      }
   }

   private void unbind(String var1) {
      InitialContext var2 = null;

      try {
         var2 = new InitialContext();
         var2.unbind(var1);
         NonSerializableFactory.unbind(var1);
      } catch (NamingException var12) {
         this.log.warn("Failed to unbind scheduler with jndiName: " + var1, var12);
      } finally {
         if (var2 != null) {
            try {
               var2.close();
            } catch (NamingException var11) {
            }
         }
      }
   }
}
