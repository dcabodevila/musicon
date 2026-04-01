package es.musicalia.gestmusica.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestErrorThrowingController {

    @GetMapping("/eventos/test-errors/runtime-boom")
    public String runtimeBoom() {
        throw new IllegalStateException("forced runtime exception");
    }
}
