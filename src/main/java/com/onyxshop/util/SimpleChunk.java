package com.onyxshop.util;

import org.bukkit.Chunk;

public class SimpleChunk {
   public final String world;
   public final int x1;
   public final int z1;

   public SimpleChunk(Chunk chunk) {
      this.world = chunk.getWorld().getName();
      this.x1 = chunk.getX();
      this.z1 = chunk.getZ();
   }

   public boolean contains(SimpleLocation simpleLocation) {
      return this.world.equals(simpleLocation.world) && this.x1 == simpleLocation.x >> 4 && this.z1 == simpleLocation.z >> 4;
   }
}
