package org.opensilex.core.experiment.dal;

import org.eclipse.rdf4j.model.vocabulary.LIST;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.core.AbstractDaoTest;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.utils.ListWithPagination;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Renaud COLIN
 */
public class ExperimentDAOTest extends AbstractDaoTest {

    protected static ExperimentDAO xpDao;
    protected static final String xpGraph = "set/experiments";

    protected static ProjectDAO projectDAO;
    protected static ProjectModel projectModel;
    protected static final String projectGraph = "project";

    @BeforeClass
    public static void initialize() throws Exception {

        AbstractDaoTest.initialize();

        xpDao = new ExperimentDAO(service);
        projectDAO = new ProjectDAO(service);
    }

    @Before
    public void initGraph() throws Exception {
        projectModel = new ProjectModel();
        projectModel.setName("TEST PROJECT");
        projectDAO.create(projectModel);
    }

    @Override
    protected List<String> getGraphsToCleanNames() {
        return Arrays.asList(xpGraph, projectGraph);
    }

    protected ExperimentModel getModel(int i) {

        ExperimentModel xpModel = new ExperimentModel();
        String label = "test xp" + i;
        xpModel.setLabel(label);
        xpModel.setStartDate(LocalDate.now());
        xpModel.setEndDate(LocalDate.now().plusDays(200));
        xpModel.setCampaign(2019);

        xpModel.setProjects(Collections.singletonList(projectModel));
        xpModel.setComment("a comment about an xp");
        xpModel.setKeywords(Arrays.asList("test", "project", "opensilex"));
        xpModel.setObjectives("this project has for objective to pass all the TU !");

        return xpModel;
    }

    @Test
    public void create() throws Exception {

        int count = service.count(ExperimentModel.class, null);
        assertEquals("the initial ExperimentModel count must be 0", 0, count);

        xpDao.create(getModel(0));

        count = service.count(ExperimentModel.class, null);
        assertEquals("the count must be equals to 1 since the experiment has been created", 1, count);
    }

    @Test
    public void createAll() throws Exception {

        int count = service.count(ExperimentModel.class, null);
        assertEquals(count, 0);

        int n = 20;
        List<ExperimentModel> xps = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            xps.add(getModel(i));
        }
        xpDao.createAll(xps);

        count = service.count(ExperimentModel.class, null);
        assertEquals("the count must be equals to " + n + " since " + n + " experiment have been created", n, count);
    }

    protected void testEquals(final ExperimentModel xp, final ExperimentModel otherXp) {

        final String errorMsg = "fetched object different than model";

        assertEquals(errorMsg, otherXp.getUri(), xp.getUri());
        assertEquals(errorMsg, otherXp.getLabel(), xp.getLabel());
        assertEquals(errorMsg, otherXp.getStartDate(), xp.getStartDate());
        assertEquals(errorMsg, otherXp.getEndDate(), xp.getEndDate());
        assertEquals(errorMsg, otherXp.getCampaign(), xp.getCampaign());
        assertEquals(errorMsg, otherXp.getObjectives(), xp.getObjectives());
        assertEquals(errorMsg, otherXp.getComment(), xp.getComment());
        assertEquals(errorMsg, otherXp.getKeywords(), xp.getKeywords());
        assertEquals(errorMsg, otherXp.getProjects(), xp.getProjects());
        assertEquals(errorMsg, otherXp.getScientificSupervisors(), xp.getScientificSupervisors());
        assertEquals(errorMsg, otherXp.getTechnicalSupervisors(), xp.getTechnicalSupervisors());
        assertEquals(errorMsg, otherXp.getGroups(), xp.getGroups());
    }

    @Test
    public void getByUri() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        ExperimentModel daoXpModel = xpDao.get(xpModel.getUri());
        assertNotNull(daoXpModel);

        testEquals(xpModel, daoXpModel);
    }

    @Test
    public void getAllXp() throws Exception {

        int n = 100;
        List<ExperimentModel> xps = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            xps.add(getModel(i));
        }
        xpDao.createAll(xps);

        int pageSize = 10;
        int nbPage = n / pageSize;
        for (int i = 0; i < nbPage; i++) {
            ListWithPagination<ExperimentModel> xpModelResults = xpDao.search(null, null, i, pageSize);
            List<ExperimentModel> xpsFromDao = xpModelResults.getList();
            assertEquals(pageSize, xpsFromDao.size());
        }

    }

    @Test
    public void searchWithDataType() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        ExperimentSearchDTO searchDTO = new ExperimentSearchDTO();
        searchDTO.setLabel(xpModel.getLabel());
        searchDTO.setCampaign(xpModel.getCampaign());
        searchDTO.setStartDate(xpModel.getStartDate());

        ListWithPagination<ExperimentModel> xpModelResults = xpDao.search(searchDTO, null, 0, 10);
        assertNotNull("one experiment should be fetched from db", xpModelResults);
        assertEquals("one experiment should be fetched from db", xpModelResults.getList().size(), 1);

        testEquals(xpModel, xpModelResults.getList().get(0));
    }


    @Test
    public void searchWithObjectUriType() throws Exception {

        // create two projects
        List<ProjectModel> projects = new ArrayList<>();
        projects.add(projectModel);

        ProjectModel project2 = new ProjectModel();
        project2.setName("TEST PROJECT");
        projects.add(projectDAO.create(project2));

        ExperimentModel xpModel = getModel(0);
        xpModel.setProjects(projects);
        xpDao.create(xpModel);

        ExperimentSearchDTO searchDTO = new ExperimentSearchDTO();
        searchDTO.setProjects(Arrays.asList(projectModel.getUri(), project2.getUri()));

        List<ExperimentModel> xpModelResults = xpDao.search(searchDTO, null, 0, 10).getList();

        assertNotNull("no experiment found from db", xpModelResults);
        assertEquals("the experiment uri should be contained into the result list", xpModelResults.get(0).getUri(), xpModel.getUri());
        testEquals(xpModel, xpModelResults.get(0));

        // create a 2nd xp with a shared project with the 1th xp

        ExperimentModel xpModel2 = getModel(1);
        xpModel2.setProjects(Collections.singletonList(project2));
        xpDao.create(xpModel2);

        xpModelResults = xpDao.search(searchDTO, null, 0, 10).getList();
        assertNotNull("no experiment found from db", xpModelResults);

        assertTrue(xpModelResults.contains(xpModel2));
        assertTrue(xpModelResults.contains(xpModel));

    }

    @Test
    public void searchFail() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        ExperimentSearchDTO getDTO = new ExperimentSearchDTO();

        // set a bad label in order to check if the result set from dao is empty
        getDTO.setLabel(xpModel.getLabel() + "str");

        getDTO.setProjects(xpModel.getProjects()
                .stream()
                .map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList()));

        ListWithPagination<ExperimentModel> xpModelResults = xpDao.search(getDTO, null, 0, 10);
        assertNotNull("empty results object", xpModelResults);
        assertTrue("no experiment should be found from db according getDTO", xpModelResults.getList().isEmpty());

        // reset the label by setting it to null, then a result must exists
        getDTO.setLabel(null);
        getDTO.setCampaign(xpModel.getCampaign());

        xpModelResults = xpDao.search(getDTO, null, 0, 10);
        assertNotNull("no experiment found from db", xpModelResults);
        assertEquals("the experiment uri should be contained into the result list", xpModelResults.getList().get(0).getUri(), xpModel.getUri());
        testEquals(xpModel, xpModelResults.getList().get(0));

    }

    @Test
    public void searchArchived() throws Exception {

        // create an archived and an unarchived xp
        LocalDate currentDate = LocalDate.now();
        ExperimentModel archivedXp = getModel(0);
        archivedXp.setStartDate(currentDate.minusDays(3));
        archivedXp.setEndDate(currentDate.minusDays(1));
        xpDao.create(archivedXp);

        ExperimentModel unarchivedXp = getModel(1);
        unarchivedXp.setStartDate(currentDate.minusDays(3));
        unarchivedXp.setEndDate(currentDate.plusDays(3));
        xpDao.create(unarchivedXp);

        // try to retrieve xps from dao
        List<ExperimentModel> archivedXps = xpDao.search(new ExperimentSearchDTO().setEnded(true), null, 0, 10).getList();
        assertEquals(1, archivedXps.size());
        assertTrue(archivedXps.contains(archivedXp));

        List<ExperimentModel> unarchivedXps = xpDao.search(new ExperimentSearchDTO().setEnded(false), null, 0, 10).getList();
        assertEquals(1, unarchivedXps.size());
        assertTrue(unarchivedXps.contains(unarchivedXp));

        // search all archived projects
        List<ExperimentModel> allXps = xpDao.search(new ExperimentSearchDTO(), null, 0, 10).getList();
        assertEquals(2, allXps.size());
        assertTrue(allXps.contains(archivedXp));
        assertTrue(allXps.contains(unarchivedXp));

    }

    @Test
    public void update() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        // update attributes
        xpModel.setStartDate(LocalDate.now());
        xpModel.setEndDate(LocalDate.now().plusDays(300));

        xpModel.setLabel("new label");
        xpModel.setComment("new comments about model");
        xpModel.setObjectives("new objective");
        xpModel.setCampaign(2020);
        xpDao.update(xpModel);

        // get the new xp model and check that all fields are equals
        ExperimentModel daoXpModel = xpDao.get(xpModel.getUri());
        assertNotNull("no experiment found from db", daoXpModel);
        testEquals(xpModel, daoXpModel);
    }

    @Test
    public void delete() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        int oldCount = service.count(ExperimentModel.class, null);
        assertEquals("one experiment should have been created", 1, oldCount);

        xpDao.delete(xpModel.getUri());

        int newCount = service.count(ExperimentModel.class, null);
        assertEquals("the experiment must no longer exists", 0, newCount);
        assertFalse("the experiment URI must no longer exists", xpDao.sparql.uriExists(xpModel.getUri()));
    }

    @Test
    public void deleteAll() throws Exception {

        int n = 20;
        List<ExperimentModel> xps = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            xps.add(getModel(i));
        }
        xpDao.createAll(xps);

        int oldCount = service.count(ExperimentModel.class, null);
        assertEquals(n + " experiments should have been created", oldCount, n);

        xpDao.deleteAll(xps.stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList()));

        int newCount = service.count(ExperimentModel.class, null);
        assertEquals("all experiments should have been deleted", 0, newCount);

        for (ExperimentModel xp : xps) {
            assertFalse("the ExperimentModel " + xp.getUri() + " should have been deleted", xpDao.sparql.uriExists(xp.getUri()));
        }
    }

}