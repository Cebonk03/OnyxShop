package com.onyxshop.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;

public class SaveTask {
   private static final Cache<ConfigManager.CFile, SaveTask.DelayedTask> tasks = CacheBuilder.newBuilder()
      .expireAfterWrite(1000L, TimeUnit.MILLISECONDS)
      .build();

   public SaveTask() {
   }

   public static void addSaveTask(ConfigManager.CFile var0) {
      if (tasks.getIfPresent(var0) == null) {
         tasks.put(var0, new SaveTask.DelayedTask(var0, SaveTask.TaskType.SAVE));
      }
   }

   public static void addBackupTask(ConfigManager.CFile var0) {
      if (tasks.getIfPresent(var0) == null) {
         tasks.put(var0, new SaveTask.DelayedTask(var0, SaveTask.TaskType.BACKUP));
      }
   }

   public static final class DelayedTask {
      public DelayedTask(ConfigManager.CFile var1, SaveTask.TaskType var2) {
         if (var2 == SaveTask.TaskType.SAVE) {
            OnyxShop.getInstance().runKillableTaskLater(var1::save, 20L);
         } else if (var2 == SaveTask.TaskType.BACKUP) {
            ConfigUtil.createBackup(var1.getName(), true);
         }
      }
   }

   public static enum TaskType {
      SAVE,
      BACKUP;

      private TaskType() {
      }
   }
}
