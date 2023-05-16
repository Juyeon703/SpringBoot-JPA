package jpabook.jpashop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//== 테이블, 컬럼명 생성 전략 ==//
// 스프링부트에서 하이버네이트 기본 매핑 전략을 변경해서 실제 테이블 필드명을 다름 (SpringPhysicalNamingStrategy)
// 1. 카멜 => _(언더스코어) -----> memberPoint => member_point
// 2. .(점) => _(언더스코어)
// 3. 대문자 => 소문자

// <== 전략 변경 ==> //
// 1. 논리명 생성: 명시적으로 컬럼, 테이블명을 직접 적지 않으면 ImplicitNamingStrategy 사용
// spring.jpa.hibernate.naming.implicit-strategy : 테이블이나, 컬럼명을 명시하지 않을 때 논리명적용,
// 2. 물리명 적용:
// spring.jpa.hibernate.naming.physical-strategy : 모든 논리명에 적용됨, 실제 테이블에 적용
// => (username usernm 등으로 회사 룰로 바꿀 수 있음)

// 스프링 부트 기본 설정
// spring.jpa.hibernate.naming.implicit-strategy: org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy
// spring.jpa.hibernate.naming.physical-strategy: org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy

@Entity
@Table(name = "orders")
@Getter @Setter
public class Order {

    @Id @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // @XToMany => FetchType.LAZY 지연로딩
    // @XToOne => FetchType.EAGER 즉시로딩
    // 즉시로딩(EAGER)는 예측이 어렵고, JPQL 실행 시 n+1 문제가 발생한다.
    // 지연로딩(LAZY)로 설정하고, 문제 발생 시 fetch join 또는 엔티티 그래프 기능을 사용한다.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    private LocalDateTime orderDate; // 주문시간

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // 주문상태 [ORDER, CANCEL]

    //== 연관관계 편의 메서드 ==//

    public void setMember(Member member) {
        this.member = member;
        member.getOrders().add(this);

        // Member member = new Member();
        // Order order = new Order();
        // member.getOrders().add(order);
        // order.setMember(member);
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
        delivery.setOrder(this);
    }

    //== 생성 메서드 ==//
    public static Order createOrder(Member member, Delivery delivery, OrderItem... orderItems) {
        Order order = new Order();
        order.setMember(member);
        order.setDelivery(delivery);
        for (OrderItem orderItem : orderItems) {
            order.addOrderItem(orderItem);
        }
        order.setStatus(OrderStatus.ORDER);
        order.setOrderDate(LocalDateTime.now());
        return order;
    }

    //== 비즈니스 로직 ==//
    // 주문 취소
    public void cancel() {
        if (delivery.getStatus() == DeliveryStatus.COMP) {
            throw new IllegalStateException("이미 배송완료된 상품은 취소가 불가능합니다.");
        }

        this.setStatus(OrderStatus.CANCEL);
        for (OrderItem orderItem : orderItems) {
            orderItem.cancel();
        }
    }

    //== 조회 로직 ==//
    // 전체 주문 가격 조회
    public int getTotalPrice() {

        // return orderItems.stream()
        //         .mapToInt(OrderItem::getTotalPrice)
        //         .sum();

        int totalPrice = 0;
        for (OrderItem orderItem : orderItems) {
            totalPrice += orderItem.getTotalPrice();
        }
        return totalPrice;
    }
}
