package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;

@QuarkusTest
class HealthEndpointTest {
    @Test
    void reportsServiceAsUp() {
        given()
                .accept("application/json")
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("status", is("UP"))
                .body("checks", empty());
    }

    @Test
    void acceptsMinimalRequestWithoutHeadersOrBody() {
        when()
                .get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks", empty());
    }
}
