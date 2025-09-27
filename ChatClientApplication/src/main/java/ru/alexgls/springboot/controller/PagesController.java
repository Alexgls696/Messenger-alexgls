package ru.alexgls.springboot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
public class PagesController {


    @GetMapping("login")
    public String login(@RequestParam(required = false) String path) {
        return "login";
    }

    @GetMapping("logout")
    public String logout() {
        return "logout";
    }

    @GetMapping("/setup-profile")
    public String setupProfile() {
        return "setup-profile";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @RequestMapping(value = {"/chats", "/index", "/"})
    public String index() {
        return "chats";
    }

    @GetMapping("error-page")
    public String errorPage(@RequestParam String reason, Model model) {
        String message = switch (reason) {
            case "error-401" -> "401: Вход в систему не выполнен";
            case "error-403" -> "403: У вас недостаточно прав для выполнения этой операции";
            default -> "Неизвестная ошибка";
        };
        model.addAttribute("message", message);
        return "error_page";
    }
}
