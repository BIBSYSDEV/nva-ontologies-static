package no.sikt.nva;

import io.micronaut.runtime.Micronaut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
