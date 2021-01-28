//******************************************************************************
//                          ProvenanceUpdateDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import javax.validation.constraints.NotNull;

/**
 * Provenance Update DTO
 * @author Alice Boizet
 */
public class ProvenanceUpdateDTO extends ProvenanceCreationDTO {
    
    @Override
    @NotNull    
    @ApiModelProperty(value = "uri of the provenance being updated", example = "air_temperature_acquisition")
    public URI getUri() {
        return uri;
    }
    
}
