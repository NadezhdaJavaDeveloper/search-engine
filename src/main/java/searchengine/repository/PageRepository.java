package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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


    Optional<PageEntity> findByPathAndSiteId(String path, int siteId);

    @Query(value = "SELECT id from page where site_id = :siteId", nativeQuery = true)
    List<Integer> findBySiteId(int siteId);

    @Query(value = "SELECT path from page where id = :id", nativeQuery = true)
    String findPathById(int id);


    @Query(value = "SELECT id from page where path = :path", nativeQuery = true)
    int findByPath(String path);


}
