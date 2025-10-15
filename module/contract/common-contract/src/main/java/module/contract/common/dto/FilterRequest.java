package module.contract.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class FilterRequest {
    @Singular("filter")
    Map<String,String> filters;
    @Min(1) @Max(1000) Integer limit;
}
