package org.eol.globi.server;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.CSVUtil;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.when;

public class InteractionControllerTest {

    @Test
    public void interactionTypes() throws IOException {
        String interactionTypes = new InteractionController().getInteractionTypes(null);
        assertThat(interactionTypes, is(notNullValue()));
    }

    @Test
    public void interactionFieldsJson() throws IOException {
        String interactionFields = new InteractionController().getInteractionFields(null);
        JsonNode jsonNode = new ObjectMapper().readTree(interactionFields);
        assertThat(jsonNode.get(ResultField.TAXON_NAME.getLabel()).get("description").asText(), is(notNullValue()));
    }

    @Test
    public void interactionFieldsCSV() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("csv");

        String interactionFields = new InteractionController().getInteractionFields(request);
        assertThat(interactionFields, containsString("name,description"));
    }

    @Test
    public void interactionTypesCSV() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("csv");

        String interactionTypes = new InteractionController().getInteractionTypes(request);
        LabeledCSVParser parser = CSVUtil.createLabeledCSVParser(IOUtils.toInputStream(interactionTypes));
        assertThat(parser.getLabels(), is(new String[]{"interaction", "source", "target", "termIRI"}));
        while (parser.getLine() != null) {
            assertThat(parser.getValueByLabel("interaction"), is(notNullValue()));
            assertThat(parser.getValueByLabel("source"), is(notNullValue()));
            assertThat(parser.getValueByLabel("target"), is(notNullValue()));
            assertThat(parser.getValueByLabel("termIRI"), is(notNullValue()));
        }
    }

    @Test
    public void findSupportedInteractionTypes() throws IOException {
        String interactionTypes = new InteractionController().getInteractionTypes(null);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(interactionTypes);
        for (JsonNode interactionType : jsonNode) {
            assertThat(interactionType.has("source"), is(true));
            assertThat(interactionType.has("target"), is(true));
            assertThat(interactionType.has("termIRI"), is(true));
        }
        assertThat(jsonNode.has(CypherQueryBuilder.INTERACTION_HAS_PARASITE), is(true));
        assertThat(jsonNode.has(CypherQueryBuilder.INTERACTION_PARASITE_OF), is(true));
        assertThat(jsonNode.has(CypherQueryBuilder.INTERACTION_PREYED_UPON_BY), is(true));
        assertThat(jsonNode.has(CypherQueryBuilder.INTERACTION_PREYS_ON), is(true));
        assertThat(jsonNode.has(CypherQueryBuilder.INTERACTION_HAS_PATHOGEN), is(true));
        assertThat(jsonNode.has(CypherQueryBuilder.INTERACTION_SYMBIONT_OF), is(true));
        assertThat(jsonNode.has(CypherQueryBuilder.INTERACTION_INTERACTS_WITH), is(true));
    }

}
