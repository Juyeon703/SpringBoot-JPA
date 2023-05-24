package study.datajpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item implements Persistable<String> {

    /**
     * save() 메서드
     *   - 새로운 엔티티면 저장( persist )
     *   - 새로운 엔티티가 아니면 병합( merge )
     *
     * 새로운 엔티티를 판단하는 기본 전략
     *   - 식별자가 객체일 때 null 로 판단 (@GeneratedValue 사용 시)
     *   - 식별자가 자바 기본 타입일 때 0 으로 판단 (primitive type)
     *   - Persistable 인터페이스를 구현해서 판단 로직 변경 가능 (implements Persistable<T>)
     */
    @Id
    private String itemId;

    @CreatedDate
    private LocalDateTime createdDate;

    public Item(String id) {
        this.itemId = id;
    }

    @Override
    public String getId() {
        return itemId;
    }

    @Override
    public boolean isNew() {
        return createdDate == null;
    }
}
