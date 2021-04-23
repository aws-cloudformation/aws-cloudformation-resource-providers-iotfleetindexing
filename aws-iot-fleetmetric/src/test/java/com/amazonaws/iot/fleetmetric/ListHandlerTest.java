package com.amazonaws.iot.fleetmetric;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListFleetMetricsRequest;
import software.amazon.awssdk.services.iot.model.ListFleetMetricsResponse;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.List;

import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_ARN;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_ARN2;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_NAME;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_NAME2;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_NAME_AND_ARN;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_NAME_AND_ARN2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ListHandler(mock(IotClient.class));
    }

    @AfterEach
    public void afterEach() {
        verifyNoMoreInteractions(proxy);
    }

    @Test
    public void handleRequest_HappyCase_VerifyRequestResponse() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .nextToken("nextToken1")
                .build();

        ListFleetMetricsRequest expectedRequest = ListFleetMetricsRequest.builder()
                .nextToken(request.getNextToken())
                .build();

        ListFleetMetricsResponse listResponse = ListFleetMetricsResponse.builder()
                .fleetMetrics(FLEET_METRIC_NAME_AND_ARN, FLEET_METRIC_NAME_AND_ARN2)
                .nextToken("nextToken2")
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(expectedRequest), any()))
                .thenReturn(listResponse);

        ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        assertThat(response.getNextToken()).isEqualTo("nextToken2");
        List<ResourceModel> expectedModels = Arrays.asList(
                ResourceModel.builder()
                        .metricName(FLEET_METRIC_NAME)
                        .metricArn(FLEET_METRIC_ARN)
                        .build(),
                ResourceModel.builder()
                        .metricName(FLEET_METRIC_NAME2)
                        .metricArn(FLEET_METRIC_ARN2)
                        .build());
        assertThat(response.getResourceModels()).isEqualTo(expectedModels);
    }

    @Test
    public void handleRequest_ThrowUnauthorized_VerifyTranslation() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .nextToken("nextToken1")
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(UnauthorizedException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }
}
