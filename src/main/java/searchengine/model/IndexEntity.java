package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "page_index",
        uniqueConstraints = @UniqueConstraint(name = "uniqueLemma", columnNames = {"lemma_id", "page_id"}))
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "page_id", referencedColumnName = "id")
    private PageEntity pageId;
    @ManyToOne
    @JoinColumn(name = "lemma_id", referencedColumnName = "id")
    private LemmaEntity lemmaId;
    @Column(name = "lemma_rank")
    private Integer rank;

    public IndexEntity(PageEntity pageId, LemmaEntity lemmaId, Integer rank) {
        this.pageId = pageId;
        this.lemmaId = lemmaId;
        this.rank = rank;
    }
}
