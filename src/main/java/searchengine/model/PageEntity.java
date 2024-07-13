package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import searchengine.services.siteparser.PageNode;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Data
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
    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "site_id")
    private SiteEntity siteId;
    @Column(name = "path", columnDefinition = "VARCHAR(255)")
    private String path;
    @Column(name = "code")
    private Integer statusCode;
    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String pageContent;

    private PageEntity mapToPageEntity(PageNode page) {
        PageEntity pageEntity = new PageEntity();

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
