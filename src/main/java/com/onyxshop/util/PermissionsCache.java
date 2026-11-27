package com.onyxshop.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PermissionsCache {
   private static final LoadingCache<PermissionHolder, Boolean> PERMISSIONS_CACHE = CacheBuilder.newBuilder()
      .expireAfterWrite(10L, TimeUnit.SECONDS)
      .build(new CacheLoader<PermissionHolder, Boolean>() {
         public Boolean load(PermissionHolder permissionHolder) {
            Player player = Bukkit.getPlayer(permissionHolder.getOwner());
            return player != null && player.hasPermission(permissionHolder.getPermission());
         }
      });

   public PermissionsCache() {
   }

   public static boolean hasPermission(Player player, String permission) {
      try {
         return (Boolean)PERMISSIONS_CACHE.get(new PermissionHolder(player, permission));
      } catch (ExecutionException exception) {
         return player.hasPermission(permission);
      }
   }

   public static boolean hasPermission(CommandSender sender, String permission) {
      try {
         return !(sender instanceof Player) || (Boolean)PERMISSIONS_CACHE.get(new PermissionHolder((Player)sender, permission));
      } catch (ExecutionException exception) {
         return sender.hasPermission(permission);
      }
   }

   public static boolean hasPermission(Object value, String permission) {
      try {
         return !(value instanceof Player) || (Boolean)PERMISSIONS_CACHE.get(new PermissionHolder((Player)value, permission));
      } catch (ExecutionException exception) {
         return ((CommandSender)value).hasPermission(permission);
      }
   }
}
