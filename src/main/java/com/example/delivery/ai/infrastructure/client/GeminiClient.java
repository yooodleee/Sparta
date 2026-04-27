package com.example.delivery.ai.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class GeminiClient {

    private final RestClient restClient;
    private final String apiUrl;

    public GeminiClient(
            RestClient.Builder restClientBuilder,
            @Value("${gemini.api-url}") String apiUrl,
            @Value("${gemini.api-key}") String apiKey){

        this.restClient = restClientBuilder
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("Content-Type","application/json")
                .build();
         this.apiUrl = apiUrl;
    }

    public String generateMenuDescription(String menuName){

        //GenerationConfig
        GeminiDto.GenerationConfig config = GeminiDto.GenerationConfig.builder()
                .temperature(0.8)
                .candidateCount(1)
                .build();

        //시스템 인스트럭션(절대 규칙 부여)
        GeminiDto.SystemInstruction instruction = GeminiDto.SystemInstruction.builder()
                .parts(List.of(GeminiDto.Part.builder()
                    .text("너는 배달 앱의 메뉴 설명을 전문적으로 작성하는 10년차 카피라이터야." +
                        "1. 글자 수는 공백 포함 50자 이내로 짧고 강렬하게 작성할 것." +
                        "2. 메뉴, 음식과 관련이 없는 요청이 들어오면 응답하지 말고 '메뉴 이름을 정확히 입력해주세요'라고만 답변할 것." +
                        "3. 모르는 정보나 메뉴와 관계없는 미사어구(예:세계 최고의 맛 등)는 지양하고, 재료와 맛에만 집중할 것." +
                        "4. 비속어, 욕설 금지." +
                        "5. 메뉴 이름이 들어오면 고객이 당장 주문하고 싶게끔 식감과 풍미를 생생하게 묘사할 것.")
                    .build()))
                .build();

        //안전 설정(Safety Settings)
        List<GeminiDto.SafetySetting> safetySettings = List.of(
                GeminiDto.SafetySetting.builder().category("HARM_CATEGORY_HATE_SPEECH").threshold("BLOCK_LOW_AND_ABOVE").build(),
                GeminiDto.SafetySetting.builder().category("HARM_CATEGORY_HARASSMENT").threshold("BLOCK_LOW_AND_ABOVE").build(),
                GeminiDto.SafetySetting.builder().category("HARM_CATEGORY_SEXUALLY_EXPLICIT").threshold("BLOCK_LOW_AND_ABOVE").build(),
                GeminiDto.SafetySetting.builder().category("HARM_CATEGORY_DANGEROUS_CONTENT").threshold("BLOCK_LOW_AND_ABOVE").build()
        );

        GeminiDto.Request requestDto = GeminiDto.Request.builder()
                .system_instruction(instruction)
                .contents(buildFewShotAndActualRequest(menuName))
                .safetySettings(safetySettings)
                .generationConfig(config)
                .build();

        try{
            GeminiDto.Response response = restClient.post()
                    .uri(apiUrl)
                    .body(requestDto)
                    .retrieve()
                    .body(GeminiDto.Response.class);
        //정상 답변 텍스트 추출
            if(response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()){
                String resultText = response.getCandidates().get(0).getContent().getParts().get(0).getText().trim();

                //AI 파이프라인 연결
                //이상한 질문은 Fallback 메시지로 변환
                if(resultText.contains("메뉴 이름을 정확히 입력해주세요")){
                    return "직접 입력해주세요";
                }
                return resultText;
            }
        } catch (Exception e){
            //통신 에러, 타임아웃시 서비스가 죽지않도록
            //나중에 서킷 브레이커 에러 로직 반영 예정
            System.err.println("AI API 통신 중 오류 발생" + e.getMessage());
        }
        //모든 예외(빈 응답, 에러 등)에 대한 Fallback
        return "직접 입력해주세요";
    }

    private List<GeminiDto.Content> buildFewShotAndActualRequest(String actualMenuName){
        List<GeminiDto.Content> contents = new ArrayList<>();

        //Few-shot 프롬프팅 적용 : 모범 답안 예시 주입
        contents.add(makeContent("user", "(사장이 좋아하는)스테이크덮밥"));
        contents.add(makeContent("model", "사장님이 주 3회 점심시간마다 만들어 먹는 덮밥"));

        contents.add(makeContent("user", "(매출 1위)로제크림파스타"));
        contents.add(makeContent("model", "동네 1등 파스타로 이 가게를 먹여 살리는 매출 1위 메뉴"));

        contents.add(makeContent("user", "오리지널"));
        contents.add(makeContent("model", "기름끼 쪽 뺀 오븐구이 치킨의 오리지널!"));

        contents.add(makeContent("user", "시카고 딥디쉬 피자"));
        contents.add(makeContent("model", "입안 가득 느껴지는 다섯가지 프리미엄 치즈 맛에 매콤한 아라비아따 소스로 화룡점정!"));

        contents.add(makeContent("user", "마늘 간장 치킨"));
        contents.add(makeContent("model", "알싸한 통마늘의 풍미와 특제 숙성 간장이 만나 깊은 감칠맛을 내는 바학한 치킨"));

        contents.add(makeContent("user", "치즈 돈까스"));
        contents.add(makeContent("model", "바삭한 튀김옷을 베어 물면 고소한 모짜렐라 치즈가 폭포수처럼 쏟아지는 극강의 고소함"));

        //함정 질문 예시
        String[] badRequests = {
                "오늘 날씨 어떄?", "라면 칼로리 얼마야?", "김치찌개 만드는 법 뭐야?", "메뉴 선정을 어떻게 하는 게 좋을까?",
                "강남 맛집 어디야?", "오늘 환율 얼마야?", "요즘 선호하는 음식이 뭐야?", "인기 많은 메뉴는?", "비 오는 날 먹고 싶은 음식은?",
                "음식 가격은 얼마 정도로 할까?", "메뉴 선정 기준이 뭘까?", "파이썬으로 웹크롤링 어떻게 해?", "고양이는 어떤 음식을 좋아해?"
        };

        for(String badWord : badRequests){
            contents.add(makeContent("user", badWord));
            contents.add(makeContent("model", "메뉴 이름을 정확히 입력해주세요"));
        }

        contents.add(makeContent("user", actualMenuName));

        return contents;
    }

    private GeminiDto.Content makeContent(String role, String text){
        return GeminiDto.Content.builder()
                .role(role)
                .parts(List.of(GeminiDto.Part.builder().text(text).build()))
                .build();
    }

}
