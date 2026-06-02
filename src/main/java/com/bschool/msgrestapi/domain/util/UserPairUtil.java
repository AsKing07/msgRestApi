package com.bschool.msgrestapi.domain.util;

import com.bschool.msgrestapi.domain.entity.User;

public final class UserPairUtil {

    private UserPairUtil() {
    }

    public record OrderedUsers(User low, User high) {
    }

    public static OrderedUsers order(User a, User b) {
        if (a.getId().equals(b.getId())) {
            throw new IllegalArgumentException("Un utilisateur ne peut pas être associé à lui-même");
        }
        return a.getId() < b.getId() ? new OrderedUsers(a, b) : new OrderedUsers(b, a);
    }
}
