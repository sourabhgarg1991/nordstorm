package restassured;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class SampleApiTest {

    @Test
    public void testGetUserDetails() {
        // Set the base URI for the API
        RestAssured.baseURI = "https://jsonplaceholder.typicode.com";

        // Perform a GET request to a specific endpoint
        Response response = given()
                .when()
                .get("/users/1") // Request details for user with ID 1
                .then()
                .statusCode(200) // Assert that the status code is 200 (OK)
                .body("id", equalTo(1)) // Assert the 'id' field in the response body
                .body("name", notNullValue()) // Assert that 'name' field is not null
                .body("email", containsString("@")) // Assert that 'email' contains '@'
                .extract().response(); // Extract the response object for further assertions if needed

        // Print the response body for debugging or verification
        System.out.println("Response body:\n" + response.asString());
    }

    @Test
    public void testGetAllUsers() {
        RestAssured.baseURI = "https://jsonplaceholder.typicode.com";

        given()
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0)) // Assert that the response is an array with more than zero elements
                .body("[0].username", equalTo("Bret")); // Assert the username of the first user
    }
}