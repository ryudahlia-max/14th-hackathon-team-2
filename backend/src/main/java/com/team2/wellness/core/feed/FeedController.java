package com.team2.wellness.core.feed;

import com.team2.wellness.common.api.CursorPage;
import com.team2.wellness.common.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feed")
public class FeedController {

    private final CurrentUser currentUser;
    private final FeedService feedService;

    public FeedController(CurrentUser currentUser, FeedService feedService) {
        this.currentUser = currentUser;
        this.feedService = feedService;
    }

    @GetMapping
    CursorPage<FeedService.FeedItem> feed(
            Authentication authentication,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return feedService.feed(currentUser.id(authentication), cursor, limit);
    }
}
