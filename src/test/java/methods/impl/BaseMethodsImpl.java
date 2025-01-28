package methods.impl;

import com.example.creditservice.model.error.CustomError;
import com.example.creditservice.model.request.AuthenticationRequest;
import com.example.creditservice.model.request.CreateOrder;
import com.example.creditservice.model.request.DeleteOrder;
import com.example.creditservice.model.request.RegisterRequest;
import com.example.creditservice.model.response.DataResponseLoanOrder;
import com.example.creditservice.model.response.DataResponseStatus;
import com.example.creditservice.model.response.DataResponseTariff;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import methods.BaseMethods;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.HttpURLConnection;
import java.sql.Timestamp;
import java.util.ArrayList;

import static io.restassured.RestAssured.given;

public class BaseMethodsImpl implements BaseMethods {

    @Override
    @Step("Аутентификация")
    public String authenticate(AuthenticationRequest authenticationRequest) {
        return given()
                .when()
                .contentType(ContentType.JSON)
                .body(authenticationRequest)
                .post("auth/authenticate")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .extract()
                .body()
                .jsonPath()
                .getString("token");
    }

    @Step ("Регистрация")
    public String register(RegisterRequest registerRequest) {
        return given()
                .when()
                .contentType(ContentType.JSON)
                .body(registerRequest)
                .post("auth/register")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .extract()
                .body()
                .jsonPath()
                .getString("token");
    }

    @Override
    @Step("Получение тарифов, ожидаемый статус код - 200")
    public DataResponseTariff getTariffs() {
        return given()
                .when()
                .get("loan-service/getTariffs")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .extract()
                .body()
                .jsonPath()
                .getObject("data", DataResponseTariff.class);
    }

    @Override
    @Step("Отправка заявки, ожидаемый статус код - 200")
    public DataResponseLoanOrder postOrderPositive(String token, CreateOrder createOrder) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(createOrder)
                .when()
                .post("loan-service/order")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .extract()
                .body()
                .jsonPath()
                .getObject("data", DataResponseLoanOrder.class);
    }

    @Override
    @Step("Некорректная отправка заявки")
    public CustomError postOrderNegative(String token, CreateOrder createOrder) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(createOrder)
                .when()
                .post("loan-service/order")
                .then()
                .statusCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .extract()
                .body()
                .jsonPath()
                .getObject("error", CustomError.class);
    }

    @Override
    @Step("Некорректная отправка заявки")
    public CustomError postOrderNegative(String token, JSONObject jsonObject) {
        return given()
                .header("Authorization", "Bearer " + token)
                .body(jsonObject.toString())
                .contentType("application/json")
                .when()
                .post("loan-service/order")
                .then()
                .statusCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .extract()
                .body()
                .jsonPath()
                .getObject("error", CustomError.class);
    }

    @Override
    @Step("Отправка заявки, возвращаем статус код")
    public int statusCodePostOrder(String token, CreateOrder createOrder) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(createOrder)
                .when()
                .post("loan-service/order")
                .getStatusCode();
    }

    @Override
    @Step("Получение статуса заявки")
    public DataResponseStatus getOrderStatusPositive(String token, String orderId) {
        return given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("loan-service/getStatusOrder?orderId=" + orderId)
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .extract()
                .body()
                .jsonPath()
                .getObject("data", DataResponseStatus.class);
    }

    @Override
    @Step("Отправка некорректного запроса на получение статуса заявки")
    public CustomError getOrderStatusNegative(String token, String orderId) {
        return given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("loan-service/getStatusOrder?orderId=" + orderId)
                .then()
                .statusCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .extract()
                .body()
                .jsonPath()
                .getObject("error", CustomError.class);
    }

    @Override
    @Step("Отправка запроса на получение статуса заявки, возвращаем статус код")
    public int statusCodeGetOrderStatus(String token, String orderId) {
        return given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("loan-service/getStatusOrder?orderId=" + orderId)
                .getStatusCode();
    }

    @Override
    @Step("Удаление заявки")
    public void deleteOrderPositive(String token, DeleteOrder deleteOrder) {
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(deleteOrder)
                .when()
                .delete("loan-service/deleteOrder")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK);
    }

    @Override
    @Step("Отправка некорректного запроса на удаление заявки")
    public CustomError deleteOrderNegative(String token, DeleteOrder deleteOrder) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(deleteOrder)
                .when()
                .delete("loan-service/deleteOrder")
                .then()
                .statusCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .extract()
                .body()
                .jsonPath()
                .getObject("error", CustomError.class);
    }

    @Override
    @Step("Отправка запроса на удаление заявки, возвращаем статус код")
    public int statusCodeDeleteOrder(String token, DeleteOrder deleteOrder) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(deleteOrder)
                .when()
                .delete("loan-service/deleteOrder")
                .getStatusCode();
    }

    @Override
    @Step("Установка статуса заявки")
    public void setStatus(JdbcTemplate jdbcTemplate, String status, String orderId) {
        jdbcTemplate.update(
                "UPDATE loan_order SET status = ?, time_update=? WHERE order_id=?",
                status,
                new Timestamp(System.currentTimeMillis()),
                orderId
        );
    }

    @Override
    @Step("Установка статуса заявки")
    public void setStatus(JdbcTemplate jdbcTemplate, String status, String orderId, int time) {
        jdbcTemplate.update(
                "UPDATE loan_order SET status = ?, time_update=? WHERE order_id=?",
                status,
                new Timestamp(System.currentTimeMillis() - time),
                orderId
        );
    }

    @Override
    @Step("Удаление заявки из базы данных")
    public void deleteOrderByOrderId(JdbcTemplate jdbcTemplate, String orderId) {
        jdbcTemplate.update("DELETE FROM loan_order WHERE order_id =?", orderId);
    }

    @Override
    @Step("Проверка удаления заявки из базы данных")
    public int checkingDelete(JdbcTemplate jdbcTemplate, String orderId) {
        return jdbcTemplate.queryForObject("SELECT count(id) FROM loan_order WHERE order_id =?", Integer.class, orderId);
    }

    @Override
    @Step
    public String addNewUser(RegisterRequest registerRequest) {
        given()
                .when()
                .contentType(ContentType.JSON)
                .body(registerRequest)
                .post("auth/register");

        return authenticate(new AuthenticationRequest(registerRequest.getEmail(), registerRequest.getPassword()));
    }

    @Override
    @Step("Установка роли пользователя - ROLE_ADMIN")
    public void setRole(JdbcTemplate jdbcTemplate, int userId) {
        if (!jdbcTemplate.queryForObject("SELECT role FROM users WHERE id =?", String.class, userId).equals("ROLE_ADMIN")) {
            jdbcTemplate.update("UPDATE users SET role='ROLE_ADMIN' WHERE id=?", userId);
        }
    }

    public ArrayList<String> createUsers() {
        ArrayList<String> tokenList = new ArrayList<>();
        tokenList.add(authenticate(new AuthenticationRequest("ivanov@mail.ru", "1234")));
        tokenList.add(addNewUser(new RegisterRequest("Иван", "Сидоров", "sidorov@mail.ru", "12345")));
        tokenList.add(addNewUser(new RegisterRequest("Петр", "Петров", "petrov@mail.ru", "123456")));
        tokenList.add(addNewUser(new RegisterRequest("Вика", "Элекина", "elekina@mail.ru", "1234567")));
        tokenList.add(addNewUser(new RegisterRequest("Иван", "Васильевич", "vasiliev@mail.ru", "12345678")));
        tokenList.add(addNewUser(new RegisterRequest("Роман", "Романов", "romanov@mail.ru", "123456789")));
        tokenList.add(addNewUser(new RegisterRequest("Степан", "Степанов", "stepanov@mail.ru", "1234567891")));
        tokenList.add(addNewUser(new RegisterRequest("Петр", "Смирнов", "smirnov@mail.ru", "98765")));
        return tokenList;
    }


}