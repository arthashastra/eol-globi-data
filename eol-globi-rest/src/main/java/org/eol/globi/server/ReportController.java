package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.CypherQuery;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ReportController {

    @RequestMapping(value = "/reports/studies", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery studies(@RequestParam(required = false) final String source) throws IOException {
        String cypherQuery = "START report = node:reports(" + (StringUtils.isBlank(source) ? "'source:*'" : "source={source}") + ") "
                + " WHERE has(report.title) "
                + " RETURN report.citation? as " + ResultField.STUDY_CITATION
                + ", report.externalId? as " + ResultField.STUDY_URL
                + ", report.doi? as " + ResultField.STUDY_DOI
                + ", report.source as " + ResultField.STUDY_SOURCE_CITATION
                + ", report.nInteractions as " + ResultField.NUMBER_OF_INTERACTIONS
                + ", report.nTaxa as " + ResultField.NUMBER_OF_DISTINCT_TAXA
                + ", report.nStudies? as " + ResultField.NUMBER_OF_STUDIES
                + ", report.nSources? as " + ResultField.NUMBER_OF_SOURCES;
        Map<String, String> params = StringUtils.isBlank(source) ? CypherQueryBuilder.EMPTY_PARAMS : new HashMap<String, String>() {{
            put("source", source);
        }};

        return new CypherQuery(cypherQuery, params);
    }

    @RequestMapping(value = "/reports/sources", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery sources(@RequestParam(required = false) final String source) throws IOException {
        String cypherQuery = "START report = node:reports(" + (StringUtils.isBlank(source) ? "'source:*'" : "source={source}") + ") "
                + " WHERE not(has(report.title)) AND has(report.source)"
                + " RETURN report.citation? as " + ResultField.STUDY_CITATION
                + ", report.externalId? as " + ResultField.STUDY_URL
                + ", report.doi? as " + ResultField.STUDY_DOI
                + ", report.source as " + ResultField.STUDY_SOURCE_CITATION
                + ", report.nInteractions as " + ResultField.NUMBER_OF_INTERACTIONS
                + ", report.nTaxa as " + ResultField.NUMBER_OF_DISTINCT_TAXA
                + ", report.nStudies? as " + ResultField.NUMBER_OF_STUDIES
                + ", report.nSources? as " + ResultField.NUMBER_OF_SOURCES;

        Map<String, String> params = StringUtils.isBlank(source) ? CypherQueryBuilder.EMPTY_PARAMS : new HashMap<String, String>() {{
            put("source", source);
        }};

        return new CypherQuery(cypherQuery, params);
    }

    @RequestMapping(value = "/reports/collections", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery collections() throws IOException {
        String cypherQuery = "START report = node:reports('collection:*')" +
                " WHERE not(has(report.title)) AND not(has(report.source)) "
                + " RETURN report.citation? as " + ResultField.STUDY_CITATION
                + ", report.externalId? as " + ResultField.STUDY_URL
                + ", report.doi? as " + ResultField.STUDY_DOI
                + ", report.source? as " + ResultField.STUDY_SOURCE_CITATION
                + ", report.nInteractions as " + ResultField.NUMBER_OF_INTERACTIONS
                + ", report.nTaxa as " + ResultField.NUMBER_OF_DISTINCT_TAXA
                + ", report.nStudies? as " + ResultField.NUMBER_OF_STUDIES
                + ", report.nSources? as " + ResultField.NUMBER_OF_SOURCES;
        return new CypherQuery(cypherQuery);
    }

}
