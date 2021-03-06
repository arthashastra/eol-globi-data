package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForBaremoreTest extends GraphDBTestCase {

    @Test
    public void importLine() throws StudyImporterException, NodeFactoryException {
        String csvContent = "\"Shark Id\",\"Date\",\"Survey type\",\"Shark TL\",\"Gape length (cm)\",\"Mat State\",\"Prey code\",\"Common name\",\"Prey No\",\"No in stomach\",\"SL (mm)\",\"FL\",\"TL\",\"VC\",\"Length\",\"Height\",\"Other measurement\",\"Measurement type\",\"Weight (g)\",\"% Dig\",\"Notes\"\n" +
                "13,2/7/2003,\"Dependent\",68,,\"Juv\",\"ATCR\",\"Atlantic croaker\",1,1,,,,,,,,,2.81,90,\"1 pr eyes, otos.  Weighed with otos\"\n" +
                "131,3/14/2003,\"Dependent\",68,,\"Juv\",\"ATCR\",\"Atlantic croaker\",4,1,,,,,,,,,8.25,90,\"Bones, scales, vert in pieces. 1 pair otos\"\n" +
                "394,12/16/2003,\"Dependent\",103,,\"Mat\",\"ATCR\",\"Atlantic croaker\",1,1,175,,,,,,,,64.24,30,\n" +
                "401,12/16/2003,\"Dependent\",85,,\"Juv\",\"ATCR\",\"Atlantic croaker\",2,1,,,192,,,,,,76.27,0,\"In stomach but suspciously undigested.  Some scales gone\"\n" +
                "402,12/16/2003,\"Dependent\",97,,\"Mat\",\"ATCR\",\"Atlantic croaker\",1,1,,,,120,,,,,51.88,60,\n" +
                "402,12/16/2003,\"Dependent\",97,,\"Mat\",\"ATCR\",\"Atlantic croaker\",2,1,,,,,,,,,6.28,90,\"W/ otos\"";

        StudyImporterForBaremore studyImporterFor = new StudyImporterForBaremore(new TestParserFactory(csvContent), nodeFactory);

        Study study = studyImporterFor.importStudy();
        assertNotNull(nodeFactory.findTaxonByName("Squatina dumeril"));
        assertNotNull(nodeFactory.findTaxonByName("Atlantic croaker"));

        Iterable<Relationship> collectedRels = study.getSpecimens();
        int totalRels = validateSpecimen(collectedRels);

        assertThat(totalRels, Is.is(11));
    }

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporterForBaremore studyImporterFor = new StudyImporterForBaremore(new ParserFactoryImpl(), nodeFactory);

        Study study = studyImporterFor.importStudy();
        assertNotNull(nodeFactory.findTaxonByName("Squatina dumeril"));

        Iterable<Relationship> collectedRels = study.getSpecimens();
        int totalRels = validateSpecimen(collectedRels);

        assertThat(totalRels, Is.is(1450));

    }

    private int validateSpecimen(Iterable<Relationship> collectedRels) {
        int totalRels = 0;
        for (Relationship rel : collectedRels) {
            assertTrue(rel.hasProperty(Specimen.DATE_IN_UNIX_EPOCH));
            Node specimen = rel.getEndNode();
            assertNotNull(specimen);
            Iterable<Relationship> rels = specimen.getRelationships(Direction.OUTGOING, InteractType.ATE);
            for (Relationship relationship : rels) {
                assertTrue(specimen.hasProperty(Specimen.LENGTH_IN_MM));
                assertTrue(specimen.hasProperty(Specimen.LIFE_STAGE_LABEL));
            }
            Relationship collectedAtRelationship = specimen.getSingleRelationship(RelTypes.COLLECTED_AT, Direction.OUTGOING);
            assertNotNull(collectedAtRelationship);
            Node locationNode = collectedAtRelationship.getEndNode();
            assertNotNull(locationNode);
            assertThat((Double) locationNode.getProperty(Location.LATITUDE), is(29.219302));
            assertThat((Double) locationNode.getProperty(Location.LONGITUDE), is(-87.06665));
            totalRels++;
        }
        return totalRels;
    }
}
