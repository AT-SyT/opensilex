//******************************************************************************
//                         MongoDBService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.ClientSession;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
import com.mongodb.client.result.DeleteResult;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.Order;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.exceptions.NoSQLInvalidUriListException;
import org.opensilex.nosql.mongodb.codec.ObjectCodec;
import org.opensilex.nosql.mongodb.codec.URICodec;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceDefaultDefinition(config = MongoDBConfig.class)
public class MongoDBService extends BaseService {

    private final static Logger LOGGER = LoggerFactory.getLogger(MongoDBService.class);
    private final String URI_FIELD = "uri";

    private final String dbName;
    private MongoClient mongoClient;
    private ClientSession session = null;
    private MongoDatabase db;
    public final static int SIZE_MAX = 10000;
    private URI baseURI;
    private static String defaultTimezone;

    public MongoDBService(MongoDBConfig config) {
        super(config);
        dbName = config.database();
        defaultTimezone = config.timezone();
    }

    @Override
    public void startup() throws OpenSilexModuleNotFoundException {
        mongoClient = getMongoDBClient();
        baseURI = getBaseURI();
        db = mongoClient.getDatabase(dbName);
    }

    @Override
    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public URI getBaseURI() throws OpenSilexModuleNotFoundException {
        return getOpenSilex().getModuleByClass(SPARQLModule.class).getBaseURI();
    }

    public static String getDefaultTimeZone() {
        return defaultTimezone;
    }

    public MongoDBConfig getImplementedConfig() {
        return (MongoDBConfig) this.getConfig();
    }

    public MongoDatabase getDatabase() {
        return this.db;
    }

    public ClientSession getSession() {
        return this.session;
    }

    private int transactionLevel = 0;

    public void startTransaction() {
        if (transactionLevel == 0) {
            LOGGER.debug("MONGO TRANSACTION START");
            session = mongoClient.startSession();
            session.startTransaction();
        }
        transactionLevel++;
    }

    public void commitTransaction() {
        transactionLevel--;
        if (transactionLevel == 0) {
            LOGGER.debug("MONGO TRANSACTION COMMIT");
            session.commitTransaction();
            session.close();
            session = null;
        }
    }

    public void rollbackTransaction() throws Exception {
        if (transactionLevel != 0) {
            LOGGER.error("MONGO TRANSACTION ROLLBACK");
            transactionLevel = 0;
            session.abortTransaction();
            session.close();
            session = null;
        }

    }

    public <T extends MongoModel> void create(T instance, Class<T> instanceClass, String collectionName, String prefix) throws Exception {
        LOGGER.debug("MONGO CREATE - Collection : " + collectionName);
        if (instance.getUri() == null) {
            generateUniqueUriIfNullOrValidateCurrent(instance, prefix, collectionName);
        }
        
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);        
        if (session != null) {
            collection.insertOne(session, instance);
        } else {
            collection.insertOne(instance);
        }            
    }

    public <T extends MongoModel> void createAll(List<T> instances, Class<T> instanceClass, String collectionName, String prefix) throws Exception {
        LOGGER.debug("MONGO CREATE - Collection : " + collectionName);
        for (T instance : instances) {
            if (instance.getUri() == null) {
                generateUniqueUriIfNullOrValidateCurrent(instance, prefix, collectionName);
            }
        }

        startTransaction();
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);

        try {
            collection.insertMany(session, instances);
            commitTransaction();
        } catch (Exception exception) {
            rollbackTransaction();
            throw exception;
        } finally {
            if (session != null) {
                session.close();
                session = null;
            }
        }

    }

    public <T> T findByURI(Class<T> instanceClass, String collectionName, URI uri) throws NoSQLInvalidURIException {
        LOGGER.debug("MONGO FIND BY URI - Collection : " + collectionName);
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        T instance = (T) collection.find(eq(URI_FIELD, uri)).first();
        if (instance == null) {
            throw new NoSQLInvalidURIException(uri);
        } else {
            return instance;
        }

    }

    public <T> List<T> findByURIs(Class<T> instanceClass, String collectionName, List<URI> uris) {
        LOGGER.debug("MONGO FIND BY URIS - Collection : " + collectionName);
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        Document listFilter = new Document();
        listFilter.append("$in", uris);
        Document filter = new Document();
        filter.append(URI_FIELD, listFilter);
        FindIterable<T> queryResult = collection.find(filter);
        List<T> instances = new ArrayList<>();
        for (T res : queryResult) {
            instances.add(res);
        }
        return instances;
    }

    public <T> boolean uriExists(Class<T> instanceClass, String collectionName, URI uri) {
        LOGGER.debug("MONGO URI EXISTS - Collection : " + collectionName);
        try {
            T instance = findByURI(instanceClass, collectionName, uri);
            return true;
        } catch (NoSQLInvalidURIException ex) {
            return false;
        }
    }

    public Set<URI> getMissingUrisFromCollection(String collectionName, Set<URI> uris) {
        LOGGER.debug("MONGO MISSING URIS - Collection : " + collectionName);
        Document listFilter = new Document();
        listFilter.append("$in", uris);
        Document filter = new Document();
        filter.append(URI_FIELD, listFilter);

        Set foundedURIs = distinct(URI_FIELD, URI.class, collectionName, filter);

        uris.removeAll(foundedURIs);
        return uris;
    }

    public Document buildSort(List<OrderBy> orderByList) {
        Document sort = new Document();
        if (orderByList != null && !orderByList.isEmpty()) {
            for (OrderBy orderBy : orderByList) {
                if (orderBy.getOrder().equals(Order.ASCENDING)) {
                    sort.put(orderBy.getFieldName(), 1);
                } else if (orderBy.getOrder().equals(Order.DESCENDING)) {
                    sort.put(orderBy.getFieldName(), -1);
                }
            }
        }
        return sort;
    }

    public <T> ListWithPagination<T> searchWithPagination(
            Class<T> instanceClass,
            String collectionName,
            Document filter,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) {

        List<T> results = new ArrayList<T>();
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        long resultsNumber = collection.countDocuments(filter);
        int total = (int) resultsNumber;

        LOGGER.debug("MONGO SEARCH WITH PAGINATION - Collection : " + collectionName + " - Order : " + LogOrderList(orderByList) + " - Filter : " + filter.toString());

        if (total > 0) {
            Document sort = buildSort(orderByList);

            FindIterable<T> queryResult = collection.find(filter).sort(sort).skip(page * pageSize).limit(pageSize);

            for (T res : queryResult) {
                results.add(res);
            }
        }

        return new ListWithPagination(results, page, pageSize, total);

    }

    public <T> List<T> search(
            Class<T> instanceClass,
            String collectionName,
            Document filter,
            List<OrderBy> orderByList) {

        LOGGER.debug("MONGO SEARCH - Collection : " + collectionName + " - Order : " + LogOrderList(orderByList) + " - Filter : " + filter.toString());

        List<T> results = new ArrayList<T>();
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        Document sort = buildSort(orderByList);
        FindIterable<T> queryResult = collection.find(filter).sort(sort);

        for (T res : queryResult) {
            results.add(res);
        }

        return results;

    }

    public <T> Set<T> distinct(
            String field,
            Class<T> resultClass,
            String collectionName,
            Document filter) {
        LOGGER.debug("MONGO SEARCH - Collection : " + collectionName + " - Field : " + field + " - Filter : " + filter.toString());
        Set<T> results = new HashSet<>();
        MongoCollection<T> collection = db.getCollection(collectionName, resultClass);

        DistinctIterable<T> queryResult = collection.distinct(field, filter, resultClass);

        for (T res : queryResult) {
            results.add(res);
        }

        return results;
    }
    
    public <T extends MongoModel> void delete(Class<T> instanceClass, String collectionName, URI uri) throws NoSQLInvalidURIException {
        LOGGER.debug("MONGO DELETE - Collection : " + collectionName);
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        T instance = findByURI(instanceClass, collectionName, uri);
        if (instance == null) {
            throw new NoSQLInvalidURIException(uri);
        } else {
            if (session != null) {
                collection.deleteOne(session, eq(URI_FIELD, uri));
            } else {
                collection.deleteOne(eq(URI_FIELD, uri));
            }
        }
    }

    public <T extends MongoModel> void delete(Class<T> instanceClass, String collectionName, List<URI> uris) throws NoSQLInvalidURIException, Exception {
        LOGGER.debug("MONGO DELETE - Collection : " + collectionName);
        Set<URI> notFoundedURIs = checkUriListExists(instanceClass, collectionName, (Set<URI>) uris);

        if (notFoundedURIs.isEmpty()) {
            startTransaction();
            MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
            for (URI uri : uris) {
                try {
                    collection.deleteOne(session, eq(URI_FIELD, uri));
                    commitTransaction();
                } catch (Exception exception) {
                    rollbackTransaction();
                    throw exception;
                } finally {
                    session.close();
                    session = null;
                }

            }
        } else {
            throw new NoSQLInvalidUriListException(uris);
        }
    }

    public <T extends MongoModel> void update(T newInstance, Class<T> instanceClass, String collectionName) throws NoSQLInvalidURIException {
        LOGGER.debug("MONGO UPDATE - Collection : " + collectionName);
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        T instance = findByURI(instanceClass, collectionName, newInstance.getUri());
        if (instance == null) {
            throw new NoSQLInvalidURIException(newInstance.getUri());
        } else {
            if (session != null) {
                collection.findOneAndReplace(session, eq(URI_FIELD, newInstance.getUri()), newInstance);
            } else {
                collection.findOneAndReplace(eq(URI_FIELD, newInstance.getUri()), newInstance);
            }
            
        }
    }

    public final MongoClient getMongoDBClient() {
        return getMongoDBClient(getImplementedConfig());
    }

    public static MongoClient getMongoDBClient(MongoDBConfig cfg) {
        String connectionString = "mongodb://";

        if (cfg.username() != null && cfg.password() != null && !cfg.username().isEmpty() && !cfg.password().isEmpty()) {
            connectionString += cfg.username() + ":" + cfg.password() + "@";
        }
        connectionString += cfg.host() + ":" + cfg.port();
        connectionString += "/" + cfg.database();

        Map<String, String> options = new HashMap<>();
        options.putAll(cfg.options());
        if (!StringUtils.isEmpty(cfg.authDB())) {
            options.put("authSource", cfg.authDB());
        }
        if (options.size() > 0) {
            connectionString += "?";
            Set<String> keys = options.keySet();
            int i = 0;
            for (String key : keys) {
                String value = options.get(key);
                connectionString += key + "=" + value;
                i++;

                if (i < options.size()) {
                    connectionString += "&";
                }
            }
        }
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromCodecs(new URICodec(), new ObjectCodec()),
                CodecRegistries.fromProviders(
                        new GeoJsonCodecProvider(),
                        PojoCodecProvider.builder().register(URI.class).automatic(true).build()
                )
        );

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .codecRegistry(codecRegistry)
                .build();
        return MongoClients.create(clientSettings);
    }

    /**
     * Method to generate a valid URI for the instance
     *
     * @param <T>
     * @param instance will be updated by a new generated URI
     * @param prefix
     * @param collectionName
     * @throws Exception
     */
    public <T extends MongoModel> void generateUniqueUriIfNullOrValidateCurrent(T instance, String prefix, String collectionName) throws Exception {
        URI uri = instance.getUri();

        if (uri == null) {

            int retry = 0;
            String graphPrefix = baseURI.resolve(prefix).toString();
            uri = instance.generateURI(graphPrefix, instance, retry);
            while (uriExists(instance.getClass(), collectionName, uri)) {
                uri = instance.generateURI(graphPrefix, instance, retry++);
            }
            instance.setUri(uri);

        } else if (uriExists(instance.getClass(), collectionName, uri)) {
            throw new NoSQLAlreadyExistingUriException(uri);
        }
    }

    private <T extends MongoModel> Set<URI> checkUriListExists(Class<T> instanceClass, String collectionName, Set<URI> uris) {
        Set foundedURIs = new HashSet<>();
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        Document filter = new Document();
        Document inFilter = new Document();
        inFilter.put("$in", uris);
        filter.put(URI_FIELD, inFilter);
        foundedURIs = distinct(URI_FIELD, instanceClass, collectionName, filter);
        uris.removeAll(foundedURIs);
        return uris;
    }

    public <T extends MongoModel> DeleteResult deleteOnCriteria(Class<T> instanceClass, String collectionName, Document filter) throws Exception {
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        try {
            DeleteResult result = collection.deleteMany(session, filter);
            commitTransaction();
            return result;
        } catch (Exception exception) {
            rollbackTransaction();
            throw exception;
        } finally {
            session.close();
            session = null;
        }
    }

    /**
     * Format order to string
     *
     * @param orders
     * @return
     */
    private String LogOrderList(List<OrderBy> orders) {
        if (orders == null || orders.isEmpty()) {
            return "No order";
        } else {
            return orders.toString();
        }
    }
}
