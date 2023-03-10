package com.icloud;

import io.confluent.ksql.function.udf.Udf;
import io.confluent.ksql.function.udf.UdfDescription;
import io.confluent.ksql.function.udf.UdfParameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UdfDescription(
        name = "remove_stop_words",
        description = "A UDF that removes stop words from a string of text",
        version = "1.0.0",
        author = "twosom"
)
public class RemoveStopWordsUdf {

    private final List<String> stopWords = List.of("a", "and", "are", "but", "or", "over", "the");

    private List<String> stringToWords(final String source) {
        return Stream.of(source.toLowerCase().split(" "))
                .collect(Collectors.toCollection(ArrayList<String>::new));
    }

    private String wordsToString(final List<String> words) {
        return String.join(" ", words);
    }

    @Udf(description = "Remove the default stop words from a string of text")
    public String apply(
            @UdfParameter(value = "source", description = "the raw source string") final String source
    ) {
        List<String> words = stringToWords(source);
        words.removeAll(stopWords);
        return wordsToString(words);
    }

    @Udf(description = "Remove the custom stop words from a string of text")
    public String apply(@UdfParameter(value = "source", description = "the raw source string") final String source,
                        @UdfParameter(value = "stopWords", description = "the custom stop words collection") final Collection<String> stopWords) {
        List<String> words = stringToWords(source);
        words.removeAll(stopWords);
        return wordsToString(words);
    }
}