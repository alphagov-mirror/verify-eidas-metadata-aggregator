package uk.gov.ida.metadataaggregator.core;

import java.util.List;

public class DecodingResults {
    private final List<String> urls;
    private final List<String> invalidEncodingUrls;

    public DecodingResults(List<String> urls, List<String> invalidEncodingUrls) {
        this.urls = urls;
        this.invalidEncodingUrls = invalidEncodingUrls;
    }

    public List<String> urls(){
        return urls;
    }

    public List<String> invalidEncodingUrls(){
        return invalidEncodingUrls;
    }
}
