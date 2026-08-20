package com.team2.wellness.engagement.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.team2.wellness.engagement.port.out.CoreAccessPort;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SafeFuturePromptBuilderTest {

    private final SafeFuturePromptBuilder builder = new SafeFuturePromptBuilder();

    @Test
    void promptContainsRoutineTypeAndMissCountWithoutUnsafeMarkup() {
        var occurrence = new CoreAccessPort.MissedRoutineOccurrence(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "물 마시기<script>",
                "수분 & 건강",
                7,
                LocalDate.of(2026, 8, 18)
        );

        String prompt = builder.build(occurrence);

        assertThat(prompt)
                .contains("<title>물 마시기 script</title>")
                .contains("<category>수분 건강</category>")
                .contains("<missed_occurrences_last_366_scheduled_days>7")
                .contains("<most_recent_missed_date>2026-08-18")
                .contains("identity source, not merely style inspiration")
                .contains("exact same")
                .contains("Do not invent, replace, recast")
                .doesNotContain("<script>");
    }
}
