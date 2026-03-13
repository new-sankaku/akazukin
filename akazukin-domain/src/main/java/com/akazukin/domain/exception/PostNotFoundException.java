package com.akazukin.domain.exception;

import java.util.UUID;

public class PostNotFoundException extends DomainException {

    public PostNotFoundException(UUID postId) {
        super("POST_NOT_FOUND", "Post not found: " + postId);
    }
}
