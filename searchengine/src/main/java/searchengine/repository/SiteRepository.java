package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.config.Site;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {


    Optional<SiteEntity> findByName(String name);

    Optional<SiteEntity> findByUrl(String url);



//    @Modifying
//    @Query("UPDATE 'site' SET 'status_time' = :value where 'id' = :id")
//    int updateStatusTime(@Param("value") LocalDateTime statusTime, @Param("id") int id);




}

