package com.masprog.shopping_list_api.shoppinglist;

import com.masprog.shopping_list_api.auth.exception.AuthenticatedUserNotFoundException;
import com.masprog.shopping_list_api.shoppinglist.exception.ShoppingListNotFoundException;
import com.masprog.shopping_list_api.shoppinglist.exception.ShoppingListValidationException;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingListService {

    private final ShoppingListRepository shoppingListRepository;
    private final UserRepository userRepository;

    public ShoppingListService(
            ShoppingListRepository shoppingListRepository,
            UserRepository userRepository
    ) {
        this.shoppingListRepository = shoppingListRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ShoppingList create(String name, String authenticatedEmail) {

        String normalizedName = normalizeName(name);

        User user = getAuthenticatedUser(authenticatedEmail);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName(normalizedName);
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(user);

        return shoppingListRepository.save(shoppingList);
    }

    @Transactional(readOnly = true)
    public Page<ShoppingList> findAll(
            String authenticatedEmail,
            Pageable pageable
    ) {
        return findAll(authenticatedEmail, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ShoppingList> findAll(
            String authenticatedEmail,
            ShoppingListStatus status,
            Pageable pageable
    ) {
        User user = getAuthenticatedUser(authenticatedEmail);

        if (status == null) {
            return shoppingListRepository.findByUserId(
                    user.getId(),
                    pageable
            );
        }

        return shoppingListRepository.findByUserIdAndStatus(
                user.getId(),
                status,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public ShoppingList findById(Long listId, String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);

        return shoppingListRepository.findByIdAndUserId(
                listId,
                user.getId()
        ).orElseThrow(() ->
                new ShoppingListNotFoundException("Shopping list not found")
        );
    }

    @Transactional
    public ShoppingList rename(Long listId, String name, String authenticatedEmail) {

        String normalizedName = normalizeName(name);

        ShoppingList shoppingList = findById(listId, authenticatedEmail);

        if (shoppingList.getStatus() != ShoppingListStatus.ACTIVE) {
            throw new ShoppingListValidationException(
                    "Only active shopping lists can be modified"
            );
        }

        shoppingList.setName(normalizedName);

        return shoppingList;
    }

    @Transactional
    public void delete(Long listId, String authenticatedEmail) {

        ShoppingList shoppingList = findById(
                listId,
                authenticatedEmail
        );

        shoppingListRepository.delete(shoppingList);
    }

    @Transactional
    public ShoppingList complete(Long listId, String authenticatedEmail) {

        ShoppingList shoppingList = findById(
                listId,
                authenticatedEmail
        );

        throw new ShoppingListValidationException(
                "Cannot complete an empty shopping list"
        );
    }



    private User getAuthenticatedUser(String authenticatedEmail) {
        return userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new AuthenticatedUserNotFoundException(
                                "Authenticated user not found"
                        ));
    }

    private String normalizeName(String name) {

        if (name == null || name.isBlank()) {
            throw new ShoppingListValidationException(
                    "Shopping list name is required"
            );
        }

        return name.trim().replaceAll("\\s+", " ");
    }
}