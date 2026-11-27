package com.onyxshop.util;

import java.util.Arrays;
import org.bukkit.Bukkit;

public class ServerInfo {
   private static ServerInfo.ServerType type;
   private static ServerInfo.Version version;
   private static boolean components;
   private static final String CB_PACKAGE = Bukkit.getServer().getClass().getPackage().getName();
   private static final boolean NEW_VERSION_SCHEMA = Bukkit.getBukkitVersion().split("-")[0].split("\\.")[0].length() == 2;

   public ServerInfo() {
   }

   public static boolean supportsSpigot() {
      return type != ServerInfo.ServerType.BUKKIT;
   }

   public static boolean supportsPaper() {
      return type != ServerInfo.ServerType.BUKKIT && type != ServerInfo.ServerType.SPIGOT;
   }

   public static boolean usesMojangMappings() {
      try {
         return version.newerOrEqualAs(ServerInfo.Version.v1_21_R1) && Class.forName("net.minecraft.server.MinecraftServer").getDeclaredField("LOGGER") != null;
      } catch (ClassNotFoundException exception) {
         return false;
      } catch (NullPointerException | NoSuchFieldException exception) {
         return false;
      }
   }

   public static boolean isPaper() {
      return type == ServerInfo.ServerType.PAPER;
   }

   public static boolean isFolia() {
      return type == ServerInfo.ServerType.FOLIA;
   }

   public static boolean supportsComponents() {
      return components;
   }

   public static ServerInfo.Version getVersion() {
      return version;
   }

   public static boolean isNMS() {
      return version.getSimple() > ServerInfo.Version.v1_16_R3.getSimple();
   }

   static {
      try {
         Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
         type = ServerInfo.ServerType.FOLIA;
      } catch (ClassNotFoundException exception) {
         try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            type = ServerInfo.ServerType.PAPER;
         } catch (ClassNotFoundException exception2) {
            try {
               Class.forName("org.spigotmc.SpigotConfig");
               type = ServerInfo.ServerType.SPIGOT;
            } catch (ClassNotFoundException exception3) {
               type = ServerInfo.ServerType.BUKKIT;
            }
         }
      }

      try {
         version = ServerInfo.Version.valueOf(CB_PACKAGE.split("\\.")[3]);
      } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException exception) {
         version = ServerInfo.Version.getFromSimple(Bukkit.getBukkitVersion().split("-")[0].split("\\.(?!\\d)")[0]);
      }

      components = version != null && version.newerOrEqualAs(ServerInfo.Version.v1_20_R4);
   }

   private static enum ServerType {
      BUKKIT,
      SPIGOT,
      PAPER,
      FOLIA;

      private ServerType() {
      }
   }

   public static enum Version {
      v26_3("26.3", "26.3.1", "26.3.2"),
      v26_2("26.2", "26.2.1", "26.2.2"),
      v26_1("26.1", "26.1.1", "26.1.2"),
      v1_21_R7("1.21.11", "1.21.12"),
      v1_21_R6("1.21.9", "1.21.10"),
      v1_21_R5("1.21.6", "1.21.7", "1.21.8"),
      v1_21_R4("1.21.5"),
      v1_21_R3("1.21.4"),
      v1_21_R2("1.21.2", "1.21.3"),
      v1_21_R1("1.21", "1.21.1"),
      v1_20_R4("1.20.5", "1.20.6"),
      v1_20_R3("1.20.3", "1.20.4"),
      v1_20_R2("1.20.2"),
      v1_20_R1("1.20", "1.20.1"),
      v1_19_R3("1.19.4"),
      v1_19_R2("1.19.3"),
      v1_19_R1("1.19", "1.19.1", "1.19.2"),
      v1_18_R2("1.18.2"),
      v1_18_R1("1.18", "1.18.1"),
      v1_17_R1("1.17", "1.17.1"),
      v1_16_R3("1.16.4", "1.16.5"),
      v1_16_R2("1.16.2", "1.16.3"),
      v1_16_R1("1.16", "1.16.1"),
      v1_15_R1("1.15", "1.15.1", "1.15.2"),
      v1_14_R1("1.14", "1.14.1", "1.14.2", "1.14.3", "1.14.4"),
      v1_13_R2("1.13.1", "1.13.2"),
      v1_13_R1("1.13"),
      v1_12_R1("1.12", "1.12.1", "1.12.2"),
      v1_11_R1("1.11", "1.11.1", "1.11.2"),
      v1_10_R1("1.10", "1.10.1", "1.10.2"),
      v1_9_R2("1.9.3", "1.9.4"),
      v1_9_R1("1.9", "1.9.1", "1.9.2"),
      v1_8_R3("1.8.4", "1.8.5", "1.8.6", "1.8.7", "1.8.8"),
      v1_8_R2("1.8.1", "1.8.2", "1.8.3"),
      v1_8_R1("1.8"),
      UNKOWN_VERSION("0.0");

      private final String[] versions;

      private Version(String... values) {
         this.versions = values;
      }

      public static ServerInfo.Version getFromSimple(String text) {
         for (ServerInfo.Version version : values()) {
            if (Arrays.asList(version.versions).contains(text)) {
               return version;
            }
         }

         return UNKOWN_VERSION;
      }

      public int getMajor() {
         if (this == UNKOWN_VERSION) {
            return 0;
         } else if (ServerInfo.NEW_VERSION_SCHEMA) {
            return Integer.parseInt(this.name().split("_", 2)[0].replace("v", ""));
         } else {
            String name = this.name().split("_")[1];
            if (name.length() == 1) {
               name = "0" + name;
            }

            return Integer.parseInt(name);
         }
      }

      public int getVersion() {
         if (this == UNKOWN_VERSION) {
            return 0;
         } else {
            String name = this.name().split("_", 2)[0].replace("v", "");
            String text = this.name().split("_")[1];
            return Integer.parseInt(name + text);
         }
      }

      public int getSimple() {
         if (this == UNKOWN_VERSION) {
            return 0;
         } else {
            String name = this.name().split("_", 2)[0].replace("v", "");
            String text = this.name().split("_")[1];
            if (text.length() == 1) {
               text = "0" + text;
            }

            if (ServerInfo.NEW_VERSION_SCHEMA) {
               return Integer.parseInt(name + text);
            } else {
               String text2 = String.valueOf(this.name().charAt(this.name().length() - 1));
               return Integer.parseInt(name + text + text2);
            }
         }
      }

      public boolean newerOrEqualAs(ServerInfo.Version version) {
         return this.getSimple() >= version.getSimple();
      }

      public boolean olderOrEqualAs(ServerInfo.Version version) {
         return this.getSimple() <= version.getSimple();
      }
   }
}
