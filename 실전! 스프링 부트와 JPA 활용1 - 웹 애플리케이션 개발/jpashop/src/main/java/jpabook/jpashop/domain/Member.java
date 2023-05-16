package jpabook.jpashop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter // @Setter는 되도록 지양
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String name;

    @Embedded
    private Address address;

    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();
    // 컬렉션은 필드에서 초기화! -> NULL 문제에서 안전
    // 하이버네이트는 엔티티티를 영속화 할 때, 하이버네이트가 제공하는 내장 컬렉션으로 변경한다.

    // Member member = new Member();
    // System.out.println(member.getOrders().getClass());
    // em.persist(member);
    // System.out.println(member.getOrders().getClass());

    // ==> 출력 결과
    // class java.util.ArrayList
    // class org.hibernate.collection.internal.PersistentBag
}


