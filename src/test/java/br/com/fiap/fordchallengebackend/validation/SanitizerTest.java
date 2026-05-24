package br.com.fiap.fordchallengebackend.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SanitizerTest {

    static Stream<Arguments> xssInputs() {
        return Stream.of(
            Arguments.of("<script>alert('xss')</script>", "&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;"),
            Arguments.of("<img src=x onerror=alert(1)>", "&lt;img src=x onerror=alert(1)&gt;"),
            Arguments.of("<a href='javascript:void(0)'>click</a>", "&lt;a href=&#x27;javascript:void(0)&#x27;&gt;click&lt;/a&gt;"),
            Arguments.of("<iframe src='malicious.html'></iframe>", "&lt;iframe src=&#x27;malicious.html&#x27;&gt;&lt;/iframe&gt;"),
            Arguments.of("<embed src='evil.swf'>", "&lt;embed src=&#x27;evil.swf&#x27;&gt;"),
            Arguments.of("normal text", "normal text"),
            Arguments.of("Texto com acentuacao valida", "Texto com acentuacao valida"),
            Arguments.of("Hello & Welcome", "Hello &amp; Welcome"),
            Arguments.of("Usuário testou", "Usuário testou")
        );
    }

    @ParameterizedTest
    @MethodSource("xssInputs")
    void shouldSanitizeAgainstXss(String input, String expected) {
        assertEquals(expected, Sanitizer.sanitizeAgainstXss(input));
    }

    @Test
    void shouldReturnEmptyForNullXss() {
        assertEquals("", Sanitizer.sanitizeAgainstXss(null));
    }

    static Stream<Arguments> sqlInputs() {
        return Stream.of(
            Arguments.of("1'; DROP TABLE users; --", "1'' DROP TABLE users "),
            Arguments.of("'; SELECT * FROM users;", "'' SELECT * FROM users"),
            Arguments.of("test--comment", "testcomment"),
            Arguments.of("/* block comment */", ""),
            Arguments.of("normal text", "normal text"),
            Arguments.of("O'Brien", "O''Brien"),
            Arguments.of("", ""),
            Arguments.of("a;b;c", "abc")
        );
    }

    @ParameterizedTest
    @MethodSource("sqlInputs")
    void shouldSanitizeAgainstSqlInjection(String input, String expected) {
        assertEquals(expected, Sanitizer.sanitizeAgainstSqlInjection(input));
    }

    @Test
    void shouldReturnEmptyForNullSql() {
        assertEquals("", Sanitizer.sanitizeAgainstSqlInjection(null));
    }

    static Stream<Arguments> combinedInputs() {
        return Stream.of(
            Arguments.of("<script>alert(1)</script>", "&lt;script&gt;alert(1)&lt;/script&gt;"),
            Arguments.of("clean text", "clean text"),
            Arguments.of("São Paulo", "São Paulo"),
            Arguments.of("a' OR 1=1 --", "a&#x27;&#x27; OR 1=1 ")
        );
    }

    @ParameterizedTest
    @MethodSource("combinedInputs")
    void shouldApplyBothSanitizers(String input, String expected) {
        assertEquals(expected, Sanitizer.sanitizeAll(input));
    }

    @Test
    void shouldReturnEmptyForNullAll() {
        assertEquals("", Sanitizer.sanitizeAll(null));
    }
}
