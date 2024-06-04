package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    @Modifying
    @Transactional
    void deleteByPage(PageEntity page);

    List<LemmaEntity> findByPage(PageEntity page);

    //List<IndexEntity> findByPageEntityAndLemmaEntity(PageEntity pageEntity, LemmaEntity lemmaEntity);
}
