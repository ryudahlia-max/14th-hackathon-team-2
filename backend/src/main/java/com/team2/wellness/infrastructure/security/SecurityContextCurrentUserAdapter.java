package com.team2.wellness.infrastructure.security;

import com.team2.wellness.common.security.CurrentUser;
import com.team2.wellness.engagement.port.out.CurrentUserPort;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentUserAdapter implements CurrentUserPort {

    private final CurrentUser currentUser;

    public SecurityContextCurrentUserAdapter(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public UUID currentUserId() {
        return currentUser.id(SecurityContextHolder.getContext().getAuthentication());
    }
}
