package com.masprog.shopping_list_api.shoppingitem;

import com.masprog.shopping_list_api.category.Category;
import com.masprog.shopping_list_api.category.CategoryRepository;
import com.masprog.shopping_list_api.category.CategoryType;
import com.masprog.shopping_list_api.config.JpaAuditingConfig;
import com.masprog.shopping_list_api.product.Product;
import com.masprog.shopping_list_api.product.ProductRepository;
import com.masprog.shopping_list_api.shoppinglist.ShoppingList;
import com.masprog.shopping_list_api.shoppinglist.ShoppingListRepository;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaAuditingConfig.class)
class ShoppingItemRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private ShoppingItemRepository shoppingItemRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldInitializeShoppingItemWithDefaultValues() {

        ShoppingItem item = new ShoppingItem();

        assertThat(item.getQuantity())
                .isEqualByComparingTo(BigDecimal.ONE);

        assertThat(item.isChecked()).isFalse();
        assertThat(item.getCheckedAt()).isNull();
    }

    @Test
    void shouldPersistShoppingItem() {

        // Arrange: utilizador
        User user = new User();
        user.setName("Shopping Item User");
        user.setEmail("shopping.item@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Utilizar uma categoria DEFAULT criada pelas migrations
        Category category = categoryRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No category available"));

        // Produto
        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        // Lista
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        // Item
        ShoppingItem shoppingItem = new ShoppingItem();
        shoppingItem.setShoppingList(savedList);
        shoppingItem.setProduct(savedProduct);
        shoppingItem.setQuantity(new BigDecimal("2.500"));
        shoppingItem.setUnit(Unit.KG);

        // Act
        ShoppingItem savedItem =
                shoppingItemRepository.saveAndFlush(shoppingItem);

        Long itemId = savedItem.getId();

        entityManager.clear();

        ShoppingItem persistedItem = shoppingItemRepository
                .findById(itemId)
                .orElseThrow();

        // Assert
        assertThat(persistedItem.getId()).isNotNull();

        assertThat(persistedItem.getQuantity())
                .isEqualByComparingTo("2.500");

        assertThat(persistedItem.getUnit()).isEqualTo(Unit.KG);

        assertThat(persistedItem.isChecked()).isFalse();
        assertThat(persistedItem.getCheckedAt()).isNull();

        assertThat(persistedItem.getShoppingList().getId())
                .isEqualTo(savedList.getId());

        assertThat(persistedItem.getProduct().getId())
                .isEqualTo(savedProduct.getId());

        assertThat(persistedItem.getCreatedAt()).isNotNull();
        assertThat(persistedItem.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRejectDuplicateProductInSameShoppingList() {

        // Arrange: utilizador
        User user = new User();
        user.setName("Duplicate Item User");
        user.setEmail("duplicate.item@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Categoria existente
        Category category = categoryRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        // Produto
        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        // Lista
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        // Primeiro item
        ShoppingItem firstItem = new ShoppingItem();
        firstItem.setShoppingList(savedList);
        firstItem.setProduct(savedProduct);
        firstItem.setUnit(Unit.KG);

        shoppingItemRepository.saveAndFlush(firstItem);

        // Segundo item com o mesmo produto na mesma lista
        ShoppingItem duplicateItem = new ShoppingItem();
        duplicateItem.setShoppingList(savedList);
        duplicateItem.setProduct(savedProduct);
        duplicateItem.setUnit(Unit.KG);

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingItemRepository.saveAndFlush(duplicateItem)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-2.5"})
    void shouldRejectShoppingItemWithInvalidQuantity(String quantity) {

        // Arrange
        User user = new User();
        user.setName("Invalid Quantity User");
        user.setEmail("invalid.quantity." + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        ShoppingItem item = new ShoppingItem();
        item.setShoppingList(savedList);
        item.setProduct(savedProduct);
        item.setQuantity(new BigDecimal(quantity));
        item.setUnit(Unit.KG);

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingItemRepository.saveAndFlush(item)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectShoppingItemWithoutUnit() {

        // Arrange
        User user = new User();
        user.setName("Shopping Item Without Unit User");
        user.setEmail("item.without.unit." + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        ShoppingItem item = new ShoppingItem();
        item.setShoppingList(savedList);
        item.setProduct(savedProduct);

        // Não definir a unidade
        item.setUnit(null);

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingItemRepository.saveAndFlush(item)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectShoppingItemWithoutShoppingList() {

        // Arrange
        User user = new User();
        user.setName("Shopping Item Without List User");
        user.setEmail("item.without.list." + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        ShoppingItem item = new ShoppingItem();
        item.setProduct(savedProduct);
        item.setQuantity(BigDecimal.ONE);
        item.setUnit(Unit.KG);

        // Não associar nenhuma lista
        item.setShoppingList(null);

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingItemRepository.saveAndFlush(item)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectShoppingItemWithoutProduct() {

        // Arrange: utilizador
        User user = new User();
        user.setName("Shopping Item Without Product User");
        user.setEmail("item.without.product." + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Criar lista
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        // Criar item sem produto
        ShoppingItem item = new ShoppingItem();
        item.setShoppingList(savedList);
        item.setQuantity(BigDecimal.ONE);
        item.setUnit(Unit.UNIT);

        // RN-ITEM-02: produto obrigatório
        item.setProduct(null);

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingItemRepository.saveAndFlush(item)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldDeleteShoppingItemsWhenShoppingListIsDeletedButKeepProducts() {

        // Arrange: utilizador
        User user = new User();
        user.setName("Cascade Delete User");
        user.setEmail("cascade.delete." + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Categoria
        Category category = categoryRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        // Produto
        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        // Lista de compras
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        // Item da lista
        ShoppingItem item = new ShoppingItem();
        item.setShoppingList(savedList);
        item.setProduct(savedProduct);
        item.setUnit(Unit.KG);

        ShoppingItem savedItem =
                shoppingItemRepository.saveAndFlush(item);

        Long listId = savedList.getId();
        Long itemId = savedItem.getId();
        Long productId = savedProduct.getId();

        // Act: limpar o contexto antes de eliminar a lista
        entityManager.flush();
        entityManager.clear();

        shoppingListRepository.deleteById(listId);
        shoppingListRepository.flush();

        entityManager.clear();

        // Assert: lista e item eliminados
        assertThat(shoppingListRepository.existsById(listId))
                .isFalse();

        assertThat(shoppingItemRepository.existsById(itemId))
                .isFalse();

        // Assert: produto preservado
        assertThat(productRepository.existsById(productId))
                .isTrue();
    }
    @Test
    void shouldCountShoppingItemsByShoppingListId() {

        // Arrange
        User user = new User();
        user.setName("Shopping Items Count User");
        user.setEmail("shopping.items.count."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        // Act
        long totalItems = shoppingItemRepository
                .countByShoppingListId(savedList.getId());

        // Assert
        assertThat(totalItems).isZero();
    }

    @Test
    void shouldCountOnlyItemsBelongingToShoppingList() {

        // Arrange
        User user = new User();
        user.setName("Shopping Items Count User");
        user.setEmail("shopping.items.count."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        ShoppingList firstList = new ShoppingList();
        firstList.setName("Compras de Outubro");
        firstList.setUser(savedUser);
        ShoppingList savedFirstList =
                shoppingListRepository.saveAndFlush(firstList);

        ShoppingList secondList = new ShoppingList();
        secondList.setName("Compras de Novembro");
        secondList.setUser(savedUser);
        ShoppingList savedSecondList =
                shoppingListRepository.saveAndFlush(secondList);

        String[] productNames = {"Arroz", "Leite", "Açúcar", "Massa"};

        for (int i = 0; i < productNames.length; i++) {

            Product product = new Product();
            product.setName(productNames[i]);
            product.setCategory(category);
            product.setUser(savedUser);

            Product savedProduct = productRepository.saveAndFlush(product);

            ShoppingItem item = new ShoppingItem();
            item.setShoppingList(i < 3 ? savedFirstList : savedSecondList);
            item.setProduct(savedProduct);
            item.setQuantity(BigDecimal.ONE);
            item.setUnit(Unit.UNIT);

            shoppingItemRepository.saveAndFlush(item);
        }

        // Act
        long totalFirstList = shoppingItemRepository
                .countByShoppingListId(savedFirstList.getId());

        long totalSecondList = shoppingItemRepository
                .countByShoppingListId(savedSecondList.getId());

        // Assert
        assertThat(totalFirstList).isEqualTo(3);
        assertThat(totalSecondList).isEqualTo(1);
    }

    @Test
    void shouldCountCheckedItemsByShoppingListId() {

        // Arrange
        User user = new User();
        user.setName("Shopping Items Checked Count User");
        user.setEmail("shopping.items.checked."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String[] productNames = {"Arroz", "Leite", "Açúcar"};

        for (int i = 0; i < productNames.length; i++) {

            Product product = new Product();
            product.setName(productNames[i]);
            product.setCategory(category);
            product.setUser(savedUser);

            Product savedProduct = productRepository.saveAndFlush(product);

            ShoppingItem item = new ShoppingItem();
            item.setShoppingList(savedList);
            item.setProduct(savedProduct);
            item.setQuantity(BigDecimal.ONE);
            item.setUnit(Unit.UNIT);

            // Dois produtos comprados e um pendente
            item.setChecked(i < 2);

            if (item.isChecked()) {
                item.setCheckedAt(LocalDateTime.now());
            }

            shoppingItemRepository.saveAndFlush(item);
        }

        // Act
        long checkedItems = shoppingItemRepository
                .countByShoppingListIdAndCheckedTrue(savedList.getId());

        // Assert
        assertThat(checkedItems).isEqualTo(2);
    }

    @Test
    void shouldReturnEmptyCountsWhenShoppingListIdsAreEmpty() {

        // Act
        var counts = shoppingItemRepository
                .countItemsByShoppingListIds(List.of());

        // Assert
        assertThat(counts).isEmpty();
    }

    @Test
    void shouldCountItemsGroupedByShoppingListIds() {

        // Arrange
        User user = new User();
        user.setName("Shopping Items Grouped Count User");
        user.setEmail("shopping.items.grouped."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        ShoppingList firstList = new ShoppingList();
        firstList.setName("Lista A");
        firstList.setUser(savedUser);

        ShoppingList savedFirstList =
                shoppingListRepository.saveAndFlush(firstList);

        ShoppingList secondList = new ShoppingList();
        secondList.setName("Lista B");
        secondList.setUser(savedUser);

        ShoppingList savedSecondList =
                shoppingListRepository.saveAndFlush(secondList);

        ShoppingList emptyList = new ShoppingList();
        emptyList.setName("Lista vazia");
        emptyList.setUser(savedUser);

        ShoppingList savedEmptyList =
                shoppingListRepository.saveAndFlush(emptyList);

        String[] productNames = {"Arroz", "Leite", "Açúcar", "Massa"};

        for (int i = 0; i < productNames.length; i++) {

            Product product = new Product();
            product.setName(productNames[i]);
            product.setCategory(category);
            product.setUser(savedUser);

            Product savedProduct = productRepository.saveAndFlush(product);

            ShoppingItem item = new ShoppingItem();
            item.setShoppingList(i < 3 ? savedFirstList : savedSecondList);
            item.setProduct(savedProduct);
            item.setQuantity(BigDecimal.ONE);
            item.setUnit(Unit.UNIT);

            // Lista A: 3 itens, 2 comprados
            // Lista B: 1 item, 1 comprado
            item.setChecked(i < 2 || i == 3);

            if (item.isChecked()) {
                item.setCheckedAt(LocalDateTime.now());
            }

            shoppingItemRepository.saveAndFlush(item);
        }

        // Act
        List<ShoppingListItemCount> counts =
                shoppingItemRepository.countItemsByShoppingListIds(
                        List.of(
                                savedFirstList.getId(),
                                savedSecondList.getId(),
                                savedEmptyList.getId()
                        )
                );

        // Assert
        assertThat(counts).hasSize(2);

        assertThat(counts)
                .anySatisfy(count -> {
                    assertThat(count.getShoppingListId())
                            .isEqualTo(savedFirstList.getId());
                    assertThat(count.getTotalItems()).isEqualTo(3L);
                    assertThat(count.getCheckedItems()).isEqualTo(2L);
                });

        assertThat(counts)
                .anySatisfy(count -> {
                    assertThat(count.getShoppingListId())
                            .isEqualTo(savedSecondList.getId());
                    assertThat(count.getTotalItems()).isEqualTo(1L);
                    assertThat(count.getCheckedItems()).isEqualTo(1L);
                });

        assertThat(counts)
                .noneSatisfy(count ->
                        assertThat(count.getShoppingListId())
                                .isEqualTo(savedEmptyList.getId())
                );
    }
}