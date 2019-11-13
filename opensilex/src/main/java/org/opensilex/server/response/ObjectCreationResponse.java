/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.response;

import java.net.URI;
import javax.ws.rs.core.Response;

/**
 *
 * @author vincent
 */
public class ObjectCreationResponse extends JsonResponse<String> {
    
    public ObjectCreationResponse(URI uri) {
        super(Response.Status.OK);
        this.metadata = new Metadata(new Pagination());
        this.metadata.addDataFile(uri);
    }
    
}
