package no.sikt.nva;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.MediaType;

@Controller
public class OntologyController {

    @Get(uri = "/{file}", produces = MediaType.APPLICATION_JSON)
    public String getFile() {
        return "ok";
    }
}
