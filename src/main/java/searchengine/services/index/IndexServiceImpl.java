package searchengine.services.index;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.repositories.SiteRepository;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {
    private final SiteRepository siteRepository;
}
