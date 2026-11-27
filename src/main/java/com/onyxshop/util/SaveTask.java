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

   public static void addSaveTask(ConfigManager.CFile cFile) {
      if (tasks.getIfPresent(cFile) == null) {
         tasks.put(cFile, new SaveTask.DelayedTask(cFile, SaveTask.TaskType.SAVE));
      }
   }

   public static void addBackupTask(ConfigManager.CFile cFile) {
      if (tasks.getIfPresent(cFile) == null) {
         tasks.put(cFile, new SaveTask.DelayedTask(cFile, SaveTask.TaskType.BACKUP));
      }
   }

   public static final class DelayedTask {
      public DelayedTask(ConfigManager.CFile cFile, SaveTask.TaskType taskType) {
         if (taskType == SaveTask.TaskType.SAVE) {
            OnyxShop.getInstance().runKillableTaskLater(cFile::save, 20L);
         } else if (taskType == SaveTask.TaskType.BACKUP) {
            ConfigUtil.createBackup(cFile.getName(), true);
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
