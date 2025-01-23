package methods;

import com.example.creditservice.model.error.CustomError;
import com.example.creditservice.model.request.AuthenticationRequest;
import com.example.creditservice.model.request.CreateOrder;
import com.example.creditservice.model.request.DeleteOrder;
import com.example.creditservice.model.request.RegisterRequest;
import com.example.creditservice.model.response.DataResponseLoanOrder;
import com.example.creditservice.model.response.DataResponseStatus;
import com.example.creditservice.model.response.DataResponseTariff;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;

public interface BaseMethods {
    String authenticate(AuthenticationRequest authenticationRequest);
    String register(RegisterRequest registerRequest);
    DataResponseTariff getTariffs();
    DataResponseLoanOrder postOrderPositive(String token, CreateOrder createOrder);
    CustomError postOrderNegative(String token, CreateOrder createOrder);
    CustomError postOrderNegative(String token, JSONObject jsonObject);
    int statusCodePostOrder(String token, CreateOrder createOrder);
    DataResponseStatus getOrderStatusPositive(String token, String orderId);
    CustomError getOrderStatusNegative(String token, String orderId);
    int statusCodeGetOrderStatus(String token, String orderId);
    void deleteOrderPositive(String token, DeleteOrder deleteOrder);
    CustomError deleteOrderNegative(String token, DeleteOrder deleteOrder);
    int statusCodeDeleteOrder(String token, DeleteOrder deleteOrder);
    void setStatus(JdbcTemplate jdbcTemplate, String status, String orderId);
    void setStatus(JdbcTemplate jdbcTemplate, String status, String orderId, int time);
    void deleteOrderByOrderId(JdbcTemplate jdbcTemplate, String orderId);
    int checkingDelete(JdbcTemplate jdbcTemplate, String orderId);
    String addNewUser(RegisterRequest registerRequest);
    void setRole(JdbcTemplate jdbcTemplate, int userId);

}
