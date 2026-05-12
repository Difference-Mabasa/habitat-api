package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.user.SwitchActiveRoleRequest;
import com.habitat.api.dto.user.UserMeResponse;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.USERS)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityUtils security;

    @GetMapping("/me")
    public UserMeResponse me() {
        return userService.getMe(security.requireUserId());
    }

    @PatchMapping("/me/active-role")
    public UserMeResponse switchActiveRole(@Valid @RequestBody SwitchActiveRoleRequest req) {
        return userService.switchActiveRole(security.requireUserId(), req.role());
    }
}
