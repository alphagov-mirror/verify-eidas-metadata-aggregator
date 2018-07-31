package uk.gov.ida.metadataaggregator.apigateway;

/**

 This source code file was derived from Will Hamill’s open source work
 “AWS Lambda HelloWorld web application with config“ and is licenced under
 the MIT open source licence.

 Blog Post:
 https://willhamill.com/2016/12/12/aws-api-gateway-lambda-proxy-request-and-response-objects

 Original Source Code:
 https://github.com/willh/lambda-helloworld-config

 We are required to include this text as well:

 MIT License

 Copyright (c) 2016 Will Hamill

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
public class Identity {

    private String cognitoIdentityPoolId;
    private String accountId;
    private String cognitoIdentityId;
    private String caller;
    private String apiKey;
    private String sourceIp;
    private String cognitoAuthenticationType;
    private String cognitoAuthenticationProvider;
    private String userArn;
    private String userAgent;
    private String user;

    public String getCognitoIdentityPoolId() {
        return cognitoIdentityPoolId;
    }

    public void setCognitoIdentityPoolId(String cognitoIdentityPoolId) {
        this.cognitoIdentityPoolId = cognitoIdentityPoolId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCognitoIdentityId() {
        return cognitoIdentityId;
    }

    public void setCognitoIdentityId(String cognitoIdentityId) {
        this.cognitoIdentityId = cognitoIdentityId;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getCognitoAuthenticationType() {
        return cognitoAuthenticationType;
    }

    public void setCognitoAuthenticationType(String cognitoAuthenticationType) {
        this.cognitoAuthenticationType = cognitoAuthenticationType;
    }

    public String getCognitoAuthenticationProvider() {
        return cognitoAuthenticationProvider;
    }

    public void setCognitoAuthenticationProvider(String cognitoAuthenticationProvider) {
        this.cognitoAuthenticationProvider = cognitoAuthenticationProvider;
    }

    public String getUserArn() {
        return userArn;
    }

    public void setUserArn(String userArn) {
        this.userArn = userArn;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

}
