package com.lul.controller;

import com.lul.dto.LoginResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public LoginResponse login() {
        return LoginResponse.builder()
            .status("success")
            .build();
    }
}