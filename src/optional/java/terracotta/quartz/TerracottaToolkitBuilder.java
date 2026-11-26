package org.terracotta.quartz;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.ToolkitInstantiationException;

public class TerracottaToolkitBuilder {
   private static final String TC_TUNNELLED_MBEAN_DOMAIN_KEY = "tunnelledMBeanDomains";
   private static final String TC_CONFIG_SNIPPET_KEY = "tcConfigSnippet";
   private static final String TC_REJOIN_KEY = "rejoin";
   private final TerracottaToolkitBuilder.TCConfigTypeStatus tcConfigTypeStatus = new TerracottaToolkitBuilder.TCConfigTypeStatus();
   private final Set<String> tunnelledMBeanDomains = Collections.synchronizedSet(new HashSet<>());
   private boolean rejoin = false;

   public TerracottaToolkitBuilder() {
   }

   public Toolkit buildToolkit() {
      if (this.tcConfigTypeStatus.getState() == TerracottaToolkitBuilder.TCConfigTypeState.INIT) {
         throw new IllegalStateException("Please set the tcConfigSnippet or tcConfigUrl before attempting to create client");
      } else {
         Properties var1 = new Properties();
         var1.setProperty("tunnelledMBeanDomains", this.getTunnelledDomainCSV());
         var1.setProperty("rejoin", Boolean.toString(this.isRejoin()));
         switch (this.tcConfigTypeStatus.getState()) {
            case TC_CONFIG_SNIPPET:
               var1.setProperty("tcConfigSnippet", this.tcConfigTypeStatus.getTcConfigSnippet());
               return this.createToolkit("toolkit:terracotta:", var1);
            case TC_CONFIG_URL:
               return this.createToolkit("toolkit:terracotta://" + this.tcConfigTypeStatus.getTcConfigUrl(), var1);
            default:
               throw new IllegalStateException("Unknown tc config type - " + this.tcConfigTypeStatus.getState());
         }
      }
   }

   private Toolkit createToolkit(String var1, Properties var2) {
      try {
         return ToolkitFactory.createToolkit(var1, var2);
      } catch (ToolkitInstantiationException var4) {
         throw new RuntimeException(var4);
      }
   }

   private String getTunnelledDomainCSV() {
      StringBuilder var1 = new StringBuilder();

      for (String var3 : this.tunnelledMBeanDomains) {
         var1.append(var3).append(",");
      }

      return var1.deleteCharAt(var1.length() - 1).toString();
   }

   public TerracottaToolkitBuilder addTunnelledMBeanDomain(String var1) {
      this.tunnelledMBeanDomains.add(var1);
      return this;
   }

   public Set<String> getTunnelledMBeanDomains() {
      return Collections.unmodifiableSet(this.tunnelledMBeanDomains);
   }

   public TerracottaToolkitBuilder removeTunnelledMBeanDomain(String var1) {
      this.tunnelledMBeanDomains.remove(var1);
      return this;
   }

   public TerracottaToolkitBuilder setTCConfigSnippet(String var1) {
      this.tcConfigTypeStatus.setTcConfigSnippet(var1);
      return this;
   }

   public String getTCConfigSnippet() {
      return this.tcConfigTypeStatus.getTcConfigSnippet();
   }

   public TerracottaToolkitBuilder setTCConfigUrl(String var1) {
      this.tcConfigTypeStatus.setTcConfigUrl(var1);
      return this;
   }

   public String getTCConfigUrl() {
      return this.tcConfigTypeStatus.getTcConfigUrl();
   }

   public boolean isConfigUrl() {
      return this.tcConfigTypeStatus.getState() == TerracottaToolkitBuilder.TCConfigTypeState.TC_CONFIG_URL;
   }

   public TerracottaToolkitBuilder setRejoin(String var1) {
      this.rejoin = Boolean.valueOf(var1);
      return this;
   }

   public boolean isRejoin() {
      return this.rejoin;
   }

   private static enum TCConfigTypeState {
      INIT,
      TC_CONFIG_SNIPPET,
      TC_CONFIG_URL;

      private TCConfigTypeState() {
      }
   }

   private static class TCConfigTypeStatus {
      private TerracottaToolkitBuilder.TCConfigTypeState state = TerracottaToolkitBuilder.TCConfigTypeState.INIT;
      private String tcConfigSnippet;
      private String tcConfigUrl;

      private TCConfigTypeStatus() {
      }

      public synchronized void setTcConfigSnippet(String var1) {
         if (this.state == TerracottaToolkitBuilder.TCConfigTypeState.TC_CONFIG_URL) {
            throw new IllegalStateException("TCConfig url was already set to - " + this.tcConfigUrl);
         } else {
            this.state = TerracottaToolkitBuilder.TCConfigTypeState.TC_CONFIG_SNIPPET;
            this.tcConfigSnippet = var1;
         }
      }

      public synchronized void setTcConfigUrl(String var1) {
         if (this.state == TerracottaToolkitBuilder.TCConfigTypeState.TC_CONFIG_SNIPPET) {
            throw new IllegalStateException("TCConfig snippet was already set to - " + this.tcConfigSnippet);
         } else {
            this.state = TerracottaToolkitBuilder.TCConfigTypeState.TC_CONFIG_URL;
            this.tcConfigUrl = var1;
         }
      }

      public synchronized String getTcConfigSnippet() {
         return this.tcConfigSnippet;
      }

      public synchronized String getTcConfigUrl() {
         return this.tcConfigUrl;
      }

      public TerracottaToolkitBuilder.TCConfigTypeState getState() {
         return this.state;
      }
   }
}
