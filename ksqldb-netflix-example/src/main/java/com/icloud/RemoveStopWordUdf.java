package com.icloud;

import io.confluent.ksql.function.udf.Udf;
import io.confluent.ksql.function.udf.UdfDescription;
import io.confluent.ksql.function.udf.UdfParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UdfDescription(
        name = "remove_stop_words",
        description = "A UDF that removes stop words from a string of text",
        version = "0.1.0",
        author = "twosom"
)
public class RemoveStopWordUdf {

    private final List<String> stopWords =
            List.of("a", "and", "are", "but", "or", "over", "the");

    private List<String> stringToWords(final String source) {
        return Stream.of(source.toLowerCase().split(" "))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String wordsToString(final List<String> words) {
        return String.join(" ", words);
    }

    @Udf(description = "Remove the default stop words from a string to text")
    public String apply(
            @UdfParameter(value = "source", description = "the raw source string") final String source
    ) {
        List<String> words = stringToWords(source);
        words.removeAll(this.stopWords);
        return wordsToString(words);
    }

    @Udf(description = "Remove the default stop words from a string to text from custom stop words")
    public String apply(
            @UdfParameter(value = "source", description = "the raw source string")
            final String source,
            @UdfParameter(value = "stop words", description = "the stop words to remove")
            final List<String> stopWords
    ) {
        List<String> words = stringToWords(source);
        words.removeAll(stopWords);
        return wordsToString(words);
    }
}
