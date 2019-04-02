//******************************************************************************
//                               LayerDTO.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: August 2017
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resources.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import opensilex.service.resources.validation.interfaces.Required;
import opensilex.service.resources.dto.manager.AbstractVerifiedClass;

/**
 * Represente le JSON soumis pour les objets de type token
 *
 * @author A. Charleroy
 */
@ApiModel
public class LogoutDTO extends AbstractVerifiedClass {

    @Required
    @ApiModelProperty(example = "2107aa78b05410a0dbb8f1d8b2d1b54b")
    public String access_token;

    @Override
    public Object createObjectFromDTO() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
