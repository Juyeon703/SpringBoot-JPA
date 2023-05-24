package study.datajpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BasicEntity extends BasicTimeEntity{
    // 실무에서 대부분의 엔티티는 등록시간, 수정시간이 필요하지만, 등록자, 수정자는 없을 수도 있다.
    // 그래서 다음과 같이 Base 타입을 분리하고, 원하는 타입을 선택해서 상속한다.
    @CreatedBy
    @Column(updatable = false)
    private String createdBy; // 작성자

    @LastModifiedBy
    private String lastModifiedBy; // 수정자
}
