package com.onyxshop.providers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.User;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import protocolsupport.api.ProtocolSupportAPI;
import protocolsupport.api.ProtocolVersion;

public class UserManager {
   private final OnyxShop plugin;
   private static final ArrayList<Integer> versions = new ArrayList<>();
   private static ViaAPI vv;
   private static FloodgateApi fAPI;
   private static String prefix = ".";
   private static boolean ps = false;
   private static final LoadingCache<UUID, User> USER_MAP = CacheBuilder.newBuilder()
      .expireAfterAccess(30L, TimeUnit.MINUTES)
      .build(new CacheLoader<UUID, User>() {
         public User load(UUID var1) {
            return UserManager.loadUser(Bukkit.getPlayer(var1));
         }
      });

   public UserManager(OnyxShop var1) {
      this.plugin = var1;
   }

   public void init() {
      if (this.plugin.getServer().getPluginManager().getPlugin("ViaVersion") != null) {
         vv = Via.getAPI();
         this.plugin.runTaskLater(() -> versions.addAll(vv.getServerVersion().supportedVersions()), 5L);
      }

      if (this.plugin.getServer().getPluginManager().getPlugin("ProtocolSupport") != null) {
         versions.addAll(Arrays.stream(ProtocolVersion.getAllSupported()).map(ProtocolVersion::getId).collect(Collectors.toList()));
         ps = true;
      }

      if (this.plugin.getServer().getPluginManager().isPluginEnabled("floodgate")) {
         fAPI = FloodgateApi.getInstance();
         prefix = this.getPrefix();
      }
   }

   private String getPrefix() {
      File var1 = new File(OnyxShop.getInstance().getDataFolder().getParent() + "/floodgate/config.yml");
      if (!var1.exists()) {
         SendMessage.logDebugMessage("Failed to find player prefix for floodgate players, using default of '.'...");
         return ".";
      } else {
         YamlConfiguration var2 = OnyxShop.getInstance().loadConfiguration(var1, "floodgate config.yml");
         return var2 == null ? "." : var2.getString("username-prefix", ".");
      }
   }

   public static User getUser(Player var0) {
      try {
         return (User)USER_MAP.get(var0.getUniqueId());
      } catch (ExecutionException var2) {
         return loadUser(var0);
      } catch (RuntimeException var3) {
         // Guava wraps loader failures (e.g. a player who quit before the entry loaded) in
         // UncheckedExecutionException; fall back to a non-cached instance instead of throwing.
         return loadUser(var0);
      }
   }

   public static User loadUser(Player var0) {
      return new User(var0);
   }

   public static void invalidate(UUID var0) {
      USER_MAP.invalidate(var0);
   }

   public static boolean isPrPlayer(Player var0) {
      if (vv != null) {
         return !versions.contains(vv.getPlayerVersion(var0.getUniqueId()));
      } else {
         return ps ? versions.contains(ProtocolSupportAPI.getConnection(var0).getVersion().getId()) : false;
      }
   }

   public static boolean isFloodgatePlayer(Player var0) {
      return fAPI != null ? fAPI.isFloodgatePlayer(var0.getUniqueId()) : var0.getName().startsWith(prefix);
   }

   public static Locale getLocale(Player var0, boolean var1) {
      return getLocale(var0, var1 ? getBedrockLocale(var0) : var0.getLocale());
   }

   public static Locale getLocale(Player var0, String var1) {
      Locale var2 = Locale.forLanguageTag(var1.replace("_", "-"));
      if (var2.getLanguage().isEmpty()) {
         SendMessage.warnMessage("Failed to parse locale of player " + var0.getName() + " for '" + var1 + "', using server default...");
         return Lang.getDefaultLocale();
      } else {
         Locale var3 = null;

         for (Locale var5 : Lang.getLocales()) {
            if (var5.equals(var2)) {
               return var2;
            }

            if (var5.getCountry().isEmpty() && var5.getLanguage().equals(var2.getLanguage())) {
               var3 = var5;
            }
         }

         if (var3 != null) {
            if (OnyxShop.getInstance().perPlayerLanguages) {
               SendMessage.logDevDebugMessage(
                  "Locale of player " + var0.getName() + " for '" + var2 + "' is not supported by plugin, using lang-" + var3 + ".yml as fallback instead..."
               );
            }

            return var3;
         } else {
            if (OnyxShop.getInstance().perPlayerLanguages) {
               SendMessage.logDebugMessage("Locale of player " + var0.getName() + " for '" + var2 + "' is not supported by plugin, using plugin default...");
            }

            return Lang.getDefaultLocale();
         }
      }
   }

   public static String getBedrockLocale(Player var0) {
      return fAPI != null ? fAPI.getPlayer(var0.getUniqueId()).getLanguageCode() : var0.getLocale();
   }

   public void reloadPlayerData() {
      this.plugin.getServer().getOnlinePlayers().forEach(var0 -> USER_MAP.refresh(var0.getUniqueId()));
   }
}
