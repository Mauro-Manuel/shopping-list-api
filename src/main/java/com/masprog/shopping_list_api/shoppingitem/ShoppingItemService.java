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
import com.masprog.shopping_list_api.shoppinglist.exception.ShoppingListNotFoundException;
import com.masprog.shopping_list_api.shoppinglist.exception.ShoppingListValidationException;
import com.masprog.shopping_list_api.auth.exception.AuthenticatedUserNotFoundException;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ShoppingItemService {

    private final ShoppingItemRepository shoppingItemRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    @Transactional
    public ShoppingItem addExistingProduct(
            Long listId,
            Long productId,
            BigDecimal quantity,
            Unit unit,
            String authenticatedEmail
    ) {

        validateShoppingItem(quantity, unit);

        User user = getAuthenticatedUser(authenticatedEmail);

        ShoppingList shoppingList = getActiveShoppingList(listId, user);

        Product product = productRepository.findById(productId)
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found"
                ));

        if (shoppingItemRepository.existsByShoppingListIdAndProductId(
                listId,
                productId
        )) {
            throw new ProductAlreadyInListException(
                    "Product already exists in shopping list"
            );
        }

        ShoppingItem item = new ShoppingItem();
        item.setShoppingList(shoppingList);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnit(unit);

        return shoppingItemRepository.save(item);
    }

    @Transactional
    public ShoppingItem addNewProduct(
            Long listId,
            String productName,
            Long categoryId,
            BigDecimal quantity,
            Unit unit,
            String authenticatedEmail
    ) {

        validateShoppingItem(quantity, unit);

        User user = getAuthenticatedUser(authenticatedEmail);

        ShoppingList shoppingList = getActiveShoppingList(listId, user);

        // Reutilizar um produto existente ou criar um novo
        Product product = productService.findOrCreate(
                productName,
                categoryId,
                authenticatedEmail
        );

        if (shoppingItemRepository.existsByShoppingListIdAndProductId(
                listId,
                product.getId()
        )) {
            throw new ProductAlreadyInListException(
                    "Product already exists in shopping list"
            );
        }

        ShoppingItem item = new ShoppingItem();
        item.setShoppingList(shoppingList);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnit(unit);

        return shoppingItemRepository.save(item);
    }


    @Transactional
    public ShoppingItem update(
            Long listId,
            Long itemId,
            BigDecimal quantity,
            Unit unit,
            String authenticatedEmail
    ) {

        // Validar a quantidade e a unidade
        validateShoppingItem(quantity, unit);

        // Identificar o utilizador autenticado
        User user = getAuthenticatedUser(authenticatedEmail);

        // Validar propriedade e estado da lista
        getActiveShoppingList(listId, user);

        // Procurar o item na lista indicada
        ShoppingItem item = shoppingItemRepository
                .findByIdAndShoppingListId(itemId, listId)
                .orElseThrow(() ->
                        new ShoppingItemNotFoundException(
                                "Shopping item not found"
                        )
                );

        // Actualizar os campos permitidos
        item.setQuantity(quantity);
        item.setUnit(unit);

        return item;
    }

    @Transactional
    public void delete(
            Long listId,
            Long itemId,
            String authenticatedEmail
    ) {

        // Identificar o utilizador autenticado
        User user = getAuthenticatedUser(authenticatedEmail);

        // Validar se a lista pertence ao utilizador e está ACTIVE
        getActiveShoppingList(listId, user);

        // Procurar o item apenas na lista informada
        ShoppingItem item = shoppingItemRepository
                .findByIdAndShoppingListId(itemId, listId)
                .orElseThrow(() ->
                        new ShoppingItemNotFoundException(
                                "Shopping item not found"
                        )
                );

        // Remover apenas o item
        shoppingItemRepository.delete(item);
    }

    @Transactional
    public ShoppingItem check(
            Long listId,
            Long itemId,
            String authenticatedEmail
    ) {

        User user = getAuthenticatedUser(authenticatedEmail);

        getActiveShoppingList(listId, user);

        ShoppingItem item = shoppingItemRepository
                .findByIdAndShoppingListId(itemId, listId)
                .orElseThrow(() ->
                        new ShoppingItemNotFoundException(
                                "Shopping item not found"
                        )
                );

        // Marcar apenas se o item ainda não estiver comprado
        if (!item.isChecked()) {
            item.setChecked(true);
            item.setCheckedAt(LocalDateTime.now());
        }

        return item;
    }

    @Transactional
    public ShoppingItem uncheck(
            Long listId,
            Long itemId,
            String authenticatedEmail
    ) {

        // Identificar o utilizador autenticado
        User user = getAuthenticatedUser(authenticatedEmail);

        // Validar proprietário e estado da lista
        getActiveShoppingList(listId, user);

        // Procurar o item na lista indicada
        ShoppingItem item = shoppingItemRepository
                .findByIdAndShoppingListId(itemId, listId)
                .orElseThrow(() ->
                        new ShoppingItemNotFoundException(
                                "Shopping item not found"
                        )
                );

        // Desmarcar o item como comprado
        if (item.isChecked()) {
            item.setChecked(false);
            item.setCheckedAt(null);
        }

        return item;
    }




    private User getAuthenticatedUser(String authenticatedEmail) {
        return userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new AuthenticatedUserNotFoundException(
                                "Authenticated user not found"
                        ));
    }

    private void validateShoppingItem(BigDecimal quantity, Unit unit) {

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ShoppingItemValidationException(
                    "Shopping item quantity must be greater than zero"
            );
        }

        if (unit == null) {
            throw new ShoppingItemValidationException(
                    "Shopping item unit is required"
            );
        }
    }

    private ShoppingList getActiveShoppingList(Long listId, User user) {

        ShoppingList shoppingList = shoppingListRepository
                .findByIdAndUserId(listId, user.getId())
                .orElseThrow(() ->
                        new ShoppingListNotFoundException("Shopping list not found")
                );

        if (shoppingList.getStatus() != ShoppingListStatus.ACTIVE) {
            throw new ShoppingListValidationException(
                    "Only active shopping lists can be modified"
            );
        }

        return shoppingList;
    }
}