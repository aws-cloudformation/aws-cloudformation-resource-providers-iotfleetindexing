package com.amazonaws.iot.fleetmetric;

import org.apache.commons.lang3.exception.ExceptionUtils;
import software.amazon.awssdk.services.iot.model.IndexNotReadyException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

    static ProgressEvent<ResourceModel, CallbackContext> translateExceptionToProgressEvent(
            ResourceModel model, Exception e, Logger logger) {

        HandlerErrorCode errorCode = translateExceptionToErrorCode(e, logger);
        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.FAILED)
                        .errorCode(errorCode)
                        .build();
        if (errorCode != HandlerErrorCode.InternalFailure) {
            progressEvent.setMessage(e.getMessage());
        }
        return progressEvent;
    }


    static HandlerErrorCode translateExceptionToErrorCode(Exception e, Logger logger) {
        logger.log(String.format("Translating exception \"%s\", stack trace: %s",
                e.getMessage(), ExceptionUtils.getStackTrace(e)));

        // For Throttling and InternalFailure, we want CFN to retry, and it will do so based on the error code.
        // Reference with Retriable/Terminal in comments for each: https://tinyurl.com/y378qdno
        if (e instanceof ResourceAlreadyExistsException) {
            return HandlerErrorCode.AlreadyExists;
        } else if (e instanceof InvalidRequestException) {
            return HandlerErrorCode.InvalidRequest;
        } else if (e instanceof LimitExceededException) {
            return HandlerErrorCode.ServiceLimitExceeded;
        } else if (e instanceof UnauthorizedException) {
            return HandlerErrorCode.AccessDenied;
        } else if (e instanceof InternalFailureException) {
            return HandlerErrorCode.InternalFailure;
        } else if (e instanceof ThrottlingException) {
            return HandlerErrorCode.Throttling;
        } else if (e instanceof ResourceNotFoundException) {
            return HandlerErrorCode.NotFound;
        } else if (e instanceof IotException && ((IotException) e).statusCode() == 403) {
            return HandlerErrorCode.AccessDenied;
        } else if (e instanceof IndexNotReadyException) {
            return HandlerErrorCode.GeneralServiceException;
        } else {
            logger.log(String.format("Unexpected exception \"%s\"", e.getMessage()));
            // Any other exception at this point is unexpected.k
            return HandlerErrorCode.InternalFailure;
        }
    }

    static Set<Tag> translateTagsToSdk(Map<String, String> tags) {

        if (tags == null) {
            return Collections.emptySet();
        }

        return tags.keySet().stream()
                .map(key -> Tag.builder()
                        .key(key)
                        .value(tags.get(key))
                        .build())
                .collect(Collectors.toSet());
    }

    static Set<com.amazonaws.iot.fleetmetric.Tag> translateTagsToCfn(
            List<software.amazon.awssdk.services.iot.model.Tag> tags) {

        if (tags == null) {
            return Collections.emptySet();
        }

        return tags.stream()
                .map(tag -> com.amazonaws.iot.fleetmetric.Tag.builder()
                        .key(tag.key())
                        .value(tag.value())
                        .build())
                .collect(Collectors.toSet());
    }
}
