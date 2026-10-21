package api;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

final class Api {

    static final String BASE = System.getProperty("api.baseUrl");
    static final String HTTP_BASE = System.getProperty("api.httpUrl");

    private static final HttpClient CLIENT = newClient();

    private Api() {
    }

    record Response(int status, String body, HttpHeaders headers) {

        String header(String name) {
            return headers.firstValue(name).orElse(null);
        }

        String text(String xpath) {
            return (String) evaluate(xpath, XPathConstants.STRING);
        }

        int count(String xpath) {
            return ((Number) evaluate("count(" + xpath + ")", XPathConstants.NUMBER)).intValue();
        }

        List<String> texts(String xpath) {
            NodeList nodes = (NodeList) evaluate(xpath, XPathConstants.NODESET);
            List<String> result = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                result.add(node.getTextContent());
            }
            return result;
        }

        private Object evaluate(String expression, javax.xml.namespace.QName type) {
            try {
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new ByteArrayInputStream(body.getBytes(UTF_8)));
                XPath xpath = XPathFactory.newInstance().newXPath();
                return xpath.evaluate(expression, document, type);
            } catch (Exception e) {
                throw new IllegalStateException("not an XML body: " + body, e);
            }
        }
    }

    static Response get(String path, String... headers) {
        return send("GET", path, null, headers);
    }

    static Response post(String path, String body, String... headers) {
        return send("POST", path, body, headers);
    }

    static Response put(String path, String body, String... headers) {
        return send("PUT", path, body, headers);
    }

    static Response delete(String path, String... headers) {
        return send("DELETE", path, null, headers);
    }

    static Response send(String method, String path, String body, String... headers) {
        Map<String, String> all = new LinkedHashMap<>();
        all.put("Accept", "application/xml");
        if (body != null) {
            all.put("Content-Type", "application/xml");
        }
        for (int i = 0; i < headers.length; i += 2) {
            all.put(headers[i], headers[i + 1]);
        }
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(10))
                .method(method, body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body, UTF_8));
        all.forEach(request::header);
        try {
            var response = CLIENT.send(request.build(), BodyHandlers.ofString(UTF_8));
            return new Response(response.statusCode(), response.body(), response.headers());
        } catch (IOException e) {
            throw new IllegalStateException("request failed: " + method + " " + BASE + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    static String encode(String value) {
        return URLEncoder.encode(value, UTF_8);
    }

    private static HttpClient newClient() {
        try (InputStream in = Files.newInputStream(Path.of(System.getProperty("api.truststore")))) {
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(in, System.getProperty("api.truststorePassword").toCharArray());
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(trustStore);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, factory.getTrustManagers(), null);
            return HttpClient.newBuilder().sslContext(context).connectTimeout(Duration.ofSeconds(5)).build();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
