package org.opensilex.integration.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.AfterClass;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.security.api.AuthenticationDTO;
import org.opensilex.rest.security.api.TokenGetDTO;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.utils.OrderBy;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.sparql.service.SPARQLService;

/**
 * @author Renaud COLIN Abstract class used for DAO testing
 */
//@Category(IntegrationTestCategory.class)
public abstract class AbstractIntegrationTest extends JerseyTest {

    protected static IntegrationTestContext context;

    @Override
    protected ResourceConfig configure() {
        try {
            // init the OpenSilex instance to use during the API test(s)
            context = new IntegrationTestContext(isDebug());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return context.getResourceConfig();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        context.shutdown();
    }

    @After
    public void clearGraph() throws URISyntaxException, SPARQLQueryException {
        context.clearGraphs(getGraphsToCleanNames());
    }

    protected boolean isDebug() {
        return false;
    }

    public SPARQLService getSparqlService() {
        return context.getSparqlService();
    }

    public AuthenticationService getAuthenticationService() {
        return context.getAuthenticationService();
    }

    protected List<String> getGraphsToCleanNames() {
        return new ArrayList<>();
    }

    protected static ObjectMapper mapper = new ObjectMapper();

    /**
     * Call the security service and return a new Token
     *
     * @return a new Token
     */
    protected TokenGetDTO getToken() {

        AuthenticationDTO authDto = new AuthenticationDTO();
        authDto.setIdentifier("admin@opensilex.org");
        authDto.setPassword("admin");

        final Response callResult = target("/security/authenticate")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(authDto, MediaType.APPLICATION_JSON_TYPE));

        JsonNode node = callResult.readEntity(JsonNode.class);

        // need to convert according a TypeReference, because the expected SingleObjectResponse is a generic object
        SingleObjectResponse<TokenGetDTO> res = mapper.convertValue(node, new TypeReference<SingleObjectResponse<TokenGetDTO>>() {
        });

        assertEquals(Response.Status.OK.getStatusCode(), callResult.getStatus());
        assertEquals(Response.Status.OK, res.getStatus());
        return res.getResult();
    }

    protected Response getJsonPostResponse(WebTarget target, Object entity) {
        return appendToken(target).post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    protected Response getJsonPostPublicResponse(WebTarget target, Object entity) {
        return target.request(MediaType.APPLICATION_JSON).post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    protected Response getJsonPutResponse(WebTarget target, Object entity) {
        return appendToken(target).put(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    protected Response getJsonPutPublicResponse(WebTarget target, Object entity) {
        return target.request(MediaType.APPLICATION_JSON).put(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    protected Response getJsonGetByUriResponse(WebTarget target, String uri) {
        return appendToken(target.resolveTemplate("uri", uri)).get();
    }

    protected Response getJsonGetByUriPublicResponse(WebTarget target, String uri) {
        return target.resolveTemplate("uri", uri).request(MediaType.APPLICATION_JSON).get();
    }

    protected Response getJsonGetPublicResponse(WebTarget target) {
        return target.request(MediaType.APPLICATION_JSON).get();
    }
    
    protected Response getDeleteByUriResponse(WebTarget target, String uri) {
        return appendToken(target.resolveTemplate("uri", uri)).delete();
    }

    protected Response getDeleteByUriPublicResponse(WebTarget target, String uri) {
        return target.resolveTemplate("uri", uri).request(MediaType.APPLICATION_JSON).delete();
    }
    
    protected Response getDeleteJsonResponse(WebTarget target) {
        return appendToken(target).delete();
    }
    
    protected WebTarget appendSearchParams(WebTarget target, int page, int pageSize, Map<String, Object> params) {
        return appendSearchParams(target, page, pageSize, Collections.emptyList(), params);
    }

    protected WebTarget appendSearchParams(WebTarget target, int page, int pageSize, List<OrderBy> orderByList, Map<String, Object> params) {

        target.queryParam("page", page)
                .queryParam("pageSize", pageSize)
                .queryParam("orderBy", orderByList);

        return appendQueryParams(target, params);
    }

    protected WebTarget appendQueryParams(WebTarget target, Map<String, Object> params) {

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (List.class.isAssignableFrom(value.getClass())) {
                for (Object v : ((List) value)) {
                    target = target.queryParam(entry.getKey(), v);
                }
            } else {
                target = target.queryParam(entry.getKey(), entry.getValue());
            }
        }
        return target;
    }

    protected Invocation.Builder appendToken(WebTarget target) {
        TokenGetDTO token = getToken();
        return target.request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + token.getToken());
    }

    protected URI extractUriFromResponse(final Response response) throws URISyntaxException {
        JsonNode node = response.readEntity(JsonNode.class);
        ObjectUriResponse postResponse = mapper.convertValue(node, ObjectUriResponse.class);
        return new URI(postResponse.getResult());
    }

    protected List<URI> extractUriListFromResponse(final Response response) {
        JsonNode node = response.readEntity(JsonNode.class);
        PaginatedListResponse<URI> listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<URI>>() {
        });
        return listResponse.getResult();
    }

    protected boolean compareMaps(Map<String, String> first, Map<String, String> second) {
        if (first.size() != second.size()) {
            return false;
        }

        return first.entrySet().stream()
                .allMatch(e -> e.getValue().equals(second.get(e.getKey())));
    }
}
