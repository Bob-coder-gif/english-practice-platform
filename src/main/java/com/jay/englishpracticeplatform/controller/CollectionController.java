package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.dto.DictEntry;
import com.jay.englishpracticeplatform.entity.UserCollection;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.exception.WordCollectionException;
import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.service.CollectionService;
import com.jay.englishpracticeplatform.service.DictionaryService;
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
    private final DictionaryService dictionaryService;

    public CollectionController(CollectionService collectionService, DictionaryService dictionaryService) {
        this.collectionService = collectionService;
        this.dictionaryService = dictionaryService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal AuthUser loginUser,
                       @RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "1") int page,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        Long userId = loginUser.getId();
        String keyword = WordService.normalizeKeyword(q);

        //网址的页码从1开始，Spring Data 的页码从0开始
        Page<UserCollection> collectionPage = collectionService.listMyWords(userId, page - 1);
        int totalPages = Math.max(collectionPage.getTotalPages(), 1);
        int validPage = Math.min(Math.max(page, 1), totalPages);

        if (validPage != page) {
            redirectAttributes.addAttribute("page", validPage);
            if (!keyword.isEmpty()) {
                redirectAttributes.addAttribute("q", keyword);
            }
            return "redirect:/collection";
        }

        // 输入了关键词：先在 words 表中搜索（标出哪些已经收藏），再在扩展词典中搜索
        if (!keyword.isEmpty()) {
            List<Word> results = collectionService.search(keyword);
            model.addAttribute("searchResults", results);
            model.addAttribute("collectedIds", collectionService.collectedIds(userId, results));

            List<DictEntry> dictResults = dictionaryService.search(keyword);
            model.addAttribute("dictResults", dictResults);
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
        return backToSearch(q, redirectAttributes);
    }

    // 收藏扩展词典中的词条：先把它变成 words 表中的一个单词，再按普通单词收藏
    // 分成两步、两个事务：第一步成功、第二步失败（比如词库满了）也没关系，
    // 只是 words 表里多了一个扩展词，下次谁收藏它都能直接用
    @PostMapping("/add-entry")
    public String addEntry(@AuthenticationPrincipal AuthUser loginUser,
                           @RequestParam Long entryId,
                           @RequestParam(defaultValue = "") String q,
                           RedirectAttributes redirectAttributes) {
        try {
            Word word = dictionaryService.findOrCreateWord(entryId);
            collectionService.add(loginUser.getId(), word.getId());
            redirectAttributes.addFlashAttribute("message", "已收藏「" + word.getSpelling() + "」");
        } catch (WordCollectionException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return backToSearch(q, redirectAttributes);
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

    // 收藏后回到刚才的搜索结果，方便继续收藏
    private String backToSearch(String q, RedirectAttributes redirectAttributes) {
        String keyword = WordService.normalizeKeyword(q);
        if (!keyword.isEmpty()) {
            redirectAttributes.addAttribute("q", keyword);
        }
        return "redirect:/collection";
    }
}