package com.onyxshop.files.config;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.exceptions.ConfigLoadException;
import com.onyxshop.util.exceptions.ConfigSaveException;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class Config extends YamlConfiguration {
   private final Path path;
   public Configuration defaults;

   public Config(File file) throws ConfigLoadException {
      this.path = file.toPath();
      this.loadConfig(file);
   }

   private void loadConfig(File file) throws ConfigLoadException {
      try {
         this.load(file);
      } catch (IOException exception) {
         throw new ConfigLoadException("A unknown IOE Exception occurred while loading " + file.getName() + "\n" + this.stackTraceToString(exception));
      } catch (InvalidConfigurationException exception) {
         throw new ConfigLoadException(
            "Invalid Yaml syntax while loading "
               + file.getName()
               + " config, use a online Yaml parser to validate the config and fix any errors.\n"
               + this.stackTraceToString(exception)
         );
      }
   }

   private String stackTraceToString(Exception exception) {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      exception.printStackTrace(printWriter);
      return stringWriter.toString();
   }

   public void reload() throws ConfigLoadException {
      this.loadConfig(this.path.toFile());
   }

   public void save() throws ConfigSaveException {
      try {
         File file = new File(this.path.toString() + ".tmp");
         this.save(file);
         java.nio.file.Files.move(
            file.toPath(), this.path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE
         );
      } catch (IOException exception) {
         throw new ConfigSaveException(
            Lang.COULD_NOT_SAVE_CONFIG.get().replace("%fileName%", this.path.getFileName().toString()) + "\n" + this.stackTraceToString(exception)
         );
      }
   }

   public void saveConfig() {
      try {
         this.save();
         this.reload();
      } catch (ConfigLoadException | ConfigSaveException exception) {
         SendMessage.errorMessage(exception.getMessage());
         ConfigManager.badYaml = this.path.getFileName().toString();
      }
   }

   public void setDef(@NotNull Configuration configuration) {
      this.defaults = configuration;
   }

   public Configuration getDef() {
      return this.defaults;
   }

   public Path getFilePath() {
      return this.path;
   }
}
