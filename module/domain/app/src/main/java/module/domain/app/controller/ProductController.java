package module.domain.app.controller;

import lombok.RequiredArgsConstructor;
import module.contract.catalog.dto.ProductRequest;
import module.contract.catalog.dto.ProductResponse;
import module.contract.common.dto.PageResponse;
import module.domain.app.usecase.ProductCommandService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductController {
    private final ProductCommandService command;

    @PreAuthorize("hasAuthority('CATALOG_READ')")
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return command.getById(id);
    }

    @PreAuthorize("hasAnyAuthority('CATALOG_READ', 'CATALOG_WRITE')")
    @GetMapping("/{id}/file/url")
    public Map<String,String> downloadUrl(@PathVariable Long id,
                                          @RequestParam(defaultValue = "600") long ttlSec) {
        return command.issueDownloadUrl(id, Duration.ofSeconds(ttlSec));
    }

    @PreAuthorize("hasAuthority('CATALOG_READ')")
    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return command.getPage(page, size);
    }

    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @PostMapping
    public ProductResponse create(@RequestBody ProductRequest req) {
        return command.create(req);
    }

    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @PostMapping("/{id}/file")
    public ProductResponse uploadProduct(@PathVariable Long id,
                                       @RequestParam("file") MultipartFile file) throws Exception {
        return command.attachProduct(
                id,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getInputStream(),
                file.getSize()
        );
    }
}
