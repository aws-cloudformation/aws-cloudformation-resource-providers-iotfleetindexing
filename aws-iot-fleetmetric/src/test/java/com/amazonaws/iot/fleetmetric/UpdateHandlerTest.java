package com.amazonaws.iot.fleetmetric;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeFleetMetricRequest;
import software.amazon.awssdk.services.iot.model.DescribeFleetMetricResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotRequest;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UpdateFleetMetricRequest;
import software.amazon.awssdk.services.iot.model.UpdateFleetMetricResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.amazonaws.iot.fleetmetric.TestConstants.DESIRED_TAGS;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_AGGREGATION_FIELD;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_AGGREGATION_TYPE_NAME;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_AGGREGATION_TYPE_VALUES;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_ARN;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_INDEX_NAME;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_LOGICAL_RESOURCE_IDENTIFIER;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_NAME;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_PERIOD;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_QUERY_STRING;
import static com.amazonaws.iot.fleetmetric.TestConstants.FLEET_METRIC_RESOURCE_MODEL;
import static com.amazonaws.iot.fleetmetric.TestConstants.MODEL_TAGS;
import static com.amazonaws.iot.fleetmetric.TestConstants.SDK_SYSTEM_TAG;
import static com.amazonaws.iot.fleetmetric.TestConstants.SYSTEM_TAG_MAP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {
    protected static final software.amazon.awssdk.services.iot.model.Tag PREVIOUS_SDK_RESOURCE_TAG =
            software.amazon.awssdk.services.iot.model.Tag.builder()
                    .key("PreviousTagKey")
                    .value("PreviousTagValue")
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.Tag DESIRED_SDK_RESOURCE_TAG =
            software.amazon.awssdk.services.iot.model.Tag.builder()
                    .key("DesiredTagKey")
                    .value("DesiredTagValue")
                    .build();

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Spy
    private UpdateHandler handler = spy(new UpdateHandler(mock(IotClient.class)));

    @AfterEach
    public void afterEach() {
        verifyNoMoreInteractions(proxy);
    }

    @Test
    public void handleRequest_BothValueAndTagsAreUpdated_VerifyRequests() {
        ResourceModel previousModel = FLEET_METRIC_RESOURCE_MODEL;
        ResourceModel desiredModel = getDesiredModel();
        Map<String, String> desiredTags = ImmutableMap.of("DesiredTagKey", "DesiredTagValue");

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousModel)
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .desiredResourceState(desiredModel)
                .desiredResourceTags(desiredTags)
                .systemTags(SYSTEM_TAG_MAP)
                .build();

        doReturn(ImmutableSet.of(PREVIOUS_SDK_RESOURCE_TAG, SDK_SYSTEM_TAG))
                .when(handler)
                .listTags(proxy, FLEET_METRIC_ARN, logger);

        doReturn(UpdateFleetMetricResponse.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(UpdateFleetMetricRequest.class), any());
        doReturn(DescribeFleetMetricResponse.builder().metricArn(FLEET_METRIC_ARN).build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeFleetMetricRequest.class), any());

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(desiredModel);

        ArgumentCaptor<IotRequest> requestCaptor = ArgumentCaptor.forClass(IotRequest.class);
        verify(proxy, times(4)).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        List<IotRequest> submittedIotRequests = requestCaptor.getAllValues();

        UpdateFleetMetricRequest submittedUpdateRequest = (UpdateFleetMetricRequest) submittedIotRequests.get(0);
        assertThat(submittedUpdateRequest.metricName()).isEqualTo(FLEET_METRIC_NAME);

        DescribeFleetMetricRequest submittedDescribeRequest = (DescribeFleetMetricRequest) submittedIotRequests.get(1);
        assertThat(submittedDescribeRequest.metricName()).isEqualTo(FLEET_METRIC_NAME);

        TagResourceRequest submittedTagRequest = (TagResourceRequest) submittedIotRequests.get(2);
        assertThat(submittedTagRequest.tags()).isEqualTo(Collections.singletonList(DESIRED_SDK_RESOURCE_TAG));
        assertThat(submittedTagRequest.resourceArn()).isEqualTo(FLEET_METRIC_ARN);

        UntagResourceRequest submittedUntagRequest = (UntagResourceRequest) submittedIotRequests.get(3);
        assertThat(submittedUntagRequest.tagKeys()).isEqualTo(Collections.singletonList("PreviousTagKey"));
        assertThat(submittedUntagRequest.resourceArn()).isEqualTo(FLEET_METRIC_ARN);
    }

    @Test
    public void updateTags_SameKeyDifferentValue_OnlyTagCall() {
        software.amazon.awssdk.services.iot.model.Tag previousTag =
                software.amazon.awssdk.services.iot.model.Tag.builder()
                        .key("DesiredTagKey")
                        .value("PreviousTagValue")
                        .build();
        Map<String, String> desiredTags = ImmutableMap.of("DesiredTagKey", "DesiredTagValue");

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(ResourceModel.builder().build())
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .desiredResourceTags(desiredTags)
                .systemTags(SYSTEM_TAG_MAP)
                .build();

        doReturn(ImmutableSet.of(previousTag, SDK_SYSTEM_TAG))
                .when(handler)
                .listTags(proxy, FLEET_METRIC_ARN, logger);

        handler.updateTags(proxy, request, FLEET_METRIC_ARN, logger);

        ArgumentCaptor<TagResourceRequest> requestCaptor = ArgumentCaptor.forClass(TagResourceRequest.class);
        verify(proxy).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        TagResourceRequest submittedTagRequest = requestCaptor.getValue();
        assertThat(submittedTagRequest.tags()).isEqualTo(Collections.singletonList(DESIRED_SDK_RESOURCE_TAG));
    }

    @Test
    public void updateTags_NoDesiredTags_OnlyUntagCall() {
        Map<String, String> desiredTags = Collections.emptyMap();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(ResourceModel.builder().build())
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .desiredResourceTags(desiredTags)
                .systemTags(SYSTEM_TAG_MAP)
                .build();

        doReturn(ImmutableSet.of(PREVIOUS_SDK_RESOURCE_TAG, SDK_SYSTEM_TAG))
                .when(handler)
                .listTags(proxy, FLEET_METRIC_ARN, logger);

        handler.updateTags(proxy, request, FLEET_METRIC_ARN, logger);

        ArgumentCaptor<UntagResourceRequest> requestCaptor = ArgumentCaptor.forClass(UntagResourceRequest.class);
        verify(proxy).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        UntagResourceRequest submittedUntagRequest = requestCaptor.getValue();
        assertThat(submittedUntagRequest.tagKeys()).isEqualTo(Collections.singletonList("PreviousTagKey"));
    }

    @Test
    public void handleRequest_UpdateThrowsInvalidRequest_VerifyTranslation() {
        ResourceModel previousModel = FLEET_METRIC_RESOURCE_MODEL;
        ResourceModel desiredModel = getDesiredModel();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .previousResourceState(previousModel)
                .desiredResourceState(desiredModel)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(InvalidRequestException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void updateTags_ApiThrowsException_BubbleUp() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(ResourceModel.builder().build())
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .desiredResourceTags(ImmutableMap.of("DesiredTagKey", "DesiredTagValue"))
                .systemTags(SYSTEM_TAG_MAP)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(InvalidRequestException.builder().build());

        assertThatThrownBy(() ->
                handler.updateTags(proxy, request, FLEET_METRIC_ARN, logger))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void handleRequest_ResourceAlreadyDeleted_VerifyException() {
        ResourceModel previousModel = FLEET_METRIC_RESOURCE_MODEL;

        ResourceModel desiredModel = getDesiredModel();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .previousResourceState(previousModel)
                .desiredResourceState(desiredModel)
                .build();

        // If the resource is already deleted, the update API throws ResourceNotFoundException. Mocking that here.
        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceNotFoundException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_ModifyReadOnlyField_VerifyTranslation() {
        // adding ARN in update request would fail
        ResourceModel desiredModel = getDesiredModel();
        desiredModel.setMetricArn(FLEET_METRIC_ARN);

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .logicalResourceIdentifier(FLEET_METRIC_LOGICAL_RESOURCE_IDENTIFIER)
                .desiredResourceTags(DESIRED_TAGS)
                .systemTags(SYSTEM_TAG_MAP)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    private ResourceModel getDesiredModel() {
        return ResourceModel.builder()
                .metricName(FLEET_METRIC_NAME)
                .queryString("newQueryString")
                .aggregationType(AggregationType.builder()
                        .name("Percentiles")
                        .values(Arrays.asList("50", "90", "99"))
                        .build())
                .indexName(FLEET_METRIC_INDEX_NAME)
                .period(FLEET_METRIC_PERIOD)
                .tags(MODEL_TAGS)
                .build();
    }
}
