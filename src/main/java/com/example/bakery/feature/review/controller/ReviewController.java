package com.example.bakery.feature.review.controller;

import com.example.bakery.feature.review.dto.ReviewDto;
import com.example.bakery.feature.review.dto.ReviewRequestDto;
import com.example.bakery.feature.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Создать отзыв (POST)
     */
    @PostMapping("/create")
    public String createReview(@ModelAttribute ReviewRequestDto dto,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Для создания отзыва необходимо войти в систему");
            return "redirect:/login";
        }

        try {
            String username = authentication.getName();
            reviewService.createReview(dto, username);
            redirectAttributes.addFlashAttribute("success", "Отзыв успешно добавлен!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }

        return "redirect:/products/" + dto.getProductId();
    }

    /**
     * Удалить отзыв
     */
    @PostMapping("/delete/{id}")
    public String deleteReview(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Необходимо войти в систему");
            return "redirect:/login";
        }

        try {
            String username = authentication.getName();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            reviewService.deleteReview(id, username, isAdmin);
            redirectAttributes.addFlashAttribute("success", "Отзыв удален");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }

        return "redirect:/reviews/my";
    }

    /**
     * Страница с моими отзывами
     */
    @GetMapping("/my")
    public String myReviews(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        var reviews = reviewService.getUserReviews(username);
        model.addAttribute("reviews", reviews);
        return "review/my-reviews";
    }
}