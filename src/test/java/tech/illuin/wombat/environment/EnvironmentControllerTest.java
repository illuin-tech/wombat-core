package tech.illuin.wombat.environment;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.environment.persistence.AssetRepository;
import tech.illuin.wombat.environment.persistence.EnvironmentRepository;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class EnvironmentControllerTest
{

    @Inject
    EnvironmentRepository repository;

    @Inject
    AssetRepository assetRepository;

    @AfterEach
    void restoreEnvironments()
    {
        // the seeded environment must stay active for the other @QuarkusTest classes,
        // and created rows must not leak between tests
        given().when().post("/environments/test/enable").then().statusCode(200);
        QuarkusTransaction.requiringNew().run(() -> {
            this.assetRepository.delete("id in ?1", List.of("ct-asset"));
            this.repository.delete("id in ?1", List.of("ct-env"));
        });
    }

    @Test
    void list_returnsSeededEnvironments()
    {
        given()
            .when().get("/environments")
            .then()
            .statusCode(200)
            .body("id", hasItem("test"))
            .body("find { it.id == 'test' }.name", is("Test"))
            .body("find { it.id == 'test' }.uuid", notNullValue())
            .body("find { it.id == 'test' }.assets.find { it.id == 'test-llm' }.uuid", notNullValue())
            .body("find { it.id == 'test' }.created_at", notNullValue())
            .body("find { it.id == 'test' }.disabled_at", nullValue())
            .body("find { it.id == 'test' }.assets.id", hasItem("test-cluster"))
            .body("find { it.id == 'test' }.assets.id", hasItem("test-llm"))
            .body("find { it.id == 'test' }.assets.find { it.id == 'test-cluster' }.namespace", is("test-ns"));
    }

    @Test
    void disable_hidesEnvironmentFromTheApp_untilEnabledAgain()
    {
        given()
            .when().post("/environments/test/disable")
            .then()
            .statusCode(200)
            .body("id", is("test"))
            .body("disabled_at", notNullValue());

        given()
            .when().get("/impact/environments")
            .then()
            .statusCode(200)
            .body("payload.id", not(hasItem("test")));

        given()
            .when().post("/environments/test/enable")
            .then()
            .statusCode(200)
            .body("disabled_at", nullValue());

        given()
            .when().get("/impact/environments")
            .then()
            .statusCode(200)
            .body("payload.id", hasItem("test"));
    }

    @Test
    void disable_isIdempotent()
    {
        String first = given()
            .when().post("/environments/test/disable")
            .then().statusCode(200)
            .extract().path("disabled_at");

        given()
            .when().post("/environments/test/disable")
            .then()
            .statusCode(200)
            .body("disabled_at", is(first));
    }

    @Test
    void create_persistsAnActiveEnvironment()
    {
        given()
            .contentType(JSON)
            .body("""
                {"id": "ct-env", "name": "Created Env"}
                """)
            .when().post("/environments")
            .then()
            .statusCode(200)
            .body("id", is("ct-env"))
            .body("name", is("Created Env"))
            .body("created_at", notNullValue())
            .body("disabled_at", nullValue());

        given()
            .when().get("/environments")
            .then()
            .statusCode(200)
            .body("id", hasItem("ct-env"));
    }

    @Test
    void create_existingEnvironment_returns409()
    {
        given()
            .contentType(JSON)
            .body("""
                {"id": "test", "name": "Duplicate"}
                """)
            .when().post("/environments")
            .then()
            .statusCode(409);
    }

    @Test
    void create_missingId_returns400()
    {
        given()
            .contentType(JSON)
            .body("""
                {"name": "No Id"}
                """)
            .when().post("/environments")
            .then()
            .statusCode(400);
    }

    @Test
    void addAsset_takesEffectImmediately_andRemoveUndoesIt()
    {
        given()
            .contentType(JSON)
            .body("""
                {"id": "ct-asset", "name": "CT Asset", "type": "LLM_STATIC", "profile_id": "test-mistral-large"}
                """)
            .when().post("/environments/test/assets")
            .then()
            .statusCode(200)
            .body("id", is("ct-asset"))
            .body("type", is("LLM_STATIC"))
            .body("profile_id", is("test-mistral-large"));

        given()
            .when().get("/impact/environments/test/assets")
            .then()
            .statusCode(200)
            .body("payload.id", hasItem("ct-asset"));

        given()
            .when().delete("/environments/test/assets/ct-asset")
            .then()
            .statusCode(204);

        given()
            .when().get("/impact/environments/test/assets")
            .then()
            .statusCode(200)
            .body("payload.id", not(hasItem("ct-asset")));
    }

    @Test
    void addAsset_existingId_returns409()
    {
        given()
            .contentType(JSON)
            .body("""
                {"id": "test-llm", "name": "Duplicate", "type": "LLM_STATIC", "profile_id": "test-mistral-large"}
                """)
            .when().post("/environments/test/assets")
            .then()
            .statusCode(409);
    }

    @Test
    void addAsset_profileTypeMismatch_returns400()
    {
        given()
            .contentType(JSON)
            .body("""
                {"id": "ct-asset", "name": "CT Asset", "type": "LLM_STATIC", "profile_id": "test-aws-c5"}
                """)
            .when().post("/environments/test/assets")
            .then()
            .statusCode(400);
    }

    @Test
    void addAsset_kubernetesWithoutNamespace_returns400()
    {
        given()
            .contentType(JSON)
            .body("""
                {"id": "ct-asset", "name": "CT Asset", "type": "KUBERNETES_API", "profile_id": "test-aws-c5", "config_path": "/kube/config"}
                """)
            .when().post("/environments/test/assets")
            .then()
            .statusCode(400);
    }

    @Test
    void addAsset_unknownEnvironment_returns404()
    {
        given()
            .contentType(JSON)
            .body("""
                {"id": "ct-asset", "name": "CT Asset", "type": "LLM_STATIC", "profile_id": "test-mistral-large"}
                """)
            .when().post("/environments/does-not-exist/assets")
            .then()
            .statusCode(404);
    }

    @Test
    void removeAsset_unknownAsset_returns404()
    {
        given()
            .when().delete("/environments/test/assets/does-not-exist")
            .then()
            .statusCode(404);
    }

    @Test
    void disable_unknownEnvironment_returns404()
    {
        given()
            .when().post("/environments/does-not-exist/disable")
            .then()
            .statusCode(404);
    }
}
