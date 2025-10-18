package module.domain.app.controller;

import lombok.RequiredArgsConstructor;
import module.domain.app.service.MemberService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
class MemberController {
    private final MemberService memberService;

    @PostMapping
    public Map<String,Object> create(@RequestBody Map<String,String> req) {
        var m = memberService.register(req.get("email"), req.get("name"));
        return Map.of("id", m.getId().toString());
    }
}
