//******************************************************************************
//                        NoSQLConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.service;

import org.opensilex.nosql.exceptions.NoSQLTransactionException;
import org.opensilex.service.ServiceConnection;

/**
 * Interface to describe big data connection required features.
 * <pre>
 * TODO: Only implement transaction for the moment, datanucleus integration
 * to achieve: http://www.datanucleus.org/
 * </pre>
 *
 * @see org.opensilex.nosql.service.BigDataService
 * @author Vincent Migot
 */
public interface NoSQLConnection extends ServiceConnection {

    /**
     * Start a transaction
     *
     * @throws NoSQLTransactionException
     */
    public void startTransaction() throws NoSQLTransactionException;

    /**
     * Commit a transaction
     *
     * @throws NoSQLTransactionException
     */
    public void commitTransaction() throws NoSQLTransactionException;

    /**
     * Rollback a transaction
     *
     * @throws NoSQLTransactionException
     */
    public void rollbackTransaction() throws NoSQLTransactionException;

}
