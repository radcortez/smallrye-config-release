package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

class SmallRyeConfigTest {
    @Test
    void getValues() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("my.list", "1,2,3,4")).build();

        List<Integer> values = config.getValues("my.list", Integer.class, ArrayList::new);
        assertEquals(Arrays.asList(1, 2, 3, 4), values);
    }

    @Test
    void getValuesConverter() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("my.list", "1,2,3,4")).build();

        List<Integer> values = config.getValues("my.list", config.getConverter(Integer.class).get(), ArrayList::new);
        assertEquals(Arrays.asList(1, 2, 3, 4), values);
    }

    @Test
    void getOptionalValues() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("my.list", "1,2,3,4")).build();

        Optional<List<Integer>> values = config.getOptionalValues("my.list", Integer.class, ArrayList::new);
        assertTrue(values.isPresent());
        assertEquals(Arrays.asList(1, 2, 3, 4), values.get());
    }

    @Test
    void getOptionalValuesConverter() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("my.list", "1,2,3,4")).build();

        Optional<List<Integer>> values = config.getOptionalValues("my.list", config.getConverter(Integer.class).get(),
                ArrayList::new);
        assertTrue(values.isPresent());
        assertEquals(Arrays.asList(1, 2, 3, 4), values.get());
    }

    @Test
    void rawValueEquals() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("my.prop", "1234")).build();

        assertTrue(config.rawValueEquals("my.prop", "1234"));
        assertFalse(config.rawValueEquals("my.prop", "0"));
    }

    @Test
    void convert() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();

        assertEquals(1234, config.convert("1234", Integer.class).intValue());
        assertNull(config.convert(null, Integer.class));
    }

    @Test
    void configValue() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("my.prop", "1234")).build();

        assertEquals("1234", config.getConfigValue("my.prop").getValue());
        assertEquals("1234", config.getValue("my.prop", ConfigValue.class).getValue());
        assertEquals("1234", config.getOptionalValue("my.prop", ConfigValue.class).get().getValue());
    }

    @Test
    void profiles() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withProfile("profile").build();
        assertEquals("profile", config.getProfiles().get(0));
    }

    @Test
    void unwrap() {
        Config config = new SmallRyeConfigBuilder().build();
        SmallRyeConfig smallRyeConfig = config.unwrap(SmallRyeConfig.class);

        assertNotNull(smallRyeConfig);
        assertThrows(IllegalArgumentException.class, () -> config.unwrap(Object.class));
    }
}
