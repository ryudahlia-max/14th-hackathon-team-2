package com.team2.wellness.common.api;

import java.util.List;

public record CursorPage<T>(List<T> items, String nextCursor) {
}
