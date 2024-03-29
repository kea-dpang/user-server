package com.example.shoppingmallserver.repository;

import com.example.shoppingmallserver.entity.user.User;

import com.example.shoppingmallserver.entity.user.UserDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 사용자의 이메일을 기반으로 사용자를 조회합니다.
     *
     * @param email 조회할 사용자의 이메일
     * @return 이메일에 해당하는 User 엔티티. 해당 이메일을 가진 사용자가 없는 경우 빈 Optional을 반환
     */
    Optional<User> findByEmail(String email);

    Page<User> findByEmailContaining(String keyword, Pageable pageable);
}
