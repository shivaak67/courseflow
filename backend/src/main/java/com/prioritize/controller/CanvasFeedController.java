package com.prioritize.controller;
import com.prioritize.service.CanvasFeedService;
import com.prioritize.security.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/integrations/canvas")
public class CanvasFeedController {
    public record ConnectRequest(@NotBlank @Size(max=2048) String feedUrl,@NotBlank @Size(max=64) String timezone) {}
    private final CanvasFeedService service;
    private final CurrentUserService current;
    public CanvasFeedController(CanvasFeedService service,CurrentUserService current) { this.service=service; this.current=current; }
    @GetMapping public CanvasFeedService.Status status() { return service.status(current.requireCurrentUserId()); }
    @PutMapping public CanvasFeedService.Status connect(@Valid @RequestBody ConnectRequest request) {
        return service.connect(current.requireCurrentUserId(),request.feedUrl(),request.timezone());
    }
    @PostMapping("/sync") public CanvasFeedService.Status sync() { return service.sync(current.requireCurrentUserId(),false); }
    @DeleteMapping @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void disconnect() { service.disconnect(current.requireCurrentUserId()); }
}
