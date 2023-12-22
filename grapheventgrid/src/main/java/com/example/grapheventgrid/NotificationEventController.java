// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.example.grapheventgrid;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.ChangeNotification;
import com.microsoft.graph.models.Subscription;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;

import okhttp3.Request;

/**
 * This controller handles requests from Azure Event Grid
 */
@RestController
public class NotificationEventController {
    private static final Logger LOG = LoggerFactory
        .getLogger(NotificationEventController.class);

    @Autowired
    private GraphClientService graphClientService;

    /**
     * Handles the validation request from Azure Event Grid
     * @param requestOrigin The value of the "Webhook-Request-Origin" header
     * @param requestRate The value of the "Webhook-Request-Rate" header
     * @return 200 response with validation headers set
     */
    @RequestMapping(path = "/", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> validateEndpoint(
        @RequestHeader("WEBHOOK-REQUEST-ORIGIN") final String requestOrigin,
        @RequestHeader(value = "WEBHOOK-REQUEST_RATE", required = false) final String requestRate) {
        final HttpHeaders responseHeaders = new HttpHeaders();
        // See
        // https://github.com/cloudevents/spec/blob/v1.0/http-webhook.md#4-abuse-protection
        // Event Grid sends the host that emits events in this header as a request
        // for our webhook to allow them to send
        if (requestOrigin != null) {
            responseHeaders.set("WebHook-Allowed-Origin", requestOrigin);
        }

        if (requestRate != null) {
            responseHeaders.set("WebHook-Allowed-Rate", requestRate);
        }

        return ResponseEntity.ok().headers(responseHeaders).build();
    }

    /**
     * Handles notifications sent from Azure Event Grid
     * @param notification The deserialized notification payload.
     * @return 202 Accepted
     */
    @PostMapping("/")
    public ResponseEntity<Void> handleNotification(
        @RequestBody final CloudEventNotification notification) {
        final String notificationType = notification.getType();

        if (!notificationType.isBlank()) {
            LOG.info("Received {} notification from Event Grid", notificationType);

            final ChangeNotification changeNotification = Objects
                .requireNonNull(notification.getData());

            try {
                if (notificationType.equalsIgnoreCase("Microsoft.Graph.UserUpdated")) {
                    handleUserUpdate(changeNotification);
                } else if (notificationType
                    .equalsIgnoreCase("Microsoft.Graph.UserDeleted")) {
                    handleUserDelete(changeNotification);
                } else if (notificationType.equalsIgnoreCase(
                    "Microsoft.Graph.SubscriptionReauthorizationRequired")) {
                    handleSubscriptionRenewal(changeNotification);
                } else {
                    LOG.info("Unexpected notification type, ignoring.");
                }
            } catch (Exception ex) {

            }
        }

        return ResponseEntity.accepted().build();
    }

    /**
     * Handles Microsoft.Graph.UserUpdated notifications
     * @param notification The deserialized ChangeNotification from the data field of the original notification
     */
    private void handleUserUpdate(@Nonnull ChangeNotification notification) {
        // The user was either created, updated, or soft-deleted.
        // The notification only contains the user's ID, so
        // get the user from Microsoft Graph if other details are needed.
        // If the user isn't found, then it was likely deleted.

        // The notification has the relative URL to the user. The .WithUrl method
        // in the Graph client can use a URL to retrieve an object.
        final GraphServiceClient<Request> graphClient = graphClientService
            .getGraphClient();

        final String userId = Objects.requireNonNull(notification.resourceData)
            .additionalDataManager().get("id").getAsString();

        try {
            final User updatedUser = Objects.requireNonNull(
                graphClient.users(Objects.requireNonNull(userId)).buildRequest().get());

            LOG.info("User {} (ID: {}) was created or updated", updatedUser.displayName,
                updatedUser.id);
        } catch (GraphServiceException ex) {
            final String errorCode = Objects.requireNonNull(
                Objects.requireNonNull(Objects.requireNonNull(ex.getError()).error).code);
            if (errorCode.equalsIgnoreCase("Request_ResourceNotFound")) {
                LOG.info("User with ID {} was soft-deleted", userId);
            } else {
                LOG.error(ex.getMessage());
            }
        }
    }

    /**
     * Handles Microsoft.Graph.UserDeleted notifications
     * @param notification The deserialized ChangeNotification from the data field of the original notification
     */
    private void handleUserDelete(@Nonnull ChangeNotification notification) {
        // The user was permanently deleted. The notification only contains
        // the user's ID, and we can no longer get the user from Graph.
        final String userId = Objects.requireNonNull(notification.resourceData)
            .additionalDataManager().get("id").getAsString();
        LOG.info("User with ID {} was deleted", userId);
    }

    /**
     * Handles Microsoft.Graph.SubscriptionReauthorizationRequired notifications
     * @param notification The deserialized ChangeNotification from the data field of the original notification
     */
    private void handleSubscriptionRenewal(@Nonnull ChangeNotification notification) {
        // The subscription needs to be renewed
        final String subscriptionId = Objects.requireNonNull(notification.subscriptionId)
            .toString();

        final GraphServiceClient<Request> graphClient = Objects
            .requireNonNull(graphClientService.getGraphClient());

        final Subscription subscriptionUpdate = new Subscription();
        subscriptionUpdate.expirationDateTime = OffsetDateTime.now().plus(1,
            ChronoUnit.HOURS);

        graphClient.subscriptions(Objects.requireNonNull(subscriptionId)).buildRequest()
            .patch(subscriptionUpdate);

        LOG.info("Subscription with ID {} renewed for another hour", subscriptionId);
    }
}
