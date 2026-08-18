package com.team2.wellness.core.feed;

import com.team2.wellness.common.api.CursorPage;
import com.team2.wellness.common.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feed")
public class FeedController {

    private final CurrentUser currentUser;
    private final FeedService feedService;
    private final FeedReactionService reactionService;

    public FeedController(CurrentUser currentUser, FeedService feedService, FeedReactionService reactionService) {
        this.currentUser = currentUser;
        this.feedService = feedService;
        this.reactionService = reactionService;
    }

    @GetMapping
    CursorPage<FeedService.FeedItem> feed(
            Authentication authentication,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return feedService.feed(currentUser.id(authentication), cursor, limit);
    }

    @PostMapping("/{completionId}/reaction")
    FeedReactionService.ReactionView react(
            Authentication authentication,
            @PathVariable java.util.UUID completionId,
            @Valid @RequestBody ReactionRequest request
    ) {
        return reactionService.react(currentUser.id(authentication), completionId, request.type());
    }

    @DeleteMapping("/{completionId}/reaction")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeReaction(Authentication authentication, @PathVariable java.util.UUID completionId) {
        reactionService.remove(currentUser.id(authentication), completionId);
    }

    @GetMapping("/reactions/received")
    java.util.List<FeedReactionService.ReceivedReactionView> received(Authentication authentication) {
        return reactionService.received(currentUser.id(authentication));
    }

    record ReactionRequest(@NotBlank String type) {
    }
}
