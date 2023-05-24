package study.datajpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
public class JpaBasicEntity {

    @Column(updatable = false) // updatable = 변경가능한가?
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    @PrePersist // persist 하기 전 이벤트 발생
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdDate = now;
        updatedDate = now;
    }

    @PreUpdate // update 하기 전 이벤트 발생
    public void preUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
