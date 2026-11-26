package com.onyxshop.methodes;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.json.simple.parser.ParseException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.config.Config;
import com.onyxshop.objects.inventorys.AuthRequest;
import com.onyxshop.objects.inventorys.CustomizeLayout;
import com.onyxshop.objects.inventorys.FileUploader;
import com.onyxshop.objects.inventorys.UpdateMessage;
import com.onyxshop.objects.layouts.SimpleCard;
import com.onyxshop.objects.layouts.SimpleLayout;
import com.onyxshop.util.ConfirmRequest;
import com.onyxshop.util.SimplePair;
import com.onyxshop.util.TimeUtils;
import com.onyxshop.util.exceptions.ConfigLoadException;
import com.onyxshop.util.exceptions.ConfigSaveException;
import com.onyxshop.util.exceptions.LayoutNotFoundException;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class MarketplaceIntegration {
   private final OnyxShop plugin = OnyxShop.getInstance();
   private static long token = new SecureRandom().nextLong();
   private final File dir = new File(this.plugin.getDataFolder() + "/layouts");
   public static String i;

   public static String getServer() {
      return ConfigManager.getConfig().getString("cloud-features.endpoint", "");
   }
   public static HashSet<String> bannedOptions = new HashSet<>(
      Arrays.asList(
         "update-checker",
         "cloud-features",
         "metrics",
         "storage-type",
         "mysql",
         "postgresql",
         "debug",
         "use-nms",
         "transaction-log",
         "advanced-transaction-log",
         "placeholders",
         "enable-disabled-worlds",
         "disabled-worlds-per-command",
         "economy-provider",
         "spawner-provider",
         "enable-discordsrv-hook",
         "discordsrv-transactions",
         "log-player-transactions",
         "drop-remaining-items-on-ground",
         "sell-shulker-boxes",
         "banned-gamemodes",
         "override-sell-command",
         "old-sellall-command",
         "commands",
         "enable-spawnerbreak",
         "enable-spawnerplace",
         "allow-spawner-type-change",
         "player-placed-spawners-drop-exp",
         "only-mine-plugin-spawners",
         "spawner-break-tools"
      )
   );
   public static HashSet<String> listOptions = new HashSet<>(Arrays.asList("shop-commands", "sellall-commands", "sellgui-commands"));

   public MarketplaceIntegration() {
      if (!this.dir.exists()) {
         this.dir.mkdirs();
      }
   }

   public void startUpload(Player var1) {
      if (!ConfigManager.getConfig().getBoolean("cloud-features.enabled", false)) {
         SendMessage.errorMessage(var1, Lang.CLOUD_DISABLED.get(var1));
         return;
      }

      new CustomizeLayout(var1, var2 -> {
         var2.setToken(token);
         new FileUploader(var1, var3 -> {
            if (var3 == null) {
               var1.sendMessage(ChatColor.RED + "Cancelled upload process");
            } else {
               new AuthRequest(var1, var4 -> {
                  var2.setAuthMethod(var4);
                  this.requestCard(var1, var2, var5 -> this.plugin.runTask(() -> {
                     Runnable var6 = () -> {
                        if (var2.getAuthMethod() == AuthRequest.AuthMethod.DISCORD) {
                           SendMessage.chatToPlayer(var1, ChatColor.GREEN + "Validation success, started upload process");
                        } else {
                           SendMessage.chatToPlayer(var1, ChatColor.GREEN + "Started upload process");
                        }

                        this.startUpload(var1, var3, var5, null, null, MarketplaceIntegration.UploadReason.CREATE);
                     };
                     switch (var4) {
                        case TEMP:
                           var6.run();
                           break;
                        case DISCORD:
                           var1.closeInventory();
                           var1.sendTitle("§c§lValidation required", "§6§lClick the link in chat to authorize", 1, 80, 1);
                           TextComponent var7 = new TextComponent(TextComponent.fromLegacyText(Lang.DISCORD_VALIDATION_REQUIRED.get().getLegacy()));
                           var7.setClickEvent(new ClickEvent(Action.OPEN_URL, this.getAuthorizeURL(var5)));
                           SendMessage.chatToPlayer(var1, var7);
                           this.checkVerification(var1, var5, 6000L, var6);
                           break;
                        default:
                           SendMessage.chatToPlayer(var1, ChatColor.RED + "Cancelled upload process");
                     }
                  }));
               });
            }
         });
      });
   }

   private void requestCard(Player var1, SimpleCard var2, Consumer<String> var3) {
      this.plugin.runKillableTask(() -> {
         String var4;
         try {
            var4 = this.sendRequest(getServer() + "/createLayout?token=" + token, var2.toString());
         } catch (Exception var7) {
            SendMessage.chatToPlayer(var1, ChatColor.RED + "Unable to reach API server at this point, see console for more details.");
            var7.printStackTrace();
            return;
         }

         try {
            UUID.fromString(var4);
         } catch (IllegalArgumentException var6) {
            SendMessage.chatToPlayer(var1, ChatColor.RED + "Failed to authorize with server, see console for details.");
            var6.printStackTrace();
            return;
         }

         var3.accept(var4);
      });
   }

   private void startUpload(Player var1, ArrayList<String> var2, String var3, String var4, String var5, MarketplaceIntegration.UploadReason var6) {
      SendMessage.chatToPlayer(var1, ChatColor.GREEN + "Started upload process, zipping files...");
      this.plugin
         .runKillableTask(
            () -> {
               Path var7;
               try {
                  var7 = this.createZipFile();
                  FileOutputStream var8 = new FileOutputStream(var7.toString());
                  ZipOutputStream var9 = new ZipOutputStream(var8);
                  this.zipLayout(var2, var9, true);
                  var9.close();
                  var8.close();
               } catch (IOException var11) {
                  SendMessage.chatToPlayer(var1, ChatColor.RED + "Failed to create a copy of the current shop layout, see console for details.");
                  var11.printStackTrace();
                  return;
               }

               SendMessage.chatToPlayer(var1, ChatColor.GREEN + "Successfully compressed layout, uploading to server...");
               SendMessage.chatToPlayer(
                  var1, ChatColor.GREEN + "This may take some time, you can continue other things while this process runs in the background..."
               );

               try {
                  int var12 = var6 == MarketplaceIntegration.UploadReason.CREATE ? this.upload(var7, var3) : this.update(var7, var3, var4, var5);
                  switch (var12) {
                     case 202:
                        this.checkStatus(
                           var1,
                           var3,
                           6000L,
                           var2xx -> {
                              if (var2xx.success) {
                                 SendMessage.chatToPlayer(
                                    var1,
                                    ChatColor.GREEN
                                       + "Your shop layout was uploaded successfully, layout id: "
                                       + ChatColor.WHITE
                                       + var3
                                 );
                              } else if (var2xx.reason.equals("deniedBadQuality")) {
                                 SendMessage.chatToPlayer(
                                    var1,
                                    ChatColor.RED
                                       + "Your shop layout with id "
                                       + var3
                                       + " was denied for low quality.\n"
                                       + ChatColor.RED
                                       + "The layout was denied for low quality."
                                 );
                                 SendMessage.warnMessage("Shop layout " + var3 + " was denied for low quality");
                              } else {
                                 SendMessage.chatToPlayer(
                                    var1,
                                    ChatColor.RED
                                       + "A error occurred while processing your shop layout, please try again soon or if this keeps happening, contact the developers."
                                 );
                                 SendMessage.warnMessage("A error occurred while uploading shop layout " + var3 + " with reason: " + var2xx.reason);
                              }

                              token = new SecureRandom().nextLong();
                           }
                        );
                        break;
                     case 406:
                     case 415:
                        SendMessage.chatToPlayer(
                           var1, ChatColor.RED + "The shop layout was denied by the server, please make sure the files contain no extra content as needed."
                        );
                        SendMessage.errorMessage("Server responded with status code '" + var12 + "'");
                        token = new SecureRandom().nextLong();
                        break;
                     default:
                        SendMessage.chatToPlayer(var1, ChatColor.RED + "The server failed to process your request at this time, see console for details.");
                        SendMessage.errorMessage("Server responded with status code '" + var12 + "'");
                        token = new SecureRandom().nextLong();
                  }
               } catch (IOException var10) {
                  SendMessage.chatToPlayer(var1, ChatColor.RED + "Error occurred while uploading layout, see console for details.");
                  token = new SecureRandom().nextLong();
                  var10.printStackTrace();
               }
            }
         );
   }

   public void startUpdate(Player var1, String var2) {
      if (!ConfigManager.getConfig().getBoolean("cloud-features.enabled", false)) {
         SendMessage.errorMessage(var1, Lang.CLOUD_DISABLED.get(var1));
         return;
      }

      this.plugin
         .runKillableTask(
            () -> {
               SimplePair var3;
               try {
                  var3 = this.getLayout(var2, "/createUpdate");
               } catch (LayoutNotFoundException var5) {
                  SendMessage.chatToPlayer(
                     var1,
                     ChatColor.RED
                        + "Failed to find a layout with ID "
                        + var2
                        + ", make sure the ID is spelled correct and the layout still exists. Keep in mind that temporary layouts are removed after 14 days."
                  );
                  return;
               } catch (NullPointerException var6) {
                  SendMessage.chatToPlayer(
                     var1, ChatColor.RED + "Failed to fetch layout from server at this time, please try again soon or see console for more details."
                  );
                  SendMessage.warnMessage(var6.getMessage());
                  var6.printStackTrace();
                  return;
               } catch (Exception var7) {
                  SendMessage.chatToPlayer(var1, ChatColor.RED + "Failed to fetch layout from server, see console for more details.");
                  var7.printStackTrace();
                  return;
               }

               if (!((SimpleLayout)var3.key).authMethod.equals("DISCORD")) {
                  SendMessage.chatToPlayer(
                     var1, ChatColor.RED + "Given layout is not uploaded using discord validation, it is not possible to update this layout!"
                  );
               } else {
                  var1.sendTitle("§c§lValidation required", "§6§lClick the link in chat to authorize", 1, 80, 1);
                  TextComponent var4 = new TextComponent(TextComponent.fromLegacyText(Lang.DISCORD_VALIDATION_REQUIRED.get().getLegacy()));
                  var4.setClickEvent(new ClickEvent(Action.OPEN_URL, this.getAuthorizeURL(var2)));
                  SendMessage.chatToPlayer(var1, var4);
                  this.checkVerification(
                     var1,
                     var2,
                     6000L,
                     () -> {
                        SendMessage.chatToPlayer(var1, ChatColor.GREEN + "Verification success!");
                        new UpdateMessage(
                           var1,
                           var4x -> new FileUploader(
                              var1,
                              var5x -> {
                                 var1.closeInventory();
                                 if (var5x == null) {
                                    var1.sendMessage(ChatColor.RED + "Cancelled update process");
                                 } else {
                                    SendMessage.chatToPlayer(
                                       var1,
                                       ChatColor.translateAlternateColorCodes(
                                          '&',
                                          "&aYou are about to update layout &c%layoutID% &awith data: \n&6Title: &e%title% \n&6Description: &e%description% \n&6Tags: \n&8- &e%tags% \n&6Compatible MC version(s): &e%mcVer% \n&6Created for plugin version: &e%plVer% \n&6Created at: &e%creationDate% \n&6Downloads: &e%downloads% \n&6Shop files: \n&8- &e%files%"
                                             .replace("%layoutID%", var2)
                                             .replace("%title%", ((SimpleLayout)var3.key).title)
                                             .replace("%description%", ((SimpleLayout)var3.key).description)
                                             .replace("%tags%", String.join("\n&8- &e", ((SimpleLayout)var3.key).tags))
                                             .replace("%files%", String.join("\n&8- &e", ((SimpleLayout)var3.key).files))
                                             .replace("%creationDate%", TimeUtils.getDate(((SimpleLayout)var3.key).creation))
                                             .replace("%downloads%", ((SimpleLayout)var3.key).downloads + "")
                                             .replace("%mcVer%", ((SimpleLayout)var3.key).mcVer + "")
                                             .replace("%plVer%", ((SimpleLayout)var3.key).plVer + "")
                                       )
                                    );
                                    new ConfirmRequest(
                                       this.plugin,
                                       ChatColor.RED
                                          + "Are you sure you want to continue? This will override the layout and update it with the current installed shop layout.\n"
                                          + ChatColor.GREEN
                                          + "Enter 'yes' or 'no' in chat to continue.",
                                       var1,
                                       var6x -> this.plugin.runKillableTask(() -> {
                                          if (var6x) {
                                             this.startUpload(var1, var5x, var2, (String)var3.value, var4x, MarketplaceIntegration.UploadReason.UPDATE);
                                          } else {
                                             SendMessage.chatToPlayer(var1, ChatColor.RED + "Cancelled update process");
                                          }
                                       })
                                    );
                                 }
                              }
                           )
                        );
                     }
                  );
               }
            }
         );
   }

   public void downloadLayout(Player var1, String var2, boolean var3) {
      if (!ConfigManager.getConfig().getBoolean("cloud-features.enabled", false)) {
         SendMessage.errorMessage(var1, Lang.CLOUD_DISABLED.get(var1));
         return;
      }

      SendMessage.chatToPlayer(var1, ChatColor.GREEN + "Started install process, retrieving layout from server...");
      this.plugin
         .runKillableTask(
            () -> {
               SimplePair var4;
               try {
                  var4 = this.getLayout(var2, "/getLayout");
               } catch (LayoutNotFoundException var6) {
                  SendMessage.chatToPlayer(
                     var1,
                     ChatColor.RED
                        + "Failed to find a layout with ID "
                        + var2
                        + ", make sure the ID is spelled correct and the layout still exists. Keep in mind that temporary layouts are removed after 14 days."
                  );
                  return;
               } catch (NullPointerException var7) {
                  SendMessage.chatToPlayer(
                     var1, ChatColor.RED + "Failed to fetch layout from server at this time, please try again soon or see console for more details."
                  );
                  SendMessage.warnMessage(var7.getMessage());
                  var7.printStackTrace();
                  return;
               } catch (Exception var8) {
                  SendMessage.chatToPlayer(var1, ChatColor.RED + "Failed to fetch layout from server, see console for more details.");
                  var8.printStackTrace();
                  return;
               }

               SendMessage.chatToPlayer(
                  var1,
                  ChatColor.translateAlternateColorCodes(
                     '&',
                     "&aYou are about to install layout &c%layoutID% &awith data: \n&6Title: &e%title% \n&6Description: &e%description% \n&6Tags: \n&8- &e%tags% \n&6Compatible MC version(s): &e%mcVer% \n&6Created for plugin version: &e%plVer% \n&6Created at: &e%creationDate% \n&6Downloads: &e%downloads% \n&6Shop files: \n&8- &e%files%"
                        .replace("%layoutID%", var2)
                        .replace("%title%", ((SimpleLayout)var4.key).title)
                        .replace("%description%", ((SimpleLayout)var4.key).description)
                        .replace("%tags%", String.join("\n&8- &e", ((SimpleLayout)var4.key).tags))
                        .replace("%files%", String.join("\n&8- &e", ((SimpleLayout)var4.key).files))
                        .replace("%creationDate%", TimeUtils.getDate(((SimpleLayout)var4.key).creation))
                        .replace("%downloads%", ((SimpleLayout)var4.key).downloads + "")
                        .replace("%mcVer%", ((SimpleLayout)var4.key).mcVer + "")
                        .replace("%plVer%", ((SimpleLayout)var4.key).plVer + "")
                  )
               );
               new ConfirmRequest(
                  this.plugin,
                  ChatColor.RED
                     + "Are you sure you want to continue? This will override the current shop layout.\n"
                     + ChatColor.GREEN
                     + "Enter 'yes' or 'no' in chat.",
                  var1,
                  var5 -> this.plugin.runKillableTask(() -> {
                     if (var5) {
                        File var6x = this.createLayout(var2);
                        Runnable var7x = () -> {
                           if (!var6x.exists()) {
                              SendMessage.chatToPlayer(var1, ChatColor.RED + "Failed to download layout from server, see console for more details.");
                           } else {
                              this.installLayout(var1, (SimpleLayout)var4.key, var6x, var3);
                           }
                        };
                        if (((SimpleLayout)var4.key).requiresAuthorization) {
                           var1.sendTitle("§c§lValidation required", "§6§lClick the link in chat to authorize", 1, 80, 1);
                           TextComponent var8x = new TextComponent(TextComponent.fromLegacyText(Lang.DISCORD_VALIDATION_REQUIRED.get().getLegacy()));
                           var8x.setClickEvent(new ClickEvent(Action.OPEN_URL, this.getAuthorizeURL((String)var4.value)));
                           SendMessage.chatToPlayer(var1, var8x);
                           this.checkVerification(var1, var2, 6000L, () -> {
                              SendMessage.chatToPlayer(var1, ChatColor.GREEN + "Verification success, downloading layout...");
                              this.downloadFile(var2, (String)var4.value, var6x, var7x);
                           });
                        } else {
                           SendMessage.chatToPlayer(var1, ChatColor.GREEN + "Downloading layout...");
                           this.downloadFile(var2, (String)var4.value, var6x, var7x);
                        }
                     } else {
                        SendMessage.chatToPlayer(var1, ChatColor.RED + "Cancelled install process");
                     }
                  })
               );
            }
         );
   }

   private void installLayout(Player var1, SimpleLayout var2, File var3, boolean var4) {
      SendMessage.chatToPlayer(var1, ChatColor.GREEN + "Successfully downloaded layout, extracting files...");
      this.backupCurrentLayout(var1, var2.files.contains("config.yml") && var4);
      boolean var5 = true;

      try {
         boolean var6 = false;
         ZipInputStream var7 = new ZipInputStream(new FileInputStream(var3));

         for (ZipEntry var8 = var7.getNextEntry(); var8 != null; var8 = var7.getNextEntry()) {
            String var9 = var8.getName();
            if (var9.equals("config.yml")) {
               if (!var4) {
                  var9 = "temp-" + var9;
               }

               var6 = true;
            }

            File var10 = new File(this.plugin.getDataFolder(), var9.replace("/", File.separator));
            if (var10.getParentFile() != null && !var10.getParentFile().exists()) {
               var10.getParentFile().mkdirs();
            }

            FileOutputStream var11 = new FileOutputStream(var10);
            byte[] var13 = new byte[1024];

            int var12;
            while ((var12 = var7.read(var13)) > 0) {
               var11.write(var13, 0, var12);
            }

            var11.close();
         }

         var7.closeEntry();
         var7.close();
         if (var2.files.contains("config.yml") && !var4) {
            try {
               File var20 = new File(this.plugin.getDataFolder(), "temp-config.yml");
               Config var22 = new Config(var20);

               for (String var26 : var22.getKeys(false)) {
                  if (!bannedOptions.contains(var26)) {
                     if (listOptions.contains(var26) && ConfigManager.getConfig().contains(var26)) {
                        this.addValues(ConfigManager.getConfig(), var22, var26);
                     } else {
                        ConfigManager.getConfig().set(var26, var22.get(var26));
                     }
                  }
               }

               ConfigManager.saveConfig();
               Files.delete(var20.toPath());
            } catch (ConfigLoadException var17) {
               SendMessage.chatToPlayer(
                  var1, ChatColor.RED + "Invalid config.yml format for downloaded layout, try to download the layout again or report this layout as broken."
               );
               SendMessage.warnMessage("A error occurred while reading the config.yml of downloaded shop layout at " + var3.getAbsolutePath());
               var17.printStackTrace();
               var5 = false;
            } catch (IOException var18) {
               SendMessage.chatToPlayer(var1, ChatColor.RED + "Failed to remove temp config from plugin folder, it may be removed manually.");
               SendMessage.warnMessage("A error occurred while trying to remove temp config at " + var3.getAbsolutePath());
               var18.printStackTrace();
               var5 = false;
            }
         }

         UpdateChecker.Version var21 = new UpdateChecker.Version(var2.requiresAuthorization ? "5.20.2" : "6.10.3");
         if (!var4
            && new UpdateChecker.Version(this.plugin.getDescription().getVersion()).isGreater(new UpdateChecker.Version("5.20.2"))
            && new UpdateChecker.Version(var2.plVer).isSmaller(var21)) {
            SendMessage.logDebugMessage("Updating section files from downloaded layout...");
            if (!var2.files.contains("config.yml")) {
               for (String var25 : var2.files) {
                  if (!var25.equals("config.yml")) {
                     try {
                        File var27 = new File(this.plugin.getDataFolder(), "sections" + File.separator + var25);
                        Config var28 = new Config(var27);
                        String var14 = var28.getString("slot");
                        if (var14 != null) {
                           ArrayList<Integer> var15 = this.plugin.calculateAmount.getSlots(var14);
                           if (var15.size() == 1) {
                              var28.set("slot", (Integer)var15.get(0) + 2);
                           } else {
                              var28.set("slot", var15.stream().map(var0 -> String.valueOf(var0 + 2)).collect(Collectors.joining(",")));
                           }
                        }

                        var28.save();
                     } catch (ConfigSaveException | ConfigLoadException var16) {
                        SendMessage.chatToPlayer(
                           var1, ChatColor.RED + "Failed to update section config '" + var25 + "', download this layout again or report this layout as broken."
                        );
                        SendMessage.warnMessage(
                           "A error occurred while reading the " + var25 + " config of downloaded shop layout at " + var3.getAbsolutePath()
                        );
                        var16.printStackTrace();
                        var5 = false;
                     }
                  }
               }
            } else if (!this.plugin.getConfigManager().reload()) {
               SendMessage.chatToPlayer(var1, ChatColor.RED + "Failed to reload configs, download this layout again or report this layout as broken.");
               SendMessage.warnMessage("A error occurred while reloading the configuration files of downloaded shop layout at " + var3.getAbsolutePath());
               var5 = false;
            }
         }

         if (var5) {
            if (var6) {
               SendMessage.chatToPlayer(var1, ChatColor.GREEN + "Successfully installed shop layout, a server restart is recommended to load all changes.");
            } else {
               SendMessage.chatToPlayer(var1, ChatColor.GREEN + "Successfully installed shop layout, use command /sreload to reload all changes.");
            }
         }

         token = new SecureRandom().nextLong();
      } catch (IOException var19) {
         SendMessage.chatToPlayer(var1, ChatColor.RED + "A unknown error occurred while extracting the downloaded file, see console for details.");
         SendMessage.warnMessage("A unknown error occurred while extracting shop layout from " + var3.getAbsolutePath());
         var19.printStackTrace();
      }
   }

   private void addValues(Config var1, Config var2, String var3) {
      Object var4 = var1.get(var3);
      Object var5 = var2.get(var3);
      if (var4 instanceof List && var5 instanceof List) {
         for (Object var9 : (List)var5) {
            if (!((List)var4).contains(var9)) {
               ((List)var4).add(var9);
            }
         }

         var1.set(var3, var4);
      } else if (var4 instanceof ConfigurationSection && var5 instanceof ConfigurationSection) {
         for (String var7 : ((ConfigurationSection)var5).getKeys(false)) {
            ((ConfigurationSection)var4).set(var7, ((ConfigurationSection)var5).get(var7));
         }

         var1.set(var3, var4);
      }
   }

   private void backupCurrentLayout(Player var1, boolean var2) {
      ArrayList<String> var3 = new ArrayList<>(Collections.singletonList("config.yml"));
      this.listFiles(new File(this.plugin.getDataFolder() + File.separator + "shops"), true).forEach(var1x -> var3.add("shops/" + var1x));
      this.listFiles(new File(this.plugin.getDataFolder() + File.separator + "sections"), true).forEach(var1x -> var3.add("sections/" + var1x));

      try {
         Path var4 = this.createZipFile();
         SendMessage.chatToPlayer(var1, ChatColor.GREEN + "Saving backup from current layout as layouts/" + var4.getFileName());
         FileOutputStream var5 = new FileOutputStream(var4.toString());
         ZipOutputStream var6 = new ZipOutputStream(var5);
         this.zipLayout(var3, var6, false);
         var6.close();
         var5.close();
      } catch (IOException var7) {
         SendMessage.chatToPlayer(var1, ChatColor.RED + "Failed to create a backup of the current shop layout, see console for details.");
         var7.printStackTrace();
      }

      if (!var2) {
         var3.remove("config.yml");
         var3.add("temp-config.yml");
      }

      var3.forEach(var1x -> {
         File var2x = new File(this.plugin.getDataFolder(), var1x.replace("/", File.separator));

         try {
            Files.delete(var2x.toPath());
         } catch (IOException var4x) {
         }
      });
      Arrays.stream(new File(this.plugin.getDataFolder() + File.separator + "shops").listFiles()).collect(Collectors.toList()).forEach(File::delete);
      Arrays.stream(new File(this.plugin.getDataFolder() + File.separator + "sections").listFiles()).collect(Collectors.toList()).forEach(File::delete);
   }

   private File createLayout(String var1) {
      String var2 = this.plugin.getDataFolder() + File.separator + "layouts" + File.separator + "downloads" + File.separator + var1;
      File var3 = new File(var2).getParentFile();
      if (!var3.exists() || var3.isFile()) {
         var3.mkdirs();
      }

      int var4 = 0;

      while (new File(var2 + (var4 > 0 ? "(" + var4 + ")" : "")).exists()) {
         var4++;
      }

      return Paths.get(var2 + (var4 > 0 ? "(" + var4 + ")" : "") + ".zip").toFile();
   }

   private String sendRequest(String var1, String var2) throws MalformedURLException , IOException, UnsupportedEncodingException{
      HttpURLConnection var3 = (HttpURLConnection)new URL(var1).openConnection();
      var3.setConnectTimeout(5000);
      var3.setDoOutput(true);
      var3.setRequestProperty("token", String.valueOf(token));
      var3.setRequestProperty("Content-Type", "application/json");
      OutputStream var4 = var3.getOutputStream();
      OutputStreamWriter var5 = new OutputStreamWriter(var4, "UTF-8");
      var5.write(var2);
      var5.flush();
      var5.close();
      var4.close();
      var3.connect();
      return new BufferedReader(new InputStreamReader(var3.getInputStream())).readLine();
   }

   private SimplePair<SimpleLayout, String> getLayout(String var1, String var2) throws MalformedURLException, IOException, LayoutNotFoundException, ParseException {
      HttpURLConnection var3 = (HttpURLConnection)new URL(getServer() + var2 + "?layout=" + var1).openConnection();
      var3.setConnectTimeout(5000);
      var3.setRequestProperty("token", String.valueOf(token));
      var3.connect();
      String var4 = new BufferedReader(new InputStreamReader(var3.getInputStream())).readLine();
      switch (var3.getResponseCode()) {
         case 200:
            SimpleLayout var5 = new SimpleLayout(var4);
            return new SimplePair<>(var5, var3.getHeaderField("token"));
         case 400:
            throw new LayoutNotFoundException();
         default:
            throw new NullPointerException("Server responded with " + var3.getResponseCode() + ": " + var4);
      }
   }

   private String getAuthorizeURL(String var1) {
      return getServer() + "/authorize?token=" + token + "&layout=" + var1;
   }

   private void checkVerification(Player var1, String var2, long var3, Runnable var5) {
      long var6 = var3 - 100L;
      this.plugin.runKillableTaskLater(() -> {
         try {
            if (((HttpURLConnection)new URL(getServer() + "/check?token=" + token + "&layout=" + var2).openConnection()).getResponseCode() == 202) {
               var5.run();
            } else if (var6 != 0L) {
               this.checkVerification(var1, var2, var6, var5);
            } else {
               SendMessage.chatToPlayer(var1, Lang.VALIDATION_EXPIRED.get());
            }
         } catch (IOException var7) {
            var7.printStackTrace();
         }
      }, 100L);
   }

   private void checkStatus(Player var1, String var2, long var3, Consumer<MarketplaceIntegration.UploadResponse> var5) {
      long var8;
      long var6 = var8 = var3 - 100L;
      this.plugin
         .runKillableTaskLater(
            () -> {
               HttpURLConnection var6x = null;

               try {
                  var6x = (HttpURLConnection)new URL(getServer() + "/getStatus?token=" + token + "&layout=" + var2).openConnection();
                  int var7 = var6x.getResponseCode();
                  if (var7 == 200) {
                     var5.accept(new MarketplaceIntegration.UploadResponse(true));
                  } else if (var7 == 422) {
                     JsonObject var8x = (JsonObject)new Gson()
                        .fromJson(new BufferedReader(new InputStreamReader(var6x.getErrorStream())).readLine(), JsonObject.class);
                     var5.accept(new MarketplaceIntegration.UploadResponse(false, var8x.get("reason").getAsString()));
                  } else if (var6 != 0L) {
                     this.checkStatus(var1, var2, var6, var5);
                  } else {
                     SendMessage.chatToPlayer(var1, ChatColor.RED + "A timeout occurred while uploading layout " + var2 + ", please try again soon.");
                  }
               } catch (IOException var12) {
                  var12.printStackTrace();
               } finally {
                  var6x.disconnect();
               }
            },
            100L
         );
   }

   private Path createZipFile() {
      DateTimeFormatter var1 = DateTimeFormatter.ofPattern("MM-dd-yyyy");
      LocalDateTime var2 = LocalDateTime.now();
      String var3 = this.plugin.getDataFolder() + File.separator + "layouts" + File.separator + "layout - " + var1.format(var2);
      int var4 = 0;

      while (new File(var3 + (var4 > 0 ? "(" + var4 + ")" : "")).exists()) {
         var4++;
      }

      return Paths.get(var3 + (var4 > 0 ? "(" + var4 + ")" : "") + ".zip");
   }

   private void zipLayout(ArrayList<String> var1, ZipOutputStream var2, boolean var2x) throws FileNotFoundException , IOException{
      File var3 = this.plugin.getDataFolder();
      if (var3.exists() && var3.isDirectory()) {
         for (String var5 : var1) {
            File var6 = new File(var3, var5.replace("/", File.separator));
            if (!var6.isDirectory()) {
               var2.putNextEntry(new ZipEntry(var5));
               if (var2x && var5.equals("config.yml")) {
                  var2.write(this.stripTelemetry(new FileInputStream(var6)));
               } else {
                  FileInputStream var7 = new FileInputStream(var6);
                  byte[] var8 = new byte[1024];

                  int var9;
                  while ((var9 = var7.read(var8)) >= 0) {
                     var2.write(var8, 0, var9);
                  }

                  var7.close();
               }
            }
         }
      }
   }

   // Uploaded layouts share config.yml with other servers: never include the local endpoints,
   // download tokens or bStats id in that file.
   private byte[] stripTelemetry(FileInputStream var1) throws IOException {
      ByteArrayOutputStream var2 = new ByteArrayOutputStream();
      byte[] var3 = new byte[1024];

      int var4;
      while ((var4 = var1.read(var3)) >= 0) {
         var2.write(var3, 0, var4);
      }

      var1.close();
      String var5 = new String(var2.toByteArray(), StandardCharsets.UTF_8);
      String var6 = var5.contains("\r\n") ? "\r\n" : "\n";
      StringBuilder var7 = new StringBuilder();
      boolean var8 = false;

      for (String var9 : var5.split(var6, -1)) {
         if (!var9.isEmpty() && !Character.isWhitespace(var9.charAt(0)) && !var9.startsWith("#")) {
            var8 = var9.startsWith("update-checker:") || var9.startsWith("cloud-features:") || var9.startsWith("metrics:");
         }

         if (!var8) {
            var7.append(var9).append(var6);
         }
      }

      return var7.toString().getBytes(StandardCharsets.UTF_8);
   }

   private int upload(Path var1, String var2) throws MalformedURLException , IOException, ProtocolException{
      URL var3 = new URL(getServer() + "/uploadLayout?layout=" + var2);
      HttpURLConnection var4 = (HttpURLConnection)var3.openConnection();
      var4.setDoOutput(true);
      var4.setRequestMethod("POST");
      var4.setRequestProperty("token", String.valueOf(token));
      var4.setRequestProperty("Content-Type", "application/zip");
      OutputStream var5 = var4.getOutputStream();

      try {
         FileInputStream var6 = new FileInputStream(new File(var1.toUri()));

         try {
            byte[] var7 = new byte[4096];

            int var8;
            while ((var8 = var6.read(var7)) != -1) {
               var5.write(var7, 0, var8);
            }

            var5.flush();
         } catch (Throwable var11) {
            try {
               var6.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }

            throw var11;
         }

         var6.close();
      } catch (Throwable var12) {
         if (var5 != null) {
            try {
               var5.close();
            } catch (Throwable var9) {
               var12.addSuppressed(var9);
            }
         }

         throw var12;
      }

      if (var5 != null) {
         var5.close();
      }

      int var13 = var4.getResponseCode();
      var4.disconnect();
      return var13;
   }

   private int update(Path var1, String var2, String var3, String var4) throws MalformedURLException , IOException, ProtocolException{
      URL var5 = new URL(getServer() + "/updateLayout?layout=" + var2 + "&token=" + token);
      HttpURLConnection var6 = (HttpURLConnection)var5.openConnection();
      var6.setDoOutput(true);
      var6.setRequestMethod("POST");
      var6.setRequestProperty("token", var3);
      var6.setRequestProperty("msg", var4);
      var6.setRequestProperty("Content-Type", "application/zip");
      OutputStream var7 = var6.getOutputStream();

      try {
         FileInputStream var8 = new FileInputStream(new File(var1.toUri()));

         try {
            byte[] var9 = new byte[4096];

            int var10;
            while ((var10 = var8.read(var9)) != -1) {
               var7.write(var9, 0, var10);
            }

            var7.flush();
         } catch (Throwable var13) {
            try {
               var8.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }

            throw var13;
         }

         var8.close();
      } catch (Throwable var14) {
         if (var7 != null) {
            try {
               var7.close();
            } catch (Throwable var11) {
               var14.addSuppressed(var11);
            }
         }

         throw var14;
      }

      if (var7 != null) {
         var7.close();
      }

      int var15 = var6.getResponseCode();
      var6.disconnect();
      return var15;
   }

   private void downloadFile(String var1, String var2, File var3, Runnable var4) {
      try {
         HttpURLConnection var5 = (HttpURLConnection)new URL(getServer() + "/installLayout?layout=" + var1 + "&token=" + token).openConnection();
         var5.setConnectTimeout(5000);
         var5.setRequestProperty("token", var2);
         var5.connect();
         BufferedInputStream var6 = new BufferedInputStream(var5.getInputStream());
         FileOutputStream var7 = new FileOutputStream(var3);
         byte[] var8 = new byte[1024];

         int var9;
         while ((var9 = var6.read(var8, 0, 1024)) != -1) {
            var7.write(var8, 0, var9);
         }

         var7.close();
         var6.close();
         var4.run();
      } catch (IOException var10) {
         SendMessage.warnMessage("Failed to download layout from server");
         var10.printStackTrace();
      }
   }

   private ArrayList<String> listFiles(File var1, boolean var2) {
      ArrayList var3 = new ArrayList();
      if (var1.exists() && var1.isDirectory()) {
         Arrays.stream(var1.listFiles()).forEach(var3x -> {
            if (var3x.isDirectory()) {
               for (String var5 : this.listFiles(var3x, var2)) {
                  var3.add(var3x.getName() + "/" + var5);
               }
            } else {
               var3.add(!var2 ? var3x.getName().split("\\.")[0] : var3x.getName());
            }
         });
      }

      return var3;
   }

   private void disableCache(HttpURLConnection var1) {
      var1.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
      var1.setRequestProperty("Pragma", "no-cache");
      var1.setRequestProperty("Expires", "0");
   }

   private static enum UploadReason {
      CREATE,
      UPDATE;

      private UploadReason() {
      }
   }

   private class UploadResponse {
      final boolean success;
      final String reason;

      public UploadResponse(boolean nullx) {
         this.success = nullx;
         this.reason = "";
      }

      public UploadResponse(boolean nullx, String nullxx) {
         this.success = nullx;
         this.reason = nullxx;
      }
   }
}
