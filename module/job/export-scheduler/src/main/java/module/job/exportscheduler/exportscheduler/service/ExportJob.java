package module.job.exportscheduler.exportscheduler.service;

import com.opencsv.CSVWriter;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExportJob {

    @PersistenceContext
    private EntityManager em;

    private final MinioClient minio = MinioClient.builder()
            .endpoint(System.getProperty("minio.endpoint", "http://localhost:9000"))
            .credentials(
                    System.getProperty("minio.access", "minio"),
                    System.getProperty("minio.secret", "minio123")
            )
            .build();

    private final String bucket = "exports";

    @Scheduled(cron = "0 0/10 * * * *") // 10분마다
    @Transactional(readOnly = true)
    public void run() throws Exception {
        // 1) 데이터 조회 (예: Product 엔티티 기준)
        List<Object[]> rows = em.createQuery("""
            select p.id, p.name, p.price from Product p
        """, Object[].class).getResultList();

        // 2) CSV 생성
        Path tmp = Files.createTempFile("product-export-", ".csv");
        try (var writer = Files.newBufferedWriter(tmp);
             var csv = new CSVWriter(writer)) {
            csv.writeNext(new String[]{"id", "name", "price"});
            for (Object[] r : rows) {
                csv.writeNext(new String[]{
                        String.valueOf(r[0]),
                        String.valueOf(r[1]),
                        String.valueOf(r[2])
                });
            }
        }

        // 3) 버킷 보장
        boolean exists = minio.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!exists) {
            minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }

        // 4) 업로드
        String object = "product/" + LocalDate.now() + "/export-" + System.currentTimeMillis() + ".csv";
        minio.putObject(PutObjectArgs.builder()
                .bucket(bucket).object(object)
                .contentType("text/csv")
                .stream(Files.newInputStream(tmp), Files.size(tmp), -1)
                .build());

        Files.deleteIfExists(tmp);
    }
}
