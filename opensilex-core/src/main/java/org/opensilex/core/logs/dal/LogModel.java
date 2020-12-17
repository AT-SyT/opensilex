/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.logs.dal;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.bson.Document;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 *
 * @author charlero
 */
 public class LogModel  extends MongoModel{

    private URI userUri;

    private String request;

    private String remoteAdress;

    private Document queryParameters;

    private LocalDateTime datetime;
     
    public LocalDateTime getDatetime() {
        return datetime;
    }

    public void setDatetime(LocalDateTime datetime) {
        this.datetime = datetime;
    }
 
    public Document getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(Document queryParmeters) {
        this.queryParameters = queryParmeters;
    }

    public URI getUserUri() {
        return userUri;
    }

    public void setUserUri(URI userUri) {
        this.userUri = userUri;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getRemoteAdress() {
        return remoteAdress;
    }

    public void setRemoteAdress(String remoteAdress) {
        this.remoteAdress = remoteAdress;
    }
    
     @Override
    public String[] getUriSegments(MongoModel instance) {
        Instant instant = datetime.atZone(ZoneOffset.UTC).toInstant();

         return new String[]{
            Timestamp.from(instant).toString(),
            userUri.toString(),
            remoteAdress
        };
    }

}
