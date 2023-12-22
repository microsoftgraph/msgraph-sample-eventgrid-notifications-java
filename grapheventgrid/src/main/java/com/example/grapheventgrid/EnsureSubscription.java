// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.example.grapheventgrid;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.microsoft.graph.models.Subscription;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SubscriptionCollectionPage;

import jakarta.annotation.PostConstruct;
import okhttp3.Request;

@Component
public class EnsureSubscription {
    private static final Logger LOG = LoggerFactory.getLogger(EnsureSubscription.class);

    @Value("${azure.subscription-id}")
    private String subscriptionId;

    @Value("${azure.resource-group}")
    private String resourceGroup;

    @Value("${azure.event-grid-topic}")
    private String eventGridTopic;

    @Value("${azure.location}")
    private String location;

    @Autowired
    private GraphClientService graphClientService;

    @PostConstruct
    public void ensureSubscription() {
        final GraphServiceClient<Request> graphClient = Objects
            .requireNonNull(graphClientService.getGraphClient());

        final SubscriptionCollectionPage subscriptions = Objects
            .requireNonNull(graphClient.subscriptions().buildRequest().get());

        if (subscriptions.getCurrentPage().size() > 0) {
            LOG.info("Subscription already exists");
            return;
        }

        try {
            LOG.info("No existing subscription found");

            final StringBuilder eventGridUrlBuilder = new StringBuilder();
            final String eventGridUrl = eventGridUrlBuilder
                .append("EventGrid:?azuresubscriptionid=").append(this.subscriptionId)
                .append("&resourcegroup=").append(this.resourceGroup)
                .append("&partnertopic=").append(this.eventGridTopic).append("&location=")
                .append(this.location).toString();

            final Subscription newSubscription = new Subscription();
            newSubscription.changeType = "updated,deleted,created";
            newSubscription.resource = "users";
            newSubscription.clientState = "SomeSecretValue";
            newSubscription.notificationUrl = eventGridUrl;
            newSubscription.lifecycleNotificationUrl = eventGridUrl;
            newSubscription.expirationDateTime = OffsetDateTime.now().plus(1,
                ChronoUnit.HOURS);

            final Subscription createdSubscription = graphClient.subscriptions()
                .buildRequest().post(newSubscription);

            LOG.info("Created new subscription with ID {}",
                Objects.requireNonNull(createdSubscription).id);
            LOG.info(
                "Please activate the {} partner topic in the Azure portal and create an event subscription. See README for details.",
                this.eventGridTopic);
        } catch (Exception ex) {
            LOG.error("Error creating subscription: {}", ex.getMessage());
        }
    }
}
