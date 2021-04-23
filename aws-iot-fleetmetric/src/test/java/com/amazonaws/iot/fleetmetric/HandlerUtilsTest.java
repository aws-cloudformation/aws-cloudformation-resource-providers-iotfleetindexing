package com.amazonaws.iot.fleetmetric;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

import java.util.Arrays;
import java.util.List;

import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_ARN;
import static com.amazonaws.iot.fleetmetric.TestConstants.SDK_MODEL_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class HandlerUtilsTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private IotClient iotClient;

    @BeforeEach
    public void setup() {
        iotClient = mock(IotClient.class);
    }

    @AfterEach
    public void afterEach() {
        verifyNoMoreInteractions(proxy);
    }

    @Test
    public void listTags_WithNextToken_VerifyPagination() {
        ListTagsForResourceRequest expectedRequest1 = ListTagsForResourceRequest.builder()
                .resourceArn(FLEET_METRIC_ARN)
                .build();
        ListTagsForResourceResponse listTagsForResourceResponse1 = ListTagsForResourceResponse.builder()
                .tags(SDK_MODEL_TAG)
                .nextToken("testToken")
                .build();
        doReturn(listTagsForResourceResponse1).when(proxy).injectCredentialsAndInvokeV2(eq(expectedRequest1), any());

        ListTagsForResourceRequest expectedRequest2 = ListTagsForResourceRequest.builder()
                .resourceArn(FLEET_METRIC_ARN)
                .nextToken("testToken")
                .build();
        software.amazon.awssdk.services.iot.model.Tag tag2 = SDK_MODEL_TAG.toBuilder().key("key2").build();
        ListTagsForResourceResponse listTagsForResourceResponse2 = ListTagsForResourceResponse.builder()
                .tags(tag2)
                .build();
        doReturn(listTagsForResourceResponse2).when(proxy).injectCredentialsAndInvokeV2(eq(expectedRequest2), any());

        List<Tag> currentTags = HandlerUtils.listTags(iotClient, proxy, FLEET_METRIC_ARN, logger);
        assertThat(currentTags).isEqualTo(Arrays.asList(SDK_MODEL_TAG, tag2));
    }
}
