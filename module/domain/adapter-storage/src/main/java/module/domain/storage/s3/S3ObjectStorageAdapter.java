package module.domain.storage.s3;

import lombok.RequiredArgsConstructor;
import module.domain.core.port.out.ObjectStoragePort;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class S3ObjectStorageAdapter implements ObjectStoragePort {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final S3Props props;

    @Override
    public String presignGet(String key, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build();

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(get)
                .build();

        return presigner.presignGetObject(presignReq).url().toString();
    }

    @Override
    public String put(String key, InputStream in, long contentLength, String contentType) {
        s3.putObject(b -> b.bucket(props.getBucket())
                        .key(key)
                        .contentType(contentType),
                RequestBody.fromInputStream(in, contentLength));

        String base = props.getPublicBaseUrl();

        return base.endsWith("/") ? base + key : base + "/" + key;
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(b -> b.bucket(props.getBucket()).key(key));
    }
}
