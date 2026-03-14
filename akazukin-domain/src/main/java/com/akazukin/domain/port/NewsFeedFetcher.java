package com.akazukin.domain.port;

import com.akazukin.domain.model.NewsItem;
import com.akazukin.domain.model.NewsSource;

import java.util.List;

public interface NewsFeedFetcher {

    List<NewsItem> fetch(NewsSource source);
}
