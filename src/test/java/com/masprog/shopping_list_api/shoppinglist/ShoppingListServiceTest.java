package com.masprog.shopping_list_api.shoppinglist;

import com.masprog.shopping_list_api.auth.exception.AuthenticatedUserNotFoundException;
import com.masprog.shopping_list_api.shoppinglist.exception.ShoppingListNotFoundException;
import com.masprog.shopping_list_api.shoppinglist.exception.ShoppingListValidationException;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

    @Mock
    private ShoppingListRepository shoppingListRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ShoppingListService shoppingListService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
    }

    // CREATE

    @Test
    void shouldCreateShoppingListWithActiveStatus() {

        // Arrange
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.save(any(ShoppingList.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShoppingList result = shoppingListService.create(
                "Compras de Outubro",
                user.getEmail()
        );

        // Assert
        assertThat(result.getName()).isEqualTo("Compras de Outubro");
        assertThat(result.getStatus()).isEqualTo(ShoppingListStatus.ACTIVE);
        assertThat(result.getUser().getId()).isEqualTo(user.getId());
        assertThat(result.getCompletedAt()).isNull();
    }
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectShoppingListCreationWhenNameIsBlank(String name) {

        assertThatThrownBy(() ->
                shoppingListService.create(name, user.getEmail())
        )
                .isInstanceOf(ShoppingListValidationException.class)
                .hasMessage("Shopping list name is required");

        verifyNoInteractions(shoppingListRepository);
    }
    @Test
    void shouldNormalizeShoppingListNameWhenCreating() {

        // Arrange
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.save(any(ShoppingList.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShoppingList result = shoppingListService.create(
                "  Compras   de   Outubro  ",
                user.getEmail()
        );

        // Assert
        assertThat(result.getName())
                .isEqualTo("Compras de Outubro");

        assertThat(result.getStatus())
                .isEqualTo(ShoppingListStatus.ACTIVE);

        verify(shoppingListRepository).save(any(ShoppingList.class));
    }
    @Test
    void shouldRejectShoppingListCreationWhenAuthenticatedUserDoesNotExist() {

        // Arrange
        String email = "unknown@example.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingListService.create("Compras de Outubro", email)
        )
                .isInstanceOf(AuthenticatedUserNotFoundException.class)
                .hasMessage("Authenticated user not found");

        verify(shoppingListRepository, never()).save(any(ShoppingList.class));
    }

    // GET ALL, FILTER AND PAGINATION
    @Test
    void shouldReturnOnlyAuthenticatedUsersShoppingLists() {

        // Arrange
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(10L);
        shoppingList.setName("Compras de Outubro");
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(user);

        Pageable pageable = PageRequest.of(0, 10);

        when(shoppingListRepository.findByUserId(user.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(shoppingList)));

        // Act
        Page<ShoppingList> result = shoppingListService.findAll(
                user.getEmail(),
                pageable
        );

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName())
                .isEqualTo("Compras de Outubro");

        verify(shoppingListRepository)
                .findByUserId(user.getId(), pageable);
    }

    @Test
    void shouldFilterAuthenticatedUsersShoppingListsByStatus() {

        // Arrange
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        ShoppingList completedList = new ShoppingList();
        completedList.setId(10L);
        completedList.setName("Compras de Setembro");
        completedList.setStatus(ShoppingListStatus.COMPLETED);
        completedList.setUser(user);

        Pageable pageable = PageRequest.of(0, 10);

        when(shoppingListRepository.findByUserIdAndStatus(
                user.getId(),
                ShoppingListStatus.COMPLETED,
                pageable
        )).thenReturn(new PageImpl<>(List.of(completedList)));

        // Act
        Page<ShoppingList> result = shoppingListService.findAll(
                user.getEmail(),
                ShoppingListStatus.COMPLETED,
                pageable
        );

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus())
                .isEqualTo(ShoppingListStatus.COMPLETED);

        verify(shoppingListRepository).findByUserIdAndStatus(
                user.getId(),
                ShoppingListStatus.COMPLETED,
                pageable
        );
    }

    // GET BY ID
    @Test
    void shouldFindOwnShoppingListById() {

        // Arrange
        Long listId = 10L;

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setName("Compras de Outubro");
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(user);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId,
                user.getId()
        )).thenReturn(Optional.of(shoppingList));

        // Act
        ShoppingList result = shoppingListService.findById(
                listId,
                user.getEmail()
        );

        // Assert
        assertThat(result.getId()).isEqualTo(listId);
        assertThat(result.getName()).isEqualTo("Compras de Outubro");
        assertThat(result.getStatus()).isEqualTo(ShoppingListStatus.ACTIVE);
        assertThat(result.getUser().getId()).isEqualTo(user.getId());

        verify(shoppingListRepository).findByIdAndUserId(
                listId,
                user.getId()
        );
    }
    @Test
    void shouldRejectAccessToAnotherUsersShoppingList() {

        // Arrange
        Long listId = 10L;

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        // A lista existe, mas não pertence ao utilizador autenticado.
        when(shoppingListRepository.findByIdAndUserId(
                listId,
                user.getId()
        )).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingListService.findById(listId, user.getEmail())
        )
                .isInstanceOf(ShoppingListNotFoundException.class)
                .hasMessage("Shopping list not found");

        verify(shoppingListRepository).findByIdAndUserId(
                listId,
                user.getId()
        );
    }

    // PATCH

    @Test
    void shouldRenameOwnActiveShoppingList() {

        // Arrange
        Long listId = 10L;

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setName("Compras de Setembro");
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(user);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId,
                user.getId()
        )).thenReturn(Optional.of(shoppingList));

        // Act
        ShoppingList result = shoppingListService.rename(
                listId,
                "  Compras   de   Outubro  ",
                user.getEmail()
        );

        // Assert
        assertThat(result.getName())
                .isEqualTo("Compras de Outubro");

        assertThat(result.getStatus())
                .isEqualTo(ShoppingListStatus.ACTIVE);

        assertThat(result.getId())
                .isEqualTo(listId);

        verify(shoppingListRepository).findByIdAndUserId(
                listId,
                user.getId()
        );
    }
    @Test
    void shouldRejectRenamingCompletedShoppingList() {

        // Arrange
        Long listId = 10L;

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setName("Compras de Setembro");
        shoppingList.setStatus(ShoppingListStatus.COMPLETED);
        shoppingList.setUser(user);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId,
                user.getId()
        )).thenReturn(Optional.of(shoppingList));

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingListService.rename(
                        listId,
                        "Compras de Outubro",
                        user.getEmail()
                )
        )
                .isInstanceOf(ShoppingListValidationException.class)
                .hasMessage("Only active shopping lists can be modified");

        assertThat(shoppingList.getName())
                .isEqualTo("Compras de Setembro");

        verify(shoppingListRepository).findByIdAndUserId(
                listId,
                user.getId()
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectRenamingShoppingListWithInvalidName(String name) {

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingListService.rename(
                        10L,
                        name,
                        user.getEmail()
                )
        )
                .isInstanceOf(ShoppingListValidationException.class)
                .hasMessage("Shopping list name is required");

        verifyNoInteractions(shoppingListRepository);
        verifyNoInteractions(userRepository);
    }

    // DELETE

    @Test
    void shouldDeleteOwnShoppingList() {

        // Arrange
        Long listId = 10L;

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setName("Compras de Outubro");
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(user);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId,
                user.getId()
        )).thenReturn(Optional.of(shoppingList));

        // Act
        shoppingListService.delete(listId, user.getEmail());

        // Assert
        verify(shoppingListRepository).findByIdAndUserId(
                listId,
                user.getId()
        );

        verify(shoppingListRepository).delete(shoppingList);
    }

    @Test
    void shouldRejectDeletingAnotherUsersShoppingList() {

        // Arrange
        Long listId = 10L;

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        // A lista não pertence ao utilizador autenticado
        when(shoppingListRepository.findByIdAndUserId(
                listId,
                user.getId()
        )).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingListService.delete(listId, user.getEmail())
        )
                .isInstanceOf(ShoppingListNotFoundException.class)
                .hasMessage("Shopping list not found");

        verify(shoppingListRepository).findByIdAndUserId(
                listId,
                user.getId()
        );

        // Garantir que nenhuma lista foi eliminada
        verify(shoppingListRepository, never())
                .delete(any(ShoppingList.class));
    }

    // PATCH

    @Test
    void shouldRejectCompletingEmptyShoppingList() {

        // Arrange
        Long listId = 10L;

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setName("Compras de Outubro");
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(user);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId,
                user.getId()
        )).thenReturn(Optional.of(shoppingList));

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingListService.complete(listId, user.getEmail())
        )
                .isInstanceOf(ShoppingListValidationException.class)
                .hasMessage("Cannot complete an empty shopping list");

        assertThat(shoppingList.getStatus())
                .isEqualTo(ShoppingListStatus.ACTIVE);

        assertThat(shoppingList.getCompletedAt())
                .isNull();
    }


}