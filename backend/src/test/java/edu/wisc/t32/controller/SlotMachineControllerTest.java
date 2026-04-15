package edu.wisc.t32.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import edu.wisc.t32.dto.SlotSpinRequest;
import edu.wisc.t32.dto.SlotSpinResponse;
import edu.wisc.t32.model.User;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.SlotMachineService;
import java.math.BigDecimal;
import java.util.List;
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
class SlotMachineControllerTest {
  private static final String VALID_TOKEN = "valid-token";

  @Mock
  private CurrentUserService currentUserService;

  @Mock
  private SlotMachineService slotMachineService;

  @InjectMocks
  private SlotMachineController slotMachineController;

  @Test
  void spinReturnsUnauthorizedWhenUserIsNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = slotMachineController.spin(null, buildRequest("10.00"));

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
  }

  @Test
  void spinReturnsSpinResultWhenRequestIsValid() {
    SlotSpinResponse spinResponse = new SlotSpinResponse();
    spinResponse.setReels(List.of("SEVEN", "SEVEN", "SEVEN"));
    spinResponse.setWager(new BigDecimal("10.00"));
    spinResponse.setPayout(new BigDecimal("100.00"));
    spinResponse.setNetChange(new BigDecimal("90.00"));
    spinResponse.setBalance(new BigDecimal("190.00"));
    spinResponse.setWon(true);
    spinResponse.setMessage("You won!");

    User user = buildUser(7);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user));
    when(slotMachineService.spin(user, new BigDecimal("10.00"))).thenReturn(spinResponse);

    ResponseEntity<?> response = slotMachineController.spin(VALID_TOKEN, buildRequest("10.00"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    SlotSpinResponse returnedResponse = (SlotSpinResponse) body.get("spin");
    assertNotNull(returnedResponse);
    assertEquals(new BigDecimal("100.00"), returnedResponse.getPayout());
    assertEquals(true, returnedResponse.isWon());
  }

  @Test
  void spinReturnsBadRequestWhenServiceRejectsWager() {
    User user = buildUser(7);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user));
    when(slotMachineService.spin(user, new BigDecimal("10.00")))
        .thenThrow(new IllegalArgumentException("Insufficient balance"));

    ResponseEntity<?> response = slotMachineController.spin(VALID_TOKEN, buildRequest("10.00"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Insufficient balance", body.get("error"));
  }

  private SlotSpinRequest buildRequest(String wager) {
    SlotSpinRequest request = new SlotSpinRequest();
    request.setWager(new BigDecimal(wager));
    return request;
  }

  private User buildUser(Integer userId) {
    User user = new User();
    user.setUserId(userId);
    return user;
  }
}
