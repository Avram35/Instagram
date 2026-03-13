package com.instagram.follow_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.instagram.follow_service.dto.FollowCountDto;
import com.instagram.follow_service.dto.FollowRequestDto;
import com.instagram.follow_service.dto.UserProfileDto;
import com.instagram.follow_service.entity.Follow;
import com.instagram.follow_service.entity.FollowRequest;
import com.instagram.follow_service.entity.FollowRequest.RequestStatus;
import com.instagram.follow_service.entity.Notification;
import com.instagram.follow_service.repository.FollowRepository;
import com.instagram.follow_service.repository.FollowRequestRepository;
import com.instagram.follow_service.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock private FollowRepository followRepository;
    @Mock private FollowRequestRepository followRequestRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private FollowService followService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(followService, "internalApiKey", "test-key");
    }

    // ---------------- follow() ----------------------

    @Nested
    @DisplayName("follow() testovi")
    class FollowTests {

        @Test
        @DisplayName("Uspesno zapracivanje javnog profila")
        void follow_publicProfile_shouldCreateFollow() {
            UserProfileDto publicUser = new UserProfileDto();
            publicUser.setPrivateProfile(false);

            when(restTemplate.exchange(contains("check-either"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", false)));
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class))).thenReturn(publicUser);
            when(followRepository.save(any(Follow.class))).thenReturn(new Follow());
            when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

            Map<String, String> result = followService.follow(1L, 2L);

            assertNotNull(result);
            assertTrue(result.get("message").contains("запратили"));
            verify(followRepository).save(any(Follow.class));
        }

        @Test
        @DisplayName("Zapracivanje privatnog profila salje zahtev")
        void follow_privateProfile_shouldCreateFollowRequest() {
            UserProfileDto privateUser = new UserProfileDto();
            privateUser.setPrivateProfile(true);

            when(restTemplate.exchange(contains("check-either"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", false)));
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class))).thenReturn(privateUser);
            when(followRequestRepository.existsBySenderIdAndReceiverIdAndStatus(1L, 2L, RequestStatus.PENDING)).thenReturn(false);
            when(followRequestRepository.findBySenderIdAndReceiverId(1L, 2L)).thenReturn(Optional.empty());
            when(followRequestRepository.save(any(FollowRequest.class))).thenReturn(new FollowRequest());
            when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

            Map<String, String> result = followService.follow(1L, 2L);

            assertNotNull(result);
            assertTrue(result.get("message").contains("Захтев"));
            verify(followRequestRepository).save(any(FollowRequest.class));
            verify(followRepository, never()).save(any(Follow.class));
        }

        @Test
        @DisplayName("Pracenje samog sebe baca IllegalArgumentException")
        void follow_self_shouldThrowIllegalArgument() {
            when(restTemplate.exchange(contains("check-either"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", false)));

            assertThrows(IllegalArgumentException.class, () -> followService.follow(1L, 1L));
        }

        @Test
        @DisplayName("Vec prati korisnika baca IllegalStateException")
        void follow_alreadyFollowing_shouldThrowIllegalState() {
            when(restTemplate.exchange(contains("check-either"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", false)));
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

            assertThrows(IllegalStateException.class, () -> followService.follow(1L, 2L));
        }

        @Test
        @DisplayName("Blokiran korisnik baca IllegalStateException")
        void follow_blocked_shouldThrowIllegalState() {
            when(restTemplate.exchange(contains("check-either"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", true)));

            assertThrows(IllegalStateException.class, () -> followService.follow(1L, 2L));
        }

        @Test
        @DisplayName("Blok servis nedostupan - dozvoljava pracenje")
        void follow_blockServiceDown_shouldAllowFollow() {
            UserProfileDto publicUser = new UserProfileDto();
            publicUser.setPrivateProfile(false);

            when(restTemplate.exchange(contains("check-either"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new RuntimeException("Connection refused"));
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class))).thenReturn(publicUser);
            when(followRepository.save(any(Follow.class))).thenReturn(new Follow());
            when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

            assertDoesNotThrow(() -> followService.follow(1L, 2L));
        }

        @Test
        @DisplayName("Dupli zahtev za privatni profil baca IllegalStateException")
        void follow_duplicateRequest_shouldThrowIllegalState() {
            UserProfileDto privateUser = new UserProfileDto();
            privateUser.setPrivateProfile(true);

            when(restTemplate.exchange(contains("check-either"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", false)));
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class))).thenReturn(privateUser);
            when(followRequestRepository.existsBySenderIdAndReceiverIdAndStatus(1L, 2L, RequestStatus.PENDING)).thenReturn(true);

            assertThrows(IllegalStateException.class, () -> followService.follow(1L, 2L));
        }

        @Test
        @DisplayName("User servis nedostupan baca RuntimeException")
        void follow_userServiceDown_shouldThrowRuntime() {
            when(restTemplate.exchange(contains("check-either"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", false)));
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            assertThrows(RuntimeException.class, () -> followService.follow(1L, 2L));
        }

        @Test
        @DisplayName("Privatni profil - postoji stari zahtev, brise se i kreira novi")
        void follow_privateProfile_existingOldRequest_shouldDeleteAndCreate() {
            UserProfileDto privateUser = new UserProfileDto();
            privateUser.setPrivateProfile(true);

            FollowRequest oldRequest = FollowRequest.builder()
                    .id(99L).senderId(1L).receiverId(2L).status(RequestStatus.REJECTED).build();

            when(restTemplate.exchange(contains("check-either"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", false)));
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class))).thenReturn(privateUser);
            when(followRequestRepository.existsBySenderIdAndReceiverIdAndStatus(1L, 2L, RequestStatus.PENDING)).thenReturn(false);
            when(followRequestRepository.findBySenderIdAndReceiverId(1L, 2L)).thenReturn(Optional.of(oldRequest));
            when(followRequestRepository.save(any(FollowRequest.class))).thenReturn(new FollowRequest());
            when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

            followService.follow(1L, 2L);

            verify(followRequestRepository).delete(oldRequest);
            verify(followRequestRepository).save(any(FollowRequest.class));
        }
    }

    // ==================== unfollow() ====================

    @Nested
    @DisplayName("unfollow() testovi")
    class UnfollowTests {

        @Test
        @DisplayName("Uspesno otpracivanje kada follow postoji")
        void unfollow_existingFollow_shouldDelete() {
            Follow follow = Follow.builder().id(1L).followerId(1L).followingId(2L).build();

            when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.of(follow));

            followService.unfollow(1L, 2L);

            verify(followRepository).delete(follow);
            verify(notificationRepository).deleteBySenderIdAndRecipientId(1L, 2L);
        }

        @Test
        @DisplayName("Otpracivanje kada postoji pending zahtev")
        void unfollow_existingRequest_shouldDeleteRequest() {
            FollowRequest request = FollowRequest.builder()
                    .id(1L).senderId(1L).receiverId(2L).status(RequestStatus.PENDING).build();

            when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.empty());
            when(followRequestRepository.findBySenderIdAndReceiverId(1L, 2L)).thenReturn(Optional.of(request));

            followService.unfollow(1L, 2L);

            verify(followRequestRepository).delete(request);
            verify(notificationRepository).deleteBySenderIdAndRecipientId(1L, 2L);
        }

        @Test
        @DisplayName("Otpracivanje kada ne prati i nema zahteva - ne baca exception")
        void unfollow_notFollowing_shouldNotThrow() {
            when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.empty());
            when(followRequestRepository.findBySenderIdAndReceiverId(1L, 2L)).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> followService.unfollow(1L, 2L));
        }
    }

    // -------------------- removeFollower() --------------------

    @Nested
    @DisplayName("removeFollower() testovi")
    class RemoveFollowerTests {

        @Test
        @DisplayName("Uspesno uklanjanje pratioce")
        void removeFollower_shouldDelete() {
            Follow follow = Follow.builder().id(1L).followerId(2L).followingId(1L).build();

            when(followRepository.findByFollowerIdAndFollowingId(2L, 1L)).thenReturn(Optional.of(follow));
            when(followRequestRepository.findBySenderIdAndReceiverId(2L, 1L)).thenReturn(Optional.empty());

            followService.removeFollower(1L, 2L);

            verify(followRepository).delete(follow);
            verify(notificationRepository).deleteBySenderIdAndRecipientId(2L, 1L);
        }

        @Test
        @DisplayName("Uklanjanje korisnika koji ne prati baca RuntimeException")
        void removeFollower_notFollowing_shouldThrow() {
            when(followRepository.findByFollowerIdAndFollowingId(2L, 1L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> followService.removeFollower(1L, 2L));
        }
    }

    // -------------------- acceptRequest() ------------------------

    @Nested
    @DisplayName("acceptRequest() testovi")
    class AcceptRequestTests {

        @Test
        @DisplayName("Uspesno prihvatanje zahteva")
        void acceptRequest_shouldCreateFollow() {
            FollowRequest request = FollowRequest.builder()
                    .id(1L).senderId(1L).receiverId(2L)
                    .status(RequestStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(followRequestRepository.findById(1L)).thenReturn(Optional.of(request));
            when(followRequestRepository.save(any())).thenReturn(request);
            when(followRepository.save(any(Follow.class))).thenReturn(new Follow());
            when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

            followService.acceptRequest(1L, 2L);

            verify(followRepository).save(any(Follow.class));
            verify(followRequestRepository).save(argThat(r -> r.getStatus() == RequestStatus.ACCEPTED));
        }

        @Test
        @DisplayName("Zahtev ne postoji baca RuntimeException")
        void acceptRequest_notFound_shouldThrow() {
            when(followRequestRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> followService.acceptRequest(99L, 2L));
        }

        @Test
        @DisplayName("Prihvatanje tudjem zahteva baca IllegalArgumentException")
        void acceptRequest_wrongUser_shouldThrow() {
            FollowRequest request = FollowRequest.builder()
                    .id(1L).senderId(1L).receiverId(2L).status(RequestStatus.PENDING).build();

            when(followRequestRepository.findById(1L)).thenReturn(Optional.of(request));

            assertThrows(IllegalArgumentException.class, () -> followService.acceptRequest(1L, 99L));
        }

        @Test
        @DisplayName("Zahtev vec obradjeni baca IllegalStateException")
        void acceptRequest_alreadyProcessed_shouldThrow() {
            FollowRequest request = FollowRequest.builder()
                    .id(1L).senderId(1L).receiverId(2L).status(RequestStatus.ACCEPTED).build();

            when(followRequestRepository.findById(1L)).thenReturn(Optional.of(request));

            assertThrows(IllegalStateException.class, () -> followService.acceptRequest(1L, 2L));
        }
    }

    // -------------------- rejectRequest() ----------------------

    @Nested
    @DisplayName("rejectRequest() testovi")
    class RejectRequestTests {

        @Test
        @DisplayName("Uspesno odbijanje zahteva")
        void rejectRequest_shouldSetRejected() {
            FollowRequest request = FollowRequest.builder()
                    .id(1L).senderId(1L).receiverId(2L).status(RequestStatus.PENDING).build();

            when(followRequestRepository.findById(1L)).thenReturn(Optional.of(request));
            when(followRequestRepository.save(any())).thenReturn(request);

            followService.rejectRequest(1L, 2L);

            verify(followRequestRepository).save(argThat(r -> r.getStatus() == RequestStatus.REJECTED));
            verify(notificationRepository).deleteBySenderIdAndRecipientId(1L, 2L);
        }

        @Test
        @DisplayName("Zahtev ne postoji baca RuntimeException")
        void rejectRequest_notFound_shouldThrow() {
            when(followRequestRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> followService.rejectRequest(99L, 2L));
        }

        @Test
        @DisplayName("Odbijanje tudjem zahteva baca IllegalArgumentException")
        void rejectRequest_wrongUser_shouldThrow() {
            FollowRequest request = FollowRequest.builder()
                    .id(1L).senderId(1L).receiverId(2L).status(RequestStatus.PENDING).build();

            when(followRequestRepository.findById(1L)).thenReturn(Optional.of(request));

            assertThrows(IllegalArgumentException.class, () -> followService.rejectRequest(1L, 99L));
        }
    }

    // ---------------------- acceptAllPendingRequests() --------------------

    @Nested
    @DisplayName("acceptAllPendingRequests() testovi")
    class AcceptAllTests {

        @Test
        @DisplayName("Prihvata sve pending zahteve")
        void acceptAllPending_shouldCreateFollowForEach() {
            FollowRequest r1 = FollowRequest.builder()
                    .id(1L).senderId(10L).receiverId(1L)
                    .status(RequestStatus.PENDING).createdAt(LocalDateTime.now()).build();
            FollowRequest r2 = FollowRequest.builder()
                    .id(2L).senderId(20L).receiverId(1L)
                    .status(RequestStatus.PENDING).createdAt(LocalDateTime.now()).build();

            when(followRequestRepository.findByReceiverIdAndStatus(1L, RequestStatus.PENDING))
                    .thenReturn(List.of(r1, r2));
            when(followRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(followRepository.save(any())).thenReturn(new Follow());
            when(notificationRepository.save(any())).thenReturn(new Notification());

            followService.acceptAllPendingRequests(1L);

            verify(followRepository, times(2)).save(any(Follow.class));
        }

        @Test
        @DisplayName("Nema pending zahteva - ne radi nista")
        void acceptAllPending_noRequests_shouldDoNothing() {
            when(followRequestRepository.findByReceiverIdAndStatus(1L, RequestStatus.PENDING))
                    .thenReturn(List.of());

            followService.acceptAllPendingRequests(1L);

            verify(followRepository, never()).save(any());
        }
    }

    // ------------------ getFollowers / getFollowing --------------------

    @Nested
    @DisplayName("getFollowers / getFollowing testovi")
    class ListTests {

        @Test
        @DisplayName("getFollowers vraca listu")
        void getFollowers_shouldReturnList() {
            Follow f = Follow.builder().id(1L).followerId(2L).followingId(1L)
                    .createdAt(LocalDateTime.now()).build();
            when(followRepository.findByFollowingId(1L)).thenReturn(List.of(f));

            var result = followService.getFollowers(1L);

            assertEquals(1, result.size());
            assertEquals(2L, result.get(0).getFollowerId());
        }

        @Test
        @DisplayName("getFollowing vraca listu")
        void getFollowing_shouldReturnList() {
            Follow f = Follow.builder().id(1L).followerId(1L).followingId(2L)
                    .createdAt(LocalDateTime.now()).build();
            when(followRepository.findByFollowerId(1L)).thenReturn(List.of(f));

            var result = followService.getFollowing(1L);

            assertEquals(1, result.size());
            assertEquals(2L, result.get(0).getFollowingId());
        }
    }

    // ------------------- getFollowCount ---------------------

    @Test
    @DisplayName("getFollowCount vraca tacne brojeve")
    void getFollowCount_shouldReturnCorrectCounts() {
        when(followRepository.countByFollowingId(1L)).thenReturn(5L);
        when(followRepository.countByFollowerId(1L)).thenReturn(3L);

        FollowCountDto result = followService.getFollowCount(1L);

        assertEquals(5L, result.getFollowersCount());
        assertEquals(3L, result.getFollowingCount());
        assertEquals(1L, result.getUserId());
    }

    // ---------------------- isFollowing -----------------------

    @Test
    @DisplayName("isFollowing vraca true kada prati")
    void isFollowing_shouldReturnTrue() {
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

        assertTrue(followService.isFollowing(1L, 2L));
    }

    @Test
    @DisplayName("isFollowing vraca false kada ne prati")
    void isFollowing_shouldReturnFalse() {
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);

        assertFalse(followService.isFollowing(1L, 2L));
    }

    // --------------------- canViewFollowList -----------------------

    @Nested
    @DisplayName("canViewFollowList testovi")
    class CanViewFollowListTests {

        @Test
        @DisplayName("Javan profil - svako moze videti listu")
        void canViewFollowList_publicProfile_shouldReturnTrue() {
            UserProfileDto dto = new UserProfileDto();
            dto.setPrivateProfile(false);

            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class))).thenReturn(dto);

            assertTrue(followService.canViewFollowList(2L, 1L));
        }

        @Test
        @DisplayName("Privatan profil - pratilac moze videti listu")
        void canViewFollowList_privateProfile_follower_shouldReturnTrue() {
            UserProfileDto dto = new UserProfileDto();
            dto.setPrivateProfile(true);

            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class))).thenReturn(dto);
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

            assertTrue(followService.canViewFollowList(2L, 1L));
        }

        @Test
        @DisplayName("Privatan profil - onaj ko ne prati korisnika ne moze videti listu")
        void canViewFollowList_privateProfile_notFollower_shouldReturnFalse() {
            UserProfileDto dto = new UserProfileDto();
            dto.setPrivateProfile(true);

            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class))).thenReturn(dto);
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);

            assertFalse(followService.canViewFollowList(2L, 1L));
        }

        @Test
        @DisplayName("Vlasnik profila uvek moze videti svoju listu")
        void canViewFollowList_ownProfile_shouldReturnTrue() {
            UserProfileDto dto = new UserProfileDto();
            dto.setPrivateProfile(true);

            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class))).thenReturn(dto);

            assertTrue(followService.canViewFollowList(1L, 1L));
        }

        @Test
        @DisplayName("currentUserId null - privatan profil vraca false")
        void canViewFollowList_nullCurrentUser_privateProfile_shouldReturnFalse() {
            UserProfileDto dto = new UserProfileDto();
            dto.setPrivateProfile(true);

            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class))).thenReturn(dto);

            assertFalse(followService.canViewFollowList(2L, null));
        }
    }
    // -------------------------- getFollowStatus ---------------------

    @Nested
    @DisplayName("getFollowStatus testovi")
    class GetFollowStatusTests {

        @Test
        @DisplayName("Vraca status pracenja i pending zahteva")
        void getFollowStatus_shouldReturnCorrectStatus() {
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);
            when(followRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
                    1L, 2L, RequestStatus.PENDING)).thenReturn(false);

            Map<String, Boolean> status = followService.getFollowStatus(1L, 2L);

            assertTrue(status.get("following"));
            assertFalse(status.get("pending"));
        }

        @Test
        @DisplayName("Vraca false za pracenje i true za pending")
        void getFollowStatus_pendingRequest_shouldReturnCorrect() {
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);
            when(followRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
                    1L, 2L, RequestStatus.PENDING)).thenReturn(true);

            Map<String, Boolean> status = followService.getFollowStatus(1L, 2L);

            assertFalse(status.get("following"));
            assertTrue(status.get("pending"));
        }
    }
    // ---------------------- hasPendingRequest ----------------------

    @Test
    @DisplayName("hasPendingRequest vraca true kada postoji zahtev")
    void hasPendingRequest_shouldReturnTrue() {
        when(followRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
                1L, 2L, RequestStatus.PENDING)).thenReturn(true);

        assertTrue(followService.hasPendingRequest(1L, 2L));
    }

    @Test
    @DisplayName("hasPendingRequest vraca false kada nema zahteva")
    void hasPendingRequest_shouldReturnFalse() {
        when(followRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
                1L, 2L, RequestStatus.PENDING)).thenReturn(false);

        assertFalse(followService.hasPendingRequest(1L, 2L));
    }

    // --------------------- getPendingRequests -----------------------

    @Test
    @DisplayName("getPendingRequests vraca listu zahteva")
    void getPendingRequests_shouldReturnList() {
        FollowRequest r = FollowRequest.builder()
                .id(1L).senderId(2L).receiverId(1L)
                .status(RequestStatus.PENDING).createdAt(LocalDateTime.now()).build();
        when(followRequestRepository.findByReceiverIdAndStatus(1L, RequestStatus.PENDING))
                .thenReturn(List.of(r));

        List<FollowRequestDto> result = followService.getPendingRequests(1L);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getSenderId());
    }

    // ---------------- deleteAllByUserId --------------------

    @Test
    @DisplayName("deleteAllByUserId brise sve podatke korisnika")
    void deleteAllByUserId_shouldDeleteEverything() {
        followService.deleteAllByUserId(1L);

        verify(followRepository).deleteByFollowerIdOrFollowingId(1L, 1L);
        verify(followRequestRepository).deleteBySenderIdOrReceiverId(1L, 1L);
        verify(notificationRepository).deleteByRecipientIdOrSenderId(1L, 1L);
    }
}