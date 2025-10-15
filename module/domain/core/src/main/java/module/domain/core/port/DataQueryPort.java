package module.domain.core.port;

import module.domain.core.model.EntityView;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface DataQueryPort {
    /** 특정 시점 이후 갱신된 엔티티 조회 (신선도/증분 용도) */
    List<EntityView> findUpdatedSince(Instant since, int limit);

    /** 단일 키-값 기반 필터 (ex. "category"="A"). 구현체는 인덱스/조건 최적화 가능 */
    List<EntityView> findByEquals(String key, String value, int limit);

    /** 단순 다중 필터(AND). 필요 최소로만 유지 */
    List<EntityView> findByAll(Map<String, String> filters, int limit);
}