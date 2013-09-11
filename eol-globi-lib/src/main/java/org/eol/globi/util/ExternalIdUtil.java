package org.eol.globi.util;

import org.eol.globi.domain.TaxonomyProvider;

import java.util.HashMap;
import java.util.Map;

public class ExternalIdUtil {
    public static String infoURLForExternalId(String externalId) {
        String url = null;
        for (Map.Entry<String, String> idPrefixToUrlPrefix : getURLPrefixMap().entrySet()) {
            if (externalId.startsWith(idPrefixToUrlPrefix.getKey())) {
                url = idPrefixToUrlPrefix.getValue() + externalId.replaceAll(idPrefixToUrlPrefix.getKey(), "");
            }
            if (url != null) {
                break;
            }

        }
        return url;
    }

    public static Map<String, String> getURLPrefixMap() {
        return new HashMap<String, String>() {{
            put(TaxonomyProvider.ID_PREFIX_EOL, "http://eol.org/pages/");
            put(TaxonomyProvider.ID_PREFIX_GULFBASE, "http://gulfbase.org/biogomx/biospecies.php?species=");
        }};
    }
}