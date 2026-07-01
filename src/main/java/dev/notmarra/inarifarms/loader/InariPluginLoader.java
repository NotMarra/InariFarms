package dev.notmarra.inarifarms.loader;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class InariPluginLoader implements PluginLoader {
    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        List<String> libraries = List.of(
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.4.0",
                "xyz.xenondevs.invui:invui:2.1.1",
                "xyz.xenondevs.invui:invui-kotlin:2.1.1",
                "org.spongepowered:configurate-yaml:4.2.0",
                "org.spongepowered:configurate-extra-kotlin:4.2.0"
        );

        for (String coords : libraries) {
            resolver.addDependency(new Dependency(new DefaultArtifact(coords), null));
        }

        resolver.addRepository(
                new RemoteRepository.Builder("xenondevs", "default", "https://repo.xenondevs.xyz/releases").build()
        );
        resolver.addRepository(
                new RemoteRepository.Builder("central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build()
        );

        classpathBuilder.addLibrary(resolver);
    }
}