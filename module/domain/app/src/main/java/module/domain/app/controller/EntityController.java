package module.domain.app.controller;

import module.contract.common.dto.FilterRequest;
import module.contract.common.dto.PageRequestDto;
import module.contract.common.dto.PageResponse;
import module.domain.core.port.DataCommandPort;
import module.domain.core.port.DataQueryPort;
import module.domain.core.port.SearchEntitiesUseCase;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import module.domain.app.dto.*;
import module.domain.core.model.EntityView;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/entity")
@RequiredArgsConstructor
public class EntityController {

    private final SearchEntitiesUseCase useCase;
    private final DataQueryPort query;
    private final DataCommandPort command;

    @GetMapping("/health")
    public String health() { return "ok"; }

    @GetMapping("/search")
    public PageResponse<EntityView> search(
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String value,
            @Valid PageRequestDto page) {

        int size = page.sizeOrDefault();
        int pageNo = page.pageOrDefault();

        List<EntityView> all = (key != null && !key.isBlank() && value != null && !value.isBlank())
                ? useCase.searchByEquals(key, value, size * (pageNo+1))
                : useCase.searchUpdatedSince(Instant.EPOCH, size * (pageNo+1));

        int from = Math.min(pageNo * size, all.size());
        int to = Math.min(from + size, all.size());
        List<EntityView> slice = from < to ? all.subList(from, to) : Collections.emptyList();

        return PageResponse.<EntityView>builder()
                .page(pageNo)
                .size(size)
                .total(all.size())
                .items(slice)
                .build();
    }

    @PostMapping("/search")
    public List<EntityView> searchByAll(@Valid @RequestBody FilterRequest req) {
        Map<String,String> filters = req.getFilters() == null ? Map.of() : req.getFilters();
        int limit = req.getLimit() == null ? 50 : req.getLimit();
        return useCase.searchByAll(filters, limit);
    }

    @GetMapping("/since")
    public List<EntityView> since(@RequestParam(defaultValue = "1970-01-01T00:00:00Z") String time,
                                  @RequestParam(defaultValue = "10") int limit) {
        return query.findUpdatedSince(Instant.parse(time), limit);
    }

    @GetMapping
    public List<EntityView> search(@RequestParam Map<String,String> filters,
                                   @RequestParam(defaultValue = "10") int limit) {
        return query.findByAll(filters, limit);
    }

    @PostMapping
    public EntityView create(@RequestBody EntityCreateRequest req) {
        return command.create(req.name(), req.label(), req.attrs());
    }
}
