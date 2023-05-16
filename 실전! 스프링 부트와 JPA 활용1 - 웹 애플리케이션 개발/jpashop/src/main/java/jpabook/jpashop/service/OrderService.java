package jpabook.jpashop.service;

import jpabook.jpashop.domain.Delivery;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.repository.ItemRepository;
import jpabook.jpashop.repository.MemberRepository;
import jpabook.jpashop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;

    // 주문
    @Transactional
    public Long order(Long memberId, Long itemId, int count) {

        // 엔티티 조회
        Member member = memberRepository.findOne(memberId);
        Item item = itemRepository.findOne(itemId);

        // 배송정보 생성
        Delivery delivery = new Delivery();
        delivery.setAddress(member.getAddress());

        // 주문상품 생성
        OrderItem orderitem = OrderItem.createOrderitem(item, item.getPrice(), count);
        /**
         * OrderItem orderItem = new OrderItem();로 객체 생성하는 것을 막기 위해
         * protected OrderItem() {} 하면 제약을 걸 수 있음.
         * => @NoArgsConstructor(access = AccessLevel.PROTECTED)
         */

        // 주문 생성
        Order order = Order.createOrder(member, delivery, orderitem);

        // 주문 저장
        orderRepository.save(order);
        /**
         * CascadeType.ALL이 있기때문에 하나만 저장해줘도 delivery, orderitem이 자동으로 persist 됨.
         * order뿐만이 아니라 다른곳에서도 참조를 한다면 Cascade.ALL하면 문제가 생길 수 있음.
         */

        return order.getId();
    }

    // 주문 취소
    @Transactional
    public void cancelOrder(Long orderId) {
        // 주문 엔티티 조회
        Order order = orderRepository.findOne(orderId);
        // 주문 취소
        order.cancel();
        /**
         * JPA 경우, 변경 감지해서 자동으로 update쿼리 날려줌.
         *
         * 도메인 모델 패턴 : 엔티티가 비즈니스 로직, 서비스계층은 단순히 엔티티에 필요한 요청 위임. => 객체지향 적극 활용
         * 트랜잭션 스크립트 패턴 : 엔티티에 비즈니스 로직 없고, 서비스 계층에서 대부분의 비즈니스 로직 처리.
         */
    }

}
