package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;

public class StudyImporterForHafner extends BaseStudyImporter {

    public static final String RESOURCE = "hafner/gopher_lice_int.csv";

    public StudyImporterForHafner(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        try {
            LabeledCSVParser parser = parserFactory.createParser(RESOURCE, "UTF-8");
            while (parser.getLine() != null) {
                String sourceCitation = "Mark S. Hafner, Philip D. Sudman, Francis X. Villablanca, Theresa A. Spradling, James W. Demastes, Steven A. Nadler. (1994). Disparate Rates of Molecular Evolution in Cospeciating Hosts and Parasites. Science 265: 1087-1090.";
                Study study = nodeFactory.getOrCreateStudy("hafner1994", "Shan Kothari, Pers. Comm. 2014.", null, ExternalIdUtil.toCitation(null, sourceCitation, null));
                study.setCitationWithTx(sourceCitation);

                String hostName = parser.getValueByLabel("Host");
                String parasiteName = parser.getValueByLabel("Parasite");
                Specimen host = nodeFactory.createSpecimen(study, hostName);
                Specimen parasite = nodeFactory.createSpecimen(study, parasiteName);
                parasite.interactsWith(host, InteractType.PARASITE_OF);
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to import [" + RESOURCE + "]", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import [" + RESOURCE + "]", e);
        }

        return null;
    }

}
