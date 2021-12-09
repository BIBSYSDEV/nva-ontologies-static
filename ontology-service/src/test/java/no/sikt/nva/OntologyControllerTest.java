package no.sikt.nva;
import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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
    public static final String TEST_DATA_NOT_FOUND_ERROR = "Test data not found: ";
    public static final String EXPECTED_TURTLE = readFileFromResources("test.ttl");
    public static final String EXPECTED_NTRIPLES = readFileFromResources("test.nt");
    public static final String EXPECTED_RDF_XML = readFileFromResources("test.rdf");
    public static final String EXPECTED_LD_JSON = readFileFromResources("test.jsonld");
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
        var request = new AwsProxyRequestBuilder(TEST_PATH, HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, APPLICATION_TURTLE)
                .build();
        var response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.OK.getCode(), response.getStatusCode());
        assertEquals(EXPECTED_TURTLE, response.getBody());
    }

    @Test
    void shouldReturnNTriplesWhenAcceptHeaderIsApplicationNTriples() {
        var request = new AwsProxyRequestBuilder(TEST_PATH, HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, APPLICATION_NTRIPLES)
                .build();
        var response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.OK.getCode(), response.getStatusCode());
        assertEquals(EXPECTED_NTRIPLES, response.getBody());
    }

    @Test
    void shouldReturnRdfXmlWhenAcceptHeaderIsApplicationRdfXml() {
        var request = new AwsProxyRequestBuilder(TEST_PATH, HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, APPLICATION_RDF_XML)
                .build();
        var response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.OK.getCode(), response.getStatusCode());
        assertEquals(EXPECTED_RDF_XML, response.getBody());
    }

    @Test
    void shouldReturnJsonLdWhenAcceptHeaderIsApplicationLdJson() {
        var request = new AwsProxyRequestBuilder(TEST_PATH, HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, APPLICATION_LD_JSON)
                .build();
        var response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.OK.getCode(), response.getStatusCode());
        assertEquals(EXPECTED_LD_JSON, response.getBody());
    }


    @Test
    void shouldReturnNotFoundWhenRequestedResourceIsNotFound() {
        var request = new AwsProxyRequestBuilder(NOT_FOUND_PATH, HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, APPLICATION_NTRIPLES)
                .build();
        var response = handler.handleRequest(request, lambdaContext);
        assertEquals(HttpStatus.NOT_FOUND.getCode(), response.getStatusCode());
    }

    @Test
    void shouldReturnNotAcceptableWhenHttpMethodIsNotGet() throws JsonProcessingException {
        var request = new AwsProxyRequestBuilder(TEST_PATH, HttpMethod.GET.toString())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .build();
        var response = handler.handleRequest(request, lambdaContext);
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

    private static String readFileFromResources(String file) {
        try {
            var url = Optional.ofNullable(OntologyControllerTest.class.getClassLoader().getResource(file))
                    .orElseThrow();
            return Files.readString(Path.of(url.getFile()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(TEST_DATA_NOT_FOUND_ERROR + file);
        }
    }
}
