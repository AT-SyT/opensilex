//******************************************************************************
//                                TraitDTO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 17 November 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resources.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.model.Trait;

/**
 * A class which contains methods to automatically check the attributes of a 
 * class, from rules defined by user.
 * Contains the list of the elements which might be send by the client  to save 
 * the database
 * @author morgane
 */
public class TraitDTO extends InstanceDefinitionDTO {
    final static Logger LOGGER = LoggerFactory.getLogger(TraitDTO.class);
    
    @Override
    public Trait createObjectFromDTO() {
        Trait trait = (Trait) super.createObjectFromDTO();
        return trait; 
    }
}
