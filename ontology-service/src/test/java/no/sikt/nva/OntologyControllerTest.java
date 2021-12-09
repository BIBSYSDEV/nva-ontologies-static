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

import static no.sikt.nva.OntologyController.APPLICATION_LD_JSON;
import static no.sikt.nva.OntologyController.APPLICATION_NTRIPLES;
import static no.sikt.nva.OntologyController.APPLICATION_RDF_XML;
import static no.sikt.nva.OntologyController.APPLICATION_TURTLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.zalando.problem.Status.NOT_ACCEPTABLE;

public class OntologyControllerTest {

    public static final String APPLICATION_PROBLEM_JSON = "application/problem+json";
    public static final String PROBLEM_JSON_DEFAULT_TYPE = "about:blank";
    public static final String NOT_ACCEPTABLE_ERROR_MESSAGE = "Specified Accept Types [application/json] not "
            + "supported. Supported types: [application/ntriples, application/ld+json, application/turtle, "
            + "application/rdf+xml]";
    public static final String TEST_PATH = "/test";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String NOT_FOUND_PATH = "/not-found";
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
        AwsProxyRequest request = new AwsProxyRequestBuilder(TEST_PATH, HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, APPLICATION_TURTLE)
                .build();
        AwsProxyResponse response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.OK.getCode(), response.getStatusCode());
        assertEquals("_:b0 a \"test\" .", response.getBody());
    }

    @Test
    void shouldReturnNTriplesWhenAcceptHeaderIsApplicationNTriples() {
        AwsProxyRequest request = new AwsProxyRequestBuilder(TEST_PATH, HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, APPLICATION_NTRIPLES)
                .build();
        AwsProxyResponse response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.OK.getCode(), response.getStatusCode());
        assertEquals("_:b0 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> \"test\" .",
                response.getBody());
    }

    @Test
    void shouldReturnRdfXmlWhenAcceptHeaderIsApplicationRdfXml() {
        AwsProxyRequest request = new AwsProxyRequestBuilder(TEST_PATH, HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, APPLICATION_RDF_XML)
                .build();
        AwsProxyResponse response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.OK.getCode(), response.getStatusCode());
        assertEquals("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" />",
                response.getBody());
    }

    @Test
    void shouldReturnJsonLdWhenAcceptHeaderIsApplicationLdJson() {
        AwsProxyRequest request = new AwsProxyRequestBuilder(TEST_PATH, HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, APPLICATION_LD_JSON)
                .build();
        AwsProxyResponse response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.OK.getCode(), response.getStatusCode());
        assertEquals("{\"@context\": {\"@vocab\": \"https://example.org/\"}, \"name\": \"test\"}",
                response.getBody());
    }


    @Test
    void shouldReturnNotFoundWhenRequestedResourceIsNotFound() {
        AwsProxyRequest request = new AwsProxyRequestBuilder(NOT_FOUND_PATH, HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, APPLICATION_NTRIPLES)
                .build();
        AwsProxyResponse response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.NOT_FOUND.getCode(), response.getStatusCode());
    }

    @Test
    void shouldReturnNotAcceptableWhenHttpMethodIsNotGet() throws JsonProcessingException {
        AwsProxyRequest request = new AwsProxyRequestBuilder(TEST_PATH, HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .build();
        AwsProxyResponse response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.NOT_ACCEPTABLE.getCode(), response.getStatusCode());
        assertEquals(APPLICATION_PROBLEM_JSON, getFirstMultiValueHeader(response));
        var body = objectMapper.readValue(response.getBody(), Problem.class);
        assertEquals(NOT_ACCEPTABLE, body.getStatus());
        assertEquals(URI.create(PROBLEM_JSON_DEFAULT_TYPE), body.getType());
        assertEquals(NOT_ACCEPTABLE_ERROR_MESSAGE, body.getDetail());
    }

    private String getFirstMultiValueHeader(AwsProxyResponse response) {
        return response.getMultiValueHeaders().get(CONTENT_TYPE).get(0);
    }
}
