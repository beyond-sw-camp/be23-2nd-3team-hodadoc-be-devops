package com.beyond.hodadoc.review.repository;

import com.beyond.hodadoc.review.domain.BadWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BadWordRepository extends JpaRepository<BadWord, Long> {

    @Query("SELECT b.word FROM BadWord b")
    List<String> findAllWords();
}
