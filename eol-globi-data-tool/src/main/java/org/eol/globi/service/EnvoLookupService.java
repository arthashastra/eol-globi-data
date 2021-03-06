package org.eol.globi.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class EnvoLookupService extends TermLookupServiceImpl {
    @Override
    protected List<URI> getMappingURIList() {
        return new ArrayList<URI>() {{
            try {
                add(new URI("http://purl.obolibrary.org/obo/envo/mappings/spire-mapping.txt"));
            } catch (URISyntaxException e) {
                // ignore
            }
        }};
    }

    @Override
    protected char getDelimiter() {
        return '\t';
    }

    @Override
    protected boolean hasHeader() {
        return false;
    }
}
