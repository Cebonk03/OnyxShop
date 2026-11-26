package com.onyxshop.util;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;

public class PermissionHolder {
   final UUID uuid;
   final String world;
   final String permission;

   public PermissionHolder(Player var1, String var2) {
      this.uuid = var1.getUniqueId();
      this.world = var1.getWorld().getName();
      this.permission = var2;
   }

   public UUID getOwner() {
      return this.uuid;
   }

   public String getWorld() {
      return this.world;
   }

   public String getPermission() {
      return this.permission;
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         PermissionHolder var2 = (PermissionHolder)var1;
         return this.uuid.equals(var2.getOwner()) && this.permission.equals(var2.getPermission()) && this.world.equals(var2.getWorld());
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.uuid, this.permission);
   }
}
