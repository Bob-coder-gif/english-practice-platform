package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.entity.UserCollection;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.exception.WordCollectionException;
import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.service.CollectionService;
import com.jay.englishpracticeplatform.service.WordService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/collection")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService){
        this.collectionService = collectionService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal AuthUser loginUser,
                       @RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "1") int page,
                       Model model,
                       RedirectAttributes redirectAttributes){
        Long userId = loginUser.getId();
        String keyword = WordService.normalizeKeyword(q);

        //网址的页码从1开始，Spring Data 的页码从0开始
        Page<UserCollection> collectionPage = collectionService.listMyWords(userId,page - 1);
        int totalPages = Math.max(collectionPage.getTotalPages(),1);
        int validPage = Math.min(Math.max(page, 1) , totalPages);

        if (validPage != page) {
            redirectAttributes.addAttribute("page", validPage);
            if (!keyword.isEmpty()) {
                redirectAttributes.addAttribute("q", keyword);
            }
            return "redirect:/collection";
        }

        // 输入了关键词：在全部词库中搜索，并标出哪些已经收藏
        if (!keyword.isEmpty()) {
            List<Word> results = collectionService.search(keyword);
            model.addAttribute("searchResults", results);
            model.addAttribute("collectedIds", collectionService.collectedIds(userId, results));
        }

        model.addAttribute("collectionPage", collectionPage);
        model.addAttribute("count", collectionPage.getTotalElements());
        model.addAttribute("maxSize", CollectionService.MAX_SIZE);
        model.addAttribute("q", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        return "collection";
    }

    @PostMapping("/add")
    public String add(@AuthenticationPrincipal AuthUser loginUser,
                      @RequestParam Long wordId,
                      @RequestParam(defaultValue = "") String q,
                      RedirectAttributes redirectAttributes) {
        try {
            Word word = collectionService.add(loginUser.getId(), wordId);
            redirectAttributes.addFlashAttribute("message", "已收藏「" + word.getSpelling() + "」");
        } catch (WordCollectionException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        // 回到刚才的搜索结果，方便继续收藏
        String keyword = WordService.normalizeKeyword(q);
        if (!keyword.isEmpty()) {
            redirectAttributes.addAttribute("q", keyword);
        }
        return "redirect:/collection";
    }

    @PostMapping("/remove")
    public String remove(@AuthenticationPrincipal AuthUser loginUser,
                         @RequestParam Long wordId,
                         @RequestParam(defaultValue = "1") int page,
                         RedirectAttributes redirectAttributes) {
        if (collectionService.remove(loginUser.getId(), wordId)) {
            redirectAttributes.addFlashAttribute("message", "已从我的词库中移除");
        }
        redirectAttributes.addAttribute("page", page);
        return "redirect:/collection";
    }
}
