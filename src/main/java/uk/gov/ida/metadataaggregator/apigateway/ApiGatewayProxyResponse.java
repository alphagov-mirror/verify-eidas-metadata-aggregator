package uk.gov.ida.metadataaggregator.apigateway;

import java.util.Map;

/*
    This source code file was derived from Will Hamill’s open source work
    “AWS Lambda HelloWorld web application with config“, which is licensed under the
    MIT open source license and which is available at:
    https://github.com/willh/lambda-helloworld-config
 */
public class ApiGatewayProxyResponse {

    private int statusCode;
    private Map<String, String> headers;
    private String body;

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public ApiGatewayProxyResponse(int statusCode, Map<String, String> headers, String body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }
}
