package com.mucommander.commons.file.impl.s3;

import com.mucommander.commons.file.*;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * A file protocol provider for the Amazon S3 protocol.
 *
 * @author Maxence Bernard
 */
public class S3ProtocolProvider implements ProtocolProvider {
    public S3ProtocolProvider() {
    }

    public AbstractFile getFile(FileURL url, Object... instantiationParams) throws IOException {
        Credentials credentials = url.getCredentials();
        if (credentials == null || credentials.getLogin().isEmpty() || credentials.getPassword().isEmpty()) {
            throw new AuthException(url);
        }

        S3Service service;
        String bucketName;

        if (instantiationParams.length == 0) {
            service = new RestS3Service(new AWSCredentials(credentials.getLogin(), credentials.getPassword()));
            Jets3tProperties props = new Jets3tProperties();
            props.setProperty("s3service.s3-endpoint", url.getHost());
        } else {
            service = (S3Service)instantiationParams[0];
        }

        String path = url.getPath();

        // Root resource
        if (("/").equals(path)) {
            return new S3Root(url, service);
        }

        // Fetch the bucket name from the URL
        StringTokenizer st = new StringTokenizer(path, "/");
        bucketName = st.nextToken();

        // Object resource
        if (st.hasMoreTokens()) {
            if (instantiationParams.length == 2) {
                return new S3Object(url, service, bucketName, (org.jets3t.service.model.S3Object)instantiationParams[1]);
            }

            return new S3Object(url, service, bucketName);
        }

        // Bucket resource
        if (instantiationParams.length == 2) {
            return new S3Bucket(url, service, (org.jets3t.service.model.S3Bucket)instantiationParams[1]);
        }

        return new S3Bucket(url, service, bucketName);
    }
}
