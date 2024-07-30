package searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import searchengine.services.siteparser.PageNode;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "pages",
        indexes = @Index(name = "fn_index_path", columnList = "path"),
        uniqueConstraints = @UniqueConstraint(name = "uniquePath", columnNames = {"site_id", "path"}))
public class PageEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "site_id")
    private SiteEntity siteId;
    @Column(name = "path", columnDefinition = "VARCHAR(255)")
    private String path;
    @Column(name = "code")
    private Integer statusCode;
    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String pageContent;
    @OneToMany(mappedBy = "pageId", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<IndexEntity> indexEntityList = new ArrayList<>();
    @Transient
    private String pageTitle;

    public static PageEntity mapToPageEntity(SiteEntity siteId, Response response, Document content) throws IOException {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setSiteId(siteId);
        pageEntity.setPath(response.url().getPath());
        pageEntity.setStatusCode(response.statusCode());
        pageEntity.setPageContent(content.text());
        pageEntity.setPageTitle(content.title());

        return pageEntity;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageEntity that = (PageEntity) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
