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
    private final ObjectMapper om = new ObjectMapper();

    @Transactional
    public Member register(String email, String name) {
        if (memberRepo.existsByEmail(email)) {
            if (memberRepo.existsByEmail(email)) throw new DuplicateEmailException(email);
        }

        Member m = memberRepo.save(
                Member.builder()
                        .id(UUID.randomUUID())
                        .email(email)
                        .name(name)
                        .status("ACTIVE")
                        .createdAt(OffsetDateTime.now())
                        .build()
        );

        String payload;
        try {
            payload = om.writeValueAsString(Map.of(
                    "type", "MemberRegistered",
                    "id", m.getId().toString(),
                    "email", m.getEmail(),
                    "name", m.getName(),
                    "status", m.getStatus()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("payload serialize error", e);
        }

        String headers;
        try {
            headers = om.writeValueAsString(Map.of(
                    "schemaName", "member-event",
                    "schemaVersion", 1,
                    "correlationId", UUID.randomUUID().toString()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("headers serialize error", e);
        }

        outboxRepo.save(
                Outbox.builder()
                        .aggregateType("member")
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
