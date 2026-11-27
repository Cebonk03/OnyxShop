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

   public void startUpload(Player player) {
      if (!ConfigManager.getConfig().getBoolean("cloud-features.enabled", false)) {
         SendMessage.errorMessage(player, Lang.CLOUD_DISABLED.get(player));
         return;
      }

      new CustomizeLayout(player, element -> {
         element.setToken(token);
         new FileUploader(player, element2 -> {
            if (element2 == null) {
               player.sendMessage(ChatColor.RED + "Cancelled upload process");
            } else {
               new AuthRequest(player, element3 -> {
                  element.setAuthMethod(element3);
                  this.requestCard(player, element, element4 -> this.plugin.runTaskLaterFor(() -> {
                     Runnable task = () -> {
                        if (element.getAuthMethod() == AuthRequest.AuthMethod.DISCORD) {
                           SendMessage.chatToPlayer(player, ChatColor.GREEN + "Validation success, started upload process");
                        } else {
                           SendMessage.chatToPlayer(player, ChatColor.GREEN + "Started upload process");
                        }

                        this.startUpload(player, element2, element4, null, null, MarketplaceIntegration.UploadReason.CREATE);
                     };
                     switch (element3) {
                        case TEMP:
                           task.run();
                           break;
                        case DISCORD:
                           player.closeInventory();
                           player.sendTitle("§c§lValidation required", "§6§lClick the link in chat to authorize", 1, 80, 1);
                           TextComponent textComponent = new TextComponent(TextComponent.fromLegacyText(Lang.DISCORD_VALIDATION_REQUIRED.get().getLegacy()));
                           textComponent.setClickEvent(new ClickEvent(Action.OPEN_URL, this.getAuthorizeURL(element4)));
                           SendMessage.chatToPlayer(player, textComponent);
                           this.checkVerification(player, element4, 6000L, task);
                           break;
                        default:
                           SendMessage.chatToPlayer(player, ChatColor.RED + "Cancelled upload process");
                     }
                  }, player, 1L));
               });
            }
         });
      });
   }

   private void requestCard(Player player, SimpleCard simpleCard, Consumer<String> task) {
      this.plugin.runKillableTask(() -> {
         String text;
         try {
            text = this.sendRequest(getServer() + "/createLayout?token=" + token, simpleCard.toString());
         } catch (Exception exception) {
            SendMessage.chatToPlayer(player, ChatColor.RED + "Unable to reach API server at this point, see console for more details.");
            exception.printStackTrace();
            return;
         }

         try {
            UUID.fromString(text);
         } catch (IllegalArgumentException exception) {
            SendMessage.chatToPlayer(player, ChatColor.RED + "Failed to authorize with server, see console for details.");
            exception.printStackTrace();
            return;
         }

         task.accept(text);
      });
   }

   private void startUpload(Player player, ArrayList<String> playerNames, String text, String text2, String text3, MarketplaceIntegration.UploadReason uploadReason) {
      SendMessage.chatToPlayer(player, ChatColor.GREEN + "Started upload process, zipping files...");
      this.plugin
         .runKillableTask(
            () -> {
               Path path;
               try {
                  path = this.createZipFile();
                  FileOutputStream fileOutputStream = new FileOutputStream(path.toString());
                  ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
                  this.zipLayout(playerNames, zipOutputStream, true);
                  zipOutputStream.close();
                  fileOutputStream.close();
               } catch (IOException exception) {
                  SendMessage.chatToPlayer(player, ChatColor.RED + "Failed to create a copy of the current shop layout, see console for details.");
                  exception.printStackTrace();
                  return;
               }

               SendMessage.chatToPlayer(player, ChatColor.GREEN + "Successfully compressed layout, uploading to server...");
               SendMessage.chatToPlayer(
                  player, ChatColor.GREEN + "This may take some time, you can continue other things while this process runs in the background..."
               );

               try {
                  int value = uploadReason == MarketplaceIntegration.UploadReason.CREATE ? this.upload(path, text) : this.update(path, text, text2, text3);
                  switch (value) {
                     case 202:
                        this.checkStatus(
                           player,
                           text,
                           6000L,
                           element -> {
                              if (element.success) {
                                 SendMessage.chatToPlayer(
                                    player,
                                    ChatColor.GREEN
                                       + "Your shop layout was uploaded successfully, layout id: "
                                       + ChatColor.WHITE
                                       + text
                                 );
                              } else if (element.reason.equals("deniedBadQuality")) {
                                 SendMessage.chatToPlayer(
                                    player,
                                    ChatColor.RED
                                       + "Your shop layout with id "
                                       + text
                                       + " was denied for low quality.\n"
                                       + ChatColor.RED
                                       + "The layout was denied for low quality."
                                 );
                                 SendMessage.warnMessage("Shop layout " + text + " was denied for low quality");
                              } else {
                                 SendMessage.chatToPlayer(
                                    player,
                                    ChatColor.RED
                                       + "A error occurred while processing your shop layout, please try again soon or if this keeps happening, contact the developers."
                                 );
                                 SendMessage.warnMessage("A error occurred while uploading shop layout " + text + " with reason: " + element.reason);
                              }

                              token = new SecureRandom().nextLong();
                           }
                        );
                        break;
                     case 406:
                     case 415:
                        SendMessage.chatToPlayer(
                           player, ChatColor.RED + "The shop layout was denied by the server, please make sure the files contain no extra content as needed."
                        );
                        SendMessage.errorMessage("Server responded with status code '" + value + "'");
                        token = new SecureRandom().nextLong();
                        break;
                     default:
                        SendMessage.chatToPlayer(player, ChatColor.RED + "The server failed to process your request at this time, see console for details.");
                        SendMessage.errorMessage("Server responded with status code '" + value + "'");
                        token = new SecureRandom().nextLong();
                  }
               } catch (IOException exception) {
                  SendMessage.chatToPlayer(player, ChatColor.RED + "Error occurred while uploading layout, see console for details.");
                  token = new SecureRandom().nextLong();
                  exception.printStackTrace();
               }
            }
         );
   }

   public void startUpdate(Player player, String playerName) {
      if (!ConfigManager.getConfig().getBoolean("cloud-features.enabled", false)) {
         SendMessage.errorMessage(player, Lang.CLOUD_DISABLED.get(player));
         return;
      }

      this.plugin
         .runKillableTask(
            () -> {
               SimplePair simplePair;
               try {
                  simplePair = this.getLayout(playerName, "/createUpdate");
               } catch (LayoutNotFoundException exception) {
                  SendMessage.chatToPlayer(
                     player,
                     ChatColor.RED
                        + "Failed to find a layout with ID "
                        + playerName
                        + ", make sure the ID is spelled correct and the layout still exists. Keep in mind that temporary layouts are removed after 14 days."
                  );
                  return;
               } catch (NullPointerException exception) {
                  SendMessage.chatToPlayer(
                     player, ChatColor.RED + "Failed to fetch layout from server at this time, please try again soon or see console for more details."
                  );
                  SendMessage.warnMessage(exception.getMessage());
                  exception.printStackTrace();
                  return;
               } catch (Exception exception) {
                  SendMessage.chatToPlayer(player, ChatColor.RED + "Failed to fetch layout from server, see console for more details.");
                  exception.printStackTrace();
                  return;
               }

               if (!((SimpleLayout)simplePair.key).authMethod.equals("DISCORD")) {
                  SendMessage.chatToPlayer(
                     player, ChatColor.RED + "Given layout is not uploaded using discord validation, it is not possible to update this layout!"
                  );
               } else {
                  player.sendTitle("§c§lValidation required", "§6§lClick the link in chat to authorize", 1, 80, 1);
                  TextComponent textComponent = new TextComponent(TextComponent.fromLegacyText(Lang.DISCORD_VALIDATION_REQUIRED.get().getLegacy()));
                  textComponent.setClickEvent(new ClickEvent(Action.OPEN_URL, this.getAuthorizeURL(playerName)));
                  SendMessage.chatToPlayer(player, textComponent);
                  this.checkVerification(
                     player,
                     playerName,
                     6000L,
                     () -> {
                        SendMessage.chatToPlayer(player, ChatColor.GREEN + "Verification success!");
                        new UpdateMessage(
                           player,
                           element -> new FileUploader(
                              player,
                              element2 -> {
                                 player.closeInventory();
                                 if (element2 == null) {
                                    player.sendMessage(ChatColor.RED + "Cancelled update process");
                                 } else {
                                    SendMessage.chatToPlayer(
                                       player,
                                       ChatColor.translateAlternateColorCodes(
                                          '&',
                                          "&aYou are about to update layout &c%layoutID% &awith data: \n&6Title: &e%title% \n&6Description: &e%description% \n&6Tags: \n&8- &e%tags% \n&6Compatible MC version(s): &e%mcVer% \n&6Created for plugin version: &e%plVer% \n&6Created at: &e%creationDate% \n&6Downloads: &e%downloads% \n&6Shop files: \n&8- &e%files%"
                                             .replace("%layoutID%", playerName)
                                             .replace("%title%", ((SimpleLayout)simplePair.key).title)
                                             .replace("%description%", ((SimpleLayout)simplePair.key).description)
                                             .replace("%tags%", String.join("\n&8- &e", ((SimpleLayout)simplePair.key).tags))
                                             .replace("%files%", String.join("\n&8- &e", ((SimpleLayout)simplePair.key).files))
                                             .replace("%creationDate%", TimeUtils.getDate(((SimpleLayout)simplePair.key).creation))
                                             .replace("%downloads%", ((SimpleLayout)simplePair.key).downloads + "")
                                             .replace("%mcVer%", ((SimpleLayout)simplePair.key).mcVer + "")
                                             .replace("%plVer%", ((SimpleLayout)simplePair.key).plVer + "")
                                       )
                                    );
                                    new ConfirmRequest(
                                       this.plugin,
                                       ChatColor.RED
                                          + "Are you sure you want to continue? This will override the layout and update it with the current installed shop layout.\n"
                                          + ChatColor.GREEN
                                          + "Enter 'yes' or 'no' in chat to continue.",
                                       player,
                                       element3 -> this.plugin.runKillableTask(() -> {
                                          if (element3) {
                                             this.startUpload(player, element2, playerName, (String)simplePair.value, element, MarketplaceIntegration.UploadReason.UPDATE);
                                          } else {
                                             SendMessage.chatToPlayer(player, ChatColor.RED + "Cancelled update process");
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

   public void downloadLayout(Player player, String playerName, boolean flag) {
      if (!ConfigManager.getConfig().getBoolean("cloud-features.enabled", false)) {
         SendMessage.errorMessage(player, Lang.CLOUD_DISABLED.get(player));
         return;
      }

      SendMessage.chatToPlayer(player, ChatColor.GREEN + "Started install process, retrieving layout from server...");
      this.plugin
         .runKillableTask(
            () -> {
               SimplePair simplePair;
               try {
                  simplePair = this.getLayout(playerName, "/getLayout");
               } catch (LayoutNotFoundException exception) {
                  SendMessage.chatToPlayer(
                     player,
                     ChatColor.RED
                        + "Failed to find a layout with ID "
                        + playerName
                        + ", make sure the ID is spelled correct and the layout still exists. Keep in mind that temporary layouts are removed after 14 days."
                  );
                  return;
               } catch (NullPointerException exception) {
                  SendMessage.chatToPlayer(
                     player, ChatColor.RED + "Failed to fetch layout from server at this time, please try again soon or see console for more details."
                  );
                  SendMessage.warnMessage(exception.getMessage());
                  exception.printStackTrace();
                  return;
               } catch (Exception exception) {
                  SendMessage.chatToPlayer(player, ChatColor.RED + "Failed to fetch layout from server, see console for more details.");
                  exception.printStackTrace();
                  return;
               }

               SendMessage.chatToPlayer(
                  player,
                  ChatColor.translateAlternateColorCodes(
                     '&',
                     "&aYou are about to install layout &c%layoutID% &awith data: \n&6Title: &e%title% \n&6Description: &e%description% \n&6Tags: \n&8- &e%tags% \n&6Compatible MC version(s): &e%mcVer% \n&6Created for plugin version: &e%plVer% \n&6Created at: &e%creationDate% \n&6Downloads: &e%downloads% \n&6Shop files: \n&8- &e%files%"
                        .replace("%layoutID%", playerName)
                        .replace("%title%", ((SimpleLayout)simplePair.key).title)
                        .replace("%description%", ((SimpleLayout)simplePair.key).description)
                        .replace("%tags%", String.join("\n&8- &e", ((SimpleLayout)simplePair.key).tags))
                        .replace("%files%", String.join("\n&8- &e", ((SimpleLayout)simplePair.key).files))
                        .replace("%creationDate%", TimeUtils.getDate(((SimpleLayout)simplePair.key).creation))
                        .replace("%downloads%", ((SimpleLayout)simplePair.key).downloads + "")
                        .replace("%mcVer%", ((SimpleLayout)simplePair.key).mcVer + "")
                        .replace("%plVer%", ((SimpleLayout)simplePair.key).plVer + "")
                  )
               );
               new ConfirmRequest(
                  this.plugin,
                  ChatColor.RED
                     + "Are you sure you want to continue? This will override the current shop layout.\n"
                     + ChatColor.GREEN
                     + "Enter 'yes' or 'no' in chat.",
                  player,
                  element -> this.plugin.runKillableTask(() -> {
                     if (element) {
                        File file = this.createLayout(playerName);
                        Runnable task = () -> {
                           if (!file.exists()) {
                              SendMessage.chatToPlayer(player, ChatColor.RED + "Failed to download layout from server, see console for more details.");
                           } else {
                              this.installLayout(player, (SimpleLayout)simplePair.key, file, flag);
                           }
                        };
                        if (((SimpleLayout)simplePair.key).requiresAuthorization) {
                           player.sendTitle("§c§lValidation required", "§6§lClick the link in chat to authorize", 1, 80, 1);
                           TextComponent textComponent = new TextComponent(TextComponent.fromLegacyText(Lang.DISCORD_VALIDATION_REQUIRED.get().getLegacy()));
                           textComponent.setClickEvent(new ClickEvent(Action.OPEN_URL, this.getAuthorizeURL((String)simplePair.value)));
                           SendMessage.chatToPlayer(player, textComponent);
                           this.checkVerification(player, playerName, 6000L, () -> {
                              SendMessage.chatToPlayer(player, ChatColor.GREEN + "Verification success, downloading layout...");
                              this.downloadFile(playerName, (String)simplePair.value, file, task);
                           });
                        } else {
                           SendMessage.chatToPlayer(player, ChatColor.GREEN + "Downloading layout...");
                           this.downloadFile(playerName, (String)simplePair.value, file, task);
                        }
                     } else {
                        SendMessage.chatToPlayer(player, ChatColor.RED + "Cancelled install process");
                     }
                  })
               );
            }
         );
   }

   private void installLayout(Player player, SimpleLayout simpleLayout, File file, boolean flag) {
      SendMessage.chatToPlayer(player, ChatColor.GREEN + "Successfully downloaded layout, extracting files...");
      this.backupCurrentLayout(player, simpleLayout.files.contains("config.yml") && flag);
      boolean flag2 = true;

      try {
         boolean flag3 = false;
         ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file));

         for (ZipEntry zipEntry = zipInputStream.getNextEntry(); zipEntry != null; zipEntry = zipInputStream.getNextEntry()) {
            String name = zipEntry.getName();
            if (name.equals("config.yml")) {
               if (!flag) {
                  name = "temp-" + name;
               }

               flag3 = true;
            }

            File file2 = new File(this.plugin.getDataFolder(), name.replace("/", File.separator));
            if (file2.getParentFile() != null && !file2.getParentFile().exists()) {
               file2.getParentFile().mkdirs();
            }

            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            byte[] value = new byte[1024];

            int value2;
            while ((value2 = zipInputStream.read(value)) > 0) {
               fileOutputStream.write(value, 0, value2);
            }

            fileOutputStream.close();
         }

         zipInputStream.closeEntry();
         zipInputStream.close();
         if (simpleLayout.files.contains("config.yml") && !flag) {
            try {
               File file2 = new File(this.plugin.getDataFolder(), "temp-config.yml");
               Config config = new Config(file2);

               for (String text : config.getKeys(false)) {
                  if (!bannedOptions.contains(text)) {
                     if (listOptions.contains(text) && ConfigManager.getConfig().contains(text)) {
                        this.addValues(ConfigManager.getConfig(), config, text);
                     } else {
                        ConfigManager.getConfig().set(text, config.get(text));
                     }
                  }
               }

               ConfigManager.saveConfig();
               Files.delete(file2.toPath());
            } catch (ConfigLoadException exception) {
               SendMessage.chatToPlayer(
                  player, ChatColor.RED + "Invalid config.yml format for downloaded layout, try to download the layout again or report this layout as broken."
               );
               SendMessage.warnMessage("A error occurred while reading the config.yml of downloaded shop layout at " + file.getAbsolutePath());
               exception.printStackTrace();
               flag2 = false;
            } catch (IOException exception) {
               SendMessage.chatToPlayer(player, ChatColor.RED + "Failed to remove temp config from plugin folder, it may be removed manually.");
               SendMessage.warnMessage("A error occurred while trying to remove temp config at " + file.getAbsolutePath());
               exception.printStackTrace();
               flag2 = false;
            }
         }

         UpdateChecker.Version version = new UpdateChecker.Version(simpleLayout.requiresAuthorization ? "5.20.2" : "6.10.3");
         if (!flag
            && new UpdateChecker.Version(this.plugin.getDescription().getVersion()).isGreater(new UpdateChecker.Version("5.20.2"))
            && new UpdateChecker.Version(simpleLayout.plVer).isSmaller(version)) {
            SendMessage.logDebugMessage("Updating section files from downloaded layout...");
            if (!simpleLayout.files.contains("config.yml")) {
               for (String text : simpleLayout.files) {
                  if (!text.equals("config.yml")) {
                     try {
                        File file2 = new File(this.plugin.getDataFolder(), "sections" + File.separator + text);
                        Config config = new Config(file2);
                        String text2 = config.getString("slot");
                        if (text2 != null) {
                           ArrayList<Integer> slots = this.plugin.calculateAmount.getSlots(text2);
                           if (slots.size() == 1) {
                              config.set("slot", (Integer)slots.get(0) + 2);
                           } else {
                              config.set("slot", slots.stream().map(element -> String.valueOf(element + 2)).collect(Collectors.joining(",")));
                           }
                        }

                        config.save();
                     } catch (ConfigSaveException | ConfigLoadException exception) {
                        SendMessage.chatToPlayer(
                           player, ChatColor.RED + "Failed to update section config '" + text + "', download this layout again or report this layout as broken."
                        );
                        SendMessage.warnMessage(
                           "A error occurred while reading the " + text + " config of downloaded shop layout at " + file.getAbsolutePath()
                        );
                        exception.printStackTrace();
                        flag2 = false;
                     }
                  }
               }
            } else if (!this.plugin.getConfigManager().reload()) {
               SendMessage.chatToPlayer(player, ChatColor.RED + "Failed to reload configs, download this layout again or report this layout as broken.");
               SendMessage.warnMessage("A error occurred while reloading the configuration files of downloaded shop layout at " + file.getAbsolutePath());
               flag2 = false;
            }
         }

         if (flag2) {
            if (flag3) {
               SendMessage.chatToPlayer(player, ChatColor.GREEN + "Successfully installed shop layout, a server restart is recommended to load all changes.");
            } else {
               SendMessage.chatToPlayer(player, ChatColor.GREEN + "Successfully installed shop layout, use command /sreload to reload all changes.");
            }
         }

         token = new SecureRandom().nextLong();
      } catch (IOException exception) {
         SendMessage.chatToPlayer(player, ChatColor.RED + "A unknown error occurred while extracting the downloaded file, see console for details.");
         SendMessage.warnMessage("A unknown error occurred while extracting shop layout from " + file.getAbsolutePath());
         exception.printStackTrace();
      }
   }

   private void addValues(Config config, Config config2, String text) {
      Object value = config.get(text);
      Object value2 = config2.get(text);
      if (value instanceof List && value2 instanceof List) {
         for (Object value3 : (List)value2) {
            if (!((List)value).contains(value3)) {
               ((List)value).add(value3);
            }
         }

         config.set(text, value);
      } else if (value instanceof ConfigurationSection && value2 instanceof ConfigurationSection) {
         for (String sectionId : ((ConfigurationSection)value2).getKeys(false)) {
            ((ConfigurationSection)value).set(sectionId, ((ConfigurationSection)value2).get(sectionId));
         }

         config.set(text, value);
      }
   }

   private void backupCurrentLayout(Player player, boolean flag) {
      ArrayList<String> values = new ArrayList<>(Collections.singletonList("config.yml"));
      this.listFiles(new File(this.plugin.getDataFolder() + File.separator + "shops"), true).forEach(entry -> values.add("shops/" + entry));
      this.listFiles(new File(this.plugin.getDataFolder() + File.separator + "sections"), true).forEach(entry -> values.add("sections/" + entry));

      try {
         Path path = this.createZipFile();
         SendMessage.chatToPlayer(player, ChatColor.GREEN + "Saving backup from current layout as layouts/" + path.getFileName());
         FileOutputStream fileOutputStream = new FileOutputStream(path.toString());
         ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
         this.zipLayout(values, zipOutputStream, false);
         zipOutputStream.close();
         fileOutputStream.close();
      } catch (IOException exception) {
         SendMessage.chatToPlayer(player, ChatColor.RED + "Failed to create a backup of the current shop layout, see console for details.");
         exception.printStackTrace();
      }

      if (!flag) {
         values.remove("config.yml");
         values.add("temp-config.yml");
      }

      values.forEach(entry -> {
         File file = new File(this.plugin.getDataFolder(), entry.replace("/", File.separator));

         try {
            Files.delete(file.toPath());
         } catch (IOException exception) {
         }
      });
      Arrays.stream(new File(this.plugin.getDataFolder() + File.separator + "shops").listFiles()).collect(Collectors.toList()).forEach(File::delete);
      Arrays.stream(new File(this.plugin.getDataFolder() + File.separator + "sections").listFiles()).collect(Collectors.toList()).forEach(File::delete);
   }

   private File createLayout(String text) {
      String text2 = this.plugin.getDataFolder() + File.separator + "layouts" + File.separator + "downloads" + File.separator + text;
      File file = new File(text2).getParentFile();
      if (!file.exists() || file.isFile()) {
         file.mkdirs();
      }

      int value = 0;

      while (new File(text2 + (value > 0 ? "(" + value + ")" : "")).exists()) {
         value++;
      }

      return Paths.get(text2 + (value > 0 ? "(" + value + ")" : "") + ".zip").toFile();
   }

   private String sendRequest(String text, String text2) throws MalformedURLException , IOException, UnsupportedEncodingException{
      HttpURLConnection httpURLConnection = (HttpURLConnection)new URL(text).openConnection();
      httpURLConnection.setConnectTimeout(5000);
      httpURLConnection.setDoOutput(true);
      httpURLConnection.setRequestProperty("token", String.valueOf(token));
      httpURLConnection.setRequestProperty("Content-Type", "application/json");
      OutputStream outputStream = httpURLConnection.getOutputStream();
      OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
      outputStreamWriter.write(text2);
      outputStreamWriter.flush();
      outputStreamWriter.close();
      outputStream.close();
      httpURLConnection.connect();
      return new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).readLine();
   }

   private SimplePair<SimpleLayout, String> getLayout(String text, String text2) throws MalformedURLException, IOException, LayoutNotFoundException, ParseException {
      HttpURLConnection httpURLConnection = (HttpURLConnection)new URL(getServer() + text2 + "?layout=" + text).openConnection();
      httpURLConnection.setConnectTimeout(5000);
      httpURLConnection.setRequestProperty("token", String.valueOf(token));
      httpURLConnection.connect();
      String text3 = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).readLine();
      switch (httpURLConnection.getResponseCode()) {
         case 200:
            SimpleLayout simpleLayout = new SimpleLayout(text3);
            return new SimplePair<>(simpleLayout, httpURLConnection.getHeaderField("token"));
         case 400:
            throw new LayoutNotFoundException();
         default:
            throw new NullPointerException("Server responded with " + httpURLConnection.getResponseCode() + ": " + text3);
      }
   }

   private String getAuthorizeURL(String text) {
      return getServer() + "/authorize?token=" + token + "&layout=" + text;
   }

   private void checkVerification(Player player, String playerName, long value, Runnable task) {
      long value2 = value - 100L;
      this.plugin.runKillableTaskLater(() -> {
         try {
            if (((HttpURLConnection)new URL(getServer() + "/check?token=" + token + "&layout=" + playerName).openConnection()).getResponseCode() == 202) {
               task.run();
            } else if (value2 != 0L) {
               this.checkVerification(player, playerName, value2, task);
            } else {
               SendMessage.chatToPlayer(player, Lang.VALIDATION_EXPIRED.get());
            }
         } catch (IOException exception) {
            exception.printStackTrace();
         }
      }, 100L);
   }

   private void checkStatus(Player player, String playerName, long value, Consumer<MarketplaceIntegration.UploadResponse> task) {
      long value2;
      long value3 = value2 = value - 100L;
      this.plugin
         .runKillableTaskLater(
            () -> {
               HttpURLConnection httpURLConnection = null;

               try {
                  httpURLConnection = (HttpURLConnection)new URL(getServer() + "/getStatus?token=" + token + "&layout=" + playerName).openConnection();
                  int responseCode = httpURLConnection.getResponseCode();
                  if (responseCode == 200) {
                     task.accept(new MarketplaceIntegration.UploadResponse(true));
                  } else if (responseCode == 422) {
                     JsonObject jsonObject = (JsonObject)new Gson()
                        .fromJson(new BufferedReader(new InputStreamReader(httpURLConnection.getErrorStream())).readLine(), JsonObject.class);
                     task.accept(new MarketplaceIntegration.UploadResponse(false, jsonObject.get("reason").getAsString()));
                  } else if (value3 != 0L) {
                     this.checkStatus(player, playerName, value3, task);
                  } else {
                     SendMessage.chatToPlayer(player, ChatColor.RED + "A timeout occurred while uploading layout " + playerName + ", please try again soon.");
                  }
               } catch (IOException exception) {
                  exception.printStackTrace();
               } finally {
                  httpURLConnection.disconnect();
               }
            },
            100L
         );
   }

   private Path createZipFile() {
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
      LocalDateTime localDateTime = LocalDateTime.now();
      String text = this.plugin.getDataFolder() + File.separator + "layouts" + File.separator + "layout - " + dateTimeFormatter.format(localDateTime);
      int value = 0;

      while (new File(text + (value > 0 ? "(" + value + ")" : "")).exists()) {
         value++;
      }

      return Paths.get(text + (value > 0 ? "(" + value + ")" : "") + ".zip");
   }

   private void zipLayout(ArrayList<String> values, ZipOutputStream zipOutputStream, boolean flag) throws FileNotFoundException , IOException{
      File file = this.plugin.getDataFolder();
      if (file.exists() && file.isDirectory()) {
         for (String text : values) {
            File file2 = new File(file, text.replace("/", File.separator));
            if (!file2.isDirectory()) {
               zipOutputStream.putNextEntry(new ZipEntry(text));
               if (flag && text.equals("config.yml")) {
                  zipOutputStream.write(this.stripTelemetry(new FileInputStream(file2)));
               } else {
                  FileInputStream fileInputStream = new FileInputStream(file2);
                  byte[] value = new byte[1024];

                  int value2;
                  while ((value2 = fileInputStream.read(value)) >= 0) {
                     zipOutputStream.write(value, 0, value2);
                  }

                  fileInputStream.close();
               }
            }
         }
      }
   }

   // Uploaded layouts share config.yml with other servers: never include the local endpoints,
   // download tokens or bStats id in that file.
   private byte[] stripTelemetry(FileInputStream fileInputStream) throws IOException {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      byte[] value = new byte[1024];

      int value2;
      while ((value2 = fileInputStream.read(value)) >= 0) {
         byteArrayOutputStream.write(value, 0, value2);
      }

      fileInputStream.close();
      String text = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
      String text2 = text.contains("\r\n") ? "\r\n" : "\n";
      StringBuilder builder = new StringBuilder();
      boolean flag = false;

      for (String text3 : text.split(text2, -1)) {
         if (!text3.isEmpty() && !Character.isWhitespace(text3.charAt(0)) && !text3.startsWith("#")) {
            flag = text3.startsWith("update-checker:") || text3.startsWith("cloud-features:") || text3.startsWith("metrics:");
         }

         if (!flag) {
            builder.append(text3).append(text2);
         }
      }

      return builder.toString().getBytes(StandardCharsets.UTF_8);
   }

   private int upload(Path path, String path2) throws MalformedURLException , IOException, ProtocolException{
      URL url = new URL(getServer() + "/uploadLayout?layout=" + path2);
      HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
      httpURLConnection.setDoOutput(true);
      httpURLConnection.setRequestMethod("POST");
      httpURLConnection.setRequestProperty("token", String.valueOf(token));
      httpURLConnection.setRequestProperty("Content-Type", "application/zip");
      OutputStream outputStream = httpURLConnection.getOutputStream();

      try {
         FileInputStream fileInputStream = new FileInputStream(new File(path.toUri()));

         try {
            byte[] value = new byte[4096];

            int value2;
            while ((value2 = fileInputStream.read(value)) != -1) {
               outputStream.write(value, 0, value2);
            }

            outputStream.flush();
         } catch (Throwable exception) {
            try {
               fileInputStream.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }

            throw exception;
         }

         fileInputStream.close();
      } catch (Throwable exception) {
         if (outputStream != null) {
            try {
               outputStream.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (outputStream != null) {
         outputStream.close();
      }

      int responseCode = httpURLConnection.getResponseCode();
      httpURLConnection.disconnect();
      return responseCode;
   }

   private int update(Path path, String path2, String text, String text2) throws MalformedURLException , IOException, ProtocolException{
      URL url = new URL(getServer() + "/updateLayout?layout=" + path2 + "&token=" + token);
      HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
      httpURLConnection.setDoOutput(true);
      httpURLConnection.setRequestMethod("POST");
      httpURLConnection.setRequestProperty("token", text);
      httpURLConnection.setRequestProperty("msg", text2);
      httpURLConnection.setRequestProperty("Content-Type", "application/zip");
      OutputStream outputStream = httpURLConnection.getOutputStream();

      try {
         FileInputStream fileInputStream = new FileInputStream(new File(path.toUri()));

         try {
            byte[] value = new byte[4096];

            int value2;
            while ((value2 = fileInputStream.read(value)) != -1) {
               outputStream.write(value, 0, value2);
            }

            outputStream.flush();
         } catch (Throwable exception) {
            try {
               fileInputStream.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }

            throw exception;
         }

         fileInputStream.close();
      } catch (Throwable exception) {
         if (outputStream != null) {
            try {
               outputStream.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (outputStream != null) {
         outputStream.close();
      }

      int responseCode = httpURLConnection.getResponseCode();
      httpURLConnection.disconnect();
      return responseCode;
   }

   private void downloadFile(String text, String text2, File file, Runnable task) {
      try {
         HttpURLConnection httpURLConnection = (HttpURLConnection)new URL(getServer() + "/installLayout?layout=" + text + "&token=" + token).openConnection();
         httpURLConnection.setConnectTimeout(5000);
         httpURLConnection.setRequestProperty("token", text2);
         httpURLConnection.connect();
         BufferedInputStream bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream());
         FileOutputStream fileOutputStream = new FileOutputStream(file);
         byte[] value = new byte[1024];

         int value2;
         while ((value2 = bufferedInputStream.read(value, 0, 1024)) != -1) {
            fileOutputStream.write(value, 0, value2);
         }

         fileOutputStream.close();
         bufferedInputStream.close();
         task.run();
      } catch (IOException exception) {
         SendMessage.warnMessage("Failed to download layout from server");
         exception.printStackTrace();
      }
   }

   private ArrayList<String> listFiles(File file, boolean flag) {
      ArrayList values = new ArrayList();
      if (file.exists() && file.isDirectory()) {
         Arrays.stream(file.listFiles()).forEach(entry -> {
            if (entry.isDirectory()) {
               for (String text : this.listFiles(entry, flag)) {
                  values.add(entry.getName() + "/" + text);
               }
            } else {
               values.add(!flag ? entry.getName().split("\\.")[0] : entry.getName());
            }
         });
      }

      return values;
   }

   private void disableCache(HttpURLConnection httpURLConnection) {
      httpURLConnection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
      httpURLConnection.setRequestProperty("Pragma", "no-cache");
      httpURLConnection.setRequestProperty("Expires", "0");
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

      public UploadResponse(boolean flag) {
         this.success = flag;
         this.reason = "";
      }

      public UploadResponse(boolean flag, String text) {
         this.success = flag;
         this.reason = text;
      }
   }
}
