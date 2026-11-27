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

   public void classloader(PluginClasspathBuilder pluginClasspathBuilder) {
      MavenLibraryResolver mavenLibraryResolver = new MavenLibraryResolver();
      mavenLibraryResolver.addRepository(new Builder("central", "default", this.getDefaultMavenCentralMirror()).build());
      mavenLibraryResolver.addDependency(new Dependency(new DefaultArtifact("org.postgresql:postgresql:42.7.4"), null));
      pluginClasspathBuilder.addLibrary(mavenLibraryResolver);
   }

   private String getDefaultMavenCentralMirror() {
      String env = System.getenv("PAPER_DEFAULT_CENTRAL_REPOSITORY");
      if (env == null) {
         env = System.getProperty("org.bukkit.plugin.java.LibraryLoader.centralURL");
      }

      if (env == null) {
         env = "https://maven-central.storage-download.googleapis.com/maven2";
      }

      return env;
   }
}
