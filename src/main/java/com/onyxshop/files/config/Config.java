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

   public Config(File var1) throws ConfigLoadException {
      this.path = var1.toPath();
      this.loadConfig(var1);
   }

   private void loadConfig(File var1) throws ConfigLoadException {
      try {
         this.load(var1);
      } catch (IOException var3) {
         throw new ConfigLoadException("A unknown IOE Exception occurred while loading " + var1.getName() + "\n" + this.stackTraceToString(var3));
      } catch (InvalidConfigurationException var4) {
         throw new ConfigLoadException(
            "Invalid Yaml syntax while loading "
               + var1.getName()
               + " config, use a online Yaml parser to validate the config and fix any errors.\n"
               + this.stackTraceToString(var4)
         );
      }
   }

   private String stackTraceToString(Exception var1) {
      StringWriter var2 = new StringWriter();
      PrintWriter var3 = new PrintWriter(var2);
      var1.printStackTrace(var3);
      return var2.toString();
   }

   public void reload() throws ConfigLoadException {
      this.loadConfig(this.path.toFile());
   }

   public void save() throws ConfigSaveException {
      try {
         File var6 = new File(this.path.toString() + ".tmp");
         this.save(var6);
         java.nio.file.Files.move(
            var6.toPath(), this.path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE
         );
      } catch (IOException var2) {
         throw new ConfigSaveException(
            Lang.COULD_NOT_SAVE_CONFIG.get().replace("%fileName%", this.path.getFileName().toString()) + "\n" + this.stackTraceToString(var2)
         );
      }
   }

   public void saveConfig() {
      try {
         this.save();
         this.reload();
      } catch (ConfigLoadException | ConfigSaveException var2) {
         SendMessage.errorMessage(var2.getMessage());
         ConfigManager.badYaml = this.path.getFileName().toString();
      }
   }

   public void setDef(@NotNull Configuration var1) {
      this.defaults = var1;
   }

   public Configuration getDef() {
      return this.defaults;
   }

   public Path getFilePath() {
      return this.path;
   }
}
