package com.msas.auth;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserRepository {

    UserEntity findByUsername(@Param("username") String username);

    UserEntity findById(@Param("userId") int userId);

    List<UserEntity> findAll();

    void insertUser(UserEntity user);

    void updateUser(UserEntity user);

    void updatePassword(@Param("userId") int userId, @Param("passwordHash") String passwordHash);

    void updateLastLogin(@Param("username") String username);

    void deleteUser(@Param("userId") int userId);
}
