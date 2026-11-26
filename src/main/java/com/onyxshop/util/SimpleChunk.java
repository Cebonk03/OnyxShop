package com.onyxshop.util;

import org.bukkit.Chunk;

public class SimpleChunk {
   public final String world;
   public final int x1;
   public final int z1;

   public SimpleChunk(Chunk var1) {
      this.world = var1.getWorld().getName();
      this.x1 = var1.getX();
      this.z1 = var1.getZ();
   }

   public boolean contains(SimpleLocation var1) {
      return this.world.equals(var1.world) && this.x1 == var1.x >> 4 && this.z1 == var1.z >> 4;
   }
}
