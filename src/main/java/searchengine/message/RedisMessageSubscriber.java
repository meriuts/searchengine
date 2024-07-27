package searchengine.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import searchengine.services.siteparser.PageParser;

import java.util.ArrayList;
import java.util.List;

@Service
public class RedisMessageSubscriber implements MessageListener {
    private final PageParser pageParser;

    @Autowired
    public RedisMessageSubscriber(PageParser pageParser) {
        this.pageParser = pageParser;
    }
    public void onMessage(final Message message, final byte[] pattern) {
        System.out.println("Message received: " + new String(message.getBody()));
        pageParser.initSaveLemmaAndIndex(new String(message.getBody()));
    }
}
