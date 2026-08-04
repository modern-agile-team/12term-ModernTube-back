package com.moderntube.moderntubo_backend;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ModernTuboBackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void jasypt() {
        String url = "jdbc:mysql://rockylinux-9:3306/modernTubo_db";
        String username = "modernTuboUser";
        String password = "modernTuboUser";
        String secret = "9f3a7c1e4b2d6f8091c3e5a7b9d1f3e5a7c9e1b3d5f7a9c1e3b5d7f9a1c3e5b7";
        System.out.println("url : " + jasyptEncoding(url));
        System.out.println("username : " + jasyptEncoding(username));
        System.out.println("password : " + jasyptEncoding(password));
        System.out.println("secret : " + jasyptEncoding(secret));
    }

    public String jasyptEncoding(String value) {
        String key = "jasyptStringEncryptorKey";
        StandardPBEStringEncryptor pbeStringEncryptor = new StandardPBEStringEncryptor();
        pbeStringEncryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        pbeStringEncryptor.setPassword(key);
        pbeStringEncryptor.setIvGenerator(new RandomIvGenerator());
        return pbeStringEncryptor.encrypt(value);
    }

}
