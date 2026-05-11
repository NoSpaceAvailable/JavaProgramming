package com.lqc.server.service;

import com.lqc.common.model.User;
import com.lqc.common.model.UserStatus;
import com.lqc.server.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    /** In-memory UserRepository that overrides DB calls. */
    private static final class FakeUserRepository extends UserRepository {
        private final Map<String, User> byUsername = new HashMap<>();
        private final AtomicLong idSeq = new AtomicLong(1);

        @Override
        public boolean existsByUsername(String username) {
            return byUsername.containsKey(username);
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(byUsername.get(username));
        }

        @Override
        public User create(String username, String passwordHash, String displayName) {
            User u = new User(idSeq.getAndIncrement(), username, displayName);
            u.setPasswordHash(passwordHash);
            u.setStatus(UserStatus.OFFLINE);
            byUsername.put(username, u);
            return u;
        }
    }

    @Test
    void register_rejectsShortUsername() {
        AuthService svc = new AuthService(new FakeUserRepository());
        var r = svc.register("ab", "password123", "Alice");
        assertFalse(r.success());
        assertTrue(r.message().toLowerCase().contains("username"));
    }

    @Test
    void register_rejectsShortPassword() {
        AuthService svc = new AuthService(new FakeUserRepository());
        var r = svc.register("alice", "short", "Alice");
        assertFalse(r.success());
        assertTrue(r.message().toLowerCase().contains("password"));
    }

    @Test
    void register_succeedsAndDefaultsDisplayName() {
        AuthService svc = new AuthService(new FakeUserRepository());
        var r = svc.register("alice", "password123", "");
        assertTrue(r.success());
        assertNotNull(r.user());
        assertEquals("alice", r.user().getDisplayName(),
                "blank display name should fall back to username");
    }

    @Test
    void register_rejectsDuplicateUsername() {
        AuthService svc = new AuthService(new FakeUserRepository());
        assertTrue(svc.register("alice", "password123", "Alice").success());
        var second = svc.register("alice", "password123", "Alice");
        assertFalse(second.success());
        assertTrue(second.message().toLowerCase().contains("taken"));
    }

    @Test
    void login_succeedsWithCorrectCredentials() {
        var repo = new FakeUserRepository();
        AuthService svc = new AuthService(repo);
        svc.register("alice", "password123", "Alice");

        var login = svc.login("alice", "password123");
        assertTrue(login.success());
        assertNotNull(login.user());
        assertEquals("alice", login.user().getUsername());
    }

    @Test
    void login_failsWithWrongPassword() {
        var repo = new FakeUserRepository();
        AuthService svc = new AuthService(repo);
        svc.register("alice", "password123", "Alice");

        var login = svc.login("alice", "wrong-password");
        assertFalse(login.success());
    }

    @Test
    void login_failsForUnknownUser() {
        AuthService svc = new AuthService(new FakeUserRepository());
        var login = svc.login("ghost", "whatever");
        assertFalse(login.success());
    }
}
