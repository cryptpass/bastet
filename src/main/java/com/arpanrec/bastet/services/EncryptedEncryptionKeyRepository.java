package com.arpanrec.bastet.services;

import com.arpanrec.bastet.model.EncryptedEncryptionKey;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EncryptedEncryptionKeyRepository extends CrudRepository<EncryptedEncryptionKey, Long> {
}
