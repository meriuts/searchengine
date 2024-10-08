package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "lemmas",
        indexes = @Index(name = "fn_lemma_path", columnList = "site_id, lemma"),
        uniqueConstraints = @UniqueConstraint(name = "uniqueLemma", columnNames = {"lemma", "site_id"}))
public class LemmaEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "site_id")
    private SiteEntity siteId;
    @Column(name = "lemma")
    private String lemma;
    @Column(name = "frequency")
    private Integer frequency;
    @OneToMany(mappedBy = "lemmaId", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<IndexEntity> indexEntityList = new ArrayList<>();

    public LemmaEntity(Integer id) {
        this.id = id;
    }

    public static LemmaEntity getLemmaEntity(SiteEntity siteId, String lemma) {
        LemmaEntity lemmaEntity = new LemmaEntity();
        lemmaEntity.setSiteId(siteId);
        lemmaEntity.setLemma(lemma);
        lemmaEntity.setFrequency(1);

        return lemmaEntity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LemmaEntity that = (LemmaEntity) o;
        return Objects.equals(lemma, that.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lemma);
    }
}
