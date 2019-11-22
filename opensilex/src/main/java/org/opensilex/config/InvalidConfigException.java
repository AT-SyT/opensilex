//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.config;

/**
 *
 * @author Vincent Migot
 */
public class InvalidConfigException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidConfigException(Exception ex) {
        super(ex);
    }
    
    public InvalidConfigException(String message) {
        super(message);
    }

}
