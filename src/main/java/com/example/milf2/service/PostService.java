package com.example.milf2.service;


import com.example.milf2.controller.PostController;
import com.example.milf2.domain.Dto.PostNUser;
import com.example.milf2.domain.Post.Post;
import com.example.milf2.domain.Post.PostLike;
import com.example.milf2.domain.user.User;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


@Service
public class PostService {


    private final ReactiveMongoTemplate template;
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final WebClientService webClientService;

    String uploadPath = "D:/files2/";

    public PostService(ReactiveMongoTemplate template, UserService userService, SubscriptionService subscriptionService, WebClientService webClientService) {
        this.template = template;
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.webClientService = webClientService;
    }

    public Flux<PostNUser> getTopPostsWithUsers() {
        return findAllPostWithUsers().sort(Comparator.comparing(obj -> obj.getPost().getLikesCount(), Comparator.reverseOrder()));
    }

    public Flux<PostNUser> findAllPostWithUsers() {

        Flux<Post> allPosts = findAllPosts().filter(post -> post.getIsCommentTo() == null).cache();

        return getPostNUserFlux(allPosts);
    }

    private Mono<User> getFirstOccurrence(Flux<User> allAuthors, Post post) {
        return allAuthors
                .filter(author -> author.getId().equals(post.getAuthorId()))
                .next();
    }

    public Flux<Post> findAllPosts() {
        return template.findAll(Post.class);
    }

    public Mono<PostNUser> getPostWithUser(String postId) {
        return template.findById(postId, Post.class)
                .flatMap(post -> Mono.zip(template.findById(post.getAuthorId(), User.class), Mono.just(post), PostNUser::new));

    }

    public Flux<PostNUser> getCommentsToPostWithUsers(String postId) {
        Flux<Post> allPosts = template.find(Query.query(Criteria.where("isCommentTo").is(postId)), Post.class).cache();

        return getPostNUserFlux(allPosts);
    }

    private Flux<PostNUser> getPostNUserFlux(Flux<Post> allPosts) {
        Flux<String> allAuthorsIds = allPosts
                .map(Post::getAuthorId)
                .distinct();

        Flux<User> allUsers = userService.getAllUsersWithIds(allAuthorsIds).cache();

        return allPosts
                .flatMap(post -> Mono.zip(getFirstOccurrence(allUsers, post), Mono.just(post), PostNUser::new))
                .sort(Comparator.comparing(obj -> obj.getPost().getDate(), Comparator.reverseOrder()));
    }


    @Transactional
    public Mono<Post> likeOrDislikePost(String postId, String authorId) {
        return getOne(postId)
                .flatMap(post -> {
                    Optional<PostLike> pl = post.getLikes().stream()
                            .filter(postLike -> postLike.getUserId().equals(authorId)).findFirst();
                    if (pl.isEmpty()) {
                        post.getLikes().add(new PostLike(authorId));
                        post.setLikesCount(post.getLikes().size());
                        return userService.getOneById(authorId)
                                .doOnSuccess(user -> subscriptionService.postLiked(user, post)).then(saveOne(post));
                    } else {
                        post.getLikes().remove(pl.get());
                        post.setLikesCount(post.getLikes().size());
                        return saveOne(post);
                    }
                });
    }

    public Mono<Post> saveOne(Post post) {
        return template.save(post);
    }

    @Transactional
    public Mono<Post> saveOneTransactional(Post post) {
        if (post.getIsCommentTo() == null) {
            return saveOne(post)
                    .flatMap(post1 ->
                            userService.getOneById(post1.getAuthorId())
                                    .doOnSuccess(user -> subscriptionService.postCreated(user, post1)).then(Mono.just(post1))

                    );
        } else {
            Update update = new Update();
            update.inc("commentsCount");

            return saveOne(post)
                    .flatMap(post1 ->
                            template.findAndModify(
                                            Query.query(Criteria.where("id").is(post1.getIsCommentTo())),
                                            update.push("commentIds", post1.getId()),
                                            FindAndModifyOptions.options().returnNew(true), Post.class)
                                    .flatMap(updatedPost -> userService.getOneById(post1.getAuthorId())
                                            .doOnSuccess(user -> subscriptionService.commentAdded(user, post1, updatedPost.getAuthorId())))
                                    .then(Mono.just(post1)));
        }
    }

    public Mono<Post> validateAndSaveMessage(Flux<FilePart> fileParts, String text, String url,
                                             PostController.GenericExtFilter genericExtFilter, Post post) {
        return fileParts
                .hasElements()
                .flatMap(aBoolean -> {
                    if (!aBoolean && text.isEmpty())
                        return Mono.error(new Throwable("Empty message!"));
                    return Mono.empty();
                })
                .then(fileParts
                        .take(6)
                        .filter(filePart -> genericExtFilter.accept(filePart.filename()))
                        .flatMap(fp -> {
                            String uuidFile = UUID.randomUUID().toString();
                            String resultFilename = uuidFile + "." + fp.filename();
                            post.getFileLocations().add(uploadPath + resultFilename);
                            return fp.transferTo(new File(uploadPath + resultFilename));
                        })
                        .then(Mono.just(url))
                        .flatMap(url1 -> {
                            if (Objects.equals(url1, ""))
                                return Mono.empty();
                            return webClientService.getMetaDtoMono(url1);
                        })
                        .map(metaDto -> {
                            post.setMetaDto(metaDto);
                            return Mono.empty();
                        })
                        .then(Mono.just(post)))
                .flatMap(this::saveOneTransactional);
    }

    public Flux<Post> getAllByAuthorId(String id) {
        return template.find(Query.query(Criteria.where("authorId").is(id)), Post.class)
                .sort(Comparator.comparing(Post::getDate, Comparator.reverseOrder()));
    }


    public Mono<Post> getOne(String id) {
        return template.findById(id, Post.class);
    }
}
