package com.amazonaws.iot.fleetmetric;

import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeFleetMetricRequest;
import software.amazon.awssdk.services.iot.model.DescribeFleetMetricResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class ReadHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public ReadHandler() {
        iotClient = IotClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        ResourceModel model = request.getDesiredResourceState();

        DescribeFleetMetricRequest describeFleetMetricRequest = DescribeFleetMetricRequest.builder()
                .metricName(model.getMetricName())
                .build();

        DescribeFleetMetricResponse describeFleetMetricResponse;
        try {
            describeFleetMetricResponse = proxy.injectCredentialsAndInvokeV2(
                    describeFleetMetricRequest, iotClient::describeFleetMetric);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }

        String metricArn = describeFleetMetricResponse.metricArn();
        logger.log(String.format("Called Describe for %s.", metricArn));

        // Now call ListTagsForResource, because describe API doesn't provide the tags.
        List<software.amazon.awssdk.services.iot.model.Tag> iotTags = listTags(proxy, metricArn, logger);
        logger.log(String.format("Called ListTags for %s.", metricArn));

        Set<Tag> responseTags = Translator.translateTagsToCfn(iotTags);

        logger.log(String.format("Successfully described %s.", metricArn));

        return ProgressEvent.defaultSuccessHandler(
                ResourceModel.builder()
                        .metricName(describeFleetMetricResponse.metricName())
                        .metricArn(describeFleetMetricResponse.metricArn())
                        .description(describeFleetMetricResponse.description())
                        .queryString(describeFleetMetricResponse.queryString())
                        .period(describeFleetMetricResponse.period())
                        .aggregationField(describeFleetMetricResponse.aggregationField())
                        .queryVersion(describeFleetMetricResponse.queryVersion())
                        .indexName(describeFleetMetricResponse.indexName())
                        .unit(describeFleetMetricResponse.unitAsString())
                        .aggregationType(AggregationType.builder()
                                .name(describeFleetMetricResponse.aggregationType().nameAsString())
                                .values(describeFleetMetricResponse.aggregationType().values())
                                .build())
                        .version((double)describeFleetMetricResponse.version())
                        .creationDate(describeFleetMetricResponse.creationDate() == null ?
                                        null : describeFleetMetricResponse.creationDate().toString())
                        .lastModifiedDate(describeFleetMetricResponse.lastModifiedDate() == null ?
                                        null : describeFleetMetricResponse.lastModifiedDate().toString())
                        .tags(responseTags)
                        .build());
    }

    // This facilitates mocking in the unit tests.
    // It would be nicer to instead pass HandlerUtils (which we can mock)
    // to the constructor, but the framework requires the constructor to have 0 args.
    @VisibleForTesting
    List<software.amazon.awssdk.services.iot.model.Tag> listTags(
            AmazonWebServicesClientProxy proxy,
            String resourceArn, Logger logger) {
        return HandlerUtils.listTags(iotClient, proxy, resourceArn, logger);
    }
}
