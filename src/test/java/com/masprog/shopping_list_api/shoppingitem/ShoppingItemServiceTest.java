package com.masprog.shopping_list_api.shoppingitem;

import com.masprog.shopping_list_api.product.Product;
import com.masprog.shopping_list_api.product.ProductRepository;
import com.masprog.shopping_list_api.product.ProductService;
import com.masprog.shopping_list_api.product.exception.ProductNotFoundException;
import com.masprog.shopping_list_api.shoppingitem.exception.ProductAlreadyInListException;
import com.masprog.shopping_list_api.shoppingitem.exception.ShoppingItemNotFoundException;
import com.masprog.shopping_list_api.shoppingitem.exception.ShoppingItemValidationException;
import com.masprog.shopping_list_api.shoppinglist.ShoppingList;
import com.masprog.shopping_list_api.shoppinglist.ShoppingListRepository;
import com.masprog.shopping_list_api.shoppinglist.ShoppingListStatus;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.masprog.shopping_list_api.shoppinglist.exception.ShoppingListValidationException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ShoppingItemServiceTest {

    @Mock
    private ShoppingItemRepository shoppingItemRepository;

    @Mock
    private ShoppingListRepository shoppingListRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ShoppingItemService shoppingItemService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
    }

    @Test
    void shouldAddExistingProductToActiveShoppingList() {

        // Arrange
        Long listId = 10L;
        Long productId = 20L;

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(user);

        Product product = new Product();
        product.setId(productId);
        product.setUser(user);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(listId, user.getId()))
                .thenReturn(Optional.of(shoppingList));

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(shoppingItemRepository.save(any(ShoppingItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShoppingItem result = shoppingItemService.addExistingProduct(
                listId,
                productId,
                new BigDecimal("2.5"),
                Unit.KG,
                user.getEmail()
        );

        // Assert
        assertThat(result.getShoppingList()).isEqualTo(shoppingList);
        assertThat(result.getProduct()).isEqualTo(product);

        assertThat(result.getQuantity())
                .isEqualByComparingTo("2.5");

        assertThat(result.getUnit()).isEqualTo(Unit.KG);
        assertThat(result.isChecked()).isFalse();
        assertThat(result.getCheckedAt()).isNull();

        verify(shoppingItemRepository).save(any(ShoppingItem.class));
    }
    @Test
    void shouldRejectAddingProductToCompletedShoppingList() {

        // Arrange
        Long listId = 10L;

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
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
                shoppingItemService.addExistingProduct(
                        listId,
                        20L,
                        BigDecimal.ONE,
                        Unit.UNIT,
                        user.getEmail()
                )
        )
                .isInstanceOf(ShoppingListValidationException.class)
                .hasMessage("Only active shopping lists can be modified");

        verifyNoInteractions(productRepository);

        verify(shoppingItemRepository, never())
                .save(any(ShoppingItem.class));
    }

    @Test
    void shouldRejectAddingAnotherUsersProductToShoppingList() {

        // Arrange
        Long listId = 10L;
        Long productId = 20L;

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(user);

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        Product product = new Product();
        product.setId(productId);
        product.setUser(otherUser);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId,
                user.getId()
        )).thenReturn(Optional.of(shoppingList));

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingItemService.addExistingProduct(
                        listId,
                        productId,
                        BigDecimal.ONE,
                        Unit.UNIT,
                        user.getEmail()
                )
        )
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found");

        verify(shoppingItemRepository, never())
                .save(any(ShoppingItem.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-1", "-2.5"})
    void shouldRejectAddingShoppingItemWithInvalidQuantity(String quantity) {

        // Arrange
        BigDecimal invalidQuantity =
                quantity == null ? null : new BigDecimal(quantity);

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingItemService.addExistingProduct(
                        10L,
                        20L,
                        invalidQuantity,
                        Unit.KG,
                        user.getEmail()
                )
        )
                .isInstanceOf(ShoppingItemValidationException.class)
                .hasMessage("Shopping item quantity must be greater than zero");

        verifyNoInteractions(shoppingItemRepository);
        verifyNoInteractions(shoppingListRepository);
        verifyNoInteractions(productRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectAddingShoppingItemWithoutUnit() {

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingItemService.addExistingProduct(
                        10L,
                        20L,
                        BigDecimal.ONE,
                        null,
                        user.getEmail()
                )
        )
                .isInstanceOf(ShoppingItemValidationException.class)
                .hasMessage("Shopping item unit is required");

        verifyNoInteractions(
                userRepository,
                shoppingListRepository,
                productRepository,
                shoppingItemRepository
        );
    }

    @Test
    void shouldRejectAddingDuplicateProductToShoppingList() {

        // Arrange
        Long listId = 10L;
        Long productId = 20L;

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(user);

        Product product = new Product();
        product.setId(productId);
        product.setUser(user);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId, user.getId()
        )).thenReturn(Optional.of(shoppingList));

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(shoppingItemRepository.existsByShoppingListIdAndProductId(
                listId, productId
        )).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingItemService.addExistingProduct(
                        listId,
                        productId,
                        BigDecimal.ONE,
                        Unit.UNIT,
                        user.getEmail()
                )
        )
                .isInstanceOf(ProductAlreadyInListException.class)
                .hasMessage("Product already exists in shopping list");

        verify(shoppingItemRepository, never())
                .save(any(ShoppingItem.class));
    }

    // PATCH

    @Test
    void shouldUpdateShoppingItemQuantityAndUnit() {

        // Arrange
        Long listId = 10L;
        Long itemId = 20L;
        String email = "test@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setUser(user);
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);

        ShoppingItem item = new ShoppingItem();
        item.setId(itemId);
        item.setShoppingList(shoppingList);
        item.setQuantity(BigDecimal.ONE);
        item.setUnit(Unit.UNIT);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId, user.getId()
        )).thenReturn(Optional.of(shoppingList));

        when(shoppingItemRepository.findByIdAndShoppingListId(itemId, listId))
                .thenReturn(Optional.of(item));

        // Act
        ShoppingItem result = shoppingItemService.update(
                listId,
                itemId,
                new BigDecimal("2.5"),
                Unit.KG,
                email
        );

        // Assert
        assertThat(result.getId()).isEqualTo(itemId);
        assertThat(result.getQuantity())
                .isEqualByComparingTo("2.5");
        assertThat(result.getUnit()).isEqualTo(Unit.KG);
    }


    @Test
    void shouldRejectUpdatingItemFromAnotherShoppingList() {

        // Arrange
        Long listId = 10L;
        Long itemId = 20L;
        String email = "test@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setUser(user);
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId, user.getId()
        )).thenReturn(Optional.of(shoppingList));

        // O item não pertence à lista indicada
        when(shoppingItemRepository.findByIdAndShoppingListId(itemId, listId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingItemService.update(
                        listId,
                        itemId,
                        new BigDecimal("2.5"),
                        Unit.KG,
                        email
                )
        )
                .isInstanceOf(ShoppingItemNotFoundException.class)
                .hasMessage("Shopping item not found");

        // Nenhum item deve ser alterado
        verify(shoppingItemRepository, never()).save(any(ShoppingItem.class));
    }

    @Test
    void shouldRejectUpdatingItemWhenShoppingListIsCompleted() {

        // Arrange
        Long listId = 10L;
        Long itemId = 20L;
        String email = "test@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setUser(user);
        shoppingList.setStatus(ShoppingListStatus.COMPLETED);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId,
                user.getId()
        )).thenReturn(Optional.of(shoppingList));

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingItemService.update(
                        listId,
                        itemId,
                        new BigDecimal("2.5"),
                        Unit.KG,
                        email
                )
        )
                .isInstanceOf(ShoppingListValidationException.class)
                .hasMessage("Only active shopping lists can be modified");

        // O item nem sequer deve ser consultado
        verify(shoppingItemRepository, never())
                .findByIdAndShoppingListId(anyLong(), anyLong());

        // Nenhum item deve ser gravado
        verify(shoppingItemRepository, never())
                .save(any(ShoppingItem.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-1", "-2.5"})
    void shouldRejectUpdatingShoppingItemWithInvalidQuantity(
            String quantity
    ) {

        // Arrange
        Long listId = 10L;
        Long itemId = 20L;
        String email = "test@example.com";

        BigDecimal invalidQuantity = quantity == null
                ? null
                : new BigDecimal(quantity);

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingItemService.update(
                        listId,
                        itemId,
                        invalidQuantity,
                        Unit.KG,
                        email
                )
        )
                .isInstanceOf(ShoppingItemValidationException.class)
                .hasMessage(
                        "Shopping item quantity must be greater than zero"
                );

        // Nenhuma consulta ou gravação deve ser realizada
        verifyNoInteractions(
                userRepository,
                shoppingListRepository,
                shoppingItemRepository
        );
    }

    @Test
    void shouldDeleteShoppingItemWithoutDeletingProduct() {

        // Arrange
        Long listId = 10L;
        Long itemId = 20L;
        String email = "test@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setUser(user);
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);

        Product product = new Product();
        product.setId(30L);
        product.setName("Arroz");
        product.setUser(user);

        ShoppingItem item = new ShoppingItem();
        item.setId(itemId);
        item.setShoppingList(shoppingList);
        item.setProduct(product);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId, user.getId()
        )).thenReturn(Optional.of(shoppingList));

        when(shoppingItemRepository.findByIdAndShoppingListId(
                itemId, listId
        )).thenReturn(Optional.of(item));

        // Act
        shoppingItemService.delete(listId, itemId, email);

        // Assert
        verify(shoppingItemRepository).delete(item);

        // O produto não deve ser removido do catálogo
        verify(productRepository, never())
                .delete(any(Product.class));
    }


    @Test
    void shouldRejectDeletingItemFromCompletedShoppingList() {

        // Arrange
        Long listId = 10L;
        Long itemId = 20L;
        String email = "test@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setUser(user);
        shoppingList.setStatus(ShoppingListStatus.COMPLETED);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId, user.getId()
        )).thenReturn(Optional.of(shoppingList));

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingItemService.delete(listId, itemId, email)
        )
                .isInstanceOf(ShoppingListValidationException.class)
                .hasMessage("Only active shopping lists can be modified");

        verify(shoppingItemRepository, never())
                .findByIdAndShoppingListId(anyLong(), anyLong());

        verify(shoppingItemRepository, never())
                .delete(any(ShoppingItem.class));
    }

    @Test
    void shouldCheckShoppingItem() {

        // Arrange
        Long listId = 10L;
        Long itemId = 20L;
        String email = "test@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setUser(user);
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);

        ShoppingItem item = new ShoppingItem();
        item.setId(itemId);
        item.setShoppingList(shoppingList);
        item.setChecked(false);
        item.setCheckedAt(null);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId, user.getId()
        )).thenReturn(Optional.of(shoppingList));

        when(shoppingItemRepository.findByIdAndShoppingListId(
                itemId, listId
        )).thenReturn(Optional.of(item));

        // Act
        ShoppingItem result = shoppingItemService.check(
                listId,
                itemId,
                email
        );

        // Assert
        assertThat(result.isChecked()).isTrue();
        assertThat(result.getCheckedAt()).isNotNull();

        verify(shoppingItemRepository)
                .findByIdAndShoppingListId(itemId, listId);
    }

    @Test
    void shouldUncheckShoppingItem() {

        // Arrange
        Long listId = 10L;
        Long itemId = 20L;
        String email = "test@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setUser(user);
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);

        ShoppingItem item = new ShoppingItem();
        item.setId(itemId);
        item.setShoppingList(shoppingList);
        item.setChecked(true);
        item.setCheckedAt(LocalDateTime.now().minusMinutes(10));

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId, user.getId()
        )).thenReturn(Optional.of(shoppingList));

        when(shoppingItemRepository.findByIdAndShoppingListId(
                itemId, listId
        )).thenReturn(Optional.of(item));

        // Act
        ShoppingItem result = shoppingItemService.uncheck(
                listId,
                itemId,
                email
        );

        // Assert
        assertThat(result.isChecked()).isFalse();
        assertThat(result.getCheckedAt()).isNull();

        verify(shoppingItemRepository)
                .findByIdAndShoppingListId(itemId, listId);
    }

    @Test
    void shouldPreserveCheckedAtWhenShoppingItemIsAlreadyChecked() {

        // Arrange
        Long listId = 10L;
        Long itemId = 20L;
        String email = "test@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setUser(user);
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);

        LocalDateTime originalCheckedAt =
                LocalDateTime.now().minusMinutes(10);

        ShoppingItem item = new ShoppingItem();
        item.setId(itemId);
        item.setShoppingList(shoppingList);
        item.setChecked(true);
        item.setCheckedAt(originalCheckedAt);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId, user.getId()
        )).thenReturn(Optional.of(shoppingList));

        when(shoppingItemRepository.findByIdAndShoppingListId(
                itemId, listId
        )).thenReturn(Optional.of(item));

        // Act
        ShoppingItem result = shoppingItemService.check(
                listId,
                itemId,
                email
        );

        // Assert
        assertThat(result.isChecked()).isTrue();

        assertThat(result.getCheckedAt())
                .isEqualTo(originalCheckedAt);
    }

    @Test
    void shouldKeepShoppingItemUncheckedWhenAlreadyUnchecked() {

        // Arrange
        Long listId = 10L;
        Long itemId = 20L;
        String email = "test@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setId(listId);
        shoppingList.setUser(user);
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);

        ShoppingItem item = new ShoppingItem();
        item.setId(itemId);
        item.setShoppingList(shoppingList);
        item.setChecked(false);
        item.setCheckedAt(null);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(shoppingListRepository.findByIdAndUserId(
                listId, user.getId()
        )).thenReturn(Optional.of(shoppingList));

        when(shoppingItemRepository.findByIdAndShoppingListId(
                itemId, listId
        )).thenReturn(Optional.of(item));

        // Act
        ShoppingItem result = shoppingItemService.uncheck(
                listId,
                itemId,
                email
        );

        // Assert
        assertThat(result.isChecked()).isFalse();
        assertThat(result.getCheckedAt()).isNull();

        verify(shoppingItemRepository)
                .findByIdAndShoppingListId(itemId, listId);

        verify(shoppingItemRepository, never())
                .save(any(ShoppingItem.class));
    }
}