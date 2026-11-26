package org.quartz.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.MutableTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLSchedulingDataProcessor implements ErrorHandler {
   public static final String QUARTZ_NS = "http://www.quartz-scheduler.org/xml/JobSchedulingData";
   public static final String QUARTZ_SCHEMA_WEB_URL = "http://www.quartz-scheduler.org/xml/job_scheduling_data_2_0.xsd";
   public static final String QUARTZ_XSD_PATH_IN_JAR = "org/quartz/xml/job_scheduling_data_2_0.xsd";
   public static final String QUARTZ_XML_DEFAULT_FILE_NAME = "quartz_data.xml";
   public static final String QUARTZ_SYSTEM_ID_JAR_PREFIX = "jar:";
   protected List<String> jobGroupsToDelete = new LinkedList<>();
   protected List<String> triggerGroupsToDelete = new LinkedList<>();
   protected List<JobKey> jobsToDelete = new LinkedList<>();
   protected List<TriggerKey> triggersToDelete = new LinkedList<>();
   protected List<JobDetail> loadedJobs = new LinkedList<>();
   protected List<MutableTrigger> loadedTriggers = new LinkedList<>();
   private boolean overWriteExistingData = true;
   private boolean ignoreDuplicates = false;
   protected Collection<Exception> validationExceptions = new ArrayList<>();
   protected ClassLoadHelper classLoadHelper;
   protected List<String> jobGroupsToNeverDelete = new LinkedList<>();
   protected List<String> triggerGroupsToNeverDelete = new LinkedList<>();
   private DocumentBuilder docBuilder = null;
   private XPath xpath = null;
   private final Logger log = LoggerFactory.getLogger(this.getClass());

   public XMLSchedulingDataProcessor(ClassLoadHelper var1) {
      this.classLoadHelper = var1;
      this.initDocumentParser();
   }

   protected void initDocumentParser() {
      DocumentBuilderFactory var1 = DocumentBuilderFactory.newInstance();
      var1.setNamespaceAware(true);
      var1.setValidating(true);
      var1.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
      var1.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", this.resolveSchemaSource());
      this.docBuilder = var1.newDocumentBuilder();
      this.docBuilder.setErrorHandler(this);
      NamespaceContext var2 = new NamespaceContext() {
         @Override
         public String getNamespaceURI(String var1) {
            if (var1 == null) {
               throw new IllegalArgumentException("Null prefix");
            } else if ("xml".equals(var1)) {
               return "http://www.w3.org/XML/1998/namespace";
            } else if ("xmlns".equals(var1)) {
               return "http://www.w3.org/2000/xmlns/";
            } else {
               return "q".equals(var1) ? "http://www.quartz-scheduler.org/xml/JobSchedulingData" : "";
            }
         }

         @Override
         public Iterator<String> getPrefixes(String var1) {
            throw new UnsupportedOperationException();
         }

         @Override
         public String getPrefix(String var1) {
            throw new UnsupportedOperationException();
         }
      };
      this.xpath = XPathFactory.newInstance().newXPath();
      this.xpath.setNamespaceContext(var2);
   }

   protected Object resolveSchemaSource() {
      InputStream var2 = null;

      try {
         var2 = this.classLoadHelper.getResourceAsStream("org/quartz/xml/job_scheduling_data_2_0.xsd");
      } finally {
         if (var2 == null) {
            this.log
               .info(
                  "Unable to load local schema packaged in quartz distribution jar. Utilizing schema online at http://www.quartz-scheduler.org/xml/job_scheduling_data_2_0.xsd"
               );
            return "http://www.quartz-scheduler.org/xml/job_scheduling_data_2_0.xsd";
         }

         InputSource var1 = new InputSource(var2);
         var1.setSystemId("http://www.quartz-scheduler.org/xml/job_scheduling_data_2_0.xsd");
         this.log.debug("Utilizing schema packaged in local quartz distribution jar.");
      }

      Object var6;
      return var6;
   }

   public boolean isOverWriteExistingData() {
      return this.overWriteExistingData;
   }

   protected void setOverWriteExistingData(boolean var1) {
      this.overWriteExistingData = var1;
   }

   public boolean isIgnoreDuplicates() {
      return this.ignoreDuplicates;
   }

   public void setIgnoreDuplicates(boolean var1) {
      this.ignoreDuplicates = var1;
   }

   public void addJobGroupToNeverDelete(String var1) {
      if (var1 != null) {
         this.jobGroupsToNeverDelete.add(var1);
      }
   }

   public boolean removeJobGroupToNeverDelete(String var1) {
      return var1 != null && this.jobGroupsToNeverDelete.remove(var1);
   }

   public List<String> getJobGroupsToNeverDelete() {
      return Collections.unmodifiableList(this.jobGroupsToDelete);
   }

   public void addTriggerGroupToNeverDelete(String var1) {
      if (var1 != null) {
         this.triggerGroupsToNeverDelete.add(var1);
      }
   }

   public boolean removeTriggerGroupToNeverDelete(String var1) {
      return var1 != null ? this.triggerGroupsToNeverDelete.remove(var1) : false;
   }

   public List<String> getTriggerGroupsToNeverDelete() {
      return Collections.unmodifiableList(this.triggerGroupsToDelete);
   }

   protected void processFile() {
      this.processFile("quartz_data.xml");
   }

   protected void processFile(String var1) {
      this.processFile(var1, this.getSystemIdForFileName(var1));
   }

   protected String getSystemIdForFileName(String var1) {
      File var2 = new File(var1);
      if (var2.exists()) {
         try {
            new FileInputStream(var2).close();
            return var2.toURI().toString();
         } catch (IOException var5) {
            return var1;
         }
      } else {
         URL var3 = this.getURL(var1);
         if (var3 == null) {
            return var1;
         } else {
            try {
               var3.openStream().close();
               return var3.toString();
            } catch (IOException var6) {
               return var1;
            }
         }
      }
   }

   protected URL getURL(String var1) {
      return this.classLoadHelper.getResource(var1);
   }

   protected void prepForProcessing() {
      this.clearValidationExceptions();
      this.setOverWriteExistingData(true);
      this.setIgnoreDuplicates(false);
      this.jobGroupsToDelete.clear();
      this.jobsToDelete.clear();
      this.triggerGroupsToDelete.clear();
      this.triggersToDelete.clear();
      this.loadedJobs.clear();
      this.loadedTriggers.clear();
   }

   protected void processFile(String var1, String var2) {
      this.prepForProcessing();
      this.log.info("Parsing XML file: " + var1 + " with systemId: " + var2);
      InputSource var3 = new InputSource(this.getInputStream(var1));
      var3.setSystemId(var2);
      this.process(var3);
      this.maybeThrowValidationException();
   }

   public void processStreamAndScheduleJobs(InputStream var1, String var2, Scheduler var3) {
      this.prepForProcessing();
      this.log.info("Parsing XML from stream with systemId: " + var2);
      InputSource var4 = new InputSource(var1);
      var4.setSystemId(var2);
      this.process(var4);
      this.executePreProcessCommands(var3);
      this.scheduleJobs(var3);
      this.maybeThrowValidationException();
   }

   protected void process(InputSource var1) {
      Document var2 = this.docBuilder.parse(var1);
      NodeList var3 = (NodeList)this.xpath.evaluate("/q:job-scheduling-data/q:pre-processing-commands/q:delete-jobs-in-group", var2, XPathConstants.NODESET);
      this.log.debug("Found " + var3.getLength() + " delete job group commands.");

      for (int var4 = 0; var4 < var3.getLength(); var4++) {
         Node var5 = var3.item(var4);
         String var6 = var5.getTextContent();
         if (var6 != null && (var6 = var6.trim()).length() != 0) {
            this.jobGroupsToDelete.add(var6);
         }
      }

      NodeList var35 = (NodeList)this.xpath
         .evaluate("/q:job-scheduling-data/q:pre-processing-commands/q:delete-triggers-in-group", var2, XPathConstants.NODESET);
      this.log.debug("Found " + var35.getLength() + " delete trigger group commands.");

      for (int var36 = 0; var36 < var35.getLength(); var36++) {
         Node var39 = var35.item(var36);
         String var7 = var39.getTextContent();
         if (var7 != null && (var7 = var7.trim()).length() != 0) {
            this.triggerGroupsToDelete.add(var7);
         }
      }

      NodeList var37 = (NodeList)this.xpath.evaluate("/q:job-scheduling-data/q:pre-processing-commands/q:delete-job", var2, XPathConstants.NODESET);
      this.log.debug("Found " + var37.getLength() + " delete job commands.");

      for (int var40 = 0; var40 < var37.getLength(); var40++) {
         Node var43 = var37.item(var40);
         String var8 = this.getTrimmedToNullString(this.xpath, "q:name", var43);
         String var9 = this.getTrimmedToNullString(this.xpath, "q:group", var43);
         if (var8 == null) {
            throw new ParseException("Encountered a 'delete-job' command without a name specified.", -1);
         }

         this.jobsToDelete.add(new JobKey(var8, var9));
      }

      NodeList var41 = (NodeList)this.xpath.evaluate("/q:job-scheduling-data/q:pre-processing-commands/q:delete-trigger", var2, XPathConstants.NODESET);
      this.log.debug("Found " + var41.getLength() + " delete trigger commands.");

      for (int var44 = 0; var44 < var41.getLength(); var44++) {
         Node var46 = var41.item(var44);
         String var48 = this.getTrimmedToNullString(this.xpath, "q:name", var46);
         String var10 = this.getTrimmedToNullString(this.xpath, "q:group", var46);
         if (var48 == null) {
            throw new ParseException("Encountered a 'delete-trigger' command without a name specified.", -1);
         }

         this.triggersToDelete.add(new TriggerKey(var48, var10));
      }

      Boolean var45 = this.getBoolean(this.xpath, "/q:job-scheduling-data/q:processing-directives/q:overwrite-existing-data", var2);
      if (var45 == null) {
         this.log.debug("Directive 'overwrite-existing-data' not specified, defaulting to " + this.isOverWriteExistingData());
      } else {
         this.log.debug("Directive 'overwrite-existing-data' specified as: " + var45);
         this.setOverWriteExistingData(var45);
      }

      Boolean var47 = this.getBoolean(this.xpath, "/q:job-scheduling-data/q:processing-directives/q:ignore-duplicates", var2);
      if (var47 == null) {
         this.log.debug("Directive 'ignore-duplicates' not specified, defaulting to " + this.isIgnoreDuplicates());
      } else {
         this.log.debug("Directive 'ignore-duplicates' specified as: " + var47);
         this.setIgnoreDuplicates(var47);
      }

      NodeList var49 = (NodeList)this.xpath.evaluate("/q:job-scheduling-data/q:schedule/q:job", var2, XPathConstants.NODESET);
      this.log.debug("Found " + var49.getLength() + " job definitions.");

      for (int var50 = 0; var50 < var49.getLength(); var50++) {
         Node var11 = var49.item(var50);
         String var12 = null;
         String var13 = this.getTrimmedToNullString(this.xpath, "q:name", var11);
         String var14 = this.getTrimmedToNullString(this.xpath, "q:group", var11);
         String var15 = this.getTrimmedToNullString(this.xpath, "q:description", var11);
         String var16 = this.getTrimmedToNullString(this.xpath, "q:job-class", var11);
         var12 = this.getTrimmedToNullString(this.xpath, "q:durability", var11);
         boolean var17 = var12 != null && var12.equals("true");
         var12 = this.getTrimmedToNullString(this.xpath, "q:recover", var11);
         boolean var18 = var12 != null && var12.equals("true");
         Class var19 = this.classLoadHelper.loadClass(var16, Job.class);
         JobDetail var20 = JobBuilder.newJob(var19).withIdentity(var13, var14).withDescription(var15).storeDurably(var17).requestRecovery(var18).build();
         NodeList var21 = (NodeList)this.xpath.evaluate("q:job-data-map/q:entry", var11, XPathConstants.NODESET);

         for (int var22 = 0; var22 < var21.getLength(); var22++) {
            Node var23 = var21.item(var22);
            String var24 = this.getTrimmedToNullString(this.xpath, "q:key", var23);
            String var25 = this.getTrimmedToNullString(this.xpath, "q:value", var23);
            var20.getJobDataMap().put(var24, var25);
         }

         if (this.log.isDebugEnabled()) {
            this.log.debug("Parsed job definition: " + var20);
         }

         this.addJobToSchedule(var20);
      }

      NodeList var51 = (NodeList)this.xpath.evaluate("/q:job-scheduling-data/q:schedule/q:trigger/*", var2, XPathConstants.NODESET);
      this.log.debug("Found " + var51.getLength() + " trigger definitions.");

      for (int var52 = 0; var52 < var51.getLength(); var52++) {
         Node var55 = var51.item(var52);
         String var56 = this.getTrimmedToNullString(this.xpath, "q:name", var55);
         String var57 = this.getTrimmedToNullString(this.xpath, "q:group", var55);
         String var58 = this.getTrimmedToNullString(this.xpath, "q:description", var55);
         String var59 = this.getTrimmedToNullString(this.xpath, "q:misfire-instruction", var55);
         String var60 = this.getTrimmedToNullString(this.xpath, "q:priority", var55);
         String var61 = this.getTrimmedToNullString(this.xpath, "q:calendar-name", var55);
         String var62 = this.getTrimmedToNullString(this.xpath, "q:job-name", var55);
         String var63 = this.getTrimmedToNullString(this.xpath, "q:job-group", var55);
         int var64 = 5;
         if (var60 != null) {
            var64 = Integer.valueOf(var60);
         }

         String var65 = this.getTrimmedToNullString(this.xpath, "q:start-time", var55);
         String var66 = this.getTrimmedToNullString(this.xpath, "q:start-time-seconds-in-future", var55);
         String var67 = this.getTrimmedToNullString(this.xpath, "q:end-time", var55);
         Date var68;
         if (var66 != null) {
            var68 = new Date(System.currentTimeMillis() + Long.valueOf(var66) * 1000L);
         } else {
            var68 = var65 != null && var65.length() != 0 ? DatatypeConverter.parseDateTime(var65).getTime() : new Date();
         }

         Date var26 = var67 != null && var67.length() != 0 ? DatatypeConverter.parseDateTime(var67).getTime() : null;
         TriggerKey var27 = TriggerKey.triggerKey(var56, var57);
         Object var28;
         if (var55.getNodeName().equals("simple")) {
            String var29 = this.getTrimmedToNullString(this.xpath, "q:repeat-count", var55);
            String var30 = this.getTrimmedToNullString(this.xpath, "q:repeat-interval", var55);
            int var31 = var29 == null ? 0 : Integer.parseInt(var29);
            long var32 = var30 == null ? 0L : Long.parseLong(var30);
            var28 = SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(var32).withRepeatCount(var31);
            if (var59 != null && var59.length() != 0) {
               if (var59.equals("MISFIRE_INSTRUCTION_FIRE_NOW")) {
                  ((SimpleScheduleBuilder)var28).withMisfireHandlingInstructionFireNow();
               } else if (var59.equals("MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT")) {
                  ((SimpleScheduleBuilder)var28).withMisfireHandlingInstructionNextWithExistingCount();
               } else if (var59.equals("MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT")) {
                  ((SimpleScheduleBuilder)var28).withMisfireHandlingInstructionNextWithRemainingCount();
               } else if (var59.equals("MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT")) {
                  ((SimpleScheduleBuilder)var28).withMisfireHandlingInstructionNowWithExistingCount();
               } else if (var59.equals("MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT")) {
                  ((SimpleScheduleBuilder)var28).withMisfireHandlingInstructionNowWithRemainingCount();
               } else if (!var59.equals("MISFIRE_INSTRUCTION_SMART_POLICY")) {
                  throw new ParseException("Unexpected/Unhandlable Misfire Instruction encountered '" + var59 + "', for trigger: " + var27, -1);
               }
            }
         } else if (var55.getNodeName().equals("cron")) {
            String var69 = this.getTrimmedToNullString(this.xpath, "q:cron-expression", var55);
            String var72 = this.getTrimmedToNullString(this.xpath, "q:time-zone", var55);
            TimeZone var75 = var72 == null ? null : TimeZone.getTimeZone(var72);
            var28 = CronScheduleBuilder.cronSchedule(var69).inTimeZone(var75);
            if (var59 != null && var59.length() != 0) {
               if (var59.equals("MISFIRE_INSTRUCTION_DO_NOTHING")) {
                  ((CronScheduleBuilder)var28).withMisfireHandlingInstructionDoNothing();
               } else if (var59.equals("MISFIRE_INSTRUCTION_FIRE_ONCE_NOW")) {
                  ((CronScheduleBuilder)var28).withMisfireHandlingInstructionFireAndProceed();
               } else if (!var59.equals("MISFIRE_INSTRUCTION_SMART_POLICY")) {
                  throw new ParseException("Unexpected/Unhandlable Misfire Instruction encountered '" + var59 + "', for trigger: " + var27, -1);
               }
            }
         } else {
            if (!var55.getNodeName().equals("calendar-interval")) {
               throw new ParseException("Unknown trigger type: " + var55.getNodeName(), -1);
            }

            String var70 = this.getTrimmedToNullString(this.xpath, "q:repeat-interval", var55);
            String var73 = this.getTrimmedToNullString(this.xpath, "q:repeat-interval-unit", var55);
            int var76 = Integer.parseInt(var70);
            IntervalUnit var78 = IntervalUnit.valueOf(var73);
            var28 = CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withInterval(var76, var78);
            if (var59 != null && var59.length() != 0) {
               if (var59.equals("MISFIRE_INSTRUCTION_DO_NOTHING")) {
                  ((CalendarIntervalScheduleBuilder)var28).withMisfireHandlingInstructionDoNothing();
               } else if (var59.equals("MISFIRE_INSTRUCTION_FIRE_ONCE_NOW")) {
                  ((CalendarIntervalScheduleBuilder)var28).withMisfireHandlingInstructionFireAndProceed();
               } else if (!var59.equals("MISFIRE_INSTRUCTION_SMART_POLICY")) {
                  throw new ParseException("Unexpected/Unhandlable Misfire Instruction encountered '" + var59 + "', for trigger: " + var27, -1);
               }
            }
         }

         MutableTrigger var71 = (MutableTrigger)TriggerBuilder.newTrigger()
            .withIdentity(var56, var57)
            .withDescription(var58)
            .forJob(var62, var63)
            .startAt(var68)
            .endAt(var26)
            .withPriority(var64)
            .modifiedByCalendar(var61)
            .withSchedule((ScheduleBuilder)var28)
            .build();
         NodeList var74 = (NodeList)this.xpath.evaluate("q:job-data-map/q:entry", var55, XPathConstants.NODESET);

         for (int var77 = 0; var77 < var74.getLength(); var77++) {
            Node var79 = var74.item(var77);
            String var33 = this.getTrimmedToNullString(this.xpath, "q:key", var79);
            String var34 = this.getTrimmedToNullString(this.xpath, "q:value", var79);
            var71.getJobDataMap().put(var33, var34);
         }

         if (this.log.isDebugEnabled()) {
            this.log.debug("Parsed trigger definition: " + var71);
         }

         this.addTriggerToSchedule(var71);
      }
   }

   protected String getTrimmedToNullString(XPath var1, String var2, Node var3) {
      String var4 = (String)var1.evaluate(var2, var3, XPathConstants.STRING);
      if (var4 != null) {
         var4 = var4.trim();
      }

      if (var4 != null && var4.length() == 0) {
         var4 = null;
      }

      return var4;
   }

   protected Boolean getBoolean(XPath var1, String var2, Document var3) {
      Node var4 = (Node)var1.evaluate(var2, var3, XPathConstants.NODE);
      if (var4 != null && var4.getTextContent() != null) {
         String var5 = var4.getTextContent();
         return !var5.equalsIgnoreCase("true") && !var5.equalsIgnoreCase("yes") && !var5.equalsIgnoreCase("y") ? Boolean.FALSE : Boolean.TRUE;
      } else {
         return null;
      }
   }

   public void processFileAndScheduleJobs(Scheduler var1, boolean var2) {
      String var3 = "quartz_data.xml";
      this.processFile(var3, this.getSystemIdForFileName(var3));
      this.setOverWriteExistingData(var2);
      this.executePreProcessCommands(var1);
      this.scheduleJobs(var1);
   }

   public void processFileAndScheduleJobs(String var1, Scheduler var2) {
      this.processFileAndScheduleJobs(var1, this.getSystemIdForFileName(var1), var2);
   }

   public void processFileAndScheduleJobs(String var1, String var2, Scheduler var3) {
      this.processFile(var1, var2);
      this.executePreProcessCommands(var3);
      this.scheduleJobs(var3);
   }

   protected List<JobDetail> getLoadedJobs() {
      return Collections.unmodifiableList(this.loadedJobs);
   }

   protected List<MutableTrigger> getLoadedTriggers() {
      return Collections.unmodifiableList(this.loadedTriggers);
   }

   protected InputStream getInputStream(String var1) {
      return this.classLoadHelper.getResourceAsStream(var1);
   }

   protected void addJobToSchedule(JobDetail var1) {
      this.loadedJobs.add(var1);
   }

   protected void addTriggerToSchedule(MutableTrigger var1) {
      this.loadedTriggers.add(var1);
   }

   private Map<JobKey, List<MutableTrigger>> buildTriggersByFQJobNameMap(List<MutableTrigger> var1) {
      HashMap var2 = new HashMap();

      for (MutableTrigger var4 : var1) {
         List<MutableTrigger> var5 = (List<MutableTrigger>)var2.get(var4.getJobKey());
         if (var5 == null) {
            var5 = new LinkedList();
            var2.put(var4.getJobKey(), var5);
         }

         var5.add(var4);
      }

      return var2;
   }

   protected void executePreProcessCommands(Scheduler var1) {
      for (String var3 : this.jobGroupsToDelete) {
         if (var3.equals("*")) {
            this.log.info("Deleting all jobs in ALL groups.");

            for (String var17 : var1.getJobGroupNames()) {
               if (!this.jobGroupsToNeverDelete.contains(var17)) {
                  for (JobKey var7 : var1.getJobKeys(GroupMatcher.jobGroupEquals(var17))) {
                     var1.deleteJob(var7);
                  }
               }
            }
         } else if (!this.jobGroupsToNeverDelete.contains(var3)) {
            this.log.info("Deleting all jobs in group: {}", var3);

            for (JobKey var5 : var1.getJobKeys(GroupMatcher.jobGroupEquals(var3))) {
               var1.deleteJob(var5);
            }
         }
      }

      for (String var11 : this.triggerGroupsToDelete) {
         if (var11.equals("*")) {
            this.log.info("Deleting all triggers in ALL groups.");

            for (String var19 : var1.getTriggerGroupNames()) {
               if (!this.triggerGroupsToNeverDelete.contains(var19)) {
                  for (TriggerKey var21 : var1.getTriggerKeys(GroupMatcher.triggerGroupEquals(var19))) {
                     var1.unscheduleJob(var21);
                  }
               }
            }
         } else if (!this.triggerGroupsToNeverDelete.contains(var11)) {
            this.log.info("Deleting all triggers in group: {}", var11);

            for (TriggerKey var18 : var1.getTriggerKeys(GroupMatcher.triggerGroupEquals(var11))) {
               var1.unscheduleJob(var18);
            }
         }
      }

      for (JobKey var12 : this.jobsToDelete) {
         if (!this.jobGroupsToNeverDelete.contains(var12.getGroup())) {
            this.log.info("Deleting job: {}", var12);
            var1.deleteJob(var12);
         }
      }

      for (TriggerKey var13 : this.triggersToDelete) {
         if (!this.triggerGroupsToNeverDelete.contains(var13.getGroup())) {
            this.log.info("Deleting trigger: {}", var13);
            var1.unscheduleJob(var13);
         }
      }
   }

   protected void scheduleJobs(Scheduler var1) {
      LinkedList var2 = new LinkedList<>(this.getLoadedJobs());
      LinkedList<MutableTrigger> var3 = new LinkedList<>(this.getLoadedTriggers());
      this.log.info("Adding " + var2.size() + " jobs, " + var3.size() + " triggers.");
      Map var4 = this.buildTriggersByFQJobNameMap(var3);
      Iterator var5 = var2.iterator();

      while (var5.hasNext()) {
         JobDetail var6 = (JobDetail)var5.next();
         var5.remove();
         JobDetail var7 = null;

         try {
            var7 = var1.getJobDetail(var6.getKey());
         } catch (JobPersistenceException var16) {
            if (!(var16.getCause() instanceof ClassNotFoundException) || !this.isOverWriteExistingData()) {
               throw var16;
            }

            this.log.info("Removing job: " + var6.getKey());
            var1.deleteJob(var6.getKey());
         }

         if (var7 != null) {
            if (!this.isOverWriteExistingData() && this.isIgnoreDuplicates()) {
               this.log.info("Not overwriting existing job: " + var7.getKey());
               continue;
            }

            if (!this.isOverWriteExistingData() && !this.isIgnoreDuplicates()) {
               throw new ObjectAlreadyExistsException(var6);
            }
         }

         if (var7 != null) {
            this.log.info("Replacing job: " + var6.getKey());
         } else {
            this.log.info("Adding job: " + var6.getKey());
         }

         List<MutableTrigger> var8 = (List<MutableTrigger>)var4.get(var6.getKey());
         if (!var6.isDurable() && (var8 == null || var8.size() == 0)) {
            if (var7 == null) {
               throw new SchedulerException("A new job defined without any triggers must be durable: " + var6.getKey());
            }

            if (var7.isDurable() && var1.getTriggersOfJob(var6.getKey()).size() == 0) {
               throw new SchedulerException("Can't change existing durable job without triggers to non-durable: " + var6.getKey());
            }
         }

         if (var7 == null && !var6.isDurable()) {
            boolean var9 = true;

            for (MutableTrigger var11 : var8) {
               var3.remove(var11);
               if (var11.getStartTime() == null) {
                  var11.setStartTime(new Date());
               }

               Trigger var12 = var1.getTrigger(var11.getKey());
               if (var12 != null) {
                  if (this.isOverWriteExistingData()) {
                     if (this.log.isDebugEnabled()) {
                        this.log.debug("Rescheduling job: " + var11.getJobKey() + " with updated trigger: " + var11.getKey());
                     }

                     if (!var12.getJobKey().equals(var11.getJobKey())) {
                        this.log.warn("Possibly duplicately named ({}) triggers in jobs xml file! ", var11.getKey());
                     }

                     var1.rescheduleJob(var11.getKey(), var11);
                  } else {
                     if (!this.isIgnoreDuplicates()) {
                        throw new ObjectAlreadyExistsException(var11);
                     }

                     this.log.info("Not overwriting existing trigger: " + var12.getKey());
                  }
               } else {
                  if (this.log.isDebugEnabled()) {
                     this.log.debug("Scheduling job: " + var11.getJobKey() + " with trigger: " + var11.getKey());
                  }

                  try {
                     if (var9) {
                        var1.scheduleJob(var6, var11);
                        var9 = false;
                     } else {
                        var1.scheduleJob(var11);
                     }
                  } catch (ObjectAlreadyExistsException var15) {
                     if (this.log.isDebugEnabled()) {
                        this.log
                           .debug(
                              "Adding trigger: "
                                 + var11.getKey()
                                 + " for job: "
                                 + var6.getKey()
                                 + " failed because the trigger already existed.  "
                                 + "This is likely due to a race condition between multiple instances "
                                 + "in the cluster.  Will try to reschedule instead."
                           );
                     }

                     var1.rescheduleJob(var11.getKey(), var11);
                  }
               }
            }
         } else if (var8 != null && var8.size() > 0) {
            var1.addJob(var6, true, true);
         } else {
            var1.addJob(var6, true, false);
         }
      }

      for (MutableTrigger var18 : var3) {
         if (var18.getStartTime() == null) {
            var18.setStartTime(new Date());
         }

         Trigger var19 = var1.getTrigger(var18.getKey());
         if (var19 != null) {
            if (this.isOverWriteExistingData()) {
               if (this.log.isDebugEnabled()) {
                  this.log.debug("Rescheduling job: " + var18.getJobKey() + " with updated trigger: " + var18.getKey());
               }

               if (!var19.getJobKey().equals(var18.getJobKey())) {
                  this.log.warn("Possibly duplicately named ({}) triggers in jobs xml file! ", var18.getKey());
               }

               var1.rescheduleJob(var18.getKey(), var18);
            } else {
               if (!this.isIgnoreDuplicates()) {
                  throw new ObjectAlreadyExistsException(var18);
               }

               this.log.info("Not overwriting existing trigger: " + var19.getKey());
            }
         } else {
            if (this.log.isDebugEnabled()) {
               this.log.debug("Scheduling job: " + var18.getJobKey() + " with trigger: " + var18.getKey());
            }

            try {
               var1.scheduleJob(var18);
            } catch (ObjectAlreadyExistsException var14) {
               if (this.log.isDebugEnabled()) {
                  this.log
                     .debug(
                        "Adding trigger: "
                           + var18.getKey()
                           + " for job: "
                           + var18.getJobKey()
                           + " failed because the trigger already existed.  "
                           + "This is likely due to a race condition between multiple instances "
                           + "in the cluster.  Will try to reschedule instead."
                     );
               }

               var1.rescheduleJob(var18.getKey(), var18);
            }
         }
      }
   }

   @Override
   public void warning(SAXParseException var1) {
      this.addValidationException(var1);
   }

   @Override
   public void error(SAXParseException var1) {
      this.addValidationException(var1);
   }

   @Override
   public void fatalError(SAXParseException var1) {
      this.addValidationException(var1);
   }

   protected void addValidationException(SAXException var1) {
      this.validationExceptions.add(var1);
   }

   protected void clearValidationExceptions() {
      this.validationExceptions.clear();
   }

   protected void maybeThrowValidationException() throws ValidationException {
      if (this.validationExceptions.size() > 0) {
         throw new ValidationException("Encountered " + this.validationExceptions.size() + " validation exceptions.", this.validationExceptions);
      }
   }
}
