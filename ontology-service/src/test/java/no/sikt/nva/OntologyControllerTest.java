package no.sikt.nva;
import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.micronaut.function.aws.proxy.MicronautLambdaHandler;
import org.zalando.problem.Problem;
import org.zalando.problem.jackson.ProblemModule;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.zalando.problem.Status.NOT_ACCEPTABLE;

public class OntologyControllerTest {

    private static MicronautLambdaHandler handler;
    private static final Context lambdaContext = new MockLambdaContext();
    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void setupSpec() {
        try {
            handler = new MicronautLambdaHandler();
            objectMapper = handler.getApplicationContext().getBean(ObjectMapper.class);
            objectMapper.registerModule(new ProblemModule());

        } catch (ContainerInitializationException e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    public static void cleanupSpec() {
        handler.getApplicationContext().close();
    }

    @Test
    void shouldReturnTurtleWhenAcceptHeaderIsApplicationTurtle() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/test", HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, "application/turtle")
                .build();
        AwsProxyResponse response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.OK.getCode(), response.getStatusCode());
        assertEquals("_:b0 a \"test\" .", response.getBody());
    }

    @Test
    void shouldReturnNTriplesWhenAcceptHeaderIsApplicationNTriples() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/test", HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, "application/ntriples")
                .build();
        AwsProxyResponse response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.OK.getCode(), response.getStatusCode());
        assertEquals("_:b0 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> \"test\" .",
                response.getBody());
    }

    @Test
    void shouldReturnRdfXmlWhenAcceptHeaderIsApplicationRdfXml() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/test", HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, "application/rdf+xml")
                .build();
        AwsProxyResponse response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.OK.getCode(), response.getStatusCode());
        assertEquals("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" />",
                response.getBody());
    }

    @Test
    void shouldReturnJsonLdWhenAcceptHeaderIsApplicationLdJson() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/test", HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, "application/ld+json")
                .build();
        AwsProxyResponse response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.OK.getCode(), response.getStatusCode());
        assertEquals("{\"@context\": {\"@vocab\": \"https://example.org/\"}, \"name\": \"test\"}",
                response.getBody());
    }


    @Test
    void shouldReturnNotFoundWhenRequestedResourceIsNotFound() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/not-found", HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, "application/ntriples")
                .build();
        AwsProxyResponse response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.NOT_FOUND.getCode(), response.getStatusCode());
    }

    @Test
    void shouldReturnNotAcceptableWhenHttpMethodIsNotGet() throws JsonProcessingException {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/test", HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .build();
        AwsProxyResponse response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.NOT_ACCEPTABLE.getCode(), response.getStatusCode());
        assertEquals("application/problem+json", getFirstMultiValueHeader(response));
        var body = objectMapper.readValue(response.getBody(), Problem.class);
        assertEquals(NOT_ACCEPTABLE, body.getStatus());
        assertEquals(URI.create("about:blank"), body.getType());
        assertEquals("Specified Accept Types [application/json] not supported. Supported types: [application/ntriples, application/ld+json, application/turtle, application/rdf+xml]", body.getDetail());
    }

    private String getFirstMultiValueHeader(AwsProxyResponse response) {
        return response.getMultiValueHeaders().get("Content-Type").get(0);
    }
}
