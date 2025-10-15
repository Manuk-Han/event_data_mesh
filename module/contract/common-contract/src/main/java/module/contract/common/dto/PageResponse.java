package module.contract.common.dto;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PageResponse<T> {
    int page;
    int size;
    long total;
    @Singular("item") List<T> items;
}


