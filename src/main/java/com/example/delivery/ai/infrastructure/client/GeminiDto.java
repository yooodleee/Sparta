package com.example.delivery.ai.infrastructure.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class GeminiDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request{
        private SystemInstruction system_instruction;
        private List<Content> contents;
        private List<SafetySetting> safetySettings;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemInstruction{
        private List<Part> parts;
    }

    //사용자 질문과 AI 답변
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content{
        private String role; // "user" 또는 "model"
        private List<Part> parts;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part{
        private String text;
    }

    //구글 안전 설정용 DTO
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SafetySetting{
        private String category;
        private String threshold;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response{
        private List<Candidate> candidates;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Candidate{
            private Content content;
        }
    }
}
