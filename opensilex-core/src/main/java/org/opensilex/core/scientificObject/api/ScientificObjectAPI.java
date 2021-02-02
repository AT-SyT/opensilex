//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.scientificObject.api;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.MongoWriteException;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.apache.jena.ext.com.google.common.cache.Cache;
import org.apache.jena.ext.com.google.common.cache.CacheBuilder;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.ontology.api.CSVValidationDTO;
import org.opensilex.core.ontology.dal.CSVValidationModel;
import org.opensilex.core.scientificObject.dal.ExperimentalObjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.TokenGenerator;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.infrastructure.dal.InfrastructureDAO;
import org.opensilex.core.infrastructure.dal.InfrastructureFacilityModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.dal.CSVCell;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectURIGenerator;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.server.exceptions.ForbiddenException;
import org.opensilex.server.response.ListItemDTO;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.utils.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Julien BONNEFONT
 */
@Api(ScientificObjectAPI.CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_ID)
@Path("/core/scientific_objects")
@ApiCredentialGroup(
        groupId = ScientificObjectAPI.CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_ID,
        groupLabelKey = ScientificObjectAPI.CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_LABEL_KEY
)
public class ScientificObjectAPI {

    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_ID = "Scientific Objects";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_LABEL_KEY = "credential-groups.scientific-objects";

    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID = "scientific-objects-modification";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_LABEL_KEY = "credential.scientific-objects.modification";

    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID = "scientific-objects-delete";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_LABEL_KEY = "credential.scientific-objects.delete";

    public static final String GEOMETRY_COLUMN_ID = "geometry";
    public static final String INVALID_GEOMETRY = "Invalid geometry (longitude must be between -180 and 180 and latitude must be between -90 and 90, no self-intersection, ...)";

    private final static Logger LOGGER = LoggerFactory.getLogger(ScientificObjectDAO.class);
    public static final String SCIENTIFIC_OBJECT_EXAMPLE_TYPE = "oeso:Plot";
    public static final String SCIENTIFIC_OBJECT_EXAMPLE_TYPE_NAME = "Plot";
    public static final String SCIENTIFIC_OBJECT_EXAMPLE_NAME = "Plot 12";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @POST
    @Path("by_uris")
    @ApiOperation("Get scientific objet list of a given experiment URI")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return list of scientific objects corresponding to the given context URI", response = ScientificObjectNodeDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectsListByUris(
            @ApiParam(value = "Experiment URI", example = "http://example.com/") @QueryParam("experiment") URI contextURI,
            @ApiParam(value = "Scientific object uris") List<URI> objectsURI
    ) throws Exception {
        if (objectsURI == null) {
            objectsURI = new ArrayList<>();
        }
        validateContextAccess(contextURI);

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        List<ScientificObjectModel> scientificObjects = dao.searchByURIs(contextURI, objectsURI, currentUser);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        HashMap<String, Geometry> mapGeo = geoDAO.getGeometryByUris(contextURI, objectsURI);
        List<ScientificObjectNodeDTO> dtoList = scientificObjects.stream().map((model) -> ScientificObjectNodeDTO.getDTOFromModel(model, mapGeo.get(SPARQLDeserializers.getExpandedURI(model.getUri())))).collect(Collectors.toList());

        return new PaginatedListResponse<ScientificObjectNodeDTO>(dtoList).getResponse();
    }

    @GET
    @Path("used_types")
    @ApiOperation("get used scientific object types")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return scientific object types list", response = ListItemDTO.class, responseContainer = "List")
    })
    public Response getUsedTypes(
            @ApiParam(value = "Experiment URI", example = "http://example.com/") @QueryParam("experiment") @ValidURI URI experimentURI
    ) throws Exception {

        validateContextAccess(experimentURI);

        SelectBuilder select = new SelectBuilder();

        if (experimentURI != null) {
            Node context = SPARQLDeserializers.nodeURI(experimentURI);
            select.addGraph(context, "?uri", RDF.type, "?type");
        } else if (!currentUser.isAdmin()) {
            ExperimentDAO xpDO = new ExperimentDAO(sparql);
            Set<URI> graphFilterURIs = xpDO.getUserExperiments(currentUser);

            select.addGraph("?g", "?uri", RDF.type, "?type");
            select.addFilter(SPARQLQueryHelper.inURIFilter("?g", graphFilterURIs));
        }

        select.addVar("?type ?label");
        select.setDistinct(true);
        select.addWhere("?type", Ontology.subClassStrict, Oeso.ScientificObject);
        select.addWhere("?type", RDFS.label, "?label");
        select.addFilter(SPARQLQueryHelper.langFilter("label", currentUser.getLanguage()));

        List<ListItemDTO> types = new ArrayList<>();

        sparql.executeSelectQuery(select, (row) -> {
            try {
                URI uri = new URI(row.getStringValue("type"));
                String label = row.getStringValue("label");
                ListItemDTO listItem = new ListItemDTO();
                listItem.setUri(uri);
                listItem.setLabel(label);
                types.add(listItem);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });

        return new PaginatedListResponse<>(types).getResponse();
    }

    @GET
    @Path("geometry")
    @ApiOperation("Get scientific objet list with geometry of a given context (experiment or organization) URI")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return list of scientific objects whose geometry corresponds to the given context URI", response = ScientificObjectNodeDTO.class, responseContainer = "List")
    })
    public Response searchScientificObjectsWithGeometryListByUris(
            @ApiParam(value = "Context URI", example = "http://example.com/", required = true) @QueryParam("experiment") @NotNull URI contextURI
    ) throws Exception {

        validateContextAccess(contextURI);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        Instant test_start = Instant.now();
        FindIterable<GeospatialModel> mapGeo = geoDAO.getGeometryByGraphList(contextURI);
        Instant test_end = Instant.now();

        List<ScientificObjectNodeDTO> dtoList = new ArrayList<>();
        int lengthMapGeo = 0;

        for (GeospatialModel geospatialModel : mapGeo) {
            ScientificObjectNodeDTO dtoFromModel = ScientificObjectNodeDTO.getDTOFromModel(geospatialModel);
            dtoList.add(dtoFromModel);
            lengthMapGeo++;
        }

        LOGGER.debug(lengthMapGeo + " space entities recovered " + Duration.between(test_start, test_end).toMillis() + " milliseconds elapsed");
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("children")
    @ApiOperation("Get list of scientific object children")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return list of scientific objects children corresponding to the parent URI", response = ScientificObjectNodeWithChildrenDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectsChildren(
            @ApiParam(value = "Parent object URI", example = "http://example.com/") @QueryParam("parent") URI parentURI,
            @ApiParam(value = "Experiment URI", example = "http://example.com/") @QueryParam("experiment") @ValidURI URI experimentURI,
            @ApiParam(value = "Facility", example = "diaphen:serre-2") @QueryParam("facility") @ValidURI URI facility,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        validateContextAccess(experimentURI);

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        if (experimentURI == null) {
            experimentURI = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        }
        ListWithPagination<ScientificObjectModel> scientificObjects = dao.searchChildren(experimentURI, parentURI, facility, page, pageSize, currentUser);

        ListWithPagination<ScientificObjectNodeWithChildrenDTO> dtoList = scientificObjects.convert(ScientificObjectNodeWithChildrenDTO.class, ScientificObjectNodeWithChildrenDTO::getDTOFromModel);
        return new PaginatedListResponse<ScientificObjectNodeWithChildrenDTO>(dtoList).getResponse();
    }

    @GET
    @ApiOperation("Search list of scientific objects")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return scientific objects corresponding to the given search parameters", response = ScientificObjectNodeDTO.class, responseContainer = "List")
    })
    public Response searchScientificObjects(
            @ApiParam(value = "Experiment URI", example = "http://example.com/") @QueryParam("experiment") final URI contextURI,
            @ApiParam(value = "Regex pattern for filtering by name", example = ".*") @DefaultValue(".*") @QueryParam("name") String pattern,
            @ApiParam(value = "RDF type filter", example = "vocabulary:Plant") @QueryParam("rdf_types") @ValidURI List<URI> rdfTypes,
            @ApiParam(value = "Parent URI", example = "http://example.com/") @QueryParam("parent") @ValidURI URI parentURI,
            @ApiParam(value = "Germplasm URI", example = "http://aims.fao.org/aos/agrovoc/c_1066") @QueryParam("germplasm") @ValidURI URI germplasm,
            @ApiParam(value = "Factors URI", example = "vocabulary:Irrigation") @QueryParam("factors") @ValidURI List<URI> factors,
            @ApiParam(value = "Factor levels URI", example = "vocabulary:IrrigationStress") @QueryParam("factor_levels") @ValidURI List<URI> factorLevels,
            @ApiParam(value = "Facility", example = "diaphen:serre-2") @QueryParam("facility") @ValidURI URI facility,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);

        List<URI> contextURIs = new ArrayList<>();

        if (contextURI != null) {
            if (sparql.uriExists(ExperimentModel.class, contextURI)) {
                xpDAO.validateExperimentAccess(contextURI, currentUser);
                contextURIs.add(contextURI);
            }
        } else if (!currentUser.isAdmin()) {
            contextURIs.addAll(xpDAO.getUserExperiments(currentUser));
        } else {
            contextURIs.add(sparql.getDefaultGraphURI(ScientificObjectModel.class));
        }

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        ListWithPagination<ScientificObjectModel> scientificObjects = dao.search(contextURIs, pattern, rdfTypes, parentURI, germplasm, factors, factorLevels, facility, page, pageSize, currentUser);

        ListWithPagination<ScientificObjectNodeDTO> dtoList = scientificObjects.convert(ScientificObjectNodeDTO.class, ScientificObjectNodeDTO::getDTOFromModel);

        return new PaginatedListResponse<ScientificObjectNodeDTO>(dtoList).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get scientific object detail")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return scientific object details corresponding to the given object URI", response = ScientificObjectDetailDTO.class)
    })
    public Response getScientificObjectDetail(
            @ApiParam(value = "scientific object URI", example = "http://example.com/", required = true)
            @PathParam("uri") @ValidURI @NotNull URI objectURI,
            @ApiParam(value = "Context URI", example = "http://example.com/")
            @QueryParam("experiment") @ValidURI URI contextURI
    ) throws Exception {

        validateContextAccess(contextURI);
        if (contextURI == null) {
            contextURI = sparql.getDefaultGraphURI(ScientificObjectModel.class);;
        }
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        ExperimentalObjectModel model = dao.getObjectByURI(objectURI, contextURI, currentUser);
        GeospatialModel geometryByURI = geoDAO.getGeometryByURI(objectURI, contextURI);

        if (model == null) {
            throw new NotFoundURIException("Scientific object uri not found:", objectURI);
        }

        return new SingleObjectResponse<>(ScientificObjectDetailDTO.getDTOFromModel(model, geometryByURI)).getResponse();
    }

    @GET
    @Path("{uri}/experiments")
    @ApiOperation("Get scientific object detail, globally and by experiments, ...")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return scientific object details corresponding to the given context and object URI", response = ScientificObjectDetailByContextDTO.class)
    })
    public Response getScientificObjectDetailByContext(
            @ApiParam(value = "scientific object URI", example = "http://example.com/", required = true)
            @PathParam("uri") @ValidURI @NotNull URI objectURI
    ) throws Exception {

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        List<URI> contexts = dao.getObjectContexts(objectURI);

        List<ScientificObjectDetailByContextDTO> dtoList = new ArrayList<>();
        for (URI contextURI : contexts) {
            ExperimentModel experiment = getExperiment(contextURI);

            ExperimentalObjectModel model = dao.getObjectByURI(objectURI, contextURI, currentUser);
            GeospatialModel geometryByURI = geoDAO.getGeometryByURI(objectURI, contextURI);
            if (model != null) {
                ScientificObjectDetailByContextDTO dto = ScientificObjectDetailByContextDTO.getDTOFromModel(model, experiment, geometryByURI);
                dtoList.add(dto);
            }
        }

        if (dtoList.size() == 0) {
            throw new NotFoundURIException("Scientific object uri not found:", objectURI);
        } else {
            return new PaginatedListResponse<>(dtoList).getResponse();
        }
    }

    @POST
    @ApiOperation("Create a scientific object for the given context")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create a scientific object", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "A scientific object with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createScientificObject(
            @ApiParam(value = "Scientific object description", required = true)
            @NotNull
            @Valid ScientificObjectCreationDTO descriptionDto
    ) throws Exception {

        URI contextURI = descriptionDto.getExperiment();
        validateContextAccess(contextURI);

        URI globalScientificObjectGraph = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        boolean globalCopy = false;
        if (contextURI == null) {
            contextURI = globalScientificObjectGraph;
        } else {
            globalCopy = true;
        }

        URI soType = descriptionDto.getType();

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        nosql.startTransaction();
        sparql.startTransaction();
        try {
            URI soURI = dao.create(contextURI, soType, descriptionDto.getUri(), descriptionDto.getName(), descriptionDto.getRelations(), currentUser);

            if (globalCopy) {
                UpdateBuilder update = new UpdateBuilder();
                Node soNode = SPARQLDeserializers.nodeURI(soURI);
                Node graphNode = SPARQLDeserializers.nodeURI(globalScientificObjectGraph);
                update.addInsert(graphNode, soNode, RDF.type, SPARQLDeserializers.nodeURI(soType));
                update.addInsert(graphNode, soNode, RDFS.label, descriptionDto.getName());
                sparql.executeUpdateQuery(update);
            }

            if (descriptionDto.getGeometry() != null) {
                GeospatialModel geospatialModel = new GeospatialModel();
                geospatialModel.setUri(soURI);
                geospatialModel.setName(descriptionDto.getName());
                geospatialModel.setRdfType(soType);
                geospatialModel.setGraph(contextURI);
                geospatialModel.setGeometry(GeospatialDAO.geoJsonToGeometry(descriptionDto.getGeometry()));
                geoDAO.create(geospatialModel);
                nosql.commitTransaction();
            } else {
                nosql.rollbackTransaction();
            }

            sparql.commitTransaction();

            return new ObjectUriResponse(Response.Status.CREATED, soURI).getResponse();
        } catch (MongoWriteException | CodecConfigurationException mongoException) {
            try {
                sparql.rollbackTransaction(mongoException);
                nosql.rollbackTransaction();
            } catch (Exception e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, INVALID_GEOMETRY, mongoException).getResponse();
            }
            throw mongoException;
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
            nosql.rollbackTransaction();
            throw ex;
        }
    }

    @PUT
    @ApiOperation("Update a scientific object for the given experiment")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Scientific object updated", response = ObjectUriResponse.class)
    })
    public Response updateScientificObject(
            @ApiParam(value = "Scientific object description", required = true)
            @NotNull
            @Valid ScientificObjectCreationDTO descriptionDto
    ) throws Exception {

        URI contextURI = descriptionDto.getExperiment();
        validateContextAccess(contextURI);
        if (contextURI == null) {
            contextURI = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        }
        URI soType = descriptionDto.getType();

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        nosql.startTransaction();
        sparql.startTransaction();
        try {

            URI soURI = dao.update(contextURI, soType, descriptionDto.getUri(), descriptionDto.getName(), descriptionDto.getRelations(), currentUser);

            if (descriptionDto.getGeometry() != null) {
                GeospatialModel geospatialModel = new GeospatialModel();
                geospatialModel.setUri(soURI);
                geospatialModel.setName(descriptionDto.getName());
                geospatialModel.setRdfType(soType);
                geospatialModel.setGraph(contextURI);
                geospatialModel.setGeometry(GeospatialDAO.geoJsonToGeometry(descriptionDto.getGeometry()));
                geoDAO.update(geospatialModel, soURI, contextURI);
            } else {
                geoDAO.delete(soURI, contextURI);
            }

            sparql.commitTransaction();
            nosql.commitTransaction();

            return new ObjectUriResponse(soURI).getResponse();
        } catch (MongoWriteException | CodecConfigurationException mongoException) {
            try {
                sparql.rollbackTransaction(mongoException);
                nosql.rollbackTransaction();
            } catch (Exception e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, INVALID_GEOMETRY, mongoException).getResponse();
            }
            throw mongoException;
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            nosql.rollbackTransaction();
            throw ex;
        }
    }

    @DELETE
    @ApiOperation("Delete a scientific object")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Scientific object deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Scientific object URI not found", response = ErrorResponse.class)
    })
    public Response deleteScientificObject(
            @ApiParam(value = "scientific object URI", example = "http://example.com/", required = true)
            @QueryParam("uri") @ValidURI @NotNull URI objectURI,
            @ApiParam(value = "Experiment URI", example = "http://example.com/")
            @QueryParam("experiment") @ValidURI URI contextURI
    ) throws Exception {

        validateContextAccess(contextURI);

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        nosql.startTransaction();
        sparql.startTransaction();
        try {
            if (contextURI == null) {
                sparql.deleteByURI(sparql.getDefaultGraph(ScientificObjectModel.class), objectURI);
            } else {
                dao.delete(contextURI, objectURI, currentUser);
            }
            geoDAO.delete(objectURI, contextURI);

            sparql.commitTransaction();
            nosql.commitTransaction();

            return new ObjectUriResponse(Response.Status.OK, objectURI).getResponse();
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            nosql.rollbackTransaction();
            throw ex;
        }
    }

    @POST
    @Path("import")
    @ApiOperation(value = "Import a CSV file for the given experiment URI and scientific object type.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data file and metadata saved", response = CSVValidationDTO.class)
    })
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importCSV(
            @ApiParam(value = "File description with metadata", required = true, type = "string")
            @NotNull
            @Valid
            @FormDataParam("description") ScientificObjectCsvDescriptionDTO descriptionDto,
            @ApiParam(value = "Data file", required = true, type = "file")
            @NotNull
            @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {
        URI contextURI = descriptionDto.getExperiment();
        if (contextURI == null) {
            contextURI = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        }
        String validationToken = descriptionDto.getValidationToken();

        CSVValidationModel errors;
        if (validationToken == null) {
            errors = getCSVValidationModel(contextURI, file, currentUser);
        } else {
            errors = filesValidationCache.getIfPresent(validationToken);
            if (errors == null) {
                errors = getCSVValidationModel(contextURI, file, currentUser);
            } else {
                Map<String, Claim> claims = TokenGenerator.getTokenClaims(validationToken);
                contextURI = new URI(claims.get(CLAIM_CONTEXT_URI).asString());
            }
        }

        CSVValidationDTO csvValidation = new CSVValidationDTO();

        csvValidation.setErrors(errors);

        final URI graphURI = contextURI;
        if (!errors.hasErrors()) {
            Map<Integer, Geometry> geometries = (Map<Integer, Geometry>) errors.getObjectsMetadata().get(GEOMETRY_COLUMN_ID);
            if (geometries != null && geometries.size() > 0) {
                GeospatialDAO geoDAO = new GeospatialDAO(nosql);

                nosql.startTransaction();
                sparql.startTransaction();
                try {
                    List<SPARQLResourceModel> objects = errors.getObjects();
                    sparql.create(SPARQLDeserializers.nodeURI(graphURI), objects);

                    List<GeospatialModel> geospacialModels = new ArrayList<>();
                    geometries.forEach((rowIndex, geometry) -> {
                        SPARQLResourceModel object = objects.get(rowIndex - 1);
                        GeospatialModel geospatialModel = new GeospatialModel();
                        geospatialModel.setUri(object.getUri());
                        geospatialModel.setName(object.getRelations().get(0).getValue());
                        geospatialModel.setRdfType(object.getType());
                        geospatialModel.setGraph(graphURI);
                        geospatialModel.setGeometry(geometry);
                        geospatialModels.add(geospatialModel);
                    });

                    geoDAO.createAll(geospacialModels);
                    sparql.commitTransaction();
                    nosql.commitTransaction();

                } catch (Exception ex) {
                    nosql.rollbackTransaction();
                    sparql.rollbackTransaction(ex);
                }
            } else {

                List<SPARQLResourceModel> objects = errors.getObjects();
                sparql.create(SPARQLDeserializers.nodeURI(graphURI), objects);
            }

            csvValidation.setNbLinesImported(errors.getObjects().size());
            csvValidation.setValidationToken("done");
        }

        return new SingleObjectResponse<CSVValidationDTO>(csvValidation).getResponse();
    }

    @POST
    @Path("export")
    @ApiOperation("Export a given list of scientific object URIs to csv data file")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data file exported")
    })
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportCSV(
            @ApiParam("CSV export configuration") @Valid ScientificObjectCsvExportDTO dto
    ) throws Exception {
        URI contextURI = dto.getExperiment();
        validateContextAccess(contextURI);

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        Node contextNode;
        if (contextURI == null) {
            contextNode = sparql.getDefaultGraph(ScientificObjectModel.class);
        } else {
            contextNode = SPARQLDeserializers.nodeURI(contextURI);
        }
        List<ScientificObjectModel> objects = sparql.getListByURIs(contextNode, ScientificObjectModel.class, dto.getObjects(), currentUser.getLanguage());

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        HashMap<String, Geometry> geospacialMap = geoDAO.getGeometryByUris(null, dto.getObjects());

        List<String> customColumns = new ArrayList<>();
        customColumns.add(Oeso.isPartOf.toString());
        customColumns.add(Oeso.hasCreationDate.toString());
        customColumns.add(Oeso.hasDestructionDate.toString());
        customColumns.add(GEOMETRY_COLUMN_ID);

        BiFunction<String, ScientificObjectModel, String> customValueGenerator = (columnID, value) -> {
            if (value == null) {
                return null;
            }
            if (columnID.equals(Oeso.isPartOf.toString())) {
                if (value.getParent() != null) {
                    return SPARQLDeserializers.formatURI(value.getParent().getUri()).toString();
                } else {
                    return null;
                }
            } else if (columnID.equals(Oeso.hasCreationDate.toString()) && value.getCreationDate() != null) {
                return value.getCreationDate().toString();
            } else if (columnID.equals(Oeso.hasDestructionDate.toString()) && value.getDestructionDate() != null) {
                return value.getDestructionDate().toString();
            } else if (columnID.equals(GEOMETRY_COLUMN_ID)) {
                String uriString = SPARQLDeserializers.getExpandedURI(value.getUri());
                if (geospacialMap.containsKey(uriString)) {
                    Geometry geo = geospacialMap.get(uriString);
                    try {
                        return GeospatialDAO.geometryToWkt(geo);
                    } catch (JsonProcessingException | ParseException ex) {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        };
        String csvContent = ontologyDAO.exportCSV(
                objects,
                new URI(Oeso.ScientificObject.toString()),
                currentUser.getLanguage(),
                customValueGenerator,
                customColumns,
                (colId1, colId2) -> {
                    if (colId1.equals(colId2)) {
                        return 0;
                    } else if (colId1.equals(GEOMETRY_COLUMN_ID)) {
                        return 1;
                    } else if (colId2.equals(GEOMETRY_COLUMN_ID)) {
                        return -1;
                    } else {
                        return colId1.compareTo(colId2);
                    }
                });

        String csvName = "scientific-object-export.csv";
        return Response.ok(csvContent.getBytes(), MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + csvName + "\"")
                .build();
    }

    @POST
    @Path("import_validation")
    @ApiOperation(value = "Validate a CSV file for the given experiment URI and scientific object type.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "CSV validation errors or a validation token used for CSV import", response = CSVValidationDTO.class)
    })
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateCSV(
            @ApiParam(value = "File description with metadata", required = true, type = "string")
            @Valid
            @FormDataParam("description") ScientificObjectCsvDescriptionDTO descriptionDto,
            @ApiParam(value = "Data file", required = true, type = "file")
            @NotNull
            @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {

        URI contextURI = descriptionDto.getExperiment();

        if (contextURI == null) {
            contextURI = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        }

        CSVValidationModel csvValidationModel = getCSVValidationModel(contextURI, file, currentUser);

        CSVValidationDTO csvValidation = new CSVValidationDTO();
        csvValidation.setErrors(csvValidationModel);

        if (!csvValidationModel.hasErrors()) {
            csvValidation.setValidationToken(generateCSVValidationToken(contextURI));
            filesValidationCache.put(csvValidation.getValidationToken(), csvValidationModel);
        }

        return new SingleObjectResponse<CSVValidationDTO>(csvValidation).getResponse();
    }

    private CSVValidationModel getCSVValidationModel(URI contextURI, InputStream file, UserModel currentUser) throws Exception {
        HashMap<String, BiConsumer<CSVCell, CSVValidationModel>> customValidators = new HashMap<>();

        if (contextURI == null) {
            contextURI = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        }

        ScientificObjectURIGenerator uriGenerator = new ScientificObjectURIGenerator(contextURI);

        Map<String, List<CSVCell>> parentNamesToReplace = new HashMap<>();
        customValidators.put(Oeso.isPartOf.toString(), (cell, csvErrors) -> {
            String value = cell.getValue();
            if (!URIDeserializer.validateURI(value)) {
                if (!parentNamesToReplace.containsKey(value)) {
                    parentNamesToReplace.put(value, new ArrayList<>());
                }
                parentNamesToReplace.get(value).add(cell);
            }
        });

        if (sparql.uriExists(ExperimentModel.class, contextURI)) {

            ExperimentDAO xpDAO = new ExperimentDAO(sparql);
            xpDAO.validateExperimentAccess(contextURI, currentUser);
            ExperimentModel xp = sparql.getByURI(ExperimentModel.class, contextURI, currentUser.getLanguage());

            List<String> factorLevelURIs = new ArrayList<>();
            for (FactorModel factor : xp.getFactors()) {
                for (FactorLevelModel factorLevel : factor.getFactorLevels()) {
                    factorLevelURIs.add(SPARQLDeserializers.getExpandedURI(factorLevel.getUri()));
                }
            }

            customValidators.put(Oeso.hasFactorLevel.toString(), (cell, csvErrors) -> {
                try {
                    if (!cell.getValue().isEmpty()) {
                        String factorLevelURI = SPARQLDeserializers.getExpandedURI(new URI(cell.getValue()));
                        if (!factorLevelURIs.contains(factorLevelURI)) {
                            csvErrors.addInvalidValueError(cell);
                        }
                    }
                } catch (URISyntaxException ex) {
                    csvErrors.addInvalidURIError(cell);
                }
            });

            List<String> germplasmStringURIs = new ArrayList<>();
            List<URI> germplasmURIs = new ArrayList<>();

            for (SpeciesModel germplasm : xp.getSpecies()) {
                germplasmStringURIs.add(SPARQLDeserializers.getExpandedURI(germplasm.getUri()));
                germplasmURIs.add(germplasm.getUri());
            }

            if (germplasmURIs.size() > 0) {
                GermplasmDAO dao = new GermplasmDAO(sparql, nosql);
                List<URI> subSpecies = dao.getGermplasmURIsBySpecies(germplasmURIs, currentUser.getLanguage());
                for (URI germplasmURI : subSpecies) {
                    germplasmStringURIs.add(SPARQLDeserializers.getExpandedURI(germplasmURI));
                }
            }

            customValidators.put(Oeso.hasGermplasm.toString(), (cell, csvErrors) -> {
                try {
                    if (!cell.getValue().isEmpty()) {
                        String germplasmURI = SPARQLDeserializers.getExpandedURI(new URI(cell.getValue()));
                        if (!germplasmStringURIs.contains(germplasmURI)) {
                            csvErrors.addInvalidValueError(cell);
                        }
                    }
                } catch (URISyntaxException ex) {
                    csvErrors.addInvalidURIError(cell);
                }
            });

            List<String> facilityStringURIs = new ArrayList<>();
            List<InfrastructureFacilityModel> facilities = xpDAO.getAvailableFacilities(contextURI, currentUser);
            for (InfrastructureFacilityModel facility : facilities) {
                facilityStringURIs.add(SPARQLDeserializers.getExpandedURI(facility.getUri()));
            }

            customValidators.put(Oeso.hasFacility.toString(), (cell, csvErrors) -> {
                try {
                    if (!cell.getValue().isEmpty()) {
                        String facilityURI = SPARQLDeserializers.getExpandedURI(new URI(cell.getValue()));
                        if (!facilityStringURIs.contains(facilityURI)) {
                            csvErrors.addInvalidValueError(cell);
                        }
                    }
                } catch (URISyntaxException ex) {
                    csvErrors.addInvalidURIError(cell);
                }
            });
        }

        List<String> customColumns = new ArrayList<>();
        customColumns.add(GEOMETRY_COLUMN_ID);

        Map<Integer, Geometry> geometries = new HashMap<>();
        customValidators.put(GEOMETRY_COLUMN_ID, (cell, csvErrors) -> {
            String wktGeometry = cell.getValue();
            if (wktGeometry != null && !wktGeometry.isEmpty()) {
                try {
                    Geometry geometry = GeospatialDAO.wktToGeometry(wktGeometry);
                    geometries.put(cell.getRowIndex(), geometry);
                } catch (JsonProcessingException | ParseException ex) {
                    csvErrors.addInvalidValueError(cell);
                }
            }
        });

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);

        int firstRow = 3;
        CSVValidationModel validationResult = ontologyDAO.validateCSV(contextURI, new URI(Oeso.ScientificObject.getURI()), file, firstRow, currentUser, customValidators, customColumns, uriGenerator);

        URI partOfURI = new URI(Oeso.isPartOf.toString());
        final URI graphURI = contextURI;
        parentNamesToReplace.forEach((name, cells) -> {
            List<URI> parentURIs = validationResult.getObjectNameUris(name);
            if (parentURIs == null || parentURIs.size() == 0) {
                // Case parent name not found in file
                cells.forEach((cell) -> {
                    validationResult.addInvalidValueError(cell);
                });
            } else if (parentURIs.size() == 1) {
                cells.forEach((cell) -> {
                    int rowIndex = cell.getRowIndex() - 1;
                    SPARQLResourceModel object = validationResult.getObjects().get(rowIndex);
                    object.addRelation(graphURI, partOfURI, URI.class, parentURIs.get(0).toString());
                });
            } else {
                // Case multiple objects with the same name, can not chose correct parent
                cells.forEach((cell) -> {
                    validationResult.addInvalidValueError(cell);
                });
            }
        });

        validationResult.addObjectMetadata(GEOMETRY_COLUMN_ID, geometries);

        return validationResult;
    }

    private void validateContextAccess(URI contextURI) throws Exception {
        if (contextURI == null) {
            if (!currentUser.isAdmin()) {
                throw new ForbiddenException("You must be an administrator to add global scientific object");
            }
        } else if (sparql.uriExists(ExperimentModel.class, contextURI)) {
            ExperimentDAO xpDAO = new ExperimentDAO(sparql);

            xpDAO.validateExperimentAccess(contextURI, currentUser);
        }
    }

    private ExperimentModel getExperiment(URI experimentURI) throws Exception {
        if (sparql.uriExists(ExperimentModel.class, experimentURI)) {
            ExperimentDAO xpDAO = new ExperimentDAO(sparql);

            try {
                ExperimentModel xp = xpDAO.get(experimentURI, currentUser);
                return xp;
            } catch (Exception ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    private static String generateCSVValidationToken(URI experiementURI) throws NoSuchAlgorithmException, IOException {
        Map<String, Object> additionalClaims = new HashMap<>();
        additionalClaims.put(CLAIM_CONTEXT_URI, experiementURI);
        return TokenGenerator.getValidationToken(5, ChronoUnit.MINUTES, additionalClaims);
    }

    private static Cache<String, CSVValidationModel> filesValidationCache;

    static {
        filesValidationCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    /**
     * Experiment URI claim key
     */
    private static final String CLAIM_CONTEXT_URI = "context";

}
