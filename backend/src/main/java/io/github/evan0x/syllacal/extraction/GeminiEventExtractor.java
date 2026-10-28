package io.github.evan0x.syllacal.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiEventExtractor implements EventExtractor {

	private static final String BASE_URL = "https://generativelanguage.googleapis.com";
	private static final int MAX_RETRIES = 3;
	private static final long INITIAL_BACKOFF_MS = 200L;

	private final RestClient restClient;
	private final JsonMapper jsonMapper;
	private final String apiKey;
	private final String model;
	private final String promptTemplate;

	@Autowired
	public GeminiEventExtractor(
			RestClient.Builder restClientBuilder,
			JsonMapper jsonMapper,
			@Value("${gemini.api-key}") String apiKey,
			@Value("${gemini.model}") String model,
			@Value("classpath:prompts/extract-events.txt") Resource promptResource) {
		this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
		this.jsonMapper = jsonMapper;
		this.apiKey = apiKey;
		this.model = model;
		this.promptTemplate = readPrompt(promptResource);
	}

	/** Package-visible constructor for unit tests (no network). */
	GeminiEventExtractor(JsonMapper jsonMapper, String promptTemplate) {
		this.restClient = null;
		this.jsonMapper = jsonMapper;
		this.apiKey = "";
		this.model = "test";
		this.promptTemplate = promptTemplate;
	}

	private static String readPrompt(Resource promptResource) {
		try {
			return promptResource.getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to load prompts/extract-events.txt", e);
		}
	}

	@Override
	public List<ExtractedEvent> extract(String syllabusText, LocalDate termStart, LocalDate termEnd) {
		String systemInstruction = promptTemplate
				.replace("{TERM_START}", termStart.toString())
				.replace("{TERM_END}", termEnd.toString());

		Map<String, Object> requestBody = buildRequest(systemInstruction, syllabusText);
		String responseBody = callWithRetry(requestBody);
		String modelJson = extractModelText(responseBody);
		return parseEventsJson(modelJson);
	}

	List<ExtractedEvent> parseEventsJson(String modelJson) {
		try {
			EventsPayload payload = jsonMapper.readValue(modelJson, EventsPayload.class);
			if (payload == null || payload.events() == null) {
				throw new ExtractionException("Gemini response JSON does not match the expected schema");
			}
			return List.copyOf(payload.events());
		}
		catch (ExtractionException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ExtractionException("Gemini response is not valid JSON for the expected schema", e);
		}
	}

	private String callWithRetry(Map<String, Object> requestBody) {
		RuntimeException lastFailure = null;
		for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
			try {
				return postGenerateContent(requestBody);
			}
			catch (RetryableGeminiException e) {
				lastFailure = e;
				if (attempt == MAX_RETRIES) {
					break;
				}
				sleep(INITIAL_BACKOFF_MS * (1L << attempt));
			}
		}
		throw new ExtractionException("Gemini API failed after retries", lastFailure);
	}

	private String postGenerateContent(Map<String, Object> requestBody) {
		return restClient.post()
				.uri("/v1beta/models/{model}:generateContent", model)
				.header("x-goog-api-key", apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(requestBody)
				.exchange((request, response) -> {
					int status = response.getStatusCode().value();
					if (status == 429 || response.getStatusCode().is5xxServerError()) {
						throw new RetryableGeminiException("Gemini API returned HTTP " + status);
					}
					byte[] bytes = response.getBody().readAllBytes();
					String body = new String(bytes, StandardCharsets.UTF_8);
					if (response.getStatusCode().isError()) {
						throw new ExtractionException("Gemini API returned HTTP " + status);
					}
					return body;
				});
	}

	private String extractModelText(String responseBody) {
		try {
			JsonNode root = jsonMapper.readTree(responseBody);
			JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
			String text = null;
			if (parts.isArray()) {
				for (JsonNode part : parts) {
					if (part.path("thought").asBoolean(false)) {
						continue;
					}
					if (part.path("text").isString() && !part.path("text").asString().isBlank()) {
						text = part.path("text").asString();
					}
				}
			}
			if (text == null) {
				throw new ExtractionException("Gemini response did not contain model text");
			}
			return text;
		}
		catch (ExtractionException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ExtractionException("Failed to read Gemini API response", e);
		}
	}

	private Map<String, Object> buildRequest(String systemInstruction, String syllabusText) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("systemInstruction", Map.of(
				"parts", List.of(Map.of("text", systemInstruction))));
		body.put("contents", List.of(Map.of(
				"parts", List.of(Map.of("text", syllabusText)))));

		Map<String, Object> generationConfig = new LinkedHashMap<>();
		generationConfig.put("temperature", 0);
		generationConfig.put("thinkingConfig", Map.of("thinkingLevel", "minimal"));
		generationConfig.put("responseMimeType", "application/json");
		generationConfig.put("responseSchema", responseSchema());
		body.put("generationConfig", generationConfig);
		return body;
	}

	private static Map<String, Object> responseSchema() {
		Map<String, Object> eventProperties = new LinkedHashMap<>();
		eventProperties.put("title", Map.of("type", "STRING"));
		eventProperties.put("type", Map.of(
				"type", "STRING",
				"enum", List.of("ASSIGNMENT", "EXAM", "QUIZ", "PROJECT", "OTHER")));
		eventProperties.put("date", nullableString());
		eventProperties.put("time", nullableString());
		eventProperties.put("weight_percent", nullableNumber());
		eventProperties.put("confidence", Map.of(
				"type", "STRING",
				"enum", List.of("HIGH", "MEDIUM", "LOW")));
		eventProperties.put("source_text", Map.of("type", "STRING"));

		Map<String, Object> eventSchema = new LinkedHashMap<>();
		eventSchema.put("type", "OBJECT");
		eventSchema.put("properties", eventProperties);
		eventSchema.put("required", List.of(
				"title", "type", "date", "time", "weight_percent", "confidence", "source_text"));

		Map<String, Object> eventsArray = new LinkedHashMap<>();
		eventsArray.put("type", "ARRAY");
		eventsArray.put("items", eventSchema);

		Map<String, Object> root = new LinkedHashMap<>();
		root.put("type", "OBJECT");
		root.put("properties", Map.of("events", eventsArray));
		root.put("required", List.of("events"));
		return root;
	}

	private static Map<String, Object> nullableString() {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "STRING");
		schema.put("nullable", true);
		return schema;
	}

	private static Map<String, Object> nullableNumber() {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "NUMBER");
		schema.put("nullable", true);
		return schema;
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ExtractionException("Interrupted while retrying Gemini API call", e);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record EventsPayload(List<ExtractedEvent> events) {
	}

	private static final class RetryableGeminiException extends RuntimeException {
		RetryableGeminiException(String message) {
			super(message);
		}
	}
}
