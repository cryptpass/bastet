package com.arpanrec.bastet.services;

import com.arpanrec.bastet.model.KeyValue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface KeyValueRepository extends JpaRepository<KeyValue, Long> {

    @Query("select max(kv.version) from key_value_t kv where kv.key = ?1 and kv.deleted = false")
    Optional<Integer> findTopCurrentVersion(String key);

    KeyValue findByKeyAndVersionAndDeletedFalse(String key, Integer version);

    @Modifying
    @Query("update key_value_t kv set kv.deleted = true where kv.key = ?1")
    void setDeletedTrue(String key);

    @Query("select distinct kv.key from key_value_t kv where kv.key like ?1% and kv.deleted = false")
    List<String> findAllKeysLike(String key);
}
