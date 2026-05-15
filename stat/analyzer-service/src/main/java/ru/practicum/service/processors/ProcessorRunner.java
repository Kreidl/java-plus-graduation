package ru.practicum.service.processors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProcessorRunner implements CommandLineRunner {
    final EventSimilarityProcessor eventSimilarityProcessor;
    final UserActionProcessor userActionProcessor;

    @Override
    public void run(String... args) throws Exception {
        Thread eventSimilarityThread = new Thread(eventSimilarityProcessor);
        eventSimilarityThread.setName("EventSimilarityThread");
        eventSimilarityThread.start();

        userActionProcessor.start();
    }
}
