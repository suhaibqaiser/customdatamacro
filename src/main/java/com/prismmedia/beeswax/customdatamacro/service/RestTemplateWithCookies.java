package com.prismmedia.beeswax.customdatamacro.service;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class RestTemplateWithCookies extends RestTemplate {

    private List<String> cookies = null;

    public RestTemplateWithCookies() {
    }

    public RestTemplateWithCookies(ClientHttpRequestFactory requestFactory) {
        super(requestFactory);
    }

    private synchronized List<String> getCoookies() {
        return cookies;
    }

    private synchronized void setCoookies(List<String> cookies) {
        this.cookies = cookies;
    }

    public synchronized void resetCoookies() {
        this.cookies = null;
    }

    private void processHeaders(HttpHeaders headers) {
        final List<String> cookies = headers.get("Set-Cookie");
        if (cookies != null && !cookies.isEmpty()) {
            setCoookies(cookies);
        }
    }

    @Override
    protected <T extends Object> T doExecute(URI url, HttpMethod method, final RequestCallback requestCallback, final ResponseExtractor<T> responseExtractor) throws RestClientException {
        final List<String> cookies = getCoookies();

        return super.doExecute(url, method, new RequestCallback() {
            @Override
            public void doWithRequest(ClientHttpRequest chr) throws IOException {
                if(cookies != null) {
                    for(String cookie : cookies) {
                        chr.getHeaders().add("Cookie", cookie);
                    }
                }
                requestCallback.doWithRequest(chr);
            }

        }, new ResponseExtractor<T>() {
            @Override
            public T extractData(ClientHttpResponse chr) throws IOException {
                processHeaders(chr.getHeaders());
                return responseExtractor.extractData(chr);
            }
        });
    }

}
