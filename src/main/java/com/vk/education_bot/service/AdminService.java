package com.vk.education_bot.service;

import com.vk.education_bot.entity.Admin;
import com.vk.education_bot.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    private final AdminRepository adminRepository;

    @Autowired
    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    public boolean checkAdmin(Long userId) {
        return adminRepository.existsByVkId(userId);
    }

    public List<Long> getAllAdminIds() {
        List<Long> list = new ArrayList<>();
        for (Admin admin : adminRepository.findAll()) {
            list.add(admin.getVkId());
        }
        return list;
    }

    public void addAdmin(Long userId) {
        Admin newAdmin = new Admin(userId);
        adminRepository.save(newAdmin);
    }

    public void deleteAdmin(Long userId) {
        Admin admin = adminRepository.findByVkId(userId);
        adminRepository.delete(admin);
    }
}
