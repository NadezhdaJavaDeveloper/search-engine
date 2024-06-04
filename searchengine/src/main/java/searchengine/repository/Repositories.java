package searchengine.repository;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Repositories {

    private IndexRepository indexRepository;
    private LemmaRepository lemmaRepository;
    private PageRepository pageRepository;
    private SiteRepository siteRepository;

    public Repositories(IndexRepository indexRepository, LemmaRepository lemmaRepository, PageRepository pageRepository, SiteRepository siteRepository) {
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }
}
