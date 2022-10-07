package com.example.milf2.domain.Dto;

import com.example.milf2.domain.Post.Post;
import com.example.milf2.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostNUser {
    private User user;
    private Post post;
}
