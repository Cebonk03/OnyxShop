package com.onyxshop.util;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;

public class PermissionHolder {
   final UUID uuid;
   final String world;
   final String permission;

   public PermissionHolder(Player player, String permission) {
      this.uuid = player.getUniqueId();
      this.world = player.getWorld().getName();
      this.permission = permission;
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
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (value != null && this.getClass() == value.getClass()) {
         PermissionHolder permissionHolder = (PermissionHolder)value;
         return this.uuid.equals(permissionHolder.getOwner()) && this.permission.equals(permissionHolder.getPermission()) && this.world.equals(permissionHolder.getWorld());
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.uuid, this.permission);
   }
}
