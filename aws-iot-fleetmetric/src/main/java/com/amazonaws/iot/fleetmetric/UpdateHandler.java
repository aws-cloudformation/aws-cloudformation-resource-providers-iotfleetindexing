package com.amazonaws.iot.fleetmetric;

import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.AggregationType;
import software.amazon.awssdk.services.iot.model.DescribeFleetMetricRequest;
import software.amazon.awssdk.services.iot.model.DescribeFleetMetricResponse;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UpdateFleetMetricRequest;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.amazonaws.iot.fleetmetric.HandlerUtils.AWS_SYSTEM_TAG_PREFIX;

@RequiredArgsConstructor
public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public UpdateHandler() {
        iotClient = IotClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        ResourceModel desiredModel = request.getDesiredResourceState();
        String desiredArn = desiredModel.getMetricArn();
        if (!StringUtils.isEmpty(desiredArn)) {
            logger.log("MetricArn cannot be updated and will be ignored. Caller tried setting it to " + desiredArn);
        }

        try {
            UpdateFleetMetricRequest updateFleetMetricRequest = UpdateFleetMetricRequest.builder()
                    .metricName(desiredModel.getMetricName())
                    .description(desiredModel.getDescription())
                    .queryString(desiredModel.getQueryString())
                    .period(desiredModel.getPeriod())
                    .aggregationField(desiredModel.getAggregationField())
                    .queryVersion(desiredModel.getQueryVersion())
                    .indexName(desiredModel.getIndexName())
                    .unit(desiredModel.getUnit())
                    .aggregationType(AggregationType.builder()
                            .name(desiredModel.getAggregationType().getName())
                            .values(desiredModel.getAggregationType().getValues())
                            .build())
                    .build();

            proxy.injectCredentialsAndInvokeV2(updateFleetMetricRequest,
                    iotClient::updateFleetMetric);

            logger.log(String.format("UpdateFleetMetric for %s.", desiredModel.getMetricName()));
        } catch (RuntimeException e) {
            ProgressEvent<ResourceModel, CallbackContext> event = Translator.translateExceptionToProgressEvent(desiredModel, e, logger);
            return event;
        }

        // For an exiting resource, we have to update via TagResource API, Update API doesn't take tags.
        try {
            DescribeFleetMetricRequest describeFleetMetricRequest = DescribeFleetMetricRequest.builder()
                    .metricName(desiredModel.getMetricName())
                    .build();
            DescribeFleetMetricResponse describeFleetMetricResponse = proxy.injectCredentialsAndInvokeV2(
                    describeFleetMetricRequest, iotClient::describeFleetMetric);
            String actualArn = describeFleetMetricResponse.metricArn();

            logger.log(String.format("DescribeFleetMetric for %s.", desiredModel.getMetricName()));

            updateTags(proxy, request, actualArn, logger);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(desiredModel, e, logger);
        }

        return ProgressEvent.defaultSuccessHandler(request.getDesiredResourceState());
    }

    @VisibleForTesting
    void updateTags(AmazonWebServicesClientProxy proxy,
                    ResourceHandlerRequest<ResourceModel> request,
                    String resourceArn,
                    Logger logger) {
        // Note: we're intentionally getting currentTags by calling ListTags rather than getting
        // the previous state from CFN. This is in order to overwrite out-of-band changes.
        // For example, if we used request.getPreviousResourceTags instead of ListTags, if a user added a new tag
        // via TagResource and didn't add it to the template, we wouldn't know about it and wouldn't untag it.
        // Yet we should, otherwise the resource wouldn't equate the template.
        Set<Tag> currentTags = listTags(proxy, resourceArn, logger);

        // Combine all tags in one map that we'll use for the request
        Map<String, String> allDesiredTagsMap = new HashMap<>();
        if (request.getDesiredResourceTags() != null) {
            // DesiredResourceTags includes both model and stack-level tags.
            // Reference: https://tinyurl.com/yyxtd7w6
            // TODO add system tags back once FleetMetric auth is ready
            // allDesiredTagsMap.putAll(request.getDesiredResourceTags());
            request.getDesiredResourceTags().entrySet().stream()
                    .filter(e -> !e.getKey().startsWith(AWS_SYSTEM_TAG_PREFIX))
                    .forEach(e -> allDesiredTagsMap.put(e.getKey(), e.getValue()));
        }
        if (request.getSystemTags() != null) {
            // There are also system tags provided separately.
            // SystemTags are the default stack-level tags with aws:cloudformation prefix.
            // TODO add system tags back once FleetMetric auth is ready
            // allDesiredTagsMap.putAll(request.getSystemTags());
        } else {
            // System tags should never get updated as they are the stack id, stack name,
            // and logical resource id.
            logger.log("Unexpectedly, system tags are null in the update request for " + resourceArn);
        }
        Set<Tag> desiredTags = Translator.translateTagsToSdk(allDesiredTagsMap);
        Set<String> desiredTagKeys = desiredTags.stream()
                .map(Tag::key)
                .collect(Collectors.toSet());

        // TODO add system tags back once FleetMetric auth is ready
        Set<String> tagKeysToDetach = currentTags.stream()
                .filter(tag -> !tag.key().startsWith(AWS_SYSTEM_TAG_PREFIX))
                .filter(tag -> !desiredTagKeys.contains(tag.key()))
                .map(Tag::key)
                .collect(Collectors.toSet());
        Set<Tag> tagsToAttach = desiredTags.stream()
                .filter(tag -> !currentTags.contains(tag))
                .collect(Collectors.toSet());

        if (!tagsToAttach.isEmpty()) {
            TagResourceRequest tagResourceRequest = TagResourceRequest.builder()
                    .resourceArn(resourceArn)
                    .tags(tagsToAttach)
                    .build();
            proxy.injectCredentialsAndInvokeV2(tagResourceRequest, iotClient::tagResource);
            logger.log(String.format("Called TagResource for %s.", resourceArn));
        }

        if (!tagKeysToDetach.isEmpty()) {
            UntagResourceRequest untagResourceRequest = UntagResourceRequest.builder()
                    .resourceArn(resourceArn)
                    .tagKeys(tagKeysToDetach)
                    .build();
            proxy.injectCredentialsAndInvokeV2(untagResourceRequest, iotClient::untagResource);
            logger.log(String.format("Called UntagResource for %s.", resourceArn));
        }
    }

    // This facilitates mocking in the unit tests.
    // It would be nicer to instead pass HandlerUtils (which we can mock)
    // to the constructor, but the framework requires the constructor to have 0 args.
    @VisibleForTesting
    Set<Tag> listTags(AmazonWebServicesClientProxy proxy,
                      String resourceArn, Logger logger) {
        List<Tag> tags = HandlerUtils.listTags(iotClient, proxy, resourceArn, logger);
        return new HashSet<>(tags);
    }
}
