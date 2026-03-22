package com.akazukin.sdk.twitter.model;

public record TweetMetrics(
    String id,
    int likeCount,
    int replyCount,
    int retweetCount,
    int impressionCount
) {
}
