package com.jessepreiner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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

        List<Response> responsesList = completionStages
                .stream()
                .map(CompletionStage::toCompletableFuture)
                .map(completableFuture -> {
                    CompletableFuture<Response> responseCompletableFuture = completableFuture.thenApply(y -> new SuccessResponse(new RequestView(UUID.randomUUID().toString())));
                    return responseCompletableFuture
                            .exceptionally((ex) -> new FailureResponse(new RequestView(UUID.randomUUID().toString()), ex.getMessage()));
                })
                .map(CompletableFuture::join).collect(Collectors.toList());

        HashMap<String, String> resultViaCollect = resultViaCollect(responsesList);
        HashMap<String, String> resultViaReduce = resultViaReduce(responsesList);

        System.out.println(resultViaCollect);
        System.out.println(resultViaReduce);
        System.out.println("Responses count is " + responsesList.size() + ". Requests count is " + completionStages.size());
    }

    private static HashMap<String, String> resultViaCollect(List<Response> responses) {
        return responses.stream()
                        .collect(HashMap::new,
                                mutatingAccumulator(),
                                HashMap::putAll);
    }

    private static HashMap<String, String> resultViaReduce(List<Response> responses) {
        return responses.stream()
                        .reduce(new HashMap<>(),
                                nonMutatingAccumulator(),
                                (hashMap1, hashMap2) -> {
                                    hashMap1.putAll(hashMap2);
                                    return hashMap1;
                                });
    }

    // BiConsumer seems to do in place mutation
    private static BiConsumer<HashMap<String, String>, Response> mutatingAccumulator() {
        return (map, response) -> {
            if (response instanceof SuccessResponse) {
                map.put("success", map.getOrDefault("success", "").concat(((SuccessResponse) response).view.toString()));
            } else {
                map.put("fail", map.getOrDefault("fail", "").concat(String.format("%s %s", ((FailureResponse) response).view.toString(), ((FailureResponse) response).failureMessage)));
            }
        };
    }

    // TODO does this also mutate but just return a new hashmap? Or is the accumulator a new map?
    private static BiFunction<HashMap<String, String>, Response, HashMap<String, String>> nonMutatingAccumulator() {
        return (hashMap, response) -> {
            if (response instanceof SuccessResponse) {
                hashMap.put("success", hashMap.getOrDefault("success", "").concat(((SuccessResponse) response).view.toString()));
            } else {
                hashMap.put("fail", hashMap.getOrDefault("fail", "").concat(String.format("%s %s", ((FailureResponse) response).view.toString(), ((FailureResponse) response).failureMessage)));
            }
            return hashMap;
        };
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
