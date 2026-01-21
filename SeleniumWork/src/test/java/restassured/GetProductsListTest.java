package restassured;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import restassured.model.ProductResponse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class GetProductsListTest {
    private final String url = "https://automationexercise.com/";
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testGetUserDetails() throws JsonProcessingException {
        // Set the base URI for the API
        RestAssured.baseURI = url;

        // Perform a GET request to a specific endpoint
        Response response = given()
                .when()
                .get("/api/productsList") // Request details for user with ID 1
                .then()
                .log().ifValidationFails()
                .statusCode(200)
//                .body("products[1].name", equalTo(1))// Assert that the status code is 200 (OK)
                .extract().response(); // Extract the response object for further assertions if needed

x
        restassured.model.ProductResponse productResponse = mapper.readValue(response.asString(), ProductResponse.class);

//        assert response.asString().equals("");
        assert productResponse.getProducts().size()==34;
        assert productResponse.getProducts().getFirst().getId()==1;

        // Print the response body for debugging or verification
        System.out.println("Response body:\n" + response.asString());
    }

}

