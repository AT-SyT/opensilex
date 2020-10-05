//******************************************************************************
//                         NoSQLService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.service;

import com.mongodb.client.MongoClient;
import java.util.Collection;
import javax.jdo.JDOQLTypedQuery;
import javax.jdo.PersistenceManager;
import javax.naming.NamingException;
import org.opensilex.nosql.datanucleus.DataNucleusService;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceDefaultDefinition;

/**
 * Service for big data access and storage.
 * <pre>
 * TODO: Only implement transaction for the moment, datanucleus integration
 * to achieve: http://www.datanucleus.org/
 * </pre>
 *
 * @author Vincent Migot
 */
/**
 * Interface to describe big data connection required features.
 * <pre>
 * ObjectODO: Only implement transaction for the moment, datanucleus integration
 * to achieve: http://www.datanucleus.org/
 * </pre>
 *
 * @see org.opensilex.nosql.service.NoSQLService
 * @author Vincent Migot
 */
@ServiceDefaultDefinition(implementation = DataNucleusService.class)
public interface NoSQLService extends Service {

    public Object create(Object instance) throws NamingException;

    public void delete(Class cls, Object key) throws NamingException;

    public <T> T findById(Class cls, Object key) throws NamingException;

    public Long count(JDOQLTypedQuery query) throws NamingException;

    public Object update(Object instance) throws NamingException;

    public void createAll(Collection<Object> instances) throws NamingException;

    public void deleteAll(Collection<Object> instances) throws NamingException;

    public Long deleteAll(JDOQLTypedQuery query) throws NamingException;

    public PersistenceManager getPersistentConnectionManager() throws NamingException;

    public MongoClient getMongoDBClient();
}
