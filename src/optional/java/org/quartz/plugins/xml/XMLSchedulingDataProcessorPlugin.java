package org.quartz.plugins.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.transaction.UserTransaction;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.jobs.FileScanJob;
import org.quartz.jobs.FileScanListener;
import org.quartz.plugins.SchedulerPluginWithUserTransactionSupport;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.xml.XMLSchedulingDataProcessor;

public class XMLSchedulingDataProcessorPlugin extends SchedulerPluginWithUserTransactionSupport implements FileScanListener {
   private static final int MAX_JOB_TRIGGER_NAME_LEN = 80;
   private static final String JOB_INITIALIZATION_PLUGIN_NAME = "JobSchedulingDataLoaderPlugin";
   private static final String FILE_NAME_DELIMITERS = ",";
   private boolean failOnFileNotFound = true;
   private String fileNames = "quartz_data.xml";
   private Map<String, XMLSchedulingDataProcessorPlugin.JobFile> jobFiles = new LinkedHashMap<>();
   private long scanInterval = 0L;
   boolean started = false;
   protected ClassLoadHelper classLoadHelper = null;
   private Set<String> jobTriggerNameSet = new HashSet<>();

   public XMLSchedulingDataProcessorPlugin() {
   }

   public String getFileNames() {
      return this.fileNames;
   }

   public void setFileNames(String var1) {
      this.fileNames = var1;
   }

   public long getScanInterval() {
      return this.scanInterval / 1000L;
   }

   public void setScanInterval(long var1) {
      this.scanInterval = var1 * 1000L;
   }

   public boolean isFailOnFileNotFound() {
      return this.failOnFileNotFound;
   }

   public void setFailOnFileNotFound(boolean var1) {
      this.failOnFileNotFound = var1;
   }

   @Override
   public void initialize(String var1, Scheduler var2, ClassLoadHelper var3) {
      super.initialize(var1, var2);
      this.classLoadHelper = var3;
      this.getLog().info("Registering Quartz Job Initialization Plug-in.");
      StringTokenizer var4 = new StringTokenizer(this.fileNames, ",");

      while (var4.hasMoreTokens()) {
         String var5 = var4.nextToken();
         XMLSchedulingDataProcessorPlugin.JobFile var6 = new XMLSchedulingDataProcessorPlugin.JobFile(var5);
         this.jobFiles.put(var5, var6);
      }
   }

   @Override
   public void start(UserTransaction var1) {
      try {
         if (!this.jobFiles.isEmpty()) {
            if (this.scanInterval > 0L) {
               this.getScheduler().getContext().put("JobSchedulingDataLoaderPlugin_" + this.getName(), this);
            }

            for (XMLSchedulingDataProcessorPlugin.JobFile var3 : this.jobFiles.values()) {
               if (this.scanInterval > 0L) {
                  String var4 = this.buildJobTriggerName(var3.getFileBasename());
                  TriggerKey var5 = new TriggerKey(var4, "JobSchedulingDataLoaderPlugin");
                  this.getScheduler().unscheduleJob(var5);
                  JobDetail var6 = JobBuilder.newJob()
                     .withIdentity(var4, "JobSchedulingDataLoaderPlugin")
                     .ofType(FileScanJob.class)
                     .usingJobData("FILE_NAME", var3.getFileName())
                     .usingJobData("FILE_SCAN_LISTENER_NAME", "JobSchedulingDataLoaderPlugin_" + this.getName())
                     .build();
                  SimpleTrigger var7 = TriggerBuilder.newTrigger()
                     .withIdentity(var5)
                     .withSchedule(SimpleScheduleBuilder.simpleSchedule().repeatForever().withIntervalInMilliseconds(this.scanInterval))
                     .forJob(var6)
                     .build();
                  this.getScheduler().scheduleJob(var6, var7);
                  this.getLog().debug("Scheduled file scan job for data file: {}, at interval: {}", var3.getFileName(), this.scanInterval);
               }

               this.processFile(var3);
            }
         }
      } catch (SchedulerException var11) {
         this.getLog().error("Error starting background-task for watching jobs file.", var11);
      } finally {
         this.started = true;
      }
   }

   private String buildJobTriggerName(String var1) {
      String var2 = "JobSchedulingDataLoaderPlugin_" + this.getName() + '_' + var1.replace('.', '_');
      if (var2.length() > 80) {
         var2 = var2.substring(0, 80);
      }

      int var3 = 1;

      while (!this.jobTriggerNameSet.add(var2)) {
         if (var3 > 1) {
            var2 = var2.substring(0, var2.lastIndexOf(95));
         }

         String var4 = "_" + var3++;
         if (var2.length() > 80 - var4.length()) {
            var2 = var2.substring(0, 80 - var4.length());
         }

         var2 = var2 + var4;
      }

      return var2;
   }

   @Override
   public void shutdown() {
   }

   private void processFile(XMLSchedulingDataProcessorPlugin.JobFile var1) {
      if (var1 != null && var1.getFileFound()) {
         try {
            XMLSchedulingDataProcessor var2 = new XMLSchedulingDataProcessor(this.classLoadHelper);
            var2.addJobGroupToNeverDelete("JobSchedulingDataLoaderPlugin");
            var2.addTriggerGroupToNeverDelete("JobSchedulingDataLoaderPlugin");
            var2.processFileAndScheduleJobs(var1.getFileName(), var1.getFileName(), this.getScheduler());
         } catch (Exception var3) {
            this.getLog().error("Error scheduling jobs: " + var3.getMessage(), var3);
         }
      }
   }

   public void processFile(String var1) {
      this.processFile(this.jobFiles.get(var1));
   }

   public void fileUpdated(String var1) {
      if (this.started) {
         this.processFile(var1);
      }
   }

   class JobFile {
      private String fileName;
      private String filePath;
      private String fileBasename;
      private boolean fileFound;

      protected JobFile(String var2) {
         this.fileName = var2;
         this.initialize();
      }

      protected String getFileName() {
         return this.fileName;
      }

      protected boolean getFileFound() {
         return this.fileFound;
      }

      protected String getFilePath() {
         return this.filePath;
      }

      protected String getFileBasename() {
         return this.fileBasename;
      }

      private void initialize() {
         Object var1 = null;

         try {
            String var2 = null;
            File var3 = new File(this.getFileName());
            if (!var3.exists()) {
               URL var4 = XMLSchedulingDataProcessorPlugin.this.classLoadHelper.getResource(this.getFileName());
               if (var4 != null) {
                  try {
                     var2 = URLDecoder.decode(var4.getPath(), "UTF-8");
                  } catch (UnsupportedEncodingException var17) {
                     var2 = var4.getPath();
                  }

                  var3 = new File(var2);

                  try {
                     var1 = var4.openStream();
                  } catch (IOException var16) {
                  }
               }
            } else {
               try {
                  var1 = new FileInputStream(var3);
               } catch (FileNotFoundException var15) {
               }
            }

            if (var1 == null) {
               if (XMLSchedulingDataProcessorPlugin.this.isFailOnFileNotFound()) {
                  throw new SchedulerException("File named '" + this.getFileName() + "' does not exist.");
               }

               XMLSchedulingDataProcessorPlugin.this.getLog().warn("File named '" + this.getFileName() + "' does not exist.");
            } else {
               this.fileFound = true;
            }

            this.filePath = var2 != null ? var2 : var3.getAbsolutePath();
            this.fileBasename = var3.getName();
         } finally {
            try {
               if (var1 != null) {
                  var1.close();
               }
            } catch (IOException var14) {
               XMLSchedulingDataProcessorPlugin.this.getLog().warn("Error closing jobs file " + this.getFileName(), var14);
            }
         }
      }
   }
}
