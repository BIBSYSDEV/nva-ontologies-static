package no.sikt.nva;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.PathVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class OntologyController {
    public static final Logger logger = LoggerFactory.getLogger(OntologyController.class);

    @Get(uri = "/{file}", produces = MediaType.APPLICATION_JSON)
    public String getFile(@PathVariable("file") String file) {
        logger.info("Get called with value: " + file);
        return "ok";
    }
}
