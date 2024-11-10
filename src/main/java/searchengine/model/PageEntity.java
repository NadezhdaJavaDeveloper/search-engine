package searchengine.model;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.List;
@Entity
@Table(name = "page", indexes = {@Index(columnList = "site_id, path", unique = true)})
//@Table(name = "page")

//@Table(name = "page", uniqueConstraints = {@UniqueConstraint(columnNames = {"site, path"})})
@Getter
@Setter
@EqualsAndHashCode
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "site_id")
    private SiteEntity site;

    @Column(name = "path", columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    @Column(name = "code")
    private Integer code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;

    @OneToMany(mappedBy = "page", fetch = FetchType.EAGER)
    private List<IndexEntity> indexEntityList;
}
