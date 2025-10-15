package module.job.exportscheduler.exportscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "module.domain.core")
public class ExportSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExportSchedulerApplication.class, args);
    }

}
