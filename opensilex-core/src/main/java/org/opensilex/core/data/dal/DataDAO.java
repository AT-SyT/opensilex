//******************************************************************************
//                          DataDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import org.opensilex.core.exception.DataTypeException;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.bson.Document;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.exception.NoVariableDataTypeException;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sammy
 */
public class DataDAO {

    protected final URI RDFTYPE_VARIABLE;
    private final URI RDFTYPE_SCIENTIFICOBJECT;
    public static final String DATA_COLLECTION_NAME = "data";
    public static final String FILE_COLLECTION_NAME = "file";
    public final static String FS_FILE_PREFIX = "datafile";

    protected final MongoDBService nosql;
    protected final SPARQLService sparql;
    protected final FileStorageService fs; 
    
    private final static Logger LOGGER = LoggerFactory.getLogger(DataDAO.class);
    
    public DataDAO(MongoDBService nosql, SPARQLService sparql, FileStorageService fs) throws URISyntaxException {
        this.RDFTYPE_VARIABLE = new URI(Oeso.Variable.toString());
        this.RDFTYPE_SCIENTIFICOBJECT = new URI(Oeso.ScientificObject.toString());

        this.nosql = nosql;
        this.sparql = sparql;
        this.fs = fs;
    }

    public void createIndexes() {
        IndexOptions unicityOptions = new IndexOptions().unique(true);

        MongoCollection dataCollection = nosql.getDatabase()
                .getCollection(DATA_COLLECTION_NAME, DataModel.class);
        dataCollection.createIndex(Indexes.ascending("uri"), unicityOptions);
        dataCollection.createIndex(Indexes.ascending("variable", "provenance", "scientificObjects", "date"), unicityOptions);
        dataCollection.createIndex(Indexes.ascending("variable", "scientificObjects", "date"));

        MongoCollection fileCollection = nosql.getDatabase()
                .getCollection(FILE_COLLECTION_NAME, DataModel.class);
        fileCollection.createIndex(Indexes.ascending("uri"), unicityOptions);
        fileCollection.createIndex(Indexes.ascending("provenance", "scientificObjects", "date"), unicityOptions);
        dataCollection.createIndex(Indexes.ascending("scientificObjects", "date"));

    }

    public DataModel create(DataModel instance) throws Exception, MongoWriteException {
        nosql.create(instance, DataModel.class, DATA_COLLECTION_NAME, "id/data");
        return instance;
    }

    public DataFileModel createFile(DataFileModel instance) throws Exception, MongoBulkWriteException {
        nosql.create(instance, DataFileModel.class, FILE_COLLECTION_NAME, "id/file");
        return instance;
    }

    public List<DataModel> createAll(List<DataModel> instances) throws Exception {
        createIndexes(); 
        nosql.createAll(instances, DataModel.class, DATA_COLLECTION_NAME, "id/data");
        return instances;
    } 

    public List<DataFileModel> createAllFiles(List<DataFileModel> instances) throws Exception {
        createIndexes();
        nosql.createAll(instances, DataFileModel.class, FILE_COLLECTION_NAME, "id/file");
        return instances;
    }

    public DataModel update(DataModel instance) throws NoSQLInvalidURIException {
        nosql.update(instance, DataModel.class, DATA_COLLECTION_NAME);
        return instance;
    }

    public DataFileModel updateFile(DataFileModel instance) throws NoSQLInvalidURIException {
        nosql.update(instance, DataFileModel.class, FILE_COLLECTION_NAME);
        return instance;
    }

    public ListWithPagination<DataModel> search(
            UserModel user,
            URI objectUri,
            URI variableUri,
            List<URI> provenances,
            Instant startDate,
            Instant endDate,
            Float confidenceMin,
            Float confidenceMax,
            Document metadata,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) {

        Document filter = searchFilter(objectUri, variableUri, provenances, startDate, endDate, confidenceMin, confidenceMax, metadata);

        ListWithPagination<DataModel> datas = nosql.searchWithPagination(DataModel.class, DATA_COLLECTION_NAME, filter, orderByList, page, pageSize);

        return datas;

    }
    
    public List<DataModel> search(
            UserModel user,
            URI objectUri,
            URI variableUri,
            List<URI> provenances,
            Instant startDate,
            Instant endDate,
            Float confidenceMin,
            Float confidenceMax,
            Document metadata,
            List<OrderBy> orderByList) {

        Document filter = searchFilter(objectUri, variableUri, provenances, startDate, endDate, confidenceMin, confidenceMax, metadata);

        List<DataModel> datas = nosql.search(DataModel.class, DATA_COLLECTION_NAME, filter, orderByList);

        return datas;

    }
    
    private Document searchFilter(URI objectUri, URI variableUri, List<URI> provenances, Instant startDate, Instant endDate, Float confidenceMin, Float confidenceMax, Document metadata) {
        Document filter = new Document();
        
        if (objectUri != null) {
            filter.put("scientificObjects", objectUri);
        }

        if (variableUri != null) {
            filter.put("variable", variableUri);
        }

        if (provenances != null && !provenances.isEmpty()) {
            Document inFilter = new Document();
            
            inFilter.put("$in", provenances);
            filter.put("provenance.uri", inFilter);
        }

        if (startDate != null || endDate != null) {
            Document dateFilter = new Document();
            if (startDate != null) {            
                dateFilter.put("$gte", startDate);
            }

            if (endDate != null) {
                dateFilter.put("$lt", endDate);

            }
            filter.put("date", dateFilter);
        }    
        
        if (confidenceMin != null || confidenceMax != null) {
            Document confidenceFilter = new Document();
            if (confidenceMin != null) {            
                confidenceFilter.put("$gte", confidenceMin);
            }

            if (confidenceMax != null) {
                confidenceFilter.put("$lte", confidenceMax);

            }
            filter.put("confidence", confidenceFilter);
        }
        
        if (metadata != null) {
            for (String key:metadata.keySet()) {
                filter.put("metadata." + key, metadata.get(key));
            }
        }
                
        return filter;
    }    

    public DataModel get(URI uri) throws NoSQLInvalidURIException {
        DataModel data = nosql.findByURI(DataModel.class, DATA_COLLECTION_NAME, uri);
        return data;
    }

    public DataFileModel getFile(URI uri) throws NoSQLInvalidURIException {
        DataFileModel data = nosql.findByURI(DataFileModel.class, FILE_COLLECTION_NAME, uri);
        return data;
    }
    
    public void delete(URI uri) throws NoSQLInvalidURIException, Exception {
        nosql.delete(DataModel.class, DATA_COLLECTION_NAME, uri);
    }

    public void delete(List<URI> uris) throws NoSQLInvalidURIException, Exception {
        nosql.delete(DataModel.class, DATA_COLLECTION_NAME, uris);
    }

    public void deleteFile(URI uri) throws NoSQLInvalidURIException {
        nosql.delete(DataFileModel.class, FILE_COLLECTION_NAME, uri);
    }
    
    public Document buildProvenancesListFiter(Set<URI> provenances) {
        Document listFilter = new Document();
        listFilter.append("$in", provenances);
        Document filter = new Document();
        filter.append("provenance.uri", listFilter);
        return filter;
    }

    public ListWithPagination<VariableModel> getVariablesByExperiment(URI xpUri, String language, Integer page, Integer pageSize) throws Exception {
        Set<URI> provenances = getProvenancesByExperiment(xpUri);
        if (provenances.size() > 0) {
            Set<URI> variableURIs = nosql.distinct("variable", URI.class, DATA_COLLECTION_NAME, buildProvenancesListFiter(provenances));
            int total = variableURIs.size();
            
            List<URI> list = new ArrayList<>(variableURIs);
            List<URI> listToSend = new ArrayList<>();
            if (total > 0 && (page * pageSize) < total) {
                if (page == null || page < 0) {
                    page = 0;
                }                
                int fromIndex = page*pageSize;
                int toIndex;
                if (total > fromIndex + pageSize) {
                    toIndex = fromIndex + pageSize;
                } else {
                    toIndex = total;
                }
                listToSend = list.subList(fromIndex, toIndex);
            }
            
            List<VariableModel> variables = sparql.getListByURIs(VariableModel.class, listToSend, language);
            return new ListWithPagination(variables, page, pageSize, total);
            
        } else {
            return new ListWithPagination(new ArrayList(), page, pageSize, 0);
        }
    }
    
    public List<VariableModel> getVariablesByExperiment(URI xpUri, String language) throws Exception {
        Set<URI> provenances = getProvenancesByExperiment(xpUri);
        if (provenances.size() > 0) {
            Document filter = buildProvenancesListFiter(provenances);
            Set<URI> variableURIs = nosql.distinct("variable", URI.class, DATA_COLLECTION_NAME, filter);

            return sparql.getListByURIs(VariableModel.class, variableURIs, language);
        } else {
            return new ArrayList<>();
        }
    }

    public Set<URI> getProvenancesByExperiment(URI xpUri) throws Exception {
        Document filter = new Document();
        filter.append("experiments", xpUri);
        return nosql.distinct("uri", URI.class, ProvenanceDAO.PROVENANCE_COLLECTION_NAME, filter);
    }

    public <T extends DataFileModel> void insertFile(DataFileModel model, File file) throws URISyntaxException, Exception {
        //generate URI
        nosql.generateUniqueUriIfNullOrValidateCurrent(model, "id/file", FILE_COLLECTION_NAME);

        final String filename = Base64.getEncoder().encodeToString(model.getUri().toString().getBytes());
        Path filePath = Paths.get(FS_FILE_PREFIX, filename);
        model.setPath(filePath.toString());

        nosql.startTransaction();         
        try {   
            createFile(model);
            fs.writeFile(filePath, file);
            nosql.commitTransaction();
        } catch (Exception e) {
            nosql.rollbackTransaction();
            fs.deleteIfExists(filePath);
            throw e;
        } 

    }

    public ListWithPagination<DataFileModel> searchFiles(
            UserModel user,
            URI objectUri,
            URI provenanceUri,
            Instant startDate,
            Instant endDate,
            Document metadata,
            List<OrderBy> orderBy,
            int page,
            int pageSize) {

        Document filter = searchFileFilter(objectUri, provenanceUri, startDate, endDate, metadata);

        if (objectUri != null) {
            filter.put("scientificObjects", objectUri);
        }

        if (provenanceUri != null) {
            filter.put("provenance.uri", provenanceUri);
        }

        if (startDate != null) {
            Document greater = new Document();
            greater.put("$gte", startDate);
            filter.put("date", greater);
        }

        if (endDate != null) {
            Document less = new Document();
            less.put("$lte", endDate);
            filter.put("date", less);
        }
        
        if (metadata != null) {
            for (String key:metadata.keySet()) {
                filter.put("metadata." + key, metadata.get(key));
            }
        }

        ListWithPagination<DataFileModel> files = nosql.searchWithPagination(
                DataFileModel.class, DATA_COLLECTION_NAME, filter, orderBy, page, pageSize);

        return files;

    }
    
    private Document searchFileFilter(URI objectUri, URI provenanceUri, Instant startDate, Instant endDate, Document metadata) {
        Document filter = new Document();

        if (objectUri != null) {
            filter.put("scientificObjects", objectUri);
        }

        if (provenanceUri != null) {
            filter.put("provenance.uri", provenanceUri);
        }

        if (startDate != null || endDate != null) {
            Document dateFilter = new Document();
            if (startDate != null) {            
                dateFilter.put("$gte", startDate);
            }

            if (endDate != null) {
                dateFilter.put("$lte", endDate);

            }
            filter.put("date", dateFilter);
        }   
        
        if (metadata != null) {
            for (String key:metadata.keySet()) {
                filter.put("metadata." + key, metadata.get(key));
            }
        }
        
        return filter;
    }

    public void checkVariableDataTypes(List<DataModel> datas) throws Exception {
        VariableDAO dao = new VariableDAO(sparql);
        Set<URI> variables = new HashSet<>();
        Map<URI, URI> variableTypes = new HashMap();

        for (DataModel data : datas) {
            if (data.getValue() != "NA") {
                URI variableUri = data.getVariable();
                if (!variableTypes.containsKey(variableUri)) {
                    VariableModel variable = dao.get(data.getVariable());
                    if (variable.getDataType() == null) {
                        throw new NoVariableDataTypeException(variableUri);
                    } else {
                        variableTypes.put(variableUri,variable.getDataType());
                    }                    
                }
                URI dataType = variableTypes.get(variableUri);
                
                if (!checkTypeCoherence(dataType, data.getValue()))  {
                    throw new DataTypeException(variableUri, data.getValue(), dataType);
                }

            }
        }
    }

    private Boolean checkTypeCoherence(URI dataType, Object value) {
        Boolean checkCoherence = false;
        if (dataType == null) {
            checkCoherence = true;

        } else {
            switch (dataType.toString()) {
                case "xsd:integer":
                    if ((value instanceof Integer)) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:decimal":
                    if ((value instanceof Double)) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:boolean":
                    if ((value instanceof Boolean)) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:date":
                    if ((value instanceof String)) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:datetime":
                    if ((value instanceof String)) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:string":
                    if ((value instanceof String)) {
                        checkCoherence = true;
                    }
                    break;
                default:
                    break;
            }
        }

        return checkCoherence;
    }

    public DeleteResult deleteWithFilter(URI objectUri, URI variableUri, URI provenanceUri) throws Exception {
        List<URI> provenances = new ArrayList<>();
        provenances.add(provenanceUri);
        Document filter = searchFilter(objectUri, variableUri, provenances, null, null, null, null, null);
        DeleteResult result = nosql.deleteOnCriteria(DataModel.class, DATA_COLLECTION_NAME, filter);
        return result;
    }

}
