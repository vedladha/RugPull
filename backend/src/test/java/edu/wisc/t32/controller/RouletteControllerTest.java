package edu.wisc.t32.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import edu.wisc.t32.dto.RouletteSpinRequest;
import edu.wisc.t32.dto.RouletteSpinResponse;
import edu.wisc.t32.model.User;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.RouletteService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class RouletteControllerTest {
  private static final String VALID_TOKEN = "valid-token";

  @Mock
  private CurrentUserService currentUserService;

  @Mock
  private RouletteService rouletteService;

  @InjectMocks
  private RouletteController rouletteController;

  @Test
  void spinReturnsUnauthorizedWhenUserIsNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = rouletteController.spin(
        null,
        buildRequest("10.00", "COLOR", "RED")
    );

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
  }

  @Test
  void spinReturnsSpinResultWhenRequestIsValid() {
    RouletteSpinResponse spinResponse = new RouletteSpinResponse();
    spinResponse.setWinningNumber(1);
    spinResponse.setWinningColor("RED");
    spinResponse.setBetType("COLOR");
    spinResponse.setBetValue("RED");
    spinResponse.setWager(new BigDecimal("10.00"));
    spinResponse.setPayout(new BigDecimal("20.00"));
    spinResponse.setNetChange(new BigDecimal("10.00"));
    spinResponse.setBalance(new BigDecimal("110.00"));
    spinResponse.setWon(true);
    spinResponse.setMessage("You won. The wheel landed on 1 RED.");

    User user = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user));
    when(rouletteService.spin(user, new BigDecimal("10.00"), "COLOR", "RED"))
        .thenReturn(spinResponse);

    ResponseEntity<?> response = rouletteController.spin(
        VALID_TOKEN,
        buildRequest("10.00", "COLOR", "RED")
    );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    RouletteSpinResponse returnedResponse = (RouletteSpinResponse) body.get("spin");
    assertNotNull(returnedResponse);
    assertEquals(Integer.valueOf(1), returnedResponse.getWinningNumber());
    assertEquals("RED", returnedResponse.getWinningColor());
    assertTrue(returnedResponse.isWon());
  }

  @Test
  void spinReturnsBadRequestWhenServiceRejectsWager() {
    User user = buildUser(7);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user));
    when(rouletteService.spin(user, new BigDecimal("10.00"), "COLOR", "RED"))
        .thenThrow(new IllegalArgumentException("Insufficient balance"));

    ResponseEntity<?> response = rouletteController.spin(
        VALID_TOKEN,
        buildRequest("10.00", "COLOR", "RED")
    );

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Insufficient balance", body.get("error"));
  }

  @Test
  void spinReturnsInternalServerErrorWhenServiceFails() {
    User user = buildUser(7);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user));
    when(rouletteService.spin(user, new BigDecimal("10.00"), "COLOR", "RED"))
        .thenThrow(new IllegalStateException("Internal service error fetching wallet"));

    ResponseEntity<?> response = rouletteController.spin(
        VALID_TOKEN,
        buildRequest("10.00", "COLOR", "RED")
    );

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Internal service error fetching wallet", body.get("error"));
  }

  private RouletteSpinRequest buildRequest(String wager, String betType, String betValue) {
    RouletteSpinRequest request = new RouletteSpinRequest();
    request.setWager(new BigDecimal(wager));
    request.setBetType(betType);
    request.setBetValue(betValue);
    return request;
  }

  private User buildUser(Integer userId) {
    User user = new User();
    user.setUserId(userId);
    return user;
  }
}
