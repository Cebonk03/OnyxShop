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
         public User load(UUID uniqueId) {
            return UserManager.loadUser(Bukkit.getPlayer(uniqueId));
         }
      });

   public UserManager(OnyxShop plugin) {
      this.plugin = plugin;
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
      File file = new File(OnyxShop.getInstance().getDataFolder().getParent() + "/floodgate/config.yml");
      if (!file.exists()) {
         SendMessage.logDebugMessage("Failed to find player prefix for floodgate players, using default of '.'...");
         return ".";
      } else {
         YamlConfiguration config = OnyxShop.getInstance().loadConfiguration(file, "floodgate config.yml");
         return config == null ? "." : config.getString("username-prefix", ".");
      }
   }

   public static User getUser(Player player) {
      try {
         return (User)USER_MAP.get(player.getUniqueId());
      } catch (ExecutionException exception) {
         return loadUser(player);
      } catch (RuntimeException exception) {
         // Guava wraps loader failures (e.g. a player who quit before the entry loaded) in
         // UncheckedExecutionException; fall back to a non-cached instance instead of throwing.
         return loadUser(player);
      }
   }

   public static User loadUser(Player player) {
      return new User(player);
   }

   public static void invalidate(UUID uniqueId) {
      USER_MAP.invalidate(uniqueId);
   }

   public static boolean isPrPlayer(Player player) {
      if (vv != null) {
         return !versions.contains(vv.getPlayerVersion(player.getUniqueId()));
      } else {
         return ps ? versions.contains(ProtocolSupportAPI.getConnection(player).getVersion().getId()) : false;
      }
   }

   public static boolean isFloodgatePlayer(Player player) {
      return fAPI != null ? fAPI.isFloodgatePlayer(player.getUniqueId()) : player.getName().startsWith(prefix);
   }

   public static Locale getLocale(Player player, boolean flag) {
      return getLocale(player, flag ? getBedrockLocale(player) : player.getLocale());
   }

   public static Locale getLocale(Player player, String playerName) {
      Locale locale = Locale.forLanguageTag(playerName.replace("_", "-"));
      if (locale.getLanguage().isEmpty()) {
         SendMessage.warnMessage("Failed to parse locale of player " + player.getName() + " for '" + playerName + "', using server default...");
         return Lang.getDefaultLocale();
      } else {
         Locale locale2 = null;

         for (Locale locale3 : Lang.getLocales()) {
            if (locale3.equals(locale)) {
               return locale;
            }

            if (locale3.getCountry().isEmpty() && locale3.getLanguage().equals(locale.getLanguage())) {
               locale2 = locale3;
            }
         }

         if (locale2 != null) {
            if (OnyxShop.getInstance().perPlayerLanguages) {
               SendMessage.logDevDebugMessage(
                  "Locale of player " + player.getName() + " for '" + locale + "' is not supported by plugin, using lang-" + locale2 + ".yml as fallback instead..."
               );
            }

            return locale2;
         } else {
            if (OnyxShop.getInstance().perPlayerLanguages) {
               SendMessage.logDebugMessage("Locale of player " + player.getName() + " for '" + locale + "' is not supported by plugin, using plugin default...");
            }

            return Lang.getDefaultLocale();
         }
      }
   }

   public static String getBedrockLocale(Player player) {
      return fAPI != null ? fAPI.getPlayer(player.getUniqueId()).getLanguageCode() : player.getLocale();
   }

   public void reloadPlayerData() {
      this.plugin.getServer().getOnlinePlayers().forEach(entry -> USER_MAP.refresh(entry.getUniqueId()));
   }
}
