package se.ithuset.ailabb;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.tinylog.Logger;

import se.ithuset.ApiKeys;
import se.ithuset.ailabb.services.MyChatService;

@SpringBootApplication
public class AilabbApplication {

	public static void main(String[] args) {
		SpringApplication.run(AilabbApplication.class, args);
	}

	@Bean
    ApplicationRunner interactiveChatRunner(MyChatService chatService) {
        return args -> {
            Scanner scanner = new Scanner(System.in);
            logEnvVars();
            while (true) {
                System.out.print("\nUser input: ");
                String userMessage = scanner.nextLine();

                if ("exit".equalsIgnoreCase(userMessage)) {
                    break;
                }

                chatAndPrintResult(chatService, userMessage);
            }
            scanner.close();
        };
    }

    //Send prompt to chat service, print the result as the tokens become available
    private void chatAndPrintResult(MyChatService chatService, String userMessage) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        chatService.streamingChat(userMessage)
        .subscribe(
            System.out::print,
            t -> Logger.error("Yikes, something bad happened",t), 
            latch::countDown);
        latch.await();
    }

    static void logEnvVars() {
        Logger.info("key=" + ApiKeys.AZ_OPENAI_KEY);
        Logger.info("endpoint=" + ApiKeys.AZ_OPENAI_ENDPOINT);
        Logger.info("version=" + ApiKeys.AZ_OPENAI_SERVICE_VERSION);
    }

}
