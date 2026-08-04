/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.moderntube.moderntubo_backend.repository;

import com.moderntube.moderntubo_backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);

    @Query(value = "SELECT * FROM user WHERE username LIKE CONCAT('%', :searchKeyword, '%') ORDER BY user_id DESC", countQuery = "SELECT COUNT(*) FROM users WHERE username LIKE CONCAT('%', :searchKeyword, '%')", nativeQuery = true)
    Page<User> findByUsername(@Param("searchKeyword") String searchKeyword, Pageable pageable);

    @Query(value = "SELECT * FROM user WHERE email LIKE CONCAT('%', :searchKeyword, '%') ORDER BY user_id DESC", countQuery = "SELECT COUNT(*) FROM users WHERE email LIKE CONCAT('%', :searchKeyword, '%')", nativeQuery = true)
    Page<User> findByUserEmail(@Param("searchKeyword") String searchKeyword, Pageable pageable);

    List<User> findAll();
    List<User> findByEmailIsContaining(String searchKeyword);
    List<User> findByUsernameIsContaining(String searchKeyword);
    List<User> findByNameIsContaining(String searchKeyword);

}
