/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.vocabulary.OWL2;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.PropertyModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;

/**
 *
 * @author vmigot
 */
public class RDFPropertyDTO {

    protected URI uri;

    protected URI type;

    protected Map<String, String> label;

    protected Map<String, String> comment;

    protected URI domain;

    protected URI range;
    
    protected String rangeLabel;

    protected URI parent;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public Map<String, String> getComment() {
        return comment;
    }

    public void setComment(Map<String, String> comment) {
        this.comment = comment;
    }

    public URI getDomain() {
        return domain;
    }

    public void setDomain(URI domain) {
        this.domain = domain;
    }

    public URI getRange() {
        return range;
    }

    public void setRange(URI range) {
        this.range = range;
    }

    public String getRangeLabel() {
        return rangeLabel;
    }

    public void setRangeLabel(String rangeLabel) {
        this.rangeLabel = rangeLabel;
    }

    
    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    @JsonIgnore
    public boolean isDataProperty() {
        return isDataProperty(getType());
    }

    public PropertyModel toModel(PropertyModel model) {

        model.setUri(getUri());
        model.setLabel(SPARQLLabel.fromMap(getLabel()));
        model.setComment(SPARQLLabel.fromMap(getComment()));
        ClassModel pDomain = new ClassModel();
        pDomain.setUri(getDomain());
        model.setDomain(pDomain);

        return model;
    }

    public static RDFPropertyDTO fromModel(PropertyModel model, PropertyModel parentModel) {
        RDFPropertyDTO dto = new RDFPropertyDTO();

        dto.setUri(model.getUri());
        dto.setType(model.getType());
        if (parentModel != null) {
            dto.setParent(parentModel.getUri());
        }
        dto.setLabel(model.getLabel().getAllTranslations());
        if (model.getComment() != null) {
            dto.setComment(model.getComment().getAllTranslations());
        } else {
            dto.setComment(new HashMap<>());
        }

        if (model.getDomain() != null) {
            dto.setDomain(model.getDomain().getUri());
        }

        return dto;
    }

    public static boolean isDataProperty(URI propertyType) {
        return SPARQLDeserializers.compareURIs(propertyType.toString(), OWL2.DatatypeProperty.getURI());
    }
}
