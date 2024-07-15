package searchengine.model;


import lombok.*;
import searchengine.config.Site;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sites")
public class SiteEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "site_id")
    private Integer id;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private SiteStatus status;
    @Column(name = "status_time")
    private LocalDateTime statusTime;
    @Column(name = "last_error", columnDefinition = "VARCHAR(255)")
    private String errorText;
    @Column(name = "url", columnDefinition = "VARCHAR(255)")
    private String url;
    @Column(name = "name", columnDefinition = "VARCHAR(255)")
    private String name;
    @OneToMany(mappedBy = "siteId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<PageEntity> pageEntityList = new ArrayList<>();
    @OneToMany(mappedBy = "siteId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LemmaEntity> lemmaEntityList = new ArrayList<>();

    public static SiteEntity mapToSiteEntity(Site site, SiteStatus status) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setStatus(status);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setUrl(site.getUrl());
        siteEntity.setName(site.getName());

        return siteEntity;
    }

}
