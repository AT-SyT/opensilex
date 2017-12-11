//**********************************************************************************************
//                                       ConcernItem.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 11 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  December, 11 2017
// Subject: Represents the concern item view
//***********************************************************************************************
package phis2ws.service.view.model.phis;

public class ConcernItem {
    private String uri;
    private String typeURI;

    public ConcernItem() {
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTypeURI() {
        return typeURI;
    }

    public void setTypeURI(String typeURI) {
        this.typeURI = typeURI;
    }
}
