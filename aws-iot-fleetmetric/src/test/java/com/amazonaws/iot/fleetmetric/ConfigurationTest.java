package com.amazonaws.iot.fleetmetric;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationTest {

    private Configuration configuration;

    @BeforeEach
    public void setup() {
        configuration = new Configuration();
    }

    @Test
    public void resourceDefinedTags_ModelWithTags_VerifyTranslation() {

        Set<Tag> modelTags = ImmutableSet.of(
                Tag.builder()
                        .key("resourceTagKey")
                        .value("resourceTagValue")
                        .build(),
                Tag.builder()
                        .key("resourceTagKey2")
                        .value("resourceTagValue2")
                        .build());
        ResourceModel model = ResourceModel.builder()
                .metricName(FLEET_METRIC_NAME)
                .tags(modelTags)
                .build();

        Map<String, String> result = configuration.resourceDefinedTags(model);

        assertThat(result).isEqualTo(ImmutableMap.of(
                "resourceTagKey", "resourceTagValue",
                "resourceTagKey2", "resourceTagValue2"));
    }
}
