//******************************************************************************
//                          DataProvenanceModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.bson.Document;
import org.opensilex.core.data.api.DataAPI;
import org.opensilex.core.provenance.api.ProvenanceAPI;

/**
 * Provenance model used in DataModel
 * @author Alice Boizet
 */
@JsonPropertyOrder({"uri", "prov_used","settings"})
public class DataProvenanceModel {
    @NotNull
    @ApiModelProperty(value = "provenance uri", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI)
    URI uri;
    
    @JsonProperty("prov_used")
    @ApiModelProperty(value = "list of inputs of the process described in the provenance")
    List<ProvEntityModel> provUsed;
    
    @ApiModelProperty(value = "a key-value system to store specific information")
    Document settings; 

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public List<ProvEntityModel> getProvUsed() {
        return provUsed;
    }

    public void setProvUsed(List<ProvEntityModel> provUsed) {
        this.provUsed = provUsed;
    }

    public Document getSettings() {
        return settings;
    }

    public void setSettings(Document settings) {
        this.settings = settings;
    }
    
}
