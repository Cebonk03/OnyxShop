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

   public SpawnerManager(OnyxShop plugin) {
      this.plugin = plugin;
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

      SpawnerManager.Provider provider = SpawnerManager.Provider.getFromString(ConfigManager.getConfig().getString("spawner-provider", "DEFAULT"));
      SpawnerProvider spawnerProvider = this.setup(provider, false);
      if (spawnerProvider == null && !this.waiting) {
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

   private SpawnerProvider setup(SpawnerManager.Provider provider, boolean flag) {
      switch (provider) {
         case ULTIMATESTACKER:
         case SILKSPAWNERSV2:
         case ROSESTACKER:
         case MINEABLESPAWNERS:
         case SPAWNERMETA:
         case SPAWNERLEGACY:
         case SMARTSPAWNER:
         case EPICSPAWNERS:
         case UPGRADEABLE_SPAWNERS:
            if (this.plugin.getServer().getPluginManager().getPlugin(provider.getPluginName()) != null) {
               SendMessage.infoMessage(getLog(Lang.SPAWNER_PROVIDER_FOUND.get(), provider.getName()));
               if (this.plugin.getServer().getPluginManager().getPlugin(provider.getPluginName()).isEnabled()) {
                  this.provider = this.initProvider(provider, null);
                  SendMessage.infoMessage(getLog(Lang.USING_EXTERNAL_SPAWNERS.get(), provider.getName()));
               } else {
                  this.wait(provider, null);
               }
            } else if (!flag) {
               SendMessage.warnMessage(getLog(Lang.SPAWNER_PROVIDER_NOT_FOUND.get(), provider.getName()));
            }
            break;
         case WILDSTACKER:
            if (this.plugin.getServer().getPluginManager().getPlugin(provider.getPluginName()) != null) {
               SendMessage.infoMessage(getLog(Lang.SPAWNER_PROVIDER_FOUND.get(), provider.getName()));
               WildStacker wildStacker = WildStackerAPI.getWildStacker();
               if (wildStacker != null && this.plugin.getServer().getPluginManager().getPlugin(provider.getPluginName()).isEnabled()) {
                  this.provider = this.initProvider(provider, wildStacker);
                  SendMessage.infoMessage(getLog(Lang.USING_EXTERNAL_SPAWNERS.get(), provider.getName()));
               } else {
                  this.wait(provider, wildStacker);
               }
            } else if (!flag) {
               SendMessage.warnMessage(getLog(Lang.SPAWNER_PROVIDER_NOT_FOUND.get(), provider.getName()));
            }
            break;
         case SILKSPAWNERS:
            if (this.plugin.getServer().getPluginManager().getPlugin(provider.getPluginName()) != null) {
               SendMessage.infoMessage(getLog(Lang.SPAWNER_PROVIDER_FOUND.get(), provider.getName()));

               try {
                  if (this.plugin.getServer().getPluginManager().getPlugin(provider.getPluginName()).isEnabled()) {
                     this.provider = this.initProvider(provider, SilkUtil.hookIntoSilkSpanwers());
                     SendMessage.infoMessage(getLog(Lang.USING_EXTERNAL_SPAWNERS.get(), provider.getName()));
                  } else {
                     this.wait(provider, null);
                  }
               } catch (NoClassDefFoundError noClassDefFoundError) {
                  try {
                     Class.forName("de.corneliusmay.silkspawners.plugin.SilkSpawners");
                     SendMessage.warnMessage("SilkSpawners_V1 not found but instead found SilkSpawners_V2, updating config setting to use SilkSpawnersV2...");
                     if (this.plugin.getServer().getPluginManager().getPlugin(SpawnerManager.Provider.SILKSPAWNERSV2.getPluginName()).isEnabled()) {
                        this.provider = this.initProvider(SpawnerManager.Provider.SILKSPAWNERSV2, null);
                        SendMessage.infoMessage(getLog(Lang.USING_EXTERNAL_SPAWNERS.get(), provider.getName()));
                     } else {
                        this.wait(provider, null);
                     }

                     ConfigManager.getConfig().set("spawner-provider", SpawnerManager.Provider.SILKSPAWNERSV2.name());
                     ConfigManager.saveConfig();
                  } catch (ClassNotFoundException exception) {
                     SendMessage.warnMessage(getLog(Lang.FAILED_SPAWNER_PROVIDER_INTEGRATION.get(), provider.getName()));
                  }
               }
            } else if (!flag) {
               SendMessage.warnMessage(getLog(Lang.SPAWNER_PROVIDER_NOT_FOUND.get(), provider.getName()));
            }
            break;
         case PURPUR:
            File file = new File(this.plugin.getDataFolder().getParentFile().getParentFile(), "purpur.yml");
            if (file.exists() && ReflectUtil.isClassPresent("org.purpurmc.purpur.PurpurConfig")) {
               SendMessage.infoMessage(getLog(Lang.SPAWNER_PROVIDER_FOUND.get(), provider.getName()));
               YamlConfiguration config = OnyxShop.getInstance().loadConfiguration(file, "purpur.yml");
               if (config != null && config.getBoolean("world-settings.default.gameplay-mechanics.silk-touch.enabled", false)) {
                  this.provider = this.initProvider(provider, config);
                  SendMessage.infoMessage(getLog(Lang.USING_EXTERNAL_SPAWNERS.get(), provider.getName()));
               } else {
                  SendMessage.warnMessage(getLog(Lang.FAILED_SPAWNER_PROVIDER_INTEGRATION.get(), provider.getName()));
               }
            } else if (!flag) {
               SendMessage.warnMessage(getLog(Lang.SPAWNER_PROVIDER_NOT_FOUND.get(), provider.getName()));
            }
            break;
         case DISABLED:
            this.provider = this.initProvider(provider, null);
            SendMessage.infoMessage(Lang.SPAWNER_PROVIDER_DISABLED.get());
            break;
         case DEFAULT:
            this.provider = this.initProvider(provider, null);
            SendMessage.infoMessage(Lang.USING_DEFAULT_SPAWNERS.get());
            break;
         case AUTO:
            SendMessage.infoMessage("Automatically searching for compatible spawner provider....");

            for (SpawnerManager.Provider provider2 : Arrays.asList(
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
               SpawnerProvider spawnerProvider = this.setup(provider2, true);
               if (spawnerProvider != null || this.waiting) {
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

   private static Translatable getLog(Translatable text, String text2) {
      return text.replace("%provider%", text2);
   }

   private void wait(SpawnerManager.Provider provider, @Nullable Object value) {
      this.waiting = true;
      SendMessage.infoMessage(provider.getName() + " not enabled yet, waiting...");
      this.plugin.runTaskLater(() -> {
         if (this.plugin.getServer().getPluginManager().isPluginEnabled(provider.getPluginName())) {
            this.provider = this.initProvider(provider, value);
            SendMessage.infoMessage(getLog(Lang.USING_EXTERNAL_SPAWNERS.get(), provider.getName()));
         } else {
            SendMessage.warnMessage(getLog(Lang.FAILED_SPAWNER_PROVIDER_INTEGRATION.get(), provider.getName()));
            this.provider = this.initProvider(SpawnerManager.Provider.DEFAULT, null);
            SendMessage.infoMessage(Lang.USING_DEFAULT_SPAWNERS.get());
         }

         this.waiting = false;
         Bukkit.getPluginManager().callEvent(new ProviderLoadEvent(DelayedLoadListener.Type.SPAWNERS));
      }, 1L);
   }

   public static Translatable getDefaultName(EntityType entityType) {
      Translatable text = OnyxShop.getInstance().getSpawnerName(entityType);
      if (text != null) {
         return text;
      } else {
         String[] names = entityType.name().toLowerCase().replace("_", " ").split(" ");
         StringBuffer stringBuffer = new StringBuffer();

         for (int index = 0; index < names.length; index++) {
            stringBuffer.append(Character.toUpperCase(names[index].charAt(0))).append(names[index].substring(1)).append(" ");
         }

         return Lang.DEFAULT_SPAWNER_NAME.get().replace("%spawner-type%", stringBuffer.toString().trim());
      }
   }

   private SpawnerProvider initProvider(SpawnerManager.Provider provider, @Nullable Object value) {
      switch (provider) {
         case ULTIMATESTACKER:
            return new UltimateStackerHook();
         case WILDSTACKER:
            return new WildStackerHook((WildStacker)value);
         case SILKSPAWNERS:
            return new SilkSpawnersHook((SilkUtil)value);
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
            return new PurpurSpawnerHook((YamlConfiguration)value);
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
      } catch (Exception exception) {
         try {
            Class.forName("mc.rellox.spawnerlegacyapi.SLAPI");
            return (SpawnerProvider)Class.forName("com.onyxshop.providers.spawners.SpawnerLegacyHookV3").newInstance();
         } catch (Exception exception2) {
            try {
               Class.forName("mc.rellox.spawnerlegacy.lib.spawnerlegacyapi.SLAPI");
               return (SpawnerProvider)Class.forName("com.onyxshop.providers.spawners.SpawnerLegacyHookV2").newInstance();
            } catch (Exception exception3) {
               try {
                  return (SpawnerProvider)Class.forName("com.onyxshop.providers.spawners.SpawnerLegacyHookV1").newInstance();
               } catch (Exception exception4) {
                  SendMessage.errorMessage(getLog(Lang.FAILED_SPAWNER_PROVIDER_INTEGRATION.get(), "SpawnerLegacy"));
                  exception4.printStackTrace();
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
      } catch (Exception exception) {
         try {
            return (SpawnerProvider)Class.forName("com.onyxshop.providers.spawners.SilkSpawnersV2HookV1").newInstance();
         } catch (Exception exception2) {
            SendMessage.errorMessage(getLog(Lang.FAILED_SPAWNER_PROVIDER_INTEGRATION.get(), "SilkSpawners_v2"));
            exception2.printStackTrace();
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

      private Provider(String text) {
         this.name = text;
      }

      public String getName() {
         return this.name;
      }

      public String getPluginName() {
         return this.name;
      }

      public static SpawnerManager.Provider getFromString(String text) {
         try {
            SendMessage.infoMessage(SpawnerManager.getLog(Lang.SPAWNER_COMPATIBILITY_ENABLED.get(), text));
            return valueOf(text.toUpperCase(Locale.ENGLISH));
         } catch (IllegalArgumentException exception) {
            SendMessage.warnMessage("Failed to find a spawner provider using name '" + text + "'");
            return AUTO;
         }
      }
   }
}
