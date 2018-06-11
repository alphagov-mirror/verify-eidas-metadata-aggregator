package uk.gov.ida.metadataaggregator;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class JsonAggregatorConfigBuilder {

    private final Set<String> urls = new HashSet<>();

    public static JsonAggregatorConfigBuilder newConfig() {
        return new JsonAggregatorConfigBuilder();
    }

    public JsonAggregatorConfigBuilder withMetadataUrl(String url) {
        urls.add(url);
        return this;
    }

    public String toJson() {
        return new JSONObject()
                .put("metadataUrls", urls)
                .toString();
    }

    public String toInvalidJson() {
        return new JSONObject()
                .put("invalidKey", urls)
                .toString();
    }
}
