package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.dto.DictationQuestion;
import com.jay.englishpracticeplatform.dto.DictationSource;
import com.jay.englishpracticeplatform.dto.DictationSubmission;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.service.DictationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.util.List;
import java.util.stream.Collectors;

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
    public String startEnglish(@AuthenticationPrincipal AuthUser loginUser,
                               @RequestParam WordLevel level,
                               @RequestParam DictationSource source,
                               @RequestParam int count,
                               Model model, RedirectAttributes redirectAttributes) {
        return preview(loginUser, level, source, count, "en", "听写英语", model, redirectAttributes);
    }

    @GetMapping("/cn")
    public String startChinese(@AuthenticationPrincipal AuthUser loginUser,
                               @RequestParam WordLevel level,
                               @RequestParam DictationSource source,
                               @RequestParam int count,
                               Model model, RedirectAttributes redirectAttributes) {
        return preview(loginUser, level, source, count, "cn", "听写汉语", model, redirectAttributes);
    }

    private String preview(AuthUser loginUser, WordLevel level, DictationSource source,int count,
                           String modePath, String modeLabel,
                           Model model, RedirectAttributes redirectAttributes){
        int safeCount = DictationService.COUNT_OPTIONS.contains(count) ? count : DictationService.DEFAULT_COUNT;
        List<DictationQuestion> words = dictationService.pickWords(loginUser.getId(),level,source,safeCount);

        if(words.isEmpty()){
            redirectAttributes.addFlashAttribute("error",
                    "[" + level.getLabel() + " · " + source.getLabel() + "] 中没有可以听写的单词，换一个试试");
            return "redirect:/dictation";
        }

        model.addAttribute("words",words);
        model.addAttribute("ids",words.stream()
                .map(w -> String.valueOf(w.wordId()))
                .collect(Collectors.joining(",")));
        model.addAttribute("level",level);
        model.addAttribute("source",source);
        model.addAttribute("count",safeCount);
        model.addAttribute("modePath",modePath);
        model.addAttribute("modeLabel",modeLabel);
        return "dictation-preview";
    }

    //==================答题页==========================

    private String startTest(WordLevel level, List<Long> ids, boolean withOptions, String view,
                             Model model, RedirectAttributes redirectAttributes){
        try {
            model.addAttribute("questions",dictationService.buildTest(ids,level,withOptions));
            model.addAttribute("level",level);
            return view;
        }catch (IllegalArgumentException e){
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dictation";
        }
    }

    @GetMapping("/en/test")
    public String testEnglish(@RequestParam WordLevel level,
                              @RequestParam List<Long> ids,
                              Model model,
                              RedirectAttributes redirectAttributes){
        return startTest(level,ids,false,"dictation-en",model,redirectAttributes);
    }

    @GetMapping("/cn/test")
    public String testChinese (@RequestParam WordLevel level,
                               @RequestParam List<Long> ids,
                               Model model,
                               RedirectAttributes redirectAttributes){
        return startTest(level,ids,true ,"dictation-cn",model,redirectAttributes);

    }

    // ==================== 提交和判卷 ====================

    @PostMapping("/en")
    public String submitEnglish(@AuthenticationPrincipal AuthUser loginUser,
                                @ModelAttribute DictationSubmission submission,
                                RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("result",
                    dictationService.judgeEnglish(loginUser.getId(), submission));
            return "redirect:/dictation/result";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dictation";
        }
    }

    @PostMapping("/cn")
    public String submitChinese(@AuthenticationPrincipal AuthUser loginUser,
                                @ModelAttribute DictationSubmission submission,
                                RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("result",
                    dictationService.judgeChinese(loginUser.getId(), submission));
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