import org.apache.commons.io.*;
import org.springframework.http.client.*;

import java.io.*;

public class ResponseResults {
    private final ClientHttpResponse theResponse;
    private final String body;

    ResponseResults(final ClientHttpResponse response) throws IOException {
        this.theResponse = response;
        final InputStream bodyInputStream = response.getBody();
        final StringWriter stringWriter = new StringWriter();
        IOUtils.copy(bodyInputStream, stringWriter);
        this.body = stringWriter.toString();
    }

    ClientHttpResponse getTheResponse() {
        return theResponse;
    }

    String getBody() {
        return body;
    }
}
