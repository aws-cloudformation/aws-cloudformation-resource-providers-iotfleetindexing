package com.amazonaws.iot.fleetmetric;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeFleetMetricRequest;
import software.amazon.awssdk.services.iot.model.DescribeFleetMetricResponse;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;

import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_AGGREGATION_FIELD;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_AGGREGATION_TYPE;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_AGGREGATION_TYPE_NAME;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_AGGREGATION_TYPE_VALUES;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_ARN;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_INDEX_NAME;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_NAME;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_PERIOD;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_QUERY_STRING;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_RESOURCE_MODEL;
import static com.amazonaws.iot.fleetmetric.TestConstants.SDK_MODEL_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Spy
    private ReadHandler handler = spy(new ReadHandler(mock(IotClient.class)));

    @AfterEach
    public void afterEach() {
        verifyNoMoreInteractions(proxy);
    }

    @Test
    public void handleRequest_HappyCase_VerifyRequestResponse() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(FLEET_METRIC_RESOURCE_MODEL)
                .build();

        DescribeFleetMetricRequest expectedDescribeRequest = DescribeFleetMetricRequest.builder()
                .metricName(FLEET_METRIC_NAME)
                .build();
        DescribeFleetMetricResponse describeResponse = DescribeFleetMetricResponse.builder()
                .metricName(FLEET_METRIC_NAME)
                .metricArn(FLEET_METRIC_ARN)
                .aggregationField(FLEET_METRIC_AGGREGATION_FIELD)
                .aggregationType(FLEET_METRIC_AGGREGATION_TYPE)
                .period(FLEET_METRIC_PERIOD)
                .queryString(FLEET_METRIC_QUERY_STRING)
                .indexName(FLEET_METRIC_INDEX_NAME)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(eq(expectedDescribeRequest), any()))
                .thenReturn(describeResponse);

        doReturn(Collections.singletonList(SDK_MODEL_TAG))
                .when(handler)
                .listTags(proxy, FLEET_METRIC_ARN, logger);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getMetricArn()).isEqualTo(FLEET_METRIC_ARN);
        assertThat(response.getResourceModel().getMetricName()).isEqualTo(FLEET_METRIC_NAME);
        assertThat(response.getResourceModel().getQueryString()).isEqualTo(FLEET_METRIC_QUERY_STRING);
        assertThat(response.getResourceModel().getAggregationType().getName())
                .isEqualTo(FLEET_METRIC_AGGREGATION_TYPE_NAME);
        assertThat(response.getResourceModel().getAggregationType().getValues())
                .isEqualTo(FLEET_METRIC_AGGREGATION_TYPE_VALUES);
        assertThat(response.getResourceModel().getPeriod()).isEqualTo(FLEET_METRIC_PERIOD);
        assertThat(response.getResourceModel().getIndexName()).isEqualTo(FLEET_METRIC_INDEX_NAME);
    }

    @Test
    public void handleRequest_ThrowThrottling_VerifyTranslation() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(FLEET_METRIC_RESOURCE_MODEL)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ThrottlingException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }
}
