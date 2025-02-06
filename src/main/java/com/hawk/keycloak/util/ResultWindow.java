package com.hawk.keycloak.util;

import org.keycloak.models.Constants;

import java.util.stream.Stream;

public class ResultWindow {
    public static int limitFirst(Integer first){
        if(first == null || first < 0){
            return 0;
        }

        return first;
    }

    public static int limitMax(Integer max){
        if(max == null || max < 1){
            return Constants.DEFAULT_MAX_RESULTS;
        }

        return max;
    }

    public static <T> Stream<T> limitStream(Stream<T> stream, Integer first, Integer max) {
        return stream.skip(limitFirst(first)).limit(limitMax(max));
    }
}
