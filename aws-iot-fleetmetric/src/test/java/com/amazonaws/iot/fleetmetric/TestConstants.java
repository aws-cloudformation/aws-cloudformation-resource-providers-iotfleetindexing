package com.amazonaws.iot.fleetmetric;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import software.amazon.awssdk.services.iot.model.FleetMetricNameAndArn;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestConstants {

    protected static final String FLEET_METRIC_LOGICAL_RESOURCE_IDENTIFIER = "TestFleetMetricLogicalResourceId";
    protected static final String FLEET_METRIC_NAME = "TestFleetMetricName";
    protected static final String FLEET_METRIC_ARN = "arn:aws:iot:us-east-1:123456789012:fleetmetric/"
            + FLEET_METRIC_NAME;
    protected static final String FLEET_METRIC_INDEX_NAME = "AWS_Things";
    protected static final String FLEET_METRIC_QUERY_STRING = "thingName:*";
    protected static final String FLEET_METRIC_AGGREGATION_FIELD = "testField";
    protected static final String FLEET_METRIC_AGGREGATION_TYPE_NAME = "Statistics";
    protected static final Long FLEET_METRIC_VERSION = 1L;
    protected static final Instant FLEET_METRIC_CREATION_DATE = Instant.now();
    protected static final Instant FLEET_METRIC_LAST_MODIFIED_DATE = Instant.now();
    protected static final List<String> FLEET_METRIC_AGGREGATION_TYPE_VALUES = Arrays.asList("count", "sum", "average");
    protected static final software.amazon.awssdk.services.iot.model.AggregationType FLEET_METRIC_AGGREGATION_TYPE =
            software.amazon.awssdk.services.iot.model.AggregationType.builder()
            .name(FLEET_METRIC_AGGREGATION_TYPE_NAME)
            .values(FLEET_METRIC_AGGREGATION_TYPE_VALUES)
            .build();
    protected static final Integer FLEET_METRIC_PERIOD = 60;

    protected static final FleetMetricNameAndArn FLEET_METRIC_NAME_AND_ARN = FleetMetricNameAndArn.builder()
            .metricName(FLEET_METRIC_NAME)
            .metricArn(FLEET_METRIC_ARN)
            .build();

    protected static final String FLEET_METRIC_NAME2 = "TestFleetMetricName2";
    protected static final String FLEET_METRIC_ARN2 = "arn:aws:iot:us-east-1:123456789012:fleetmetric/"
            + FLEET_METRIC_NAME2;
    protected static final FleetMetricNameAndArn FLEET_METRIC_NAME_AND_ARN2 = FleetMetricNameAndArn.builder()
            .metricName(FLEET_METRIC_NAME2)
            .metricArn(FLEET_METRIC_ARN2)
            .build();

    protected static final Set<Tag> MODEL_TAGS = ImmutableSet.of(
            Tag.builder()
                    .key("resourceTagKey")
                    .value("resourceTagValue")
                    .build());
    protected static final Map<String, String> DESIRED_TAGS = ImmutableMap.of(
            "resourceTagKey", "resourceTagValue");
    static final Map<String, String> SYSTEM_TAG_MAP = ImmutableMap.of(
            "aws:cloudformation:stack-name", "UnitTestStack");
    protected static final software.amazon.awssdk.services.iot.model.Tag SDK_MODEL_TAG =
            software.amazon.awssdk.services.iot.model.Tag.builder()
                    .key("resourceTagKey")
                    .value("resourceTagValue")
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.Tag SDK_SYSTEM_TAG =
            software.amazon.awssdk.services.iot.model.Tag.builder()
                    .key("aws:cloudformation:stack-name")
                    .value("UnitTestStack")
                    .build();

    protected static final ResourceModel FLEET_METRIC_RESOURCE_MODEL = ResourceModel.builder()
            .metricName(FLEET_METRIC_NAME)
            .queryString(FLEET_METRIC_QUERY_STRING)
            .indexName(FLEET_METRIC_INDEX_NAME)
            .aggregationField(FLEET_METRIC_AGGREGATION_FIELD)
            .aggregationType(AggregationType.builder()
                    .name(FLEET_METRIC_AGGREGATION_TYPE_NAME)
                    .values(FLEET_METRIC_AGGREGATION_TYPE_VALUES)
                    .build())
            .period(FLEET_METRIC_PERIOD)
            .tags(MODEL_TAGS)
            .build();
}
