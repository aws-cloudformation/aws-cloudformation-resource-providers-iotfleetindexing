package com.amazonaws.iot.fleetmetric;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateFleetMetricRequest;
import software.amazon.awssdk.services.iot.model.CreateFleetMetricResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static com.amazonaws.iot.fleetmetric.TestConstants.DESIRED_TAGS;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_AGGREGATION_FIELD;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_AGGREGATION_TYPE;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_AGGREGATION_TYPE_NAME;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_AGGREGATION_TYPE_VALUES;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_ARN;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_INDEX_NAME;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_LOGICAL_RESOURCE_IDENTIFIER;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_NAME;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_PERIOD;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_QUERY_STRING;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_RESOURCE_MODEL;
import static com.amazonaws.iot.fleetmetric.TestConstants.SYSTEM_TAG_MAP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler(mock(IotClient.class));
    }

    @AfterEach
    public void afterEach() {
        verifyNoMoreInteractions(proxy);
    }

    @Test
    public void handleRequest_HappyCase_VerifyRequestResponse() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(FLEET_METRIC_RESOURCE_MODEL)
                .logicalResourceIdentifier(FLEET_METRIC_LOGICAL_RESOURCE_IDENTIFIER)
                .desiredResourceTags(DESIRED_TAGS)
                .systemTags(SYSTEM_TAG_MAP)
                .build();
        CreateFleetMetricResponse createFleetMetricResponse = CreateFleetMetricResponse.builder()
                .metricName(FLEET_METRIC_NAME)
                .metricArn(FLEET_METRIC_ARN)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenReturn(createFleetMetricResponse);

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

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

        ArgumentCaptor<CreateFleetMetricRequest> requestCaptor = ArgumentCaptor.forClass(CreateFleetMetricRequest.class);
        verify(proxy).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        CreateFleetMetricRequest actualRequest = requestCaptor.getValue();
        // Order doesn't matter for tags, but they're modeled as a List, thus we have to check field by field.
        // TODO add system tags back once FleetMetric gets FAS policies ready
        // assertThat(actualRequest.tags()).contains(SDK_MODEL_TAG, SDK_SYSTEM_TAG);
        assertThat(actualRequest.metricName()).isEqualTo(FLEET_METRIC_NAME);
        assertThat(actualRequest.queryString()).isEqualTo(FLEET_METRIC_QUERY_STRING);
        assertThat(actualRequest.aggregationType()).isEqualTo(FLEET_METRIC_AGGREGATION_TYPE);
        assertThat(actualRequest.period()).isEqualTo(FLEET_METRIC_PERIOD);
    }

    @Test
    public void handleRequest_ModifyReadOnlyField_VerifyTranslation() {
        // adding ARN in create request would fail
        ResourceModel fmResourceModel = ResourceModel.builder()
                .metricArn(FLEET_METRIC_ARN)
                .metricName(FLEET_METRIC_NAME)
                .queryString(FLEET_METRIC_QUERY_STRING)
                .indexName(FLEET_METRIC_INDEX_NAME)
                .aggregationField(FLEET_METRIC_AGGREGATION_FIELD)
                .aggregationType(AggregationType.builder()
                        .name(FLEET_METRIC_AGGREGATION_TYPE_NAME)
                        .values(FLEET_METRIC_AGGREGATION_TYPE_VALUES)
                        .build())
                .period(FLEET_METRIC_PERIOD)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(fmResourceModel)
                .logicalResourceIdentifier(FLEET_METRIC_LOGICAL_RESOURCE_IDENTIFIER)
                .desiredResourceTags(DESIRED_TAGS)
                .systemTags(SYSTEM_TAG_MAP)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_ProxyThrowsAlreadyExists_VerifyTranslation() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(FLEET_METRIC_RESOURCE_MODEL)
                .logicalResourceIdentifier(FLEET_METRIC_LOGICAL_RESOURCE_IDENTIFIER)
                .desiredResourceTags(DESIRED_TAGS)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceAlreadyExistsException.builder().build());

        assertThatThrownBy(() ->
                handler.handleRequest(proxy, request, null, logger))
                .isInstanceOf(CfnAlreadyExistsException.class);
    }

    @Test
    public void handleRequest_IndexNotEnabled_VerifyTranslation() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(FLEET_METRIC_RESOURCE_MODEL)
                .logicalResourceIdentifier(FLEET_METRIC_LOGICAL_RESOURCE_IDENTIFIER)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceNotFoundException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
