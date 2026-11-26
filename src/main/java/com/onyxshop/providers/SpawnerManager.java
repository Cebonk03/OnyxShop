package com.onyxshop.providers;

import com.bgsoftware.wildstacker.api.WildStacker;
import com.bgsoftware.wildstacker.api.WildStackerAPI;
import de.dustplanet.util.SilkUtil;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import com.onyxshop.OnyxShop;
import com.onyxshop.events.SpawnerBreakEvent;
import com.onyxshop.events.SpawnerInteractEvent;
import com.onyxshop.events.SpawnerPlaceEvent;
import com.onyxshop.events.api.ProviderLoadEvent;
import com.onyxshop.events.delayed.DelayedLoadListener;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.spawners.DefaultSpawnerProvider;
import com.onyxshop.providers.spawners.EpicSpawnersHook;
import com.onyxshop.providers.spawners.MineableSpawnersHook;
import com.onyxshop.providers.spawners.PurpurSpawnerHook;
import com.onyxshop.providers.spawners.RoseStackerHook;
import com.onyxshop.providers.spawners.SilkSpawnersHook;
import com.onyxshop.providers.spawners.SilkSpawnersV2HookV2;
import com.onyxshop.providers.spawners.SmartSpawnerHook;
import com.onyxshop.providers.spawners.SpawnerLegacyHookV4;
import com.onyxshop.providers.spawners.SpawnerMetaHook;
import com.onyxshop.providers.spawners.UltimateStackerHook;
import com.onyxshop.providers.spawners.UpgradeableSpawnersHook;
import com.onyxshop.providers.spawners.WildStackerHook;
import com.onyxshop.util.ReflectUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

public class SpawnerManager {
   private SpawnerInteractEvent interactEvent;
   private SpawnerBreakEvent breakEvent;
   private SpawnerPlaceEvent placeEvent;
   private final OnyxShop plugin;
   private SpawnerProvider provider;
   private boolean waiting = false;

   public SpawnerManager(OnyxShop var1) {
      this.plugin = var1;
   }

   public void init() {
      this.provider = null;
      if (this.breakEvent != null) {
         BlockBreakEvent.getHandlerList().unregister(this.breakEvent);
      }

      if (this.placeEvent != null) {
         BlockPlaceEvent.getHandlerList().unregister(this.placeEvent);
      }

      if (this.interactEvent != null) {
         PlayerInteractEvent.getHandlerList().unregister(this.interactEvent);
      }

      SpawnerManager.Provider var1 = SpawnerManager.Provider.getFromString(ConfigManager.getConfig().getString("spawner-provider", "DEFAULT"));
      SpawnerProvider var2 = this.setup(var1, false);
      if (var2 == null && !this.waiting) {
         this.provider = this.initProvider(SpawnerManager.Provider.DEFAULT, null);
         SendMessage.infoMessage(Lang.USING_DEFAULT_SPAWNERS.get());
      }
   }

   public void initDelayed() {
      if (this.provider.getProviderName().equals("DEFAULT") && this.breakEvent != null) {
         this.breakEvent.initDelayed();
      }
   }

   public boolean doWait() {
      return this.waiting;
   }

   private SpawnerProvider setup(SpawnerManager.Provider var1, boolean var2) {
      switch (var1) {
         case ULTIMATESTACKER:
         case SILKSPAWNERSV2:
         case ROSESTACKER:
         case MINEABLESPAWNERS:
         case SPAWNERMETA:
         case SPAWNERLEGACY:
         case SMARTSPAWNER:
         case EPICSPAWNERS:
         case UPGRADEABLE_SPAWNERS:
            if (this.plugin.getServer().getPluginManager().getPlugin(var1.getPluginName()) != null) {
               SendMessage.infoMessage(getLog(Lang.SPAWNER_PROVIDER_FOUND.get(), var1.getName()));
               if (this.plugin.getServer().getPluginManager().getPlugin(var1.getPluginName()).isEnabled()) {
                  this.provider = this.initProvider(var1, null);
                  SendMessage.infoMessage(getLog(Lang.USING_EXTERNAL_SPAWNERS.get(), var1.getName()));
               } else {
                  this.wait(var1, null);
               }
            } else if (!var2) {
               SendMessage.warnMessage(getLog(Lang.SPAWNER_PROVIDER_NOT_FOUND.get(), var1.getName()));
            }
            break;
         case WILDSTACKER:
            if (this.plugin.getServer().getPluginManager().getPlugin(var1.getPluginName()) != null) {
               SendMessage.infoMessage(getLog(Lang.SPAWNER_PROVIDER_FOUND.get(), var1.getName()));
               WildStacker var9 = WildStackerAPI.getWildStacker();
               if (var9 != null && this.plugin.getServer().getPluginManager().getPlugin(var1.getPluginName()).isEnabled()) {
                  this.provider = this.initProvider(var1, var9);
                  SendMessage.infoMessage(getLog(Lang.USING_EXTERNAL_SPAWNERS.get(), var1.getName()));
               } else {
                  this.wait(var1, var9);
               }
            } else if (!var2) {
               SendMessage.warnMessage(getLog(Lang.SPAWNER_PROVIDER_NOT_FOUND.get(), var1.getName()));
            }
            break;
         case SILKSPAWNERS:
            if (this.plugin.getServer().getPluginManager().getPlugin(var1.getPluginName()) != null) {
               SendMessage.infoMessage(getLog(Lang.SPAWNER_PROVIDER_FOUND.get(), var1.getName()));

               try {
                  if (this.plugin.getServer().getPluginManager().getPlugin(var1.getPluginName()).isEnabled()) {
                     this.provider = this.initProvider(var1, SilkUtil.hookIntoSilkSpanwers());
                     SendMessage.infoMessage(getLog(Lang.USING_EXTERNAL_SPAWNERS.get(), var1.getName()));
                  } else {
                     this.wait(var1, null);
                  }
               } catch (NoClassDefFoundError var8) {
                  try {
                     Class.forName("de.corneliusmay.silkspawners.plugin.SilkSpawners");
                     SendMessage.warnMessage("SilkSpawners_V1 not found but instead found SilkSpawners_V2, updating config setting to use SilkSpawnersV2...");
                     if (this.plugin.getServer().getPluginManager().getPlugin(SpawnerManager.Provider.SILKSPAWNERSV2.getPluginName()).isEnabled()) {
                        this.provider = this.initProvider(SpawnerManager.Provider.SILKSPAWNERSV2, null);
                        SendMessage.infoMessage(getLog(Lang.USING_EXTERNAL_SPAWNERS.get(), var1.getName()));
                     } else {
                        this.wait(var1, null);
                     }

                     ConfigManager.getConfig().set("spawner-provider", SpawnerManager.Provider.SILKSPAWNERSV2.name());
                     ConfigManager.saveConfig();
                  } catch (ClassNotFoundException var7) {
                     SendMessage.warnMessage(getLog(Lang.FAILED_SPAWNER_PROVIDER_INTEGRATION.get(), var1.getName()));
                  }
               }
            } else if (!var2) {
               SendMessage.warnMessage(getLog(Lang.SPAWNER_PROVIDER_NOT_FOUND.get(), var1.getName()));
            }
            break;
         case PURPUR:
            File var3 = new File(this.plugin.getDataFolder().getParentFile().getParentFile(), "purpur.yml");
            if (var3.exists() && ReflectUtil.isClassPresent("org.purpurmc.purpur.PurpurConfig")) {
               SendMessage.infoMessage(getLog(Lang.SPAWNER_PROVIDER_FOUND.get(), var1.getName()));
               YamlConfiguration var10 = OnyxShop.getInstance().loadConfiguration(var3, "purpur.yml");
               if (var10 != null && var10.getBoolean("world-settings.default.gameplay-mechanics.silk-touch.enabled", false)) {
                  this.provider = this.initProvider(var1, var10);
                  SendMessage.infoMessage(getLog(Lang.USING_EXTERNAL_SPAWNERS.get(), var1.getName()));
               } else {
                  SendMessage.warnMessage(getLog(Lang.FAILED_SPAWNER_PROVIDER_INTEGRATION.get(), var1.getName()));
               }
            } else if (!var2) {
               SendMessage.warnMessage(getLog(Lang.SPAWNER_PROVIDER_NOT_FOUND.get(), var1.getName()));
            }
            break;
         case DISABLED:
            this.provider = this.initProvider(var1, null);
            SendMessage.infoMessage(Lang.SPAWNER_PROVIDER_DISABLED.get());
            break;
         case DEFAULT:
            this.provider = this.initProvider(var1, null);
            SendMessage.infoMessage(Lang.USING_DEFAULT_SPAWNERS.get());
            break;
         case AUTO:
            SendMessage.infoMessage("Automatically searching for compatible spawner provider....");

            for (SpawnerManager.Provider var5 : Arrays.asList(
               SpawnerManager.Provider.UPGRADEABLE_SPAWNERS,
               SpawnerManager.Provider.EPICSPAWNERS,
               SpawnerManager.Provider.WILDSTACKER,
               SpawnerManager.Provider.ROSESTACKER,
               SpawnerManager.Provider.ULTIMATESTACKER,
               SpawnerManager.Provider.SILKSPAWNERSV2,
               SpawnerManager.Provider.SILKSPAWNERS,
               SpawnerManager.Provider.MINEABLESPAWNERS,
               SpawnerManager.Provider.SPAWNERMETA,
               SpawnerManager.Provider.SPAWNERLEGACY,
               SpawnerManager.Provider.SMARTSPAWNER,
               SpawnerManager.Provider.PURPUR
            )) {
               SpawnerProvider var6 = this.setup(var5, true);
               if (var6 != null || this.waiting) {
                  break;
               }
            }

            if (this.provider == null && !this.waiting) {
               SendMessage.infoMessage("Failed to automatically find compatible spawner provider, using default...");
               this.provider = this.initProvider(SpawnerManager.Provider.DEFAULT, null);
            }
      }

      return this.provider;
   }

   public SpawnerProvider getProvider() {
      return this.provider;
   }

   private static Translatable getLog(Translatable var0, String var1) {
      return var0.replace("%provider%", var1);
   }

   private void wait(SpawnerManager.Provider var1, @Nullable Object var2) {
      this.waiting = true;
      SendMessage.infoMessage(var1.getName() + " not enabled yet, waiting...");
      this.plugin.runTaskLater(() -> {
         if (this.plugin.getServer().getPluginManager().isPluginEnabled(var1.getPluginName())) {
            this.provider = this.initProvider(var1, var2);
            SendMessage.infoMessage(getLog(Lang.USING_EXTERNAL_SPAWNERS.get(), var1.getName()));
         } else {
            SendMessage.warnMessage(getLog(Lang.FAILED_SPAWNER_PROVIDER_INTEGRATION.get(), var1.getName()));
            this.provider = this.initProvider(SpawnerManager.Provider.DEFAULT, null);
            SendMessage.infoMessage(Lang.USING_DEFAULT_SPAWNERS.get());
         }

         this.waiting = false;
         Bukkit.getPluginManager().callEvent(new ProviderLoadEvent(DelayedLoadListener.Type.SPAWNERS));
      }, 1L);
   }

   public static Translatable getDefaultName(EntityType var0) {
      Translatable var1 = OnyxShop.getInstance().getSpawnerName(var0);
      if (var1 != null) {
         return var1;
      } else {
         String[] var2 = var0.name().toLowerCase().replace("_", " ").split(" ");
         StringBuffer var3 = new StringBuffer();

         for (int var4 = 0; var4 < var2.length; var4++) {
            var3.append(Character.toUpperCase(var2[var4].charAt(0))).append(var2[var4].substring(1)).append(" ");
         }

         return Lang.DEFAULT_SPAWNER_NAME.get().replace("%spawner-type%", var3.toString().trim());
      }
   }

   private SpawnerProvider initProvider(SpawnerManager.Provider var1, @Nullable Object var2) {
      switch (var1) {
         case ULTIMATESTACKER:
            return new UltimateStackerHook();
         case WILDSTACKER:
            return new WildStackerHook((WildStacker)var2);
         case SILKSPAWNERS:
            return new SilkSpawnersHook((SilkUtil)var2);
         case SILKSPAWNERSV2:
            return this.getSilkSpawnersV2Hook();
         case ROSESTACKER:
            return new RoseStackerHook();
         case MINEABLESPAWNERS:
            return new MineableSpawnersHook();
         case SPAWNERMETA:
            return new SpawnerMetaHook();
         case SPAWNERLEGACY:
            return this.getSpawnerLegacyHook();
         case SMARTSPAWNER:
            return new SmartSpawnerHook();
         case EPICSPAWNERS:
            return new EpicSpawnersHook(this.plugin);
         case UPGRADEABLE_SPAWNERS:
            return new UpgradeableSpawnersHook(this.plugin);
         case PURPUR:
            return new PurpurSpawnerHook((YamlConfiguration)var2);
         case DISABLED:
            return new DefaultSpawnerProvider(this.plugin, null, null, null);
         case DEFAULT:
            return new DefaultSpawnerProvider(
               this.plugin,
               !ConfigManager.getConfig().getBoolean("allow-spawner-type-change") ? (this.interactEvent = new SpawnerInteractEvent()) : null,
               ConfigManager.getConfig().getBoolean("enable-spawnerplace") ? (this.placeEvent = new SpawnerPlaceEvent(this.plugin)) : null,
               ConfigManager.getConfig().getBoolean("enable-spawnerbreak") ? (this.breakEvent = new SpawnerBreakEvent(this.plugin)) : null
            );
         default:
            return new DefaultSpawnerProvider(
               this.plugin,
               !ConfigManager.getConfig().getBoolean("allow-spawner-type-change") ? (this.interactEvent = new SpawnerInteractEvent()) : null,
               ConfigManager.getConfig().getBoolean("enable-spawnerplace") ? (this.placeEvent = new SpawnerPlaceEvent(this.plugin)) : null,
               ConfigManager.getConfig().getBoolean("enable-spawnerbreak") ? (this.breakEvent = new SpawnerBreakEvent(this.plugin)) : null
            );
      }
   }

   private SpawnerProvider getSpawnerLegacyHook() {
      try {
         Class.forName("mc.rellox.spawnerlegacyapi.price.IItemPrice");
         return new SpawnerLegacyHookV4();
      } catch (Exception var6) {
         try {
            Class.forName("mc.rellox.spawnerlegacyapi.SLAPI");
            return (SpawnerProvider)Class.forName("com.onyxshop.providers.spawners.SpawnerLegacyHookV3").newInstance();
         } catch (Exception var5) {
            try {
               Class.forName("mc.rellox.spawnerlegacy.lib.spawnerlegacyapi.SLAPI");
               return (SpawnerProvider)Class.forName("com.onyxshop.providers.spawners.SpawnerLegacyHookV2").newInstance();
            } catch (Exception var4) {
               try {
                  return (SpawnerProvider)Class.forName("com.onyxshop.providers.spawners.SpawnerLegacyHookV1").newInstance();
               } catch (Exception var3) {
                  SendMessage.errorMessage(getLog(Lang.FAILED_SPAWNER_PROVIDER_INTEGRATION.get(), "SpawnerLegacy"));
                  var3.printStackTrace();
                  return null;
               }
            }
         }
      }
   }

   private SpawnerProvider getSilkSpawnersV2Hook() {
      try {
         Class.forName("de.corneliusmay.silkspawners.api.SilkSpawnersAPI");
         return new SilkSpawnersV2HookV2();
      } catch (Exception var4) {
         try {
            return (SpawnerProvider)Class.forName("com.onyxshop.providers.spawners.SilkSpawnersV2HookV1").newInstance();
         } catch (Exception var3) {
            SendMessage.errorMessage(getLog(Lang.FAILED_SPAWNER_PROVIDER_INTEGRATION.get(), "SilkSpawners_v2"));
            var3.printStackTrace();
            return null;
         }
      }
   }

   private static enum Provider {
      ULTIMATESTACKER("UltimateStacker"),
      WILDSTACKER("WildStacker"),
      SILKSPAWNERS("SilkSpawners"),
      SILKSPAWNERSV2("SilkSpawners_v2"),
      ROSESTACKER("RoseStacker"),
      MINEABLESPAWNERS("MineableSpawners"),
      SPAWNERMETA("SpawnerMeta"),
      SPAWNERLEGACY("SpawnerLegacy"),
      SMARTSPAWNER("SmartSpawner"),
      EPICSPAWNERS("EpicSpawners"),
      UPGRADEABLE_SPAWNERS("UpgradeableSpawners"),
      PURPUR("PURPUR"),
      DISABLED("DISABLED"),
      DEFAULT("DEFAULT"),
      AUTO("AUTO");

      private final String name;

      private Provider(String nullxx) {
         this.name = nullxx;
      }

      public String getName() {
         return this.name;
      }

      public String getPluginName() {
         return this.name;
      }

      public static SpawnerManager.Provider getFromString(String var0) {
         try {
            SendMessage.infoMessage(SpawnerManager.getLog(Lang.SPAWNER_COMPATIBILITY_ENABLED.get(), var0));
            return valueOf(var0.toUpperCase(Locale.ENGLISH));
         } catch (IllegalArgumentException var2) {
            SendMessage.warnMessage("Failed to find a spawner provider using name '" + var0 + "'");
            return AUTO;
         }
      }
   }
}
