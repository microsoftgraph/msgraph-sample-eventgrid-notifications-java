// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.example.grapheventgrid;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.microsoft.graph.models.ChangeNotification;

/**
 * Represents the JSON notification format sent by Azure Event Grid
 */
public class CloudEventNotification {
    private String type;
    private String specversion;
    private String source;
    private String subject;
    private String id;
    private OffsetDateTime time;
    private String datacontenttype;
    @JsonDeserialize(using = ChangeNotificationDeserializer.class)
    private ChangeNotification data;

    CloudEventNotification() {
    }

    CloudEventNotification(String type, String specversion, String source, String subject,
        String id, OffsetDateTime time, String datacontenttype, ChangeNotification data) {
        this.type = type;
        this.specversion = specversion;
        this.source = source;
        this.subject = subject;
        this.id = id;
        this.time = time;
        this.datacontenttype = datacontenttype;
        this.data = data;
    }

    public String getType() {
        return this.type;
    }

    public String getSpecversion() {
        return this.specversion;
    }

    public String getSource() {
        return this.source;
    }

    public String getSubject() {
        return this.subject;
    }

    public String getId() {
        return this.id;
    }

    public OffsetDateTime getTime() {
        return this.time;
    }

    public String getDatacontenttype() {
        return this.datacontenttype;
    }

    public ChangeNotification getData() {
        return this.data;
    }
}
