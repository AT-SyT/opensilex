//******************************************************************************
//                           ImageMetadataSparqlDAO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 11 December 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.List;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.dao.manager.SparqlDAO;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.model.ImageMetadata;

/**
 * Used to check if an image type exist in the triplestore
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ImageMetadataSparqlDAO extends SparqlDAO<ImageMetadata> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ImageMetadataSparqlDAO.class);
    
    public String rdfType;

    public ImageMetadataSparqlDAO() {
    }

    @Override
    public List<ImageMetadata> create(List<ImageMetadata> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<ImageMetadata> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<ImageMetadata> update(List<ImageMetadata> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ImageMetadata find(ImageMetadata object) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ImageMetadata findById(String id) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
