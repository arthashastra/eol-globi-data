package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ReportGeneratorTest extends GraphDBTestCase {


    @Test
    public void generateStudyReport() throws NodeFactoryException {
        createStudy("a second title", "a third source", null);
        createStudy("a title", "a third source");

        new ReportGenerator(getGraphDb()).generateReportForStudies();

        IndexHits<Node> reports = getGraphDb().index().forNodes("reports").get(Study.TITLE, "a title");
        assertThat(reports.size(), is(1));
        Node reportNode = reports.getSingle();
        assertThat((String) reportNode.getProperty(Study.TITLE), is("a title"));
        assertThat((String) reportNode.getProperty(Study.SOURCE), is("a third source"));
        assertThat((String) reportNode.getProperty(Study.CITATION), is("citation:doi:citation"));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        reports.close();

        reports = getGraphDb().index().forNodes("reports").get(Study.TITLE, "a second title");
        assertThat(reports.size(), is(1));
        reportNode = reports.getSingle();
        assertThat((String) reportNode.getProperty(Study.TITLE), is("a second title"));
        assertThat((String) reportNode.getProperty(Study.SOURCE), is("a third source"));
        assertThat(reportNode.hasProperty(Study.CITATION), is(false));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        reports.close();
    }


    @Test
    public void generateStudySourceReport() throws NodeFactoryException {
        createStudy("a title", "az source");
        createStudy("another title", "az source");
        createStudy("yet another title", "zother source", null);

        new ReportGenerator(getGraphDb()).generateReportForStudySources();

        IndexHits<Node> reports = getGraphDb().index().forNodes("reports").get(Study.SOURCE, "az source");
        Node reportNode = reports.getSingle();
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat((String) reportNode.getProperty(Study.SOURCE), is("az source"));
        reports.close();

        IndexHits<Node> otherReports = getGraphDb().index().forNodes("reports").get(Study.SOURCE, "zother source");
        Node otherReport = otherReports.getSingle();
        assertThat((String) otherReport.getProperty(Study.SOURCE), is("zother source"));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(1));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
    }

    @Test
    public void generateCollectionReport() throws NodeFactoryException {
        createStudy("a title", "source");
        createStudy("another title", "another source");

        new ReportGenerator(getGraphDb()).generateReportForCollection();

        IndexHits<Node> reports = getGraphDb().index().forNodes("reports").query("*:*");
        assertThat(reports.size(), is(1));
        Node reportNode = reports.getSingle();
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(2));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
    }

    protected Study createStudy(String title, String source) throws NodeFactoryException {
        return createStudy(title, source, "citation");
    }

    protected Study createStudy(String title, String source, String citation) throws NodeFactoryException {
        Study study = nodeFactory.getOrCreateStudy(title, source, citation);
        Specimen monkey = nodeFactory.createSpecimen(study, "Monkey");
        monkey.ate(nodeFactory.createSpecimen(study, "Banana"));
        monkey.ate(nodeFactory.createSpecimen(study, "Banana"));
        monkey.ate(nodeFactory.createSpecimen(study, "Banana"));
        monkey.ate(nodeFactory.createSpecimen(study, "Apple"));
        return study;
    }

}