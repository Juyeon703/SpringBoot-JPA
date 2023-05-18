package jpabook.jpashop.api;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderSearch;
import jpabook.jpashop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @XToOne(ManyToOne, OneToOne)
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    /**
     * 간단한 주문 조회 - 엔티티를 직접 노출
     * jackson 라이브러리는 프록시 객체를 json으로 어떻게 생성해야하는지 모름 -> 예외 발생
     * 1. Hibernate5Module 모듈 등록, LAZY = null 처리
     * 2. 양방향 관계 문제 발생 -> @JsonIgnore
     *
     * == 주의할 점==
     * 지연로딩(LAZY)를 피하기 위해 즉시로딩(EAGER)로 설정하면
     * 연관 관계가 필요없는 경우에도 데이터를 항상 조회하기 때문에 성능 문제가 발생할 수 있다.
     *
     * 만약, 지연로딩을 기본으로 하고, 성능 최적화가 필요한 경우 페치 조인(Fetch Join) 사용 => 예시 V3
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());

        // LAZY 강제 초기화
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();
        }

        return all;
    }
}
