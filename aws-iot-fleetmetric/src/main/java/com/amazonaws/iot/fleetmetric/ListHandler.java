package com.amazonaws.iot.fleetmetric;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListFleetMetricsRequest;
import software.amazon.awssdk.services.iot.model.ListFleetMetricsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ListHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public ListHandler() {
        iotClient = IotClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        ListFleetMetricsRequest listFleetMetricsRequest = ListFleetMetricsRequest.builder()
                .nextToken(request.getNextToken())
                .build();

        ListFleetMetricsResponse listFleetMetricsResponse;
        try {
            listFleetMetricsResponse = proxy.injectCredentialsAndInvokeV2(
                    listFleetMetricsRequest, iotClient::listFleetMetrics);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(request.getDesiredResourceState(), e, logger);
        }

        List<ResourceModel> models = listFleetMetricsResponse.fleetMetrics().stream()
                .map(fleetMetricNameAndArn -> ResourceModel.builder()
                        .metricName(fleetMetricNameAndArn.metricName())
                        .metricArn(fleetMetricNameAndArn.metricArn())
                        .build())
                .collect(Collectors.toList());

        logger.log(String.format("Listed %s resources for accountId %s.",
                ResourceModel.TYPE_NAME, request.getAwsAccountId()));

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(listFleetMetricsResponse.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
