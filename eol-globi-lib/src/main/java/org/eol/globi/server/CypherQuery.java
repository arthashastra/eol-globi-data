package org.eol.globi.server;

import java.util.Map;

public class CypherQuery {
    private final String query;

    private final Map<String, String> params;

    public CypherQuery(String query) {
        this(query, null);
    }

    public CypherQuery(String query, Map<String, String> params) {
        this.query = query;
        this.params = params;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getQuery() {
        return query;
    }

}