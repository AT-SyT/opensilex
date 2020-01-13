//******************************************************************************
//                      ServerExtension.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.extensions;

import org.opensilex.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension interface for OpenSilex modules which add logic at server
 * initialisation and shutdown.
 *
 * @author Vincent Migot
 */
public interface ServerExtension {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServerExtension.class);
    
    /**
     * Hook on server initialisation
     *
     * @param server Unstarted server instance
     * @throws Exception Can throw anything
     */
    public default void initServer(Server server) throws Exception {
        LOGGER.debug("Initialize server for module: " + this.getClass().getCanonicalName());
    }

    /**
     * Hook on server shutdown
     * 
     * @param server Unstopped server instance
     * @throws Exception Can throw anything
     */
    public default void shutDownServer(Server server) throws Exception {
        LOGGER.debug("Shutdown server for module: " + this.getClass().getCanonicalName());
    }
}
