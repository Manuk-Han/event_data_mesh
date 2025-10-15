package module.domain.storage.s3;

import module.domain.core.port.AssetStoragePort; // ← 실제 경로로 수정
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

@Component
public class S3AssetStorageAdapter implements AssetStoragePort {
    private final S3Client s3;
    private final S3Presigner presigner;
    private final S3Props props;

    public S3AssetStorageAdapter(S3Client s3, S3Presigner presigner, S3Props props) {
        this.s3 = s3;
        this.presigner = presigner;
        this.props = props;
    }

    @Override
    public String put(String key, InputStream content, long contentLength, String contentType) {
        ensureBucket();
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(props.getBucket())
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromInputStream(content, contentLength)
        );
        return publicUrl(key);
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build());
    }

    @Override
    public String presignGet(String key, Duration ttl) {
        var get = GetObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build();
        var req = GetObjectPresignRequest.builder()
                .getObjectRequest(get)
                .signatureDuration(ttl)
                .build();
        return presigner.presignGetObject(req).url().toString();
    }

    private void ensureBucket() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(props.getBucket()).build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                s3.createBucket(CreateBucketRequest.builder()
                        .bucket(props.getBucket()).build());
            } else {
                throw e;
            }
        }
    }

    private String publicUrl(String key) {
        var base = props.getPublicBaseUrl();
        if (base != null && !base.isBlank()) {
            if (base.endsWith("/")) base = base.substring(0, base.length()-1);
            return base + "/" + key;
        }
        return props.getEndpoint() + "/" + props.getBucket() + "/" + key;
    }
}
