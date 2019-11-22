//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.module;

import java.time.*;

/**
 *
 * @author Vincent Migot
 */
public interface ModuleUpdate {
    
    public LocalDateTime getDate();
    
    public String getDescription();
    
    public void execute() throws Exception;
}
