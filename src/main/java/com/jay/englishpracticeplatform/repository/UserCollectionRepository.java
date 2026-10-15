package com.jay.englishpracticeplatform.repository;

import com.jay.englishpracticeplatform.entity.UserCollection;
import com.jay.englishpracticeplatform.entity.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface UserCollectionRepository extends JpaRepository<UserCollection, Long> {

    //用户收藏了多少个单词
    long countByUserId(Long userId);

    //是否已经收藏了这个单词
    boolean existsByUserIdAndWordId(Long userId, Long wordId);

    //取消收藏：条件里同时带上 user_id, 只能删除自己的收藏
    @Modifying
    @Query("delete from UserCollection c where c.user.id = :userId and c.word.id = :wordId")
    int deleteByUserIdAndWordId(Long userId, Long wordId);

    //我的词库列表：JOIN FETCH 一次查出单词。避免 N+1； 计数查询单独写，不能带fetch
    @Query(
            value = "select c from UserCollection c join fetch c.word where c.user.id = :userId",
            countQuery = "select count(c) from UserCollection c where c.user.id = :userId"
    )
    Page<UserCollection> findPageByUserId(Long userId, Pageable pageable);

    // 一批单词中，哪些已经被这个用户收藏了（搜索结果里显示「已收藏」）
    @Query("select c.word.id from UserCollection c where c.user.id = :userId and c.word.id in :wordIds")
    List<Long> findCollectedWordIds(Long userId, Collection<Long> wordIds);

    //听写:从我的词库中随机选出某若干个单词
    @Query(value = """
                    SELECT word_id FROM user_collections
                    WHERE user_id = :userId
                    ORDER BY RAND()
                    LIMIT :limit
                    """, nativeQuery = true)
    List<Long> findRandomWordIds(Long userId, int limit);

    //学习：我的词库中还没有学过的单词，按收藏的先后顺序
    @Query("""
            select c.word from UserCollection c
            where c.user.id = :userId
                and not exists (
                    select 1 from UserWord uw
                    where uw.user.id = :userId and uw.word = c.word
                )
            order by c.createdAt, c.id
            """)
    List<Word> findNewWords(Long userId, Pageable pageable);

    //学习进度：我的词库中已学过的单词数量
    @Query("""
            select count(c) from UserCollection c
            where c.user.id = :userId
                and exists (
                    select 1 from UserWord uw
                    where uw.user.id = :userId and uw.word = c.word
                )
            """)
    long countLearned(Long userId);
}
