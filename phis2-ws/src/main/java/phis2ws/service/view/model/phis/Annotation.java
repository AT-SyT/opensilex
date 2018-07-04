//******************************************************************************
//                                       Annotation.java
//
// Author(s): Arnaud Charleroy<arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 14 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  14 juin 2018
// Subject: Represents an annotation
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;
import org.joda.time.DateTime;

/**
 * Represents an annotation
 *
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */
public class Annotation {

    // uri of this annotation eg.  http://www.phenome-fppn.fr/platform/id/annotation/8247af37-769c-495b-8e7e-78b1141176c2
    private String uri;

    // Creation date of this annotation format yyyy-MM-dd HH:mm:ssZ eg. 2018-06-25 15:13:59+0200
    private DateTime created;
    
    // creator of this annotations eg. http://www.phenome-fppn.fr/diaphen/id/agent/acharleroy
    private String creator;
    
    // Comment that describe this annotation eg. Ustilago maydis infection
    private String bodyValue;
    
    // motivation instance uri that describe the purpose of this annotation  eg. http://www.w3.org/ns/oa#commenting
    private String motivatedBy;
    
    // uris that are annoted by this annotation  eg. http://www.phenome-fppn.fr/diaphen/2017/o1032481
    private ArrayList<String> targets;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(DateTime created) {
        this.created = created;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getBodyValue() {
        return bodyValue;
    }

    public void setBodyValue(String bodyValue) {
        this.bodyValue = bodyValue;
    }

    public ArrayList<String> getTargets() {
        return targets;
    }

    public void setTargets(ArrayList<String> targets) {
        this.targets = targets;
    }

    public void addTarget(String target) {
        // If null arraylist is initialized
        if (this.targets == null) {
            this.targets = new ArrayList<>();
        }
        this.targets.add(target);
    }

    public String getMotivatedBy() {
        return motivatedBy;
    }

    public void setMotivatedBy(String motivatedBy) {
        this.motivatedBy = motivatedBy;
    }
}
