package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RedisHash("page")
public class PageHashEntity {
    @Id
    private String id;
    @Indexed
    private String link;
    private Integer code;
    private String content;
    private Set<String> childUrls;

}
