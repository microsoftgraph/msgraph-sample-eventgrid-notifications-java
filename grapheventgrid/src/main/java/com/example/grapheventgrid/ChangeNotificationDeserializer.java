// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.example.grapheventgrid;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.logger.DefaultLogger;
import com.microsoft.graph.models.ChangeNotification;
import com.microsoft.graph.serializer.DefaultSerializer;

public class ChangeNotificationDeserializer extends JsonDeserializer<ChangeNotification> {
    @Override
    public ChangeNotification deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException, JacksonException {
        final ObjectMapper mapper = new ObjectMapper();
        final TreeNode node = mapper.readTree(p);
        final String json = node.toString();
        if (json == null || json.isBlank()) {
            throw new JsonParseException("Invalid JSON");
        }

        try {
            final DefaultSerializer serializer = new DefaultSerializer(
                new DefaultLogger());
            return serializer.deserializeObject(json, ChangeNotification.class);
        } catch (Exception ex) {
            throw new JsonParseException(p, "Could not deserialize", ex);
        }
    }
}
