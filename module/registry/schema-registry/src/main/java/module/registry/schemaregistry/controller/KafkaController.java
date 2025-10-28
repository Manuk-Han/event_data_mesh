package module.registry.schemaregistry.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ops/consumer")
@RequiredArgsConstructor
public class KafkaController {

    private final KafkaListenerEndpointRegistry registry;

    @PostMapping("/pause")
    public String pause() {
        registry.getListenerContainers().forEach(c -> c.pause());
        return "paused";
    }

    @PostMapping("/resume")
    public String resume() {
        registry.getListenerContainers().forEach(c -> c.resume());
        return "resumed";
    }
}