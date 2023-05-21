/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.http.server.tck.tests.constraintshandler;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.Status;
import io.micronaut.http.server.tck.AssertionUtils;
import io.micronaut.http.server.tck.HttpResponseAssertion;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static io.micronaut.http.server.tck.TestScenario.asserts;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({
    "java:S5960", // We're allowed assertions, as these are used in tests only
    "checkstyle:MissingJavadocType",
    "checkstyle:DesignForExtension"
})
public class ControllerConstraintHandlerTest {
    public static final String SPEC_NAME = "ControllerConstraintHandlerTest";
    private static final HttpResponseAssertion TEAPOT_ASSERTION = HttpResponseAssertion.builder()
        .status(HttpStatus.I_AM_A_TEAPOT)
        .assertResponse(response -> {
            Optional<String> json = response.getBody(Argument.of(String.class));
            assertTrue(json.isPresent());
            assertTrue(json.get().contains("secret"));
            assertTrue(json.get().contains("password"));
        })
        .build();

    /**
     * @see <a href="https://github.com/micronaut-projects/micronaut-aws/issues/1164">micronaut-aws #1164</a>
     */
    @Test
    void testPojoConstraintViolationExceptionIsHandledViaHandler() throws IOException {
        asserts(SPEC_NAME,
            HttpRequest.POST("/constraints-via-handler", "{\"username\":\"tim@micronaut.example\",\"password\":\"secret\"}"),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .build()));

        asserts(SPEC_NAME,
            HttpRequest.POST("/constraints-via-handler", "{\"username\":\"invalidemail\",\"password\":\"secret\"}"),
            (server, request) -> AssertionUtils.assertThrows(server, request, constraintAssertion("must be a well-formed email address")));
        asserts(SPEC_NAME,
            HttpRequest.POST("/constraints-via-handler", "{\"username\":\"\",\"password\":\"secret\"}"),
            (server, request) -> AssertionUtils.assertThrows(server, request, constraintAssertion("must not be blank\"")));
        asserts(SPEC_NAME,
            HttpRequest.POST("/constraints-via-handler/with-at-nullable", "{\"username\":\"invalidemail\",\"password\":\"secret\"}"),
            (server, request) -> AssertionUtils.assertThrows(server, request, constraintAssertion("must be a well-formed email address")));
        asserts(SPEC_NAME,
            HttpRequest.POST("/constraints-via-handler/with-at-nullable", "{\"username\":\"\",\"password\":\"secret\"}"),
            (server, request) -> AssertionUtils.assertThrows(server, request, constraintAssertion("must not be blank\"")));


        asserts(SPEC_NAME,
            HttpRequest.POST("/constraints-via-on-error-method", "{\"username\":\"\",\"password\":\"secret\"}"),
            (server, request) -> AssertionUtils.assertThrows(server, request, TEAPOT_ASSERTION));

        asserts(SPEC_NAME,
            HttpRequest.POST("/constraints-via-on-error-method", "{\"password\":\"secret\"}"),
            (server, request) -> AssertionUtils.assertThrows(server, request, TEAPOT_ASSERTION));

        asserts(SPEC_NAME,
            HttpRequest.POST("/constraints-via-on-error-method/with-at-nullable", "{\"username\":\"\",\"password\":\"secret\"}"),
            (server, request) -> AssertionUtils.assertThrows(server, request, TEAPOT_ASSERTION));

        asserts(SPEC_NAME,
            HttpRequest.POST("/constraints-via-on-error-method/with-at-nullable", "{\"password\":\"secret\"}"),
            (server, request) -> AssertionUtils.assertThrows(server, request, TEAPOT_ASSERTION));
    }

    @Disabled("currently not supported")
    @Test
    void testPojoCanHaveNullabilityAnnotationsMatchingConstraints() throws IOException {

        asserts(SPEC_NAME,
            HttpRequest.POST("/constraints-via-handler/with-non-null", "{\"username\":\"invalidemail\",\"password\":\"secret\"}"),
            (server, request) -> AssertionUtils.assertThrows(server, request, constraintAssertion("must be a well-formed email address")));
        asserts(SPEC_NAME,
            HttpRequest.POST("/constraints-via-handler/with-non-null", "{\"username\":\"\",\"password\":\"secret\"}"),
            (server, request) -> AssertionUtils.assertThrows(server, request, constraintAssertion("must not be blank\"")));


        asserts(SPEC_NAME,
            HttpRequest.POST("/constraints-via-on-error-method/with-non-null", "{\"username\":\"\",\"password\":\"secret\"}"),
            (server, request) -> AssertionUtils.assertThrows(server, request, TEAPOT_ASSERTION));

        asserts(SPEC_NAME,
            HttpRequest.POST("/constraints-via-on-error-method/with-non-null", "{\"password\":\"secret\"}"),
            (server, request) -> AssertionUtils.assertThrows(server, request, TEAPOT_ASSERTION));
    }

    private static HttpResponseAssertion constraintAssertion(String expectedMessage) {
        return HttpResponseAssertion.builder()
            .status(HttpStatus.BAD_REQUEST)
            .assertResponse(response -> {
                Optional<String> json = response.getBody(Argument.of(String.class));
                assertTrue(json.isPresent());
                assertTrue(json.get().contains(expectedMessage));
            }).build();
    }

    @Controller("/constraints-via-handler")
    @Requires(property = "spec.name", value = SPEC_NAME)
    static class BodyController {
        @Post
        @Produces(MediaType.TEXT_PLAIN)
        @Status(HttpStatus.OK)
        void login(@Body @NotNull @Valid CredentialsWithoutNullabilityAnnotation credentials) {

        }

        @Post("/with-at-nullable")
        @Produces(MediaType.TEXT_PLAIN)
        @Status(HttpStatus.OK)
        void login(@Body @NotNull @Valid CredentialsWithNullable credentials) {

        }

        @Post("/with-non-null")
        @Produces(MediaType.TEXT_PLAIN)
        @Status(HttpStatus.OK)
        void login(@Body @NotNull @Valid CredentialsWithNonNull credentials) {

        }
    }

    @Controller("/constraints-via-on-error-method")
    @Requires(property = "spec.name", value = SPEC_NAME)
    static class OnErrorMethodController {
        @Post
        @Produces(MediaType.TEXT_PLAIN)
        @Status(HttpStatus.OK)
        void login(@Body @NotNull @Valid CredentialsWithoutNullabilityAnnotation credentials) {
        }

        @Post("/with-at-nullable")
        @Produces(MediaType.TEXT_PLAIN)
        @Status(HttpStatus.OK)
        void loginWithNullable(@Body @NotNull @Valid CredentialsWithNullable credentials) {
        }

        @Post("/with-non-null")
        @Produces(MediaType.TEXT_PLAIN)
        @Status(HttpStatus.OK)
        void loginWithNullable(@Body @NotNull @Valid CredentialsWithNonNull credentials) {
        }

        @Error(exception = ConstraintViolationException.class)
        @Status(HttpStatus.I_AM_A_TEAPOT)
        Optional<Map> constraintsEx(ConstraintViolationException e, HttpRequest<?> request) {
            return request.getBody(Map.class);
        }
    }

    @Introspected
    static class CredentialsWithoutNullabilityAnnotation {
        @NotBlank
        @Email
        private final String username;

        @NotBlank
        private final String password;

        CredentialsWithoutNullabilityAnnotation(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    @Introspected
    static class CredentialsWithNullable {
        @NotBlank
        @Email
        @Nullable
        private final String username;

        @NotBlank
        @Nullable
        private final String password;

        CredentialsWithNullable(@Nullable String username, @Nullable String password) {
            this.username = username;
            this.password = password;
        }

        @Nullable
        public String getUsername() {
            return username;
        }

        @Nullable
        public String getPassword() {
            return password;
        }
    }

    @Introspected
    static class CredentialsWithNonNull {
        @NotBlank
        @Email
        @NonNull
        private final String username;

        @NotBlank
        @NonNull
        private final String password;

        CredentialsWithNonNull(@NonNull String username, @NonNull String password) {
            this.username = username;
            this.password = password;
        }

        @NonNull
        public String getUsername() {
            return username;
        }

        @NonNull
        public String getPassword() {
            return password;
        }
    }
}