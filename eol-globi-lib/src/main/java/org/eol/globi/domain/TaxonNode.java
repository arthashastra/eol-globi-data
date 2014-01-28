package org.eol.globi.domain;

import org.neo4j.graphdb.Node;

import static org.eol.globi.domain.PropertyAndValueDictionary.*;

public class TaxonNode extends NamedNode implements Taxon {

    public TaxonNode(Node node) {
        super(node);
    }

    public TaxonNode(Node node, String name) {
        this(node);
        setName(name);
    }

    @Override
    public String getPath() {
        return getUnderlyingNode().hasProperty(PATH) ?
                (String) getUnderlyingNode().getProperty(PATH) : null;
    }

    @Override
    public void setPath(String path) {
        if (path != null) {
            getUnderlyingNode().setProperty(PATH, path);
        }
    }

    @Override
    public String getCommonNames() {
        return getUnderlyingNode().hasProperty(COMMON_NAMES) ?
                (String) getUnderlyingNode().getProperty(COMMON_NAMES) : null;
    }

    @Override
    public void setCommonNames(String commonNames) {
        if (commonNames != null) {
            getUnderlyingNode().setProperty(COMMON_NAMES, commonNames);
        }
    }
}