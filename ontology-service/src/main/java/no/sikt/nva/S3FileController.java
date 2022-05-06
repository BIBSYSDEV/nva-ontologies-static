package no.sikt.nva;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Controller("/file")
public class S3FileController {

    public static final Logger logger = LoggerFactory.getLogger(OntologyController.class);
    public static final String OBJECT_KEY = "objectKey";
    public static final String PATH_PARAM_OBJECTKEY = "{objectKey}";
    private static final String APPLICATION_OCTETSTREAM = "application/octet-stream";
    public static final String BUCKET_NAME = "customers-backup-sandbox";


    @Get(uri = PATH_PARAM_OBJECTKEY, produces = APPLICATION_OCTETSTREAM)
    public String getFile(@PathVariable(OBJECT_KEY) String objectKey) throws IOException {
        return getS3ResourceAsString(objectKey);
    }

    private String getS3ResourceAsString(String objectKey) throws IOException {
        logger.info("Reading from {}/{}", BUCKET_NAME, objectKey);
        AmazonS3 s3client = AmazonS3ClientBuilder
                .standard()
                .withRegion(Regions.EU_WEST_1)
                .build();

        S3Object s3object = s3client.getObject(BUCKET_NAME, objectKey);
        S3ObjectInputStream inputStream = s3object.getObjectContent();
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
    }

}
