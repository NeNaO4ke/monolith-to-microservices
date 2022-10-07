package com.example.milf2.controller;


import com.example.milf2.domain.Dto.MetaDto;
import com.example.milf2.domain.Dto.PostNUser;
import com.example.milf2.domain.Post.Post;
import com.example.milf2.domain.Subscriptions.SubscriptionEvent;
import com.example.milf2.domain.user.User;
import com.example.milf2.service.PostService;
import com.example.milf2.service.WebClientService;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.File;
import java.util.concurrent.*;

@RestController
@CrossOrigin(value = "http://localhost:3000", origins = "http://localhost:3000", allowCredentials = "true")
public class PostController {


    private final PostService postService;
    private final WebClientService webClientService;
    private final Sinks.Many<PostNUser> postCreatedSink;
    private final ConcurrentHashMap<String, Sinks.Many<SubscriptionEvent>> sinksHashMap;

    public Sinks.Many<SubscriptionEvent> personalSink() {
        return Sinks.many().multicast().onBackpressureBuffer(Integer.MAX_VALUE, false);
    }

    public PostController(PostService postService,
                          WebClientService webClientService,
                          Sinks.Many<PostNUser> postCreatedSink,
                          ConcurrentHashMap<String, Sinks.Many<SubscriptionEvent>> sinksHashMap) {
        this.postService = postService;
        this.webClientService = webClientService;
        this.postCreatedSink = postCreatedSink;
        this.sinksHashMap = sinksHashMap;
    }


    @SubscriptionMapping
    public Flux<PostNUser> postCreated() {
        return postCreatedSink.asFlux().share();
    }

    @SubscriptionMapping
    public Flux<SubscriptionEvent> personalSubscription(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        sinksHashMap.putIfAbsent(user.getId(), personalSink());
        return sinksHashMap.get(user.getId()).asFlux();
    }

    @GetMapping(value = "/parse-html", produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<MetaDto> result(@RequestParam(required = true) String url) {
        return webClientService.getMetaDtoMono(url);
    }


    @QueryMapping
    Flux<PostNUser> getGlobalFeed() {
        return postService.findAllPostWithUsers();
    }

    @QueryMapping
    Flux<Post> getAllPosts() {
        return postService.findAllPosts();
    }

    @QueryMapping
    Mono<Post> getOnePost(@Argument String id) {
        return postService.getOne(id);
    }

    @QueryMapping
    Flux<Post> getAllPostsByAuthorId(@Argument String authorId) {
        return postService.getAllByAuthorId(authorId);
    }

    @QueryMapping
    Mono<PostNUser> getPostWithUser(@Argument String postId) {
        return postService.getPostWithUser(postId);
    }

    @QueryMapping
    Flux<PostNUser> getTopPostsWithUsers() {
        return postService.getTopPostsWithUsers();
    }

    @QueryMapping
    Flux<PostNUser> getCommentsToPost(@Argument String postId) {
        return postService.getCommentsToPostWithUsers(postId);
    }

    @MutationMapping
    Mono<Post> likeOrDislikePost(@Argument String postId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return postService.likeOrDislikePost(postId, user.getId());
    }


    @PostMapping(value = "/upload-post")
    public Mono<Post> uploadPost(
            @RequestPart(value = "files", required = false) Flux<FilePart> fileParts,
            @RequestPart(value = "text", required = false) String text,
            @RequestPart(value = "link", required = false) String url,
            @RequestPart(value = "isCommentTo", required = false) String isCommentTo,
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        File uploadDir = new File("D:\\files2");
        if (!uploadDir.exists()) {
            uploadDir.mkdir();
        }

        CleanerProperties props = new CleanerProperties();
        String cleanedText = new HtmlCleaner(props).clean(text).getText().toString();

        GenericExtFilter genericExtFilter = new GenericExtFilter("png", "jpeg", "jpg", "gif", "webp");

        Post post = new Post();
        post.setText(cleanedText);
        post.setAuthorId(user.getId());

        if (isCommentTo != null)
            post.setIsCommentTo(isCommentTo);
        if (url == null)
            url = "";

        return postService.validateAndSaveMessage(fileParts, text, url, genericExtFilter, post);
    }

    public static class GenericExtFilter {
        private final String[] exts;

        public GenericExtFilter(String... exts) {
            this.exts = exts;
        }


        public boolean accept(String name) {
            for (String ext : exts) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }

            return false;
        }
    }

}

