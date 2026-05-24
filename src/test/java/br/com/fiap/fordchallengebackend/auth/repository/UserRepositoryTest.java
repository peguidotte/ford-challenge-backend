package br.com.fiap.fordchallengebackend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.fordchallengebackend.auth.domain.Role;
import br.com.fiap.fordchallengebackend.auth.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindByEmail() {
        var user = new User();
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.ROLE_USER);
        userRepository.saveAndFlush(user);

        Optional<User> found = userRepository.findByEmail("john@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
        assertThat(found.get().getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldEnforceUniqueEmail() {
        var user1 = new User();
        user1.setName("John");
        user1.setEmail("duplicate@example.com");
        user1.setPassword("pass1");
        user1.setRole(Role.ROLE_USER);
        userRepository.saveAndFlush(user1);

        var user2 = new User();
        user2.setName("Jane");
        user2.setEmail("duplicate@example.com");
        user2.setPassword("pass2");
        user2.setRole(Role.ROLE_USER);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldCheckEmailExists() {
        var user = new User();
        user.setName("John");
        user.setEmail("check@example.com");
        user.setPassword("pass");
        user.setRole(Role.ROLE_ADMIN);
        userRepository.saveAndFlush(user);

        assertThat(userRepository.existsByEmail("check@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("other@example.com")).isFalse();
    }
}
