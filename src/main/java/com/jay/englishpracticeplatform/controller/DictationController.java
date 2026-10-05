package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.common.SessionKeys;
import com.jay.englishpracticeplatform.dto.DictationQuestion;
import com.jay.englishpracticeplatform.dto.DictationSource;
import com.jay.englishpracticeplatform.dto.DictationSubmission;
import com.jay.englishpracticeplatform.dto.LoginUser;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.service.DictationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/dictation")
public class DictationController {

    private final DictationService dictationService;

    public DictationController(DictationService dictationService) {
        this.dictationService = dictationService;
    }

    // ==================== 设置页 ====================

    @GetMapping
    public String setup(Model model) {
        model.addAttribute("levels", WordLevel.values());
        model.addAttribute("sources", DictationSource.values());
        model.addAttribute("counts", DictationService.COUNT_OPTIONS);
        return "dictation";
    }

    // ==================== 出题 ====================

    @GetMapping("/en")
    public String startEnglish(@SessionAttribute(SessionKeys.LOGIN_USER) LoginUser loginUser,
                               @RequestParam WordLevel level,
                               @RequestParam DictationSource source,
                               @RequestParam int count,
                               Model model, RedirectAttributes redirectAttributes) {
        return start(loginUser, level, source, count, false, "dictation-en", model, redirectAttributes);
    }

    @GetMapping("/cn")
    public String startChinese(@SessionAttribute(SessionKeys.LOGIN_USER) LoginUser loginUser,
                               @RequestParam WordLevel level,
                               @RequestParam DictationSource source,
                               @RequestParam int count,
                               Model model, RedirectAttributes redirectAttributes) {
        return start(loginUser, level, source, count, true, "dictation-cn", model, redirectAttributes);
    }

    private String start(LoginUser loginUser, WordLevel level, DictationSource source, int count,
                         boolean withOptions, String view,
                         Model model, RedirectAttributes redirectAttributes) {
        int safeCount = DictationService.COUNT_OPTIONS.contains(count) ? count : DictationService.DEFAULT_COUNT;

        List<DictationQuestion> questions =
                dictationService.generate(loginUser.id(), level, source, safeCount, withOptions);

        if (questions.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "「" + level.getLabel() + " · " + source.getLabel() + "」中没有可以听写的单词，换一个试试");
            return "redirect:/dictation";
        }

        model.addAttribute("questions", questions);
        model.addAttribute("level", level);
        model.addAttribute("source", source);
        return view;
    }

    // ==================== 提交和判卷 ====================

    @PostMapping("/en")
    public String submitEnglish(@SessionAttribute(SessionKeys.LOGIN_USER) LoginUser loginUser,
                                @ModelAttribute DictationSubmission submission,
                                RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("result",
                    dictationService.judgeEnglish(loginUser.id(), submission));
            return "redirect:/dictation/result";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dictation";
        }
    }

    @PostMapping("/cn")
    public String submitChinese(@SessionAttribute(SessionKeys.LOGIN_USER) LoginUser loginUser,
                                @ModelAttribute DictationSubmission submission,
                                RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("result",
                    dictationService.judgeChinese(loginUser.id(), submission));
            return "redirect:/dictation/result";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dictation";
        }
    }

    // ==================== 结果页 ====================

    @GetMapping("/result")
    public String result(Model model) {
        // 结果是一次性的 Flash 数据：刷新页面后就没有了，回到设置页
        if (!model.containsAttribute("result")) {
            return "redirect:/dictation";
        }
        return "dictation-result";
    }
}