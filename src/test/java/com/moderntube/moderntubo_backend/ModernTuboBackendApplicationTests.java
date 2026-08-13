package com.moderntube.moderntubo_backend;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 여기서 application.yml에 넣을 암호화를 작업.

@SpringBootTest
class ModernTuboBackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    @Disabled
    void jasypt() {
        // DB
        String url = "";
        String urlLocalhost = "";
        String username = "";
        String password = "";

        // JWT
        String secret = "";

        // redis
        String redisPassword = "";
        String redisHost = "";
        String redisHostLocalhost = "";

        System.out.println("url : " + jasyptEncoding(url));
        System.out.println("urlLocalhost : " + jasyptEncoding(urlLocalhost));
        System.out.println("username : " + jasyptEncoding(username));
        System.out.println("password : " + jasyptEncoding(password));
        System.out.println("secret : " + jasyptEncoding(secret));
        System.out.println("redisPassword : " + jasyptEncoding(redisPassword));
        System.out.println("redisHost : " + jasyptEncoding(redisHost));
        System.out.println("redisHostLocalhost : " + jasyptEncoding(redisHostLocalhost));
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
