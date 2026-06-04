package tech.illuin.wombat.profile;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class ProfileControllerTest
{

    @Test
    void list_returnsSeededProfiles()
    {
        given()
            .when().get("/profiles")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(2))
            .body("id", hasItem("test-aws-c5"))
            .body("id", hasItem("test-gcp-n2"));
    }

    @Test
    void get_existingProfile_returnsIt()
    {
        given()
            .when().get("/profiles/test-aws-c5")
            .then()
            .statusCode(200)
            .body("id", is("test-aws-c5"))
            .body("description", is("Test AWS c5.large FRA"))
            .body("instanceType", is("c5.large"));
    }

    @Test
    void get_missingProfile_returns404()
    {
        given()
            .when().get("/profiles/does-not-exist")
            .then()
            .statusCode(404);
    }

    @Test
    void create_update_delete_cycle()
    {
        String body = """
            {
              "id": "ct-aws-m5",
              "description": "CRUD test m5",
              "provider": "aws",
              "instanceType": "m5.large",
              "location": "FRA",
              "lifespan": 43800
            }
            """;

        given()
            .contentType("application/json").body(body)
            .when().post("/profiles")
            .then()
            .statusCode(200)
            .body("id", is("ct-aws-m5"));

        given()
            .when().get("/profiles/ct-aws-m5")
            .then()
            .statusCode(200)
            .body("description", is("CRUD test m5"));

        String updated = """
            {
              "id": "ct-aws-m5",
              "description": "Renamed",
              "provider": "aws",
              "instanceType": "m5.xlarge",
              "location": "PAR",
              "lifespan": 87600
            }
            """;
        given()
            .contentType("application/json").body(updated)
            .when().put("/profiles/ct-aws-m5")
            .then()
            .statusCode(200)
            .body("description", is("Renamed"))
            .body("instanceType", is("m5.xlarge"))
            .body("location", is("PAR"))
            .body("lifespan", equalTo(87600));

        given()
            .when().delete("/profiles/ct-aws-m5")
            .then()
            .statusCode(204);

        given()
            .when().get("/profiles/ct-aws-m5")
            .then()
            .statusCode(404);
    }

    @Test
    void delete_missingProfile_returns404()
    {
        given()
            .when().delete("/profiles/does-not-exist")
            .then()
            .statusCode(404);
    }

    @Test
    void create_returnsCreatedDtoWithAllFields()
    {
        String body = """
            {
              "id": "smoke-1",
              "description": "Smoke",
              "provider": "gcp",
              "instanceType": "n2-standard-2",
              "location": "FRA",
              "lifespan": 26280
            }
            """;
        given()
            .contentType("application/json").body(body)
            .when().post("/profiles")
            .then()
            .statusCode(200)
            .body("id", is("smoke-1"))
            .body("provider", is("gcp"))
            .body("instanceType", notNullValue());

        given().when().delete("/profiles/smoke-1");
    }
}
