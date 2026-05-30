package cloneproject.Instagram.infra.aws;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

@Slf4j
@Service
public class EventBridgeService {

    private static final String SOURCE = "com.f2d.instagram";
    private static final String EVENT_BUS_NAME = "F2d-event-bus";

    private final EventBridgeClient eventBridgeClient;
    private final ObjectMapper objectMapper;

    public EventBridgeService(ObjectMapper objectMapper) {
        this.eventBridgeClient = EventBridgeClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
        this.objectMapper = objectMapper;
    }

    public void publishPostLiked(Long postId, Long memberId, String memberUsername, Long targetMemberId) {
        publish("post.liked", Map.of(
                "postId", postId,
                "memberId", memberId,
                "likerUsername", memberUsername,
                "targetMemberId", targetMemberId
        ));
    }

    public void publishUserFollowed(Long memberId, String memberUsername, Long followMemberId) {
        publish("user.followed", Map.of(
                "memberId", memberId,
                "followerUsername", memberUsername,
                "followMemberId", followMemberId
        ));
    }

    public void publishDmSent(Long senderId, String senderUsername, Long receiverId) {
        publish("dm.sent", Map.of(
                "senderId", senderId,
                "senderUsername", senderUsername,
                "receiverId", receiverId
        ));
    }

    public void publishCommentCreated(Long postId, Long commentId, Long memberId, String memberUsername, Long targetMemberId) {
        publish("comment.created", Map.of(
                "postId", postId,
                "commentId", commentId,
                "memberId", memberId,
                "commenterUsername", memberUsername,
                "targetMemberId", targetMemberId
        ));
    }

    private void publish(String detailType, Map<String, Object> detail) {
        try {
            String detailJson = objectMapper.writeValueAsString(detail);
            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                    .source(SOURCE)
                    .detailType(detailType)
                    .detail(detailJson)
                    .eventBusName(EVENT_BUS_NAME)
                    .build();
            eventBridgeClient.putEvents(PutEventsRequest.builder()
                    .entries(entry)
                    .build());
            log.info("EventBridge published. detailType={}", detailType);
        } catch (Exception e) {
            // EventBridge 실패해도 알림 저장에는 영향 없음
            log.error("EventBridge publish failed. detailType={}", detailType, e);
        }
    }
}