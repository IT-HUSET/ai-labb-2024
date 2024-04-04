package se.ithuset.ailabb.services;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Service;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import se.ithuset.ApiKeys;
import se.ithuset.ailabb.tools.TrafficDataTool;

@Service
public class MyChatService {

    /**
     * Basic blocking chat
     *
     * @param userMessage prompt from the user
     * @return the llm response
     */
    public String chat(String userMessage) {

        var model = AzureOpenAiChatModel.builder()
                .apiKey(ApiKeys.AZ_OPENAI_KEY)
                .endpoint(ApiKeys.AZ_OPENAI_ENDPOINT)
                .serviceVersion(ApiKeys.AZ_OPENAI_SERVICE_VERSION)
                .temperature(0.3)
                .logRequestsAndResponses(true)
                .build();

        return model.generate(userMessage);
    }

    /**
     * Basic streaming chat.
     *
     * @param userMessage Prompt from the user
     * @return a Flux<String> with streaming response from the model
     */
    public Flux<String> streamingChat(String userMessage) {
        var model = AzureOpenAiStreamingChatModel.builder()
                .apiKey(ApiKeys.AZ_OPENAI_KEY)
                .endpoint(ApiKeys.AZ_OPENAI_ENDPOINT)
                .serviceVersion(ApiKeys.AZ_OPENAI_SERVICE_VERSION)
                .temperature(0.3)
                .logRequestsAndResponses(true)
                .build();

        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        Thread.ofVirtual().start(() ->
            model.generate(userMessage, new StreamingResponseHandler<>() {
                @Override
                public void onNext(String token) {
                    //Logger.info("Emitting token {}", token);
                    sink.emitNext(token, (v1, v2) -> {
                        Logger.info("Failed to emit, Signal Type {}, Result: {}", v1, v2);
                        return true;
                    });
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    Logger.info("Response complete with status {}", response.finishReason());
                    sink.tryEmitComplete();
                }

                @Override
                public void onError(Throwable error) {
                    sink.tryEmitError(error);
                }
            })
        );

        Logger.info("Returning the publisher");
        return sink.asFlux();
    }

    /**
     * Streaming chat using AiServices
     *
     * @param userMessage the user prompt
     * @return a Flux<String> with the response stream
     */
    public Flux<String> streamingChatWithAiService(String userMessage) {
        var agent = getULAgent(getAzOpenAiStreamingModel());
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        agent.chat(userMessage)
            .onNext(t -> sink.emitNext(t, (v1, v2) -> {
                Logger.info("Failed to emit. Signal Type {}, Result: {}", v1, v2);
                return true;
            }))
            .onComplete(aiMessage -> sink.tryEmitComplete())
            .onError(sink::tryEmitError);

        return sink.asFlux();
    }

    /**
     * Create a ULSupportAgent using AiServices using the specified streaming chat model
     *
     * @param model The streaming chat model
     * @return The ULSupportAgent, ready to use
     */
    private ULSupportAgent getULAgent(StreamingChatLanguageModel model) {
        AiServices.builder(ULSupportAgent.class)
            .streamingChatLanguageModel(model)
            .tools(new TrafficDataTool()); //Inject
        return AiServices.create(ULSupportAgent.class, model);
    }

    /**
     * Create a new instance of a AzureOpenAiStreamingChatModel
     *
     * @return the new model
     */
    private StreamingChatLanguageModel getAzOpenAiStreamingModel() {
        return AzureOpenAiStreamingChatModel.builder()
                .apiKey(ApiKeys.AZ_OPENAI_KEY)
                .endpoint(ApiKeys.AZ_OPENAI_ENDPOINT)
                .serviceVersion(ApiKeys.AZ_OPENAI_SERVICE_VERSION)
                .temperature(0.3)
                .logRequestsAndResponses(true)
                .build();
    }
}
