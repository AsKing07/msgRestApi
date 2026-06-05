package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.FriendRequest;
import com.bschool.msgrestapi.domain.entity.Friendship;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.enums.FriendRequestStatus;
import com.bschool.msgrestapi.domain.util.UserPairUtil;
import com.bschool.msgrestapi.dto.response.ReceivedFriendRequestResponse;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.exception.ResourceNotFoundException;
import com.bschool.msgrestapi.repository.FriendRequestRepository;
import com.bschool.msgrestapi.repository.FriendshipRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.ConversationService;
import com.bschool.msgrestapi.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendRequestServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private FriendRequestRepository friendRequestRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private ConversationService conversationService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private FriendRequestServiceImpl friendRequestService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = User.builder().id(1L).build();
        userB = User.builder().id(2L).build();
    }

    // -------------------------------------------------------------------------
    // sendRequest
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("sendRequest")
    class SendRequest {

        @Test
        @DisplayName("✅ Doit créer et retourner la demande d'ami")
        void shouldCreateFriendRequest() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(userRepository.findById(2L)).thenReturn(Optional.of(userB));
            when(friendshipRepository.findByUserLowAndUserHigh(any(), any())).thenReturn(Optional.empty());
            when(friendRequestRepository.findPendingBetweenUsers(any(), any(), any())).thenReturn(Optional.empty());

            FriendRequest saved = FriendRequest.builder()
                    .id(10L)
                    .sender(userA)
                    .receiver(userB)
                    .status(FriendRequestStatus.PENDING)
                    .build();
            when(friendRequestRepository.save(any())).thenReturn(saved);

            FriendRequest result = friendRequestService.sendRequest(1L, 2L);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(FriendRequestStatus.PENDING);
            assertThat(result.getSender()).isEqualTo(userA);
            verify(notificationService).notifyFriendRequestReceived(saved);
        }

        @Test
        @DisplayName("❌ Doit lever une exception si l'utilisateur s'ajoute lui-même")
        void shouldThrowWhenSenderEqualsReceiver() {
            assertThatThrownBy(() -> friendRequestService.sendRequest(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("vous-même");
        }

        @Test
        @DisplayName("❌ Doit lever une exception si une amitié existe déjà")
        void shouldThrowWhenAlreadyFriends() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(userRepository.findById(2L)).thenReturn(Optional.of(userB));
            when(friendshipRepository.findByUserLowAndUserHigh(any(), any()))
                    .thenReturn(Optional.of(new Friendship()));

            assertThatThrownBy(() -> friendRequestService.sendRequest(1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà amis");
        }

        @Test
        @DisplayName("❌ Doit lever une exception si une demande PENDING existe déjà (envoyée par le sender)")
        void shouldThrowWhenPendingRequestAlreadySentBySender() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(userRepository.findById(2L)).thenReturn(Optional.of(userB));
            when(friendshipRepository.findByUserLowAndUserHigh(any(), any())).thenReturn(Optional.empty());

            FriendRequest existing = FriendRequest.builder().sender(userA).build();
            when(friendRequestRepository.findPendingBetweenUsers(any(), any(), any()))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> friendRequestService.sendRequest(1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà envoyé");
        }

        @Test
        @DisplayName("❌ Doit lever une exception si le receiver a déjà envoyé une demande au sender")
        void shouldThrowWhenReverseRequestAlreadyExists() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(userRepository.findById(2L)).thenReturn(Optional.of(userB));
            when(friendshipRepository.findByUserLowAndUserHigh(any(), any())).thenReturn(Optional.empty());

            // La demande existante a été envoyée par B (receiver de la nouvelle demande)
            FriendRequest existing = FriendRequest.builder().sender(userB).build();
            when(friendRequestRepository.findPendingBetweenUsers(any(), any(), any()))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> friendRequestService.sendRequest(1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà envoyé une demande d'ami");
        }

        @Test
        @DisplayName("❌ Doit lever une exception si le sender est introuvable")
        void shouldThrowWhenSenderNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> friendRequestService.sendRequest(1L, 2L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // listReceivedPending
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("listReceivedPending")
    class ListReceivedPending {

        @Test
        @DisplayName("✅ Doit retourner la liste des demandes en attente reçues")
        void shouldReturnPendingRequests() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(userB));
            when(friendRequestRepository.findReceivedPendingWithSender(eq(userB), eq(FriendRequestStatus.PENDING)))
                    .thenReturn(List.of());

            List<ReceivedFriendRequestResponse> result = friendRequestService.listReceivedPending(2L);

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("❌ Doit lever une exception si l'utilisateur est introuvable")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> friendRequestService.listReceivedPending(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // accept
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("accept")
    class Accept {

        @Test
        @DisplayName("✅ Doit accepter la demande, créer l'amitié et la conversation")
        void shouldAcceptRequest() {
            FriendRequest request = FriendRequest.builder()
                    .id(10L)
                    .sender(userA)
                    .receiver(userB)
                    .status(FriendRequestStatus.PENDING)
                    .build();

            when(friendRequestRepository.findById(10L)).thenReturn(Optional.of(request));
            when(friendshipRepository.findByUserLowAndUserHigh(any(), any())).thenReturn(Optional.empty());
            when(friendshipRepository.save(any())).thenReturn(new Friendship());
            when(conversationService.getOrCreateBetweenFriends(any(), any())).thenReturn(new Conversation());
            when(friendRequestRepository.save(any())).thenReturn(request);
            when(friendRequestRepository.findPendingBetweenUsers(any(), any(), any())).thenReturn(Optional.empty());

            FriendRequest result = friendRequestService.accept(10L, 2L);

            assertThat(result.getStatus()).isEqualTo(FriendRequestStatus.ACCEPTED);
            verify(notificationService).notifyFriendRequestAccepted(any(), any());
        }

        @Test
        @DisplayName("❌ Doit lever une exception si la demande est introuvable")
        void shouldThrowWhenRequestNotFound() {
            when(friendRequestRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> friendRequestService.accept(99L, 2L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("❌ Doit lever une exception si l'utilisateur n'est pas le receiver")
        void shouldThrowWhenNotReceiver() {
            FriendRequest request = FriendRequest.builder()
                    .id(10L)
                    .sender(userA)
                    .receiver(userB)
                    .status(FriendRequestStatus.PENDING)
                    .build();

            when(friendRequestRepository.findById(10L)).thenReturn(Optional.of(request));

            // userId = 1 alors que le receiver est 2
            assertThatThrownBy(() -> friendRequestService.accept(10L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("destinataire");
        }

        @Test
        @DisplayName("❌ Doit lever une exception si la demande n'est plus PENDING")
        void shouldThrowWhenRequestAlreadyProcessed() {
            FriendRequest request = FriendRequest.builder()
                    .id(10L)
                    .sender(userA)
                    .receiver(userB)
                    .status(FriendRequestStatus.ACCEPTED)
                    .build();

            when(friendRequestRepository.findById(10L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> friendRequestService.accept(10L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà été traitée");
        }
    }

    // -------------------------------------------------------------------------
    // decline
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("decline")
    class Decline {

        @Test
        @DisplayName("✅ Doit décliner la demande")
        void shouldDeclineRequest() {
            FriendRequest request = FriendRequest.builder()
                    .id(10L)
                    .sender(userA)
                    .receiver(userB)
                    .status(FriendRequestStatus.PENDING)
                    .build();

            when(friendRequestRepository.findById(10L)).thenReturn(Optional.of(request));
            when(friendRequestRepository.save(any())).thenReturn(request);

            FriendRequest result = friendRequestService.decline(10L, 2L);

            assertThat(result.getStatus()).isEqualTo(FriendRequestStatus.DECLINED);
        }

        @Test
        @DisplayName("❌ Doit lever une exception si la demande est introuvable")
        void shouldThrowWhenRequestNotFound() {
            when(friendRequestRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> friendRequestService.decline(99L, 2L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("❌ Doit lever une exception si l'utilisateur n'est pas le receiver")
        void shouldThrowWhenNotReceiver() {
            FriendRequest request = FriendRequest.builder()
                    .id(10L)
                    .sender(userA)
                    .receiver(userB)
                    .status(FriendRequestStatus.PENDING)
                    .build();

            when(friendRequestRepository.findById(10L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> friendRequestService.decline(10L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("destinataire");
        }
    }

    // -------------------------------------------------------------------------
    // cancel
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("✅ Doit annuler la demande si l'utilisateur est le sender")
        void shouldCancelRequest() {
            FriendRequest request = FriendRequest.builder()
                    .id(10L)
                    .sender(userA)
                    .receiver(userB)
                    .status(FriendRequestStatus.PENDING)
                    .build();

            when(friendRequestRepository.findById(10L)).thenReturn(Optional.of(request));

            friendRequestService.cancel(10L, 1L);

            assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.CANCELLED);
            verify(friendRequestRepository).save(request);
        }

        @Test
        @DisplayName("❌ Doit lever une exception si l'utilisateur n'est pas le sender")
        void shouldThrowWhenNotSender() {
            FriendRequest request = FriendRequest.builder()
                    .id(10L)
                    .sender(userA)
                    .receiver(userB)
                    .status(FriendRequestStatus.PENDING)
                    .build();

            when(friendRequestRepository.findById(10L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> friendRequestService.cancel(10L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("auteur");
        }
    }

    // -------------------------------------------------------------------------
    // listFriends
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("listFriends")
    class ListFriends {

        @Test
        @DisplayName("✅ Doit retourner la liste des amis de l'utilisateur")
        void shouldReturnFriendsList() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(friendshipRepository.findAllByUser(userA)).thenReturn(List.of(new Friendship()));

            List<Friendship> result = friendRequestService.listFriends(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("❌ Doit lever une exception si l'utilisateur est introuvable")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> friendRequestService.listFriends(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
