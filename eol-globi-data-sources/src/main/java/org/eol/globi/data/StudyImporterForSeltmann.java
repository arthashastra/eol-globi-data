package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;
import org.eol.globi.util.CSVUtil;
import org.eol.globi.util.ResourceUtil;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StudyImporterForSeltmann extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForSeltmann.class);

    private String archiveURL = "seltmann/testArchive.zip";

    public StudyImporterForSeltmann(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        DB db = DBMaker
                .newMemoryDirectDB()
                .compressionEnable()
                .transactionDisable()
                .make();
        final HTreeMap<String, Map<String, String>> assocMap = db
                .createHashMap("assocMap")
                .make();

        try {
            LOG.info("download [" + getArchiveURL() + "] started...");
            InputStream inputStream = ResourceUtil.asInputStream(getArchiveURL(), StudyImporterForSeltmann.class);
            LOG.info("download [" + getArchiveURL() + "] complete.");
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry entry;
            File assocTempFile = null;
            File occTempFile = null;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().matches("(^|(.*/))associatedTaxa.tsv$")) {
                    assocTempFile = saveToTmpFile(zipInputStream, entry);
                } else if (entry.getName().matches("(^|(.*/))occurrences.tsv$")) {
                    occTempFile = saveToTmpFile(zipInputStream, entry);
                } else {
                    IOUtils.copy(zipInputStream, new NullOutputStream());
                }
            }
            IOUtils.closeQuietly(zipInputStream);

            if (assocTempFile == null) {
                throw new StudyImporterException("failed to find expected [associatedTaxa.tsv] resource");
            }

            if (occTempFile == null) {
                throw new StudyImporterException("failed to find expected [occurrences.tsv] resource");
            }

            LabeledCSVParser parser = CSVUtil.createLabeledCSVParser(FileUtils.getUncompressedBufferedReader(new FileInputStream(assocTempFile), CharsetConstant.UTF8));
            parser.changeDelimiter('\t');
            while (parser.getLine() != null) {
                Map<String, String> prop = new HashMap<String, String>();
                addKeyValue(parser, prop, "dwc:coreid");
                addKeyValue(parser, prop, "dwc:basisOfRecord");
                addKeyValue(parser, prop, "aec:associatedScientificName");
                addKeyValue(parser, prop, "dwc:basisOfRecord");
                addKeyValue(parser, prop, "aec:associatedRelationshipTerm");
                addKeyValue(parser, prop, "aec:associatedRelationshipURI");
                addKeyValue(parser, prop, "aec:associatedLocationOnHost");
                addKeyValue(parser, prop, "aec:associatedEmergenceVerbatimDate");
                String coreId = parser.getValueByLabel("dwc:coreid");
                if (StringUtils.isBlank(coreId)) {
                    LOG.warn("no coreid for line [" + parser.getLastLineNumber() + 1 + "]");
                } else {
                    assocMap.put(coreId, prop);
                }
            }

            LabeledCSVParser occurrence = CSVUtil.createLabeledCSVParser(new FileInputStream(occTempFile));
            occurrence.changeDelimiter('\t');
            while (occurrence.getLine() != null) {
                String references = occurrence.getValueByLabel("dcterms:references");

                Study study = nodeFactory.getOrCreateStudy("seltmann" + references, references + ". " + ReferenceUtil.createLastAccessedString(getArchiveURL()), references);
                String recordId = occurrence.getValueByLabel("idigbio:recordID");
                Map<String, String> assoc = assocMap.get(recordId);
                if (assoc != null) {
                    String targetName = assoc.get("aec:associatedScientificName");
                    String sourceName = occurrence.getValueByLabel("dwc:scientificName");
                    String eventDate = occurrence.getValueByLabel("dwc:eventDate");
                    Date date = null;
                    if (StringUtils.equals(eventDate, "0000-00-00")) {
                        getLogger().warn(study, "found suspicious event date [" + eventDate + "]" + getLineMsg(occurrence));
                    } else {
                        DateTimeFormatter fmtDateTime1 = DateTimeFormat.forPattern("yyyy-MM-dd");
                        try {
                            date = fmtDateTime1.parseDateTime(eventDate.split("/")[0]).toDate();
                        } catch (org.joda.time.IllegalFieldValueException e) {
                            throw new StudyImporterException("invalid date " + getLineMsg(occurrence), e);
                        }
                    }

                    if (StringUtils.isBlank(sourceName)) {
                        getLogger().warn(study, "found blank source taxon name" + getLineMsg(occurrence));
                    }

                    if (StringUtils.isBlank(targetName)) {
                        getLogger().warn(study, "found blank associated target taxon name" + getLineMsg(occurrence));
                    }

                    if (StringUtils.isNotBlank(sourceName) && StringUtils.isNotBlank(targetName)) {
                        try {
                            createInteraction(occurrence, study, assoc, targetName, sourceName, date);
                        } catch (NodeFactoryException ex) {
                            String message = "failed to import interaction because of [" + ex.getMessage() + "]" + getLineMsg(occurrence);
                            LOG.warn(message);
                            getLogger().warn(study, message);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException(e);
        }
        return null;
    }

    protected void createInteraction(LabeledCSVParser occurrence, Study study, Map<String, String> assoc, String targetName, String sourceName, Date date) throws NodeFactoryException, StudyImporterException {
        Specimen source = nodeFactory.createSpecimen(study, sourceName);
        Specimen target = nodeFactory.createSpecimen(study, targetName);
        String interactionURI = assoc.get("aec:associatedRelationshipURI");
        final Map<String, InteractType> assocInteractMap = new HashMap<String, InteractType>() {{
            put("http://purl.obolibrary.org/obo/RO_0002444", InteractType.PARASITE_OF);
            put("http://purl.obolibrary.org/obo/RO_0002445", InteractType.HAS_PARASITE);
            put("http://purl.obolibrary.org/obo/RO_0002437", InteractType.INTERACTS_WITH);

            // interaction types that could probably be more specific (e.g. found inside, found on, emerged from)
            put("http://purl.obolibrary.org/obo/RO_0002220", InteractType.INTERACTS_WITH);
            put("http://purl.obolibrary.org/obo/RO_0001025", InteractType.INTERACTS_WITH);
            put("http://eol.org/schema/terms/emergedFrom", InteractType.INTERACTS_WITH);
            put("http://eol.org/schema/terms/foundNear", InteractType.INTERACTS_WITH);
        }
        };
        InteractType interactType = StringUtils.isBlank(interactionURI) ? InteractType.INTERACTS_WITH : assocInteractMap.get(interactionURI);
        if (interactType == null) {
            throw new StudyImporterException("found unsupported interactionURI: [" + interactionURI +"] related to" + getLineMsg(occurrence));
        }
        source.interactsWith(target, interactType);

        String sourceBasisOfRecord = occurrence.getValueByLabel("dwc:basisOfRecord");
        source.setBasisOfRecord(nodeFactory.getOrCreateBasisOfRecord(sourceBasisOfRecord, sourceBasisOfRecord));

        String targetBasisOfRecord = assoc.get("dwc:basisOfRecord");
        target.setBasisOfRecord(nodeFactory.getOrCreateBasisOfRecord(targetBasisOfRecord, targetBasisOfRecord));

        nodeFactory.setUnixEpochProperty(source, date);
        nodeFactory.setUnixEpochProperty(target, date);
        String latitude = occurrence.getValueByLabel("dwc:decimalLatitude");
        String longitude = occurrence.getValueByLabel("dwc:decimalLongitude");
        if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
            Location loc = nodeFactory.getOrCreateLocation(Double.parseDouble(latitude), Double.parseDouble(longitude), null);
            source.caughtIn(loc);
        }
    }

    private String getLineMsg(LabeledCSVParser occurrence) {
        return " on line [" + (occurrence.getLastLineNumber() + 1) + "]";
    }

    protected File saveToTmpFile(ZipInputStream zipInputStream, ZipEntry entry) throws IOException {
        File tempFile = File.createTempFile(entry.getName(), "tmp");
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile);
        IOUtils.copy(zipInputStream, fos);
        fos.flush();
        IOUtils.closeQuietly(fos);
        return tempFile;
    }

    protected void addKeyValue(LabeledCSVParser parser, Map<String, String> prop, String key) {
        prop.put(key, parser.getValueByLabel(key));
    }

    public String getArchiveURL() {
        return archiveURL;
    }

    public void setArchiveURL(String archiveURL) {
        this.archiveURL = archiveURL;
    }

    @Override
    public boolean shouldCrossCheckReference() {
        return false;
    }
}
