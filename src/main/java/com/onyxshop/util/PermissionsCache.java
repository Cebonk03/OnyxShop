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
         public Boolean load(PermissionHolder var1) {
            Player var2 = Bukkit.getPlayer(var1.getOwner());
            return var2 != null && var2.hasPermission(var1.getPermission());
         }
      });

   public PermissionsCache() {
   }

   public static boolean hasPermission(Player var0, String var1) {
      try {
         return (Boolean)PERMISSIONS_CACHE.get(new PermissionHolder(var0, var1));
      } catch (ExecutionException var3) {
         return var0.hasPermission(var1);
      }
   }

   public static boolean hasPermission(CommandSender var0, String var1) {
      try {
         return !(var0 instanceof Player) || (Boolean)PERMISSIONS_CACHE.get(new PermissionHolder((Player)var0, var1));
      } catch (ExecutionException var3) {
         return var0.hasPermission(var1);
      }
   }

   public static boolean hasPermission(Object var0, String var1) {
      try {
         return !(var0 instanceof Player) || (Boolean)PERMISSIONS_CACHE.get(new PermissionHolder((Player)var0, var1));
      } catch (ExecutionException var3) {
         return ((CommandSender)var0).hasPermission(var1);
      }
   }
}
