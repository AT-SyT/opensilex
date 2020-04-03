//******************************************************************************
//                        RestConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest;

import org.opensilex.config.ConfigDescription;
import org.opensilex.rest.authentication.AuthenticationService;

/**
 * Default configuration for OpenSilex base module
 *
 * @author Vincent Migot
 */
public interface RestConfig {

    @ConfigDescription(
            value = "Authentication service"
    )
    public AuthenticationService authentication();

    @ConfigDescription(
            value = "Option to allow multiple connection with the same account (NOT RECOMMENDED IN PRODUCTION)",
            defaultBoolean = false
    )
    public boolean allowMultiConnection();
}
