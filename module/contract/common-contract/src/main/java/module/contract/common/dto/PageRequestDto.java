package module.contract.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Value;

@Value
public class PageRequestDto {
    @Min(0) Integer page;
    @Min(1) @Max(1000) Integer size;

    public int pageOrDefault() { return page == null ? 0  : page; }
    public int sizeOrDefault() { return size == null ? 50 : size; }
}
