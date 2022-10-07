package com.example.milf2.service;

import com.example.milf2.domain.Dto.PostNUser;
import com.example.milf2.domain.Subscriptions.EventTypes;
import com.example.milf2.domain.Post.Post;
import com.example.milf2.domain.Subscriptions.SubscriptionEvent;
import com.example.milf2.domain.user.User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class SubscriptionService {

    private final Sinks.Many<PostNUser> postCreatedSink;
    private final ConcurrentHashMap<String, Sinks.Many<SubscriptionEvent>> sinksHashMap;

    public SubscriptionService(Sinks.Many<PostNUser> postCreatedSink, ConcurrentHashMap<String,
            Sinks.Many<SubscriptionEvent>> sinksHashMap) {
        this.postCreatedSink = postCreatedSink;
        this.sinksHashMap = sinksHashMap;
    }

    public void postCreated(User user, Post post) {
        postCreatedSink.tryEmitNext(new PostNUser(user, post));
    }

    public void commentAdded(User user, Post post, String id) {
        sinksHashMap.get(id)
                .tryEmitNext(new SubscriptionEvent(EventTypes.COMMENT_ADDED, new PostNUser(user, post)));
    }

    public void postLiked(User user, Post post) {
        sinksHashMap.get(post.getAuthorId())
                .tryEmitNext(new SubscriptionEvent(EventTypes.POST_LIKED, new PostNUser(user, post)));
    }
}
