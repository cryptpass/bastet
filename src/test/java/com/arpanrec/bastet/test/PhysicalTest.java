package com.arpanrec.bastet.test;

import com.arpanrec.bastet.physical.Physical;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PhysicalTest {

    private final Physical physical;

    public PhysicalTest(@Autowired Physical physical) {
        this.physical = physical;
//        final com.arpanrec.bastet.encryption.Encryptor encryptor = com.arpanrec.bastet.encryption.Encryptor.INSTANCE;
//        String masterKey = encryptor.generateKey();
//        this.physical.setMasterKey(masterKey);
    }

    @Test
    void testBasicReadWrite() {
        physical.write("test123", "test123");
        String value = physical.read("test123");
        assert value.equals("test123");
    }

    @Test
    void testListKeys() {
        List<String> allKeys = physical.listKeys("");
        for (String key : allKeys) {
            physical.deleteAll(key);
        }
        physical.write("test123", "test123");
        physical.write("test123", "test123");
        allKeys = physical.listKeys("");
        assert allKeys.size() == 1;
        assert allKeys.getFirst().equals("test123");
    }
}
