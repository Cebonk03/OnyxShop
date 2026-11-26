package com.onyxshop.util;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository.Builder;

public class PaperLibrariesLoader implements PluginLoader {
   public PaperLibrariesLoader() {
   }

   public void classloader(PluginClasspathBuilder var1) {
      MavenLibraryResolver var2 = new MavenLibraryResolver();
      var2.addRepository(new Builder("central", "default", this.getDefaultMavenCentralMirror()).build());
      var2.addDependency(new Dependency(new DefaultArtifact("org.postgresql:postgresql:42.7.4"), null));
      var1.addLibrary(var2);
   }

   private String getDefaultMavenCentralMirror() {
      String var1 = System.getenv("PAPER_DEFAULT_CENTRAL_REPOSITORY");
      if (var1 == null) {
         var1 = System.getProperty("org.bukkit.plugin.java.LibraryLoader.centralURL");
      }

      if (var1 == null) {
         var1 = "https://maven-central.storage-download.googleapis.com/maven2";
      }

      return var1;
   }
}
