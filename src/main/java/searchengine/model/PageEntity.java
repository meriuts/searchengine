package searchengine.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import searchengine.services.siteparser.PageNode;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "pages")
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "site_id")
    private SiteEntity siteId;
    @Column(name = "path")
    private String path;
    @Column(name = "code")
    private Integer statusCode;
    @Column(name = "content")
    private String pageContent;

    private PageEntity mapToPageEntity(PageNode page) {
        PageEntity pageEntity = new PageEntity();

        return pageEntity;


    }
}
