import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import model.Data;
import model.Mobile;
import model.ProductResponse;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class PostProductsListTest {
    private final String url = "https://api.restful-api.dev/";
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testGetUserDetails() throws JsonProcessingException {
        // Set the base URI for the API
        RestAssured.baseURI = url;

        // 1. Define the Request Body (Payload)
        String requestBody = "{\n" +
                "   \"name\": \"Apple MacBook Pro 16\",\n" +
                "   \"data\": {\n" +
                "      \"year\": 2019,\n" +
                "      \"price\": 1849.99,\n" +
                "      \"CPU model\": \"Intel Core i9\",\n" +
                "      \"Hard disk size\": \"1 TB\"\n" +
                "   }\n" +
                "}";

        Mobile mobile = generateMobileData();
        String request = mapper.writeValueAsString(mobile);

        // 2. Build and Send the POST Request, then Validate the Response
        Response response = given()
                // *** Given: Set up the request (headers, body, auth) ***
                .contentType(ContentType.JSON) // Set the Content-Type header
                .body(requestBody)            // Set the request payload

                .when()
                // *** When: Perform the action (HTTP method and endpoint) ***
                .post("objects")           // Change this to your actual POST endpoint

                .then()
                // *** Then: Validate the response ***
                .statusCode(200)
                .log().all()
                .extract().response();

        System.out.println("Response: "+response.asString());
    }

    private Mobile generateMobileData(){
        Data data = new Data();
        data.setPrice(1000);
        data.setYear(2025);
        data.setCpuModel("Intel Core i9");
        data.setHardDiskSize("1 TB");

        Mobile mobile = new Mobile();
        mobile.setData(data);
        mobile.setName("iphone 17");
        return mobile;
    }

}
