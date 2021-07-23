package com.amazonaws.iot.fleetmetric;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.AggregationType;
import software.amazon.awssdk.services.iot.model.CreateFleetMetricRequest;
import software.amazon.awssdk.services.iot.model.CreateFleetMetricResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.iot.fleetmetric.HandlerUtils.AWS_SYSTEM_TAG_PREFIX;

@RequiredArgsConstructor
public class CreateHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public CreateHandler() {
        iotClient = IotClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        CreateFleetMetricRequest createFleetMetricRequest = translateToCreateRequest(request, logger);

        ResourceModel model = request.getDesiredResourceState();
        if (!StringUtils.isEmpty(model.getMetricArn())) {
            logger.log(String.format("MetricArn is read-only, but the caller passed %s.", model.getMetricArn()));
            // Note: this is necessary even though MetricArn is marked readOnly in the schema.
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                    "MetricArn is a read-only property and cannot be set.");
        }

        CreateFleetMetricResponse createFleetMetricResponse;
        try {
            createFleetMetricResponse = proxy.injectCredentialsAndInvokeV2(
                    createFleetMetricRequest, iotClient::createFleetMetric);
        } catch (ResourceAlreadyExistsException e) {
            logger.log(String.format("Resource already exists %s.", model.getMetricName()));
            throw new CfnAlreadyExistsException(e);
        } catch (ResourceNotFoundException e) {
            logger.log(String.format("Indexing is not enabled when creating %s. Message: %s, stack trace: %s",
                    model.getMetricName(), e.getMessage(), ExceptionUtils.getStackTrace(e)));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, e.getMessage());
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }

        model.setMetricArn(createFleetMetricResponse.metricArn());
        logger.log(String.format("Created %s.", createFleetMetricResponse.metricArn()));

        return ProgressEvent.defaultSuccessHandler(model);
    }

    private CreateFleetMetricRequest translateToCreateRequest(
            ResourceHandlerRequest<ResourceModel> request,
            Logger logger) {

        ResourceModel model = request.getDesiredResourceState();

        // Combine all tags in one map that we'll use for the request
        Map<String, String> allTags = new HashMap<>();
        if (request.getDesiredResourceTags() != null) {
            // DesiredResourceTags includes both model and stack-level tags.
            // Reference: https://tinyurl.com/yyxtd7w6

            // TODO add system tags back once FleetMetric adds the support
            // allTags.putAll(request.getDesiredResourceTags());
            request.getDesiredResourceTags().entrySet().stream()
                    .filter(e -> !e.getKey().startsWith(AWS_SYSTEM_TAG_PREFIX))
                    .forEach(e -> allTags.put(e.getKey(), e.getValue()));
        }

        // TODO add system tags back once FleetMetric adds the support
//        if (request.getSystemTags() != null) {
//            // There are also system tags provided separately.
//            // SystemTags are the default stack-level tags with aws:cloudformation prefix
//             allTags.putAll(request.getSystemTags());
//        } else {
//            // System tags should always be present as long as the Handler is called by CloudFormation
//             logger.log("Unexpectedly, system tags are null in the create request for " +
//                  ResourceModel.TYPE_NAME + " " + model.getMetricName());
//        }

        return CreateFleetMetricRequest.builder()
                .metricName(model.getMetricName())
                .description(model.getDescription())
                .queryString(model.getQueryString())
                .period(model.getPeriod())
                .aggregationField(model.getAggregationField())
                .queryVersion(model.getQueryVersion())
                .indexName(model.getIndexName())
                .unit(model.getUnit())
                .aggregationType(AggregationType.builder()
                        .name(model.getAggregationType().getName())
                        .values(model.getAggregationType().getValues())
                        .build())
                .tags(Translator.translateTagsToSdk(allTags))
                .build();
    }
}
