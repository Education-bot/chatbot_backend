package com.vk.education_bot.repository;

import com.vk.education_bot.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    boolean existsByVkId(Long vkId);
    Admin findByVkId(Long vkId);
    void deleteAdminByVkId(Long vkId);
}
