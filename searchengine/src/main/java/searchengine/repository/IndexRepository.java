package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    @Modifying
    @Transactional
    void deleteByPage(PageEntity page);

    @Query(value = "SELECT lemma_id from indexes where page_id = :pageId", nativeQuery = true)
    List<Integer> findByPageId( int pageId);

}
