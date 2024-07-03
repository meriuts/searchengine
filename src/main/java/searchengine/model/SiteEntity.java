package searchengine.model;


import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "sites")
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "site_id")
    private Integer id;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('INDEXING', 'INDEXED')")
    private SiteStatus status;
    @Column(name = "status_time")
    private LocalDateTime statusTime;
    @Column(name = "last_error", columnDefinition = "VARCHAR(255)")
    private String errorText;
    @Column(name = "url", columnDefinition = "VARCHAR(255)")
    private String url;
    @Column(name = "name", columnDefinition = "VARCHAR(255)")
    private String name;
    @OneToMany(mappedBy = "siteId", cascade = CascadeType.ALL)
    private List<PageEntity> pageEntityList = new ArrayList<>();

}
