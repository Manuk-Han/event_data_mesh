package module.domain.core.port;

import java.io.InputStream;
import java.time.Duration;

public interface AssetStoragePort {
    String put(String key, InputStream content, long contentLength, String contentType);
    void delete(String key);
    String presignGet(String key, Duration ttl);
}
