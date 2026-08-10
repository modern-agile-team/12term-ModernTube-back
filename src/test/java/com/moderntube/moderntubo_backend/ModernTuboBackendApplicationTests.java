package com.moderntube.moderntubo_backend;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 여기서 application.yml에 넣을 암호화를 작업.

@SpringBootTest
class ModernTuboBackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void jasypt() {
        // DB
        String url = "";
        String username = "";
        String password = "";

        // JWT
        String secret = "";

        // Mail
        String mailPassword = "";
        String mailId = "";

        // redis
        String redisPassword = "";
        String redisHost = "";

        System.out.println("url : " + jasyptEncoding(url));
        System.out.println("username : " + jasyptEncoding(username));
        System.out.println("password : " + jasyptEncoding(password));
        System.out.println("secret : " + jasyptEncoding(secret));
        System.out.println("mailPassword : " + jasyptEncoding(mailPassword));
        System.out.println("mailId : " + jasyptEncoding(mailId));
        System.out.println("redisPassword : " + jasyptEncoding(redisPassword));
        System.out.println("redisHost : " + jasyptEncoding(redisHost));
    }

    public String jasyptEncoding(String value) {
        String key = "";
        StandardPBEStringEncryptor pbeStringEncryptor = new StandardPBEStringEncryptor();
        pbeStringEncryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        pbeStringEncryptor.setPassword(key);
        pbeStringEncryptor.setIvGenerator(new RandomIvGenerator());
        return pbeStringEncryptor.encrypt(value);
    }

}
