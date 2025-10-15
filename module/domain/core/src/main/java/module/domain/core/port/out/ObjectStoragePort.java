package module.domain.core.port.out;

import java.io.InputStream;
import java.time.Duration;

public interface ObjectStoragePort {
    String presignGet(String key, Duration ttl);
    String put(String key, InputStream in, long contentLength, String contentType);

    void delete(String key);
}
