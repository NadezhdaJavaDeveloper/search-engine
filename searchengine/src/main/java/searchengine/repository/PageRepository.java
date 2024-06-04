package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    @Modifying
    @Transactional
    void deleteBySite(SiteEntity site);

    @Modifying
    @Transactional
    void deleteByPathAndSite(String path, SiteEntity siteEntity);

    Optional<PageEntity> findByPathAndSite (String path, SiteEntity siteEntity);

    List<PageEntity> findBySite(SiteEntity site);


}
