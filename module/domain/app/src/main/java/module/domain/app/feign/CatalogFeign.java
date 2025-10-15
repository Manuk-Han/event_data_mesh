package module.domain.app.feign;

import module.contract.catalog.dto.SchemaDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="mesh-catalog", url="${catalog.base-url}")
public interface CatalogFeign {
    @GetMapping("/api/catalog/schema/{name}") SchemaDto getSchema(@PathVariable String name);
    @PostMapping("/api/catalog/schema")
    SchemaDto register(@RequestBody SchemaDto req);
}
