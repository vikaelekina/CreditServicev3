package com.example.creditservice;


import com.example.creditservice.model.enums.OrderStatus;
import com.example.creditservice.model.error.CustomError;
import com.example.creditservice.model.request.CreateOrder;
import com.example.creditservice.model.request.DeleteOrder;
import com.example.creditservice.model.response.DataResponseStatus;
import io.restassured.RestAssured;
import jdk.jfr.Description;
import methods.impl.BaseMethodsImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Stream;

@SpringBootTest
class CreditServiceApplicationTests {
    private JdbcTemplate jdbcTemplate;
    private static BaseMethodsImpl baseMethods = new BaseMethodsImpl();
    private static ArrayList <String> tokenList = new ArrayList<>();


    @Autowired
    public CreditServiceApplicationTests(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = "http://localhost:8080/";
        tokenList = baseMethods.createUsers();
    }

    @Test
    @Tag("Smoke")
    @Tag("Regress")
    @DisplayName("Проверка получения тарифов")
    @Description("Тест проверяет возможность получения тарифов")
    public void getTariffs() {
        Assertions.assertFalse(baseMethods.getTariffs().getTariffs().isEmpty());
    }

    @Test
    @Tag("Smoke")
    @Tag("Regress")
    @DisplayName("Проверка подачи заявки админом")
    @Description("Тест проверяет возможность отправки заявки на кредит пользователем с ролью ADMIN")
    public void submitApplicationAdmin() {
        int userID = 1;
        int tariffID = 1;
        String token = tokenList.get(userID-1);
        baseMethods.setRole(jdbcTemplate,userID);
        UUID orderId = baseMethods.postOrderPositive(token, new CreateOrder(userID, tariffID)).getOrderId();
        Assertions.assertFalse(orderId.toString().isEmpty());
        baseMethods.deleteOrderPositive(token, new DeleteOrder(userID,orderId));
    }

    @ParameterizedTest(name = "Проверка подачи дубликата заявки админом, статус начальной заявки {index}:{0}")
    @ArgumentsSource(DublicateApplicationArgumentsProvider.class)
    @Tag("Regress")
    @DisplayName("Проверка подачи дубликата заявки админом")
    @Description("Тест проверяет, что пользователь с ролью ADMIN не может подать дубликат заявки, если первоначальная заявка находится на рассмотрении, одобрена или с момента ее отклонения прошло недостаточно времени")
    public void submitDublicateApplicationAdmin(OrderStatus status, CustomError customError, int tariffId) {
        int userID = 2;
        String token = tokenList.get(userID-1);
        baseMethods.setRole(jdbcTemplate,userID);
        CreateOrder createOrder = new CreateOrder(userID, tariffId);
        UUID orderId = baseMethods.postOrderPositive(token, createOrder).getOrderId();
        baseMethods.setStatus(jdbcTemplate, status.name(), orderId.toString());
        Assertions.assertEquals(customError, baseMethods.postOrderNegative(token, createOrder));
        baseMethods.deleteOrderByOrderId(jdbcTemplate, orderId.toString());
    }

    static class DublicateApplicationArgumentsProvider implements ArgumentsProvider {
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(OrderStatus.IN_PROGRESS.name(), new CustomError("LOAN_CONSIDERATION", "Заявка на рассмотрении"), 1),
                    Arguments.of(OrderStatus.APPROVED.name(), new CustomError("LOAN_ALREADY_APPROVED", "Заявка уже одобрена"),2),
                    Arguments.of(OrderStatus.REFUSED.name(), new CustomError("TRY_LATER", "Попробуйте позже"),3)
            );
        }
    }

    @Test
    @Tag("Regress")
    @DisplayName("Проверка подачи дубликата отклоненной заявки спустя время админом")
    @Description("Тест проверяет, что пользователь с ролью ADMIN может успешно подать дубликат заявки, если с момента отклонения первой прошло заданное время")
    public void submitDublicateApplicationLaterAdmin() {
        int userID = 1;
        int tariffID = 2;
        String token = tokenList.get(userID-1);
        baseMethods.setRole(jdbcTemplate,userID);
        CreateOrder createOrder = new CreateOrder(userID, tariffID);
        UUID orderId = baseMethods.postOrderPositive(token, createOrder).getOrderId();
        baseMethods.setStatus(jdbcTemplate, OrderStatus.REFUSED.name(), orderId.toString(),120000);
        UUID orderId2 = baseMethods.postOrderPositive(token, createOrder).getOrderId();
        Assertions.assertFalse(orderId2.toString().isEmpty());
        baseMethods.deleteOrderByOrderId(jdbcTemplate, orderId.toString());
        baseMethods.deleteOrderPositive(token,new DeleteOrder(userID,orderId2));
    }

    @Test
    @Tag("Smoke")
    @Tag("Regress")
    @DisplayName("Проверка подачи заявки на другого пользователя админом")
    @Description("Тест проверят, что пользователь с ролью ADMIN не может подать заявку на кредит на другого пользователя ")
    public void submitApplicationOtherUserAdmin(){
        int userID = 1;
        int tariffID = 3;
        String token = tokenList.get(userID);
        baseMethods.setRole(jdbcTemplate,userID+1);
        Assertions.assertEquals(HttpURLConnection.HTTP_FORBIDDEN,baseMethods.statusCodePostOrder(token, new CreateOrder(userID,tariffID)));
    }

    @Test
    @Tag("Smoke")
    @Tag("Regress")
    @DisplayName("Проверка получения статуса заявки админом")
    @Description("Тест проверяет возможность получения статуса заявки пользователем с ролью ADMIN")
    public void getStatusApplicationAdmin() {
        int userID = 3;
        int tariffID = 1;
        String token = tokenList.get(userID-1);
        baseMethods.setRole(jdbcTemplate,userID);
        UUID orderId = baseMethods.postOrderPositive(token, new CreateOrder(userID, tariffID)).getOrderId();
        DataResponseStatus dataResponseStatus = baseMethods.getOrderStatusPositive(token, orderId.toString());
        Assertions.assertFalse(dataResponseStatus.getOrderStatus().name().isEmpty());
        baseMethods.deleteOrderPositive(token, new DeleteOrder(userID,orderId));
    }

    @Test
    @Tag("Regress")
    @DisplayName("Проверка получения статуса несуществующей заявки админом")
    @Description("Тест проверяет, что при попытке получения статуса пользователем с ролью админ несуществующей заявки выдается соответствующая ошибка")
    public void getStatusInvalidApplicationAdmin(){
        int userID = 3;
        int tariffID = 2;
        String token = tokenList.get(userID-1);
        baseMethods.setRole(jdbcTemplate,userID);
        String orderId = baseMethods.postOrderPositive(token, new CreateOrder(userID,tariffID)).getOrderId().toString();
        baseMethods.deleteOrderByOrderId(jdbcTemplate, orderId);
        Assertions.assertEquals(new CustomError("ORDER_NOT_FOUND","Заявка не найдена"), baseMethods.getOrderStatusNegative(token, orderId));
    }

    @Test
    @Tag("Smoke")
    @Tag("Regress")
    @DisplayName("Проверка удаления заявки админом")
    @Description("Тест проверяет, что пользователь с ролью ADMIN может удалить, сделанную заявку, пока она находится на рассмотрении")
    public void deleteApplicationAdmin() {
        int userID = 3;
        int tariffID = 3;
        String token = tokenList.get(userID-1);
        baseMethods.setRole(jdbcTemplate,userID);
        UUID orderId = baseMethods.postOrderPositive(token, new CreateOrder(userID, tariffID)).getOrderId();
        baseMethods.deleteOrderPositive(token, new DeleteOrder(userID, orderId));
        Assertions.assertEquals(0, baseMethods.checkingDelete(jdbcTemplate, orderId.toString()));
    }

    @ParameterizedTest(name = "Проверка удаления админом обработанный заявки со статусом: {index}:{0}")
    @EnumSource(value = OrderStatus.class, names = {"APPROVED", "REFUSED"})
    @Tag("Regress")
    @DisplayName("Проверка удаления обработанной заявки админом")
    @Description("Пользователь с ролью ADMIN не может удалить заявку, если ее уже одобрили или отклонили")
    public void deleteAlrdeadyProcessedSubmitAdmin(OrderStatus orderStatus) {
        int userID = 4;
        int tariffID = 1;
        String token = tokenList.get(userID-1);
        baseMethods.setRole(jdbcTemplate,userID);
        UUID orderId = baseMethods.postOrderPositive(token, new CreateOrder(userID, tariffID)).getOrderId();
        baseMethods.setStatus(jdbcTemplate, orderStatus.name(), orderId.toString());
        Assertions.assertEquals(new CustomError("ORDER_IMPOSSIBLE_TO_DELETE", "Невозможно удалить заявку"), baseMethods.deleteOrderNegative(token, new DeleteOrder(userID, orderId)));
        baseMethods.deleteOrderByOrderId(jdbcTemplate, orderId.toString());
    }

    @Test
    @Tag("Regress")
    @DisplayName("Проверка удаления заявки другого пользователя админом")
    public void deleteOtherUserApplicationAdmin(){
        int userID = 4;
        int tariffID = 2;
        String token = tokenList.get(userID-1);
        baseMethods.setRole(jdbcTemplate,userID);
        String token2 = tokenList.get(userID);
        baseMethods.setRole(jdbcTemplate,userID+1);
        UUID orderId = baseMethods.postOrderPositive(token, new CreateOrder(userID,tariffID)).getOrderId();
        Assertions.assertEquals(HttpURLConnection.HTTP_FORBIDDEN, baseMethods.statusCodeDeleteOrder(token2, new DeleteOrder(userID, orderId)));
    }

    @Test
    @Tag("Smoke")
    @Tag("Regress")
    @DisplayName("Проверка подачи заявки пользователя")
    @Description("Тест проверяет возможность отправки заявки на кредит пользователем с ролью USER")
    public void submitApplicationUSER() {
        int userID = 5;
        int tariffID = 1;
        String token = tokenList.get(userID-1);
        UUID orderId = baseMethods.postOrderPositive(token, new CreateOrder(userID, tariffID)).getOrderId();
        Assertions.assertFalse(orderId.toString().isEmpty());
        baseMethods.deleteOrderPositive(token, new DeleteOrder(userID,orderId));
    }
    @ParameterizedTest(name = "Проверка подачи дубликата заявки пользователем, статус начальной заявки {index}:{0}")
    @ArgumentsSource(DublicateApplicationArgumentsProvider.class)
    @Tag("Regress")
    @DisplayName("Проверка подачи дубликата заявки пользователем")
    @Description("Тест проверяет, что пользователь с ролью USER не может подать дубликат заявки, если первоначальная заявка находится на рассмотрении, одобрена или с момента ее отклонения прошло недостаточно времени")
    public void submitDublicateApplication(OrderStatus status, CustomError customError, int tariffId) {
        int userID = 6;
        String token = tokenList.get(userID-1);
        CreateOrder createOrder = new CreateOrder(userID, tariffId);
        UUID orderId = baseMethods.postOrderPositive(token, createOrder).getOrderId();
        baseMethods.setStatus(jdbcTemplate, status.name(), orderId.toString());
        Assertions.assertEquals(customError, baseMethods.postOrderNegative(token, createOrder));
        baseMethods.deleteOrderByOrderId(jdbcTemplate, orderId.toString());
    }

    @Test
    @Tag("Regress")
    @DisplayName("Проверка подачи дубликата отклоненной заявки спустя время пользователем")
    @Description("Тест проверяет, что пользователь с ролью USER может успешно подать дубликат заявки, если с момента отклонения первой прошло заданное время")
    public void submitDublicateApplicationLater() {
        int userID = 5;
        int tariffID = 2;
        String token = tokenList.get(userID-1);
        CreateOrder createOrder = new CreateOrder(userID, tariffID);
        UUID orderId = baseMethods.postOrderPositive(token, createOrder).getOrderId();
        baseMethods.setStatus(jdbcTemplate, OrderStatus.REFUSED.name(), orderId.toString(),120000);
        UUID orderId2 = baseMethods.postOrderPositive(token, createOrder).getOrderId();
        Assertions.assertFalse(orderId2.toString().isEmpty());
        baseMethods.deleteOrderByOrderId(jdbcTemplate, orderId.toString());
        baseMethods.deleteOrderPositive(token,new DeleteOrder(userID,orderId2));
    }

    @Test
    @Tag("Smoke")
    @Tag("Regress")
    @DisplayName("Проверка подачи заявки на другого пользователя пользователем")
    @Description("Тест проверят, что пользователь с ролью USER не может подать заявку на кредит на другого пользователя ")
    public void submitApplicationOtherUser(){
        int userID = 5;
        int tariffID = 3;
        String token = tokenList.get(userID);
        Assertions.assertEquals(baseMethods.statusCodePostOrder(token, new CreateOrder(userID,tariffID)), HttpURLConnection.HTTP_FORBIDDEN);
    }

    @Test
    @Tag("Smoke")
    @Tag("Regress")
    @DisplayName("Проверка получения статуса заявки пользователем")
    @Description("Тест проверяет возможность получения статуса заявки пользователем с ролью USER")
    public void getStatusApplicationUSER() {
        int userID = 7;
        int tariffID = 1;
        String token = tokenList.get(userID-1);
        UUID orderId = baseMethods.postOrderPositive(token, new CreateOrder(userID, tariffID)).getOrderId();
        DataResponseStatus dataResponseStatus = baseMethods.getOrderStatusPositive(token, orderId.toString());
        Assertions.assertFalse(dataResponseStatus.getOrderStatus().name().isEmpty());
        baseMethods.deleteOrderPositive(token, new DeleteOrder(userID,orderId));
    }

    @Test
    @Tag("Regress")
    @DisplayName("Проверка получения статуса несуществующей заявки пользователем")
    @Description("Тест проверяет, что при попытке получения статуса пользователем с ролью USER несуществующей заявки выдается соответствующая ошибка")
    public void getStatusInvalidApplication(){
        int userID = 7;
        int tariffID = 2;
        String token = tokenList.get(userID-1);
        String orderId = baseMethods.postOrderPositive(token, new CreateOrder(userID,tariffID)).getOrderId().toString();
        baseMethods.deleteOrderByOrderId(jdbcTemplate, orderId);
        Assertions.assertEquals(new CustomError("ORDER_NOT_FOUND","Заявка не найдена"), baseMethods.getOrderStatusNegative(token, orderId));
    }

    @Test
    @Tag("Smoke")
    @Tag("Regress")
    @DisplayName("Проверка удаления заявки пользователем")
    @Description("Тест проверяет, что пользователь с ролью USER может удалить, сделанную заявку, пока она находится на рассмотрении")
    public void deleteApplication() {
        int userID = 7;
        int tariffID = 3;
        String token = tokenList.get(userID-1);
        UUID orderId = baseMethods.postOrderPositive(token, new CreateOrder(userID, tariffID)).getOrderId();
        baseMethods.deleteOrderPositive(token, new DeleteOrder(userID, orderId));
        Assertions.assertEquals(0, baseMethods.checkingDelete(jdbcTemplate, orderId.toString()));
    }

    @ParameterizedTest(name = "Проверка удаления пользователем обработанный заявки со статусом: {index}:{0}")
    @EnumSource(value = OrderStatus.class, names = {"APPROVED", "REFUSED"})
    @Tag("Regress")
    @DisplayName("Проверка удаления обработанной заявки пользователем")
    @Description("Пользователь с ролью USER не может удалить заявку, если ее уже одобрили или отклонили")
    public void deleteAlrdeadyProcessedSubmit(OrderStatus orderStatus) {
        int userID = 8;
        int tariffID = 1;
        String token = tokenList.get(userID-1);
        UUID orderId = baseMethods.postOrderPositive(token, new CreateOrder(userID, tariffID)).getOrderId();
        baseMethods.setStatus(jdbcTemplate, orderStatus.name(), orderId.toString());
        Assertions.assertEquals(new CustomError("ORDER_IMPOSSIBLE_TO_DELETE", "Невозможно удалить заявку"), baseMethods.deleteOrderNegative(token, new DeleteOrder(userID, orderId)));
        baseMethods.deleteOrderByOrderId(jdbcTemplate, orderId.toString());
    }

    @Test
    @Tag("Regress")
    @DisplayName("Проверка удаления заявки другого пользователя")
    public void deleteOtherUserApplication(){
        int userID = 8;
        int tariffID = 2;
        String token = tokenList.get(userID-1);
        baseMethods.setRole(jdbcTemplate,userID-2);
        String token2 = tokenList.get(userID-2);
        UUID orderId = baseMethods.postOrderPositive(token, new CreateOrder(userID,tariffID)).getOrderId();
        Assertions.assertEquals(HttpURLConnection.HTTP_FORBIDDEN, baseMethods.statusCodeDeleteOrder(token2, new DeleteOrder(userID, orderId)));
        baseMethods.deleteOrderPositive(token, new DeleteOrder(userID,orderId));
    }
}
