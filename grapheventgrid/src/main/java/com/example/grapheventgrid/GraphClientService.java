// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.example.grapheventgrid;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;

import okhttp3.Request;

@Service
public class GraphClientService {
    @Value("${azure.tenant-id}")
    private String tenantId;

    @Value("${azure.client-id}")
    private String clientId;

    @Value("${azure.client-secret}")
    private String clientSecret;

    private static List<String> graphScopes = List
        .of("https://graph.microsoft.com/.default");

    private static GraphServiceClient<Request> graphClient = null;

    public GraphServiceClient<Request> getGraphClient() {
        if (GraphClientService.graphClient == null) {
            final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .tenantId(this.tenantId).clientId(this.clientId)
                .clientSecret(this.clientSecret).build();

            final TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(
                Objects.requireNonNull(GraphClientService.graphScopes),
                Objects.requireNonNull(credential));

            GraphClientService.graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider).buildClient();
        }

        return GraphClientService.graphClient;
    }
}
