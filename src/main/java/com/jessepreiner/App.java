package com.jessepreiner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BinaryOperator;

public class App {
    public static void main(String[] args) {
        List<CompletionStage<ResponseData>> completionStages = Arrays.asList(
                getResponse(false),
                getResponse(false),
                getResponse(false),
                getResponse(true),
                getResponse(false),
                getResponse(false),
                getResponse(false));

        HashMap<String, String> response = completionStages
                .stream()
                .map(CompletionStage::toCompletableFuture)
                .map(completableFuture ->
                {
                    CompletableFuture<Response> responseCompletableFuture = completableFuture.thenApply(y -> new SuccessResponse(new RequestView(UUID.randomUUID().toString())));
                    return responseCompletableFuture
                            .exceptionally((ex) -> new FailureResponse(new RequestView("failuuure"), ex.getMessage()));
                }).map(CompletableFuture::join)
                .reduce(new HashMap<>(), (a, b) -> {
                    if (b instanceof SuccessResponse) {
                        a.put("success", a.getOrDefault("success", "").concat(((SuccessResponse) b).view.toString()));
                    } else {
                        a.put("fail", a.getOrDefault("fail", "").concat(String.format("%s %s", ((FailureResponse) b).view.toString(), ((FailureResponse) b).failureMessage)));

                    }
                    return a;
                }, (a, b) -> new HashMap<>()); // i think this is needed because the accumulator type is different than stream item?

        System.out.println(response);
    }


    private static CompletionStage<ResponseData> getResponse(boolean throwEx) {
        if (throwEx) {
            return CompletableFuture.supplyAsync(() -> {
                throw new RuntimeException("I was told to do this, don't be mad: error code " + UUID.randomUUID().toString());
            });
        }
        try {
            Thread.sleep(250);
        } catch (Exception e) {
            //
        }
        return CompletableFuture.completedFuture(new ResponseData(UUID.randomUUID().toString()));

    }

    interface Response {
    }

    static class FailureResponse implements Response {
        private final Object view;
        private final String failureMessage;

        FailureResponse(Object view, String failureMessage) {
            this.view = view;
            this.failureMessage = failureMessage;
        }
    }

    static class SuccessResponse implements Response {
        private final Object view;

        SuccessResponse(Object view) {
            this.view = view;
        }
    }

    static class RequestView {
        private final String name;

        RequestView(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "RequestView{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    static class ResponseData {
        private final String data;

        ResponseData(String data) {
            this.data = data;
        }
    }

}