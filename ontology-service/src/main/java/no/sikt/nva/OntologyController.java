package no.sikt.nva;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.io.scan.ClassPathResourceLoader;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Controller
public class OntologyController {
    public static final Logger logger = LoggerFactory.getLogger(OntologyController.class);
    public static final String FILE = "file";
    public static final String APPLICATION_TURTLE = "application/turtle";
    public static final String APPLICATION_NTRIPLES = "application/ntriples";
    public static final String APPLICATION_RDF_XML = "application/rdf+xml";
    public static final String APPLICATION_LD_JSON = "application/ld+json";
    public static final String TTL = "ttl";
    public static final String NT = "nt";
    public static final String RDF = "rdf";
    public static final String JSONLD = "jsonld";
    public static final String PATH_PARAM_FILE = "{file}";
    public static final String EXTENSION_SEPARATOR = ".";

    private final ResourceLoader loader = new ResourceResolver().getLoader(ClassPathResourceLoader.class).orElseThrow();

    @Get(uri = PATH_PARAM_FILE, produces = APPLICATION_TURTLE)
    public String getTurtle(@PathVariable(FILE) String file) throws IOException {
        return getResourceAsString(file, TTL);
    }

    @Get(uri = PATH_PARAM_FILE, produces = APPLICATION_NTRIPLES)
    public String getNTriples(@PathVariable(FILE) String file) throws IOException {
        return getResourceAsString(file, NT);
    }

    @Get(uri = PATH_PARAM_FILE, produces = APPLICATION_RDF_XML)
    public String getRdfXml(@PathVariable(FILE) String file) throws IOException {
        return getResourceAsString(file, RDF);
    }

    @Get(uri = PATH_PARAM_FILE, produces = APPLICATION_LD_JSON)
    public String getLdJson(@PathVariable(FILE) String file) throws IOException {
        return getResourceAsString(file, JSONLD);
    }

    private String getResourceAsString(String file, String extension) throws IOException {
        Optional<InputStream> inputStream = loader.getResourceAsStream(file + EXTENSION_SEPARATOR + extension);
        if (inputStream.isPresent()) {
            return new String(inputStream.get().readAllBytes());
        }
        logger.warn(String.format("Requested file \"%s.%s\" was not found", file, extension));
        return null;
    }
}
