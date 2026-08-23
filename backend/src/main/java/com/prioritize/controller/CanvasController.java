package com.prioritize.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prioritize.dto.CanvasSyncResponse;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.CanvasSyncService;

@RestController
@RequestMapping("/api/canvas")
public class CanvasController {

    private final CanvasSyncService canvasSyncService;
    private final CurrentUserService currentUserService;

    public CanvasController(CanvasSyncService canvasSyncService, CurrentUserService currentUserService) {
        this.canvasSyncService = canvasSyncService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/sync")
    public CanvasSyncResponse sync() {
        return canvasSyncService.sync(currentUserService.requireCurrentUserId());
    }
}
