package org.quartz.ee.jmx.jboss;

import java.util.Arrays;
import java.util.Properties;
import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.quartz.SchedulerException;
import org.quartz.impl.RemoteMBeanScheduler;

public class JBoss4RMIRemoteMBeanScheduler extends RemoteMBeanScheduler {
   private static final String DEFAULT_PROVIDER_URL = "jnp://localhost:1099";
   private static final String RMI_ADAPTOR_JNDI_NAME = "jmx/rmi/RMIAdaptor";
   private MBeanServerConnection server = null;
   private String providerURL = "jnp://localhost:1099";

   public JBoss4RMIRemoteMBeanScheduler() {
   }

   public void setProviderURL(String var1) {
      this.providerURL = var1;
   }

   @Override
   public void initialize() {
      InitialContext var1 = null;

      try {
         var1 = new InitialContext(this.getContextProperties());
         this.server = (MBeanServerConnection)var1.lookup("jmx/rmi/RMIAdaptor");
      } catch (Exception var10) {
         throw new SchedulerException("Failed to lookup JBoss JMX RMI Adaptor.", var10);
      } finally {
         if (var1 != null) {
            try {
               var1.close();
            } catch (NamingException var9) {
            }
         }
      }
   }

   protected Properties getContextProperties() {
      Properties var1 = new Properties();
      var1.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
      var1.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
      var1.put("java.naming.provider.url", this.providerURL);
      return var1;
   }

   @Override
   protected Object getAttribute(String var1) {
      try {
         return this.server.getAttribute(this.getSchedulerObjectName(), var1);
      } catch (Exception var3) {
         throw new SchedulerException("Failed to get Scheduler MBean attribute: " + var1, var3);
      }
   }

   @Override
   protected AttributeList getAttributes(String[] var1) {
      try {
         return this.server.getAttributes(this.getSchedulerObjectName(), var1);
      } catch (Exception var3) {
         throw new SchedulerException("Failed to get Scheduler MBean attributes: " + Arrays.<String>asList(var1), var3);
      }
   }

   @Override
   protected Object invoke(String var1, Object[] var2, String[] var3) {
      try {
         return this.server.invoke(this.getSchedulerObjectName(), var1, var2, var3);
      } catch (Exception var5) {
         throw new SchedulerException("Failed to invoke Scheduler MBean operation: " + var1, var5);
      }
   }
}
