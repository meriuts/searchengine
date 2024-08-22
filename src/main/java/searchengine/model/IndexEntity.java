package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "page_index",
        uniqueConstraints = @UniqueConstraint(name = "uniquePage_index", columnNames = {"lemma_id", "page_id"}))
public class IndexEntity implements Serializable {
    @Id
    @SequenceGenerator(name = "sequence_id_auto_gen_index", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", referencedColumnName = "id")
    private PageEntity pageId;
    @ManyToOne(fetch = FetchType.LAZY)
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
