package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderSearch;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSimpleQueryDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * 간단한 주문 조회 - DTO 반환
     * - 단점 : 지연로딩으로 쿼리 1 + N번 호출
     * order 조회(1) -> o.member 조회 -> o.delivery 조회
     *              -> o.member 조회 -> o.delivery 조회
     * 현재 1 + 2 + 2번 실행됨.
     * => 지연로딩은 영속성 컨텍스트에서 조회하므로, 이미 조회된 경우 쿼리를 생략한다.
     *     (만약, 주문을 한 회원이 같다면 1 + 1 + 2 번 실행됨)
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return result;
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); // LAZY 초기화 : member 쿼리 날아감
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); // LAZY 초기화
        }
    }

    /**
     * 간단한 주문 조회 - DTO 반환 + fetch join 최적화
     * => fetch join 으로 쿼리 1번 날아감.
     * => 페치 조인으로 이미 조회된 상태이므로 지연로딩 아님. 데이터 출력됨.
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
         List<Order> orders = orderRepository.findAllWithMemberDelivery();

        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * 간단한 주문 조회 - JPA에서 DTO로 바로 조회
     * => 쿼리 1번 + 원하는 데이터만 조회
     *
     * 성능상 V3보다 좋긴하나, 재사용성은 떨어짐
     * => 애플리케이션 네트윅 용량 최적화(생각보다 미비)
     *    리포지토리 재사용성 떨어짐, API 스펙에 맞춘 코드가 리포지토리에 들어가는 단점
     */
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderRepository.findOrderDtos();
    }
}
