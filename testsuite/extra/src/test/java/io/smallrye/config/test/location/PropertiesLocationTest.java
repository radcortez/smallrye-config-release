package io.smallrye.config.test.location;

import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class PropertiesLocationTest {
    @Test
    void multipleResourcesInClassPath(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset("my.prop.one=1234\n"), "resources.properties");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        JavaArchive jarTwo = ShrinkWrap
                .create(JavaArchive.class, "resources-two.jar")
                .addAsResource(new StringAsset("my.prop.two=5678\n"), "resources.properties");

        Path filePathTwo = tempDir.resolve("resources-two.jar");
        jarTwo.as(ZipExporter.class).exportTo(filePathTwo.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/",
                "jar:" + filePathTwo.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = buildConfig("resources.properties");

            assertEquals("1234", config.getRawValue("my.prop.one"));
            assertEquals("5678", config.getRawValue("my.prop.two"));
            assertEquals(2, countSources(config, "resources.properties"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void multipleResourcesInClassPathYaml(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset("my:\n" +
                        "  prop:\n" +
                        "    one: 1234\n"), "resources.yml");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        JavaArchive jarTwo = ShrinkWrap
                .create(JavaArchive.class, "resources-two.jar")
                .addAsResource(new StringAsset("my:\n" +
                        "  prop:\n" +
                        "    two: 5678\n"), "resources.yml");

        Path filePathTwo = tempDir.resolve("resources-two.jar");
        jarTwo.as(ZipExporter.class).exportTo(filePathTwo.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {
                new URL("jar:" + filePathOne.toUri() + "!/"),
                new URL("jar:" + filePathTwo.toUri() + "!/"),
        }, contextClassLoader)) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = buildConfig("resources.yml");

            assertEquals("1234", config.getRawValue("my.prop.one"));
            assertEquals("5678", config.getRawValue("my.prop.two"));
            assertEquals(2, countSources(config, "resources.yml"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void jar(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset("my.prop.one=1234\n"), "resources.properties");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = buildConfig("jar:" + filePathOne.toUri() + "!/resources.properties");

            assertEquals("1234", config.getRawValue("my.prop.one"));
            assertEquals(1, countSources(config, "resources.properties"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void jarYaml(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset("my:\n" +
                        "  prop:\n" +
                        "    one: 1234\n"), "resources.yml");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {
                new URL("jar:" + filePathOne.toUri() + "!/")
        }, contextClassLoader)) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = buildConfig("jar:" + filePathOne.toUri() + "!/resources.yml");

            assertEquals("1234", config.getRawValue("my.prop.one"));
            assertEquals(1, countSources(config, "resources.yml"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void priorityLoadOrder(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset("my.prop.one=1234\n" +
                        "my.prop.common=1\n" +
                        "my.prop.jar.common=1\n"), "META-INF/microprofile-config.properties");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        JavaArchive jarTwo = ShrinkWrap
                .create(JavaArchive.class, "resources-two.jar")
                .addAsResource(new StringAsset("my.prop.two=5678\n" +
                        "my.prop.common=2\n" +
                        "my.prop.jar.common=2\n"), "META-INF/microprofile-config.properties");

        Path filePathTwo = tempDir.resolve("resources-two.jar");
        jarTwo.as(ZipExporter.class).exportTo(filePathTwo.toFile());

        Properties mainProperties = new Properties();
        mainProperties.setProperty("config_ordinal", "100");
        mainProperties.setProperty("my.prop.main", "main");
        mainProperties.setProperty("my.prop.common", "main");
        File mainFile = tempDir.resolve("microprofile-config.properties").toFile();
        try (FileOutputStream out = new FileOutputStream(mainFile)) {
            mainProperties.store(out, null);
        }

        Properties fallbackProperties = new Properties();
        fallbackProperties.setProperty("config_ordinal", "100");
        fallbackProperties.setProperty("my.prop.fallback", "fallback");
        fallbackProperties.setProperty("my.prop.common", "fallback");
        File fallbackFile = tempDir.resolve("fallback.properties").toFile();
        try (FileOutputStream out = new FileOutputStream(fallbackFile)) {
            fallbackProperties.store(out, null);
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/",
                "jar:" + filePathTwo.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultSources()
                    .addDiscoveredSources()
                    .addDefaultInterceptors()
                    .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS, mainFile.toURI() + "," + fallbackFile.toURI())
                    .build();

            // Check if all sources are up
            assertEquals("1234", config.getRawValue("my.prop.one"));
            assertEquals("5678", config.getRawValue("my.prop.two"));
            assertEquals("main", config.getRawValue("my.prop.main"));
            assertEquals("fallback", config.getRawValue("my.prop.fallback"));
            // This should be loaded by the first defined source in the locations configuration
            assertEquals("main", config.getRawValue("my.prop.common"));
            // This should be loaded by the first discovered source in the classpath
            assertEquals("1", config.getRawValue("my.prop.jar.common"));
            assertEquals(3, countSources(config, "microprofile-config.properties"));
            assertEquals(1, countSources(config, "fallback.properties"));
            assertTrue(stream(config.getConfigSources().spliterator(), false)
                    .filter(configSource -> configSource.getName().contains("microprofile-config.properties"))
                    .allMatch(configSource -> configSource.getOrdinal() == 100));
            assertTrue(stream(config.getConfigSources().spliterator(), false)
                    .filter(configSource -> configSource.getName().contains("fallback.properties"))
                    .allMatch(configSource -> configSource.getOrdinal() == 100));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void profilesClasspath(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset(
                        "config_ordinal=150\n" +
                                "my.prop.main=main\n" +
                                "my.prop.common=main\n" +
                                "my.prop.profile=main\n"),
                        "META-INF/config.properties")
                .addAsResource(new StringAsset(
                        "my.prop.common=common\n" +
                                "my.prop.profile=common\n"),
                        "META-INF/config-common.properties")
                .addAsResource(new StringAsset(
                        "my.prop.dev=dev\n" +
                                "my.prop.profile=dev\n"),
                        "META-INF/config-dev.properties");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        JavaArchive jarTwo = ShrinkWrap
                .create(JavaArchive.class, "resources-two.jar")
                .addAsResource(new StringAsset(
                        "config_ordinal=150\n" +
                                "my.prop.main=main\n" +
                                "my.prop.common=main\n" +
                                "my.prop.profile=main\n"),
                        "META-INF/config.properties");

        Path filePathTwo = tempDir.resolve("resources-two.jar");
        jarTwo.as(ZipExporter.class).exportTo(filePathTwo.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/",
                "jar:" + filePathTwo.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultSources()
                    .addDiscoveredSources()
                    .addDefaultInterceptors()
                    .withProfile("common,dev")
                    .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS, "META-INF/config.properties")
                    .build();

            assertEquals("main", config.getRawValue("my.prop.main"));
            assertEquals("common", config.getRawValue("my.prop.common"));
            assertEquals("dev", config.getRawValue("my.prop.profile"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void profilesJar(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset(
                        "config_ordinal=150\n" +
                                "my.prop.main=main\n" +
                                "my.prop.common=main\n" +
                                "my.prop.profile=main\n"),
                        "META-INF/config.properties")
                .addAsResource(new StringAsset(
                        "my.prop.common=common\n" +
                                "my.prop.profile=common\n"),
                        "META-INF/config-common.properties")
                .addAsResource(new StringAsset(
                        "my.prop.dev=dev\n" +
                                "my.prop.profile=dev\n"),
                        "META-INF/config-dev.properties");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultSources()
                    .addDiscoveredSources()
                    .addDefaultInterceptors()
                    .withProfile("common,dev")
                    .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS, "jar:" + filePathOne.toUri() + "!/META-INF/config.properties")
                    .build();

            assertEquals("main", config.getRawValue("my.prop.main"));
            assertEquals("common", config.getRawValue("my.prop.common"));
            assertEquals("dev", config.getRawValue("my.prop.profile"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void mixedProfiles(@TempDir Path tempDir) throws Exception {
        Properties mainProperties = new Properties();
        mainProperties.setProperty("config_ordinal", "150");
        mainProperties.setProperty("my.prop.main", "main-file");
        mainProperties.setProperty("my.prop.main.file", "main-file");
        mainProperties.setProperty("my.prop.common", "main-file");
        mainProperties.setProperty("my.prop.profile", "main-file");
        mainProperties.setProperty("order", "5");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve("config.properties").toFile())) {
            mainProperties.store(out, null);
        }

        Properties commonProperties = new Properties();
        commonProperties.setProperty("my.prop.common", "common-file");
        commonProperties.setProperty("my.prop.profile", "common-file");
        commonProperties.setProperty("order", "3");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve("config-common.properties").toFile())) {
            commonProperties.store(out, null);
        }

        Properties devProperties = new Properties();
        devProperties.setProperty("my.prop.dev", "dev-file");
        devProperties.setProperty("my.prop.profile", "dev-file");
        devProperties.setProperty("order", "1");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve("config-dev.properties").toFile())) {
            devProperties.store(out, null);
        }

        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset(
                        "config_ordinal=150\n" +
                                "my.prop.main=main-cp\n" +
                                "my.prop.main.cp=main-cp\n" +
                                "my.prop.common=main-cp\n" +
                                "my.prop.profile=main-cp\n" +
                                "order=6\n"),
                        "config.properties")
                .addAsResource(new StringAsset(
                        "my.prop.common=common-cp\n" +
                                "my.prop.profile=common-cp\n" +
                                "order=4\n"),
                        "config-common.properties")
                .addAsResource(new StringAsset(
                        "my.prop.dev=dev-cp\n" +
                                "my.prop.profile=dev-cp\n" +
                                "order=2\n"),
                        "config-dev.properties");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultSources()
                    .addDiscoveredSources()
                    .addDefaultInterceptors()
                    .withProfile("common,dev")
                    .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS,
                            tempDir.resolve("config.properties").toUri() + "," + "config.properties")
                    .build();

            assertEquals("main-file", config.getRawValue("my.prop.main.file"));
            assertEquals("main-cp", config.getRawValue("my.prop.main.cp"));
            assertEquals("main-file", config.getRawValue("my.prop.main"));
            assertEquals("common-file", config.getRawValue("my.prop.common"));
            assertEquals("dev-file", config.getRawValue("my.prop.profile"));

            List<ConfigSource> sources = stream(config.getConfigSources().spliterator(), false)
                    .filter(configSource -> configSource.getName().contains("config.properties")
                            || configSource.getName().contains("config-"))
                    .collect(toList());
            assertEquals(6, sources.size());
            assertEquals("1", sources.get(0).getValue("order"));
            assertEquals("2", sources.get(1).getValue("order"));
            assertEquals("3", sources.get(2).getValue("order"));
            assertEquals("4", sources.get(3).getValue("order"));
            assertEquals("5", sources.get(4).getValue("order"));
            assertEquals("6", sources.get(5).getValue("order"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void illegalChars(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources- space -one.jar")
                .addAsResource(new StringAsset("my.prop.main=main"), "META-INF/config.properties")
                .addAsResource(new StringAsset("my.prop.common=common"), "META-INF/config-common.properties")
                .addAsResource(new StringAsset("my.prop.dev=dev"), "META-INF/config-dev.properties");

        Path filePathOne = tempDir.resolve("resources- space -one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultSources()
                    .addDiscoveredSources()
                    .addDefaultInterceptors()
                    .withProfile("common,dev")
                    .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS, "META-INF/config.properties")
                    .build();

            assertEquals("main", config.getRawValue("my.prop.main"));
            assertEquals("common", config.getRawValue("my.prop.common"));
            assertEquals("dev", config.getRawValue("my.prop.dev"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void mixedExtensions(@TempDir Path tempDir) throws Exception {
        JavaArchive jar = ShrinkWrap
                .create(JavaArchive.class, "resources.jar")
                .addAsResource(new StringAsset("my:\n" +
                        "  prop:\n" +
                        "    one: 1234\n"), "resources.yml")
                .addAsResource(new StringAsset("my:\n" +
                        "  prop:\n" +
                        "    one: 5678\n"), "resources-prod.yaml");

        Path filePath = tempDir.resolve("resources.jar");
        jar.as(ZipExporter.class).exportTo(filePath.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {
                new URL("jar:" + filePath.toUri() + "!/")
        }, contextClassLoader)) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDiscoveredSources()
                    .addDefaultInterceptors()
                    .withProfile("prod")
                    .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS, "jar:" + filePath.toUri() + "!/resources.yml")
                    .build();

            assertEquals("5678", config.getRawValue("my.prop.one"));
            assertEquals(2, countSources(config, "resources"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void profileFilesInClassloader(@TempDir Path tempDir) throws Exception {
        Properties mainProperties = new Properties();
        mainProperties.setProperty("my.prop.main", "main");
        File mainFile = tempDir.resolve("application.properties").toFile();
        try (FileOutputStream out = new FileOutputStream(mainFile)) {
            mainProperties.store(out, null);
        }

        Properties testProperties = new Properties();
        testProperties.setProperty("my.prop.test", "test");
        File testFile = tempDir.resolve("application-test.properties").toFile();
        try (FileOutputStream out = new FileOutputStream(testFile)) {
            testProperties.store(out, null);
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { tempDir.toUri().toURL() }, contextClassLoader) {
            @Override
            public URL getResource(final String name) {
                if (name.endsWith("application-test.properties")) {
                    return null;
                }
                return super.getResource(name);
            }

            @Override
            public Enumeration<URL> getResources(final String name) throws IOException {
                if (name.endsWith("application-test.properties")) {
                    return Collections.emptyEnumeration();
                }
                return super.getResources(name);
            }

            @Override
            public Stream<URL> resources(final String name) {
                if (name.endsWith("application-test.properties")) {
                    return Stream.empty();
                }
                return super.resources(name);
            }
        }) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            urlClassLoader.getResource("application.properties");

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultSources()
                    .addDefaultInterceptors()
                    .withProfile("test")
                    .build();

            assertEquals("main", config.getRawValue("my.prop.main"));
            assertNull(config.getRawValue("my.prop.test"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private static URLClassLoader urlClassLoader(ClassLoader parent, String... urls) {
        return new URLClassLoader(Stream.of(urls).map(spec -> {
            try {
                return new URL(spec);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        }).toArray(URL[]::new), parent);
    }

    private static SmallRyeConfig buildConfig(String... locations) {
        return new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS, String.join(",", locations))
                .build();
    }

    private static int countSources(SmallRyeConfig config, String name) {
        return (int) stream(config.getConfigSources().spliterator(), false)
                .filter(configSource -> configSource.getName().contains(name))
                .count();
    }
}
