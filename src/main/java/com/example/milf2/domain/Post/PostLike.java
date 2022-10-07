package com.example.milf2.domain.Post;

import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(of = "userId")
public class PostLike {
    private String userId;
    private Date creationTime;

    public PostLike() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public PostLike(String userId) {
        this.userId = userId;
        this.creationTime = new Date();
    }
}
