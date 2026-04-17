package edu.wisc.t32.dto;

import edu.wisc.t32.model.AdSession;

/**
 * DTO sent to the frontend when a user starts watching an ad.
 */
public record AdStartResponse(
    String sessionId,
    String title,
    int durationSeconds,
    float rewardAmount,
    String videoUrl
) {

  /**
   * Creates a new add start response from a sesion and reward parameter.
   *
   * @param reward  tokens earned from watching the ad
   * @param session the ad session
   * @return the response
   */
  public static AdStartResponse fromSession(float reward, String videoUrl, AdSession session) {
    return new AdStartResponse(
        session.getId(),
        session.getAdTitle(),
        session.getRequiredDurationSeconds(),
        reward,
        videoUrl
    );
  }
}
