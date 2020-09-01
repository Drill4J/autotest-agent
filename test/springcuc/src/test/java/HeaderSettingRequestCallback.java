import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.web.client.*;

import java.io.*;
import java.util.*;

public class HeaderSettingRequestCallback implements RequestCallback {
    final Map<String, String> requestHeaders;

    private String body;

    public HeaderSettingRequestCallback(final Map<String, String> headers) {
        this.requestHeaders = headers;
    }

    public void setBody(final String postBody) {
        this.body = postBody;
    }

    @Override
    public void doWithRequest(ClientHttpRequest request) throws IOException {
        final HttpHeaders clientHeaders = request.getHeaders();
        for (final Map.Entry<String, String> entry : requestHeaders.entrySet()) {
            clientHeaders.add(entry.getKey(), entry.getValue());
        }
        if (null != body) {
            request.getBody().write(body.getBytes());
        }
    }
}
