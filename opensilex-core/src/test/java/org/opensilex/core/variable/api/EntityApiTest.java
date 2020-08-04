//******************************************************************************
//                          EntityAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.api.entity.EntityCreationDTO;
import org.opensilex.core.variable.api.entity.EntityGetDTO;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Renaud COLIN
 */
public class EntityApiTest extends AbstractSecurityIntegrationTest {

    public String path = "/core/variable/entity";

    public String getByUriPath = path + "/get/{uri}";
    public String createPath = path + "/create";
    public String updatePath = path + "/update";
    public String deletePath = path + "/delete/{uri}";

    private EntityCreationDTO getCreationDto() {
        EntityCreationDTO dto = new EntityCreationDTO();
        dto.setLabel("Artemisia absinthium");
        dto.setComment("A plant which was used in the past for building methanol");
        return dto;
    }

    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateGetAndDelete(createPath,getByUriPath, deletePath, getCreationDto());
    }

    @Test
    public void testCreateFailWithNoRequiredFields() throws Exception {

        EntityCreationDTO dtoWithNoName = new EntityCreationDTO();
        dtoWithNoName.setComment("only a comment, not a name");

        final Response postResult = getJsonPostResponse(target(createPath),dtoWithNoName);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void testGetByUriWithUnknownUri() throws Exception {
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), Oeso.Entity+"/58165");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {

        EntityCreationDTO dto = getCreationDto();
        final Response postResult = getJsonPostResponse(target(createPath), dto);

        dto.setUri(extractUriFromResponse(postResult));
        dto.setLabel("new alias");
        dto.setComment("new comment");

        final Response updateResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new xp and compare to the expected xp
        final Response getResult = getJsonGetByUriResponse(target(getByUriPath), dto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<EntityGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<EntityGetDTO>>() {
        });
        EntityGetDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(dto.getLabel(), dtoFromApi.getLabel());
        assertEquals(dto.getComment(), dtoFromApi.getComment());
    }

    @Test
    public void testGetByUri() throws Exception {

        // Try to insert an Entity, to fetch it and to get fields
        EntityCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Response getResult = getJsonGetByUriResponse(target(getByUriPath), uri.toString());

        // try to deserialize object and check if the fields value are the same
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<EntityGetDTO> getResponse =  mapper.convertValue(node, new TypeReference<SingleObjectResponse<EntityGetDTO>>() {
        });
        EntityGetDTO dtoFromDb = getResponse.getResult();
        assertNotNull(dtoFromDb);
        assertEquals(creationDTO.getLabel(),dtoFromDb.getLabel());
        assertEquals(creationDTO.getComment(),dtoFromDb.getComment());
        assertEquals(Oeso.Entity.getURI(),dtoFromDb.getType().toString());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(EntityModel.class);
    }

}