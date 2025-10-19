package module.domain.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import module.domain.app.exception.DuplicateEmailException;
import module.domain.db.entity.Member;
import module.domain.db.entity.Outbox;
import module.domain.db.repository.MemberRepository;
import module.domain.db.repository.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepo;
    private final OutboxRepository outboxRepo;
    // Spring Boot가 기본 제공하는 ObjectMapper 빈을 주입받습니다.
    private final ObjectMapper om;

    @Transactional
    public Member register(String email, String name) {
        // 1) 비즈니스 검증: 이메일 중복
        if (memberRepo.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        // 2) 멤버 저장
        Member m = memberRepo.save(
                Member.builder()
                        .id(UUID.randomUUID())
                        .email(email)
                        .name(name)
                        .status("ACTIVE")
                        .createdAt(OffsetDateTime.now())
                        .build()
        );

        // 3) 이벤트 페이로드(JSON) 생성
        String payload;
        try {
            payload = om.writeValueAsString(Map.of(
                    "type", "MemberRegistered",
                    "memberId", m.getId().toString(),
                    "email", m.getEmail(),
                    "name", m.getName()
                    // 스키마 v1은 위 3개 필수. 필요하면 필드 추가 가능.
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("payload serialize error", e);
        }

        // 4) 헤더(JSON) 생성 (스키마/추적 메타)
        String headers;
        try {
            headers = om.writeValueAsString(Map.of(
                    "dataset", "member-events",
                    "schemaVersion", "v1",
                    "correlationId", UUID.randomUUID().toString()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("headers serialize error", e);
        }

        // 5) Outbox 적재 (트랜잭션 내)
        outboxRepo.save(
                Outbox.builder()
                        .aggregateType("Member")
                        .aggregateId(m.getId().toString())
                        .eventType("MemberRegistered")
                        .payload(payload)
                        .headers(headers)
                        .published(false)
                        .createdAt(OffsetDateTime.now())
                        .build()
        );

        return m;
    }
}
