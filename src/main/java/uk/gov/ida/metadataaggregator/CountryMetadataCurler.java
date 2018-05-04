package uk.gov.ida.metadataaggregator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class CountryMetadataCurler implements CountryMetadataSource {

    @Override
    public String downloadMetadata(String url) throws IOException {

        URLConnection urlConnection = new URL(url).openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                urlConnection.getInputStream()));
        String inputLine;
        StringBuilder html = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            html.append(inputLine);
        }
        in.close();

        return html.toString();
    }
}
