package com.team2.wellness.engagement.ai.application;
import org.springframework.stereotype.Component;
@Component public class SafeFuturePromptBuilder {
    public String build() { return "Create a warm, optimistic wellness-themed future portrait. Preserve only broad adult facial likeness from the supplied reference image. No text, logos, medical claims, sexual content, violence, or identifiable third parties."; }
}
