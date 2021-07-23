package com.amazonaws.iot.fleetmetric;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

import java.util.ArrayList;
import java.util.List;

public class HandlerUtils {

    public static final String AWS_SYSTEM_TAG_PREFIX = "aws:";

    public static List<software.amazon.awssdk.services.iot.model.Tag> listTags(
            IotClient iotClient,
            AmazonWebServicesClientProxy proxy,
            String resourceArn,
            Logger logger) {

        String nextToken = null;
        List<Tag> result = new ArrayList<>();
        do {
            ListTagsForResourceRequest listTagsRequest = ListTagsForResourceRequest.builder()
                    .resourceArn(resourceArn)
                    .nextToken(nextToken)
                    .build();
            ListTagsForResourceResponse listTagsForResourceResponse = proxy.injectCredentialsAndInvokeV2(
                    listTagsRequest, iotClient::listTagsForResource);
            result.addAll(listTagsForResourceResponse.tags());
            nextToken = listTagsForResourceResponse.nextToken();
        } while (nextToken != null);

        logger.log(String.format("Listed tags for %s.", resourceArn));
        return result;
    }
}
