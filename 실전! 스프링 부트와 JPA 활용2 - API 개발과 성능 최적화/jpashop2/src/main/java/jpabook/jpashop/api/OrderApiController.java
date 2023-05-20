package jpabook.jpashop.api;

import jpabook.jpashop.domain.*;
import jpabook.jpashop.repository.OrderRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;


    /**
     * V1. 엔티티 직접 노출
     * @OneToMany 컬렉션인 일대다 관계
     *
     * - Hibernate5Module 모듈 등록, LAZY=null 처리
     * - 양방향 관계 문제 발생 -> @JsonIgnore
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();

            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }
        return all;
    }

    /**
     * V2. 엔티티를 조회해서 DTO로 변환(fetch join 사용X)
     * - 트랜잭션 안에서 지연 로딩 필요
     *
     * - Dto안에서도 엔티티 대신에 Dto 사용해서 반환.(OrderItemDto)
     * -> 껍데기만 노출하는 것이 아닌 속에 있는 내용도 DTO로 사용.
     *
     * ==> 지연로딩으로 1+n+n+n sql 실행됨.
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());

        return collect;
    }

    @Data
    static class OrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        // private List<OrderItem> orderItems;
        private List<OrderItemDto> orderItems;
        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            // orderItems = order.getOrderItems();
            // order.getOrderItems().stream().forEach(o -> o.getItem().getName()); // 없으면 orderItem = null;
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }

    }

    @Data
    static class OrderItemDto {

        private String itemName; // 상품명
        private int orderPrice; // 주문 가격
        private int count; // 주문 수량
        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }

    /**
     * V3. 엔티티를 조회해서 DTO로 변환(fetch join 사용O)
     * - 페이징 시에는 N 부분을 포기해야함(대신에 batch fetch size? 옵션 주면 N -> 1 쿼리로 변경가능)
     *
     * join(일대다)하면 order가 orderItem 갯수만큼 중복이 일어나서 그 갯수만큼 출력됐음
     * 만약 한 주문에 orderItem이 2개라면 order가 2번 출력됨.
     * ==> distinct 사용시
     *     db에 distinct 쿼리 날려주고 엔티티가 중복인 경우에 걸러서 컬렉션에 담아줌.
     *     다만, 페이징이 불가능하다.(메모리에서 페이징함 -> 위험)
     *     또한, 컬렉션 페치 조인은 1개만 사용할 수 있다. 일대 다(일대 다) 이렇게 들어가면 안됨.
     *
     * ==> SpringBoot 3.X 버전(Hibernate 6) 이상시 distinct없이 중복제거됨.
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();

        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        return result;
    }

    /**
     *  V3.1. 엔티티를 조회해서 DTO로 변환(fetch join 사용O)
     *  - 페이징 가능
     *
     *  컬렉션을 패치 조인하면 일대다 조인이 발생하므로 데이터가 예측할 수 없이 증가한다.
     *  일대다에서 1을 기준으로 페이징을 하는것이 목적인데 데이터는 다(N)를 기준으로 row를 생성한다. -> 다를 기준으로 페이징됨(메모리 페이징)
     *
     *  ===> 한계돌파 <=====
     * @XToOne 관계를 모두 페치조인한다. ToOne관계는 row수를 증가시키지 않으므로 페이징 쿼리에 영향을 주지 않는다.
     * 컬렉션은 지연 로딩으로 조회한다. 1+n+m -> 1+2(orderItem)+4(Item)
     * 지연로딩 최적화를 위해 hibernate.default_batch_fetch_size(글로벌 설정, application.properties), @BatchSize(개별 최적화, 엔티티 클래스)를 적용한다.
     * => in쿼리로 1(페치조인)+1(orderItem)+1(Item)
     *
     *  장점 :
     *  - 조인보다 DB데이터 전송량이 최적화된다.
     *
     *  즉, ToOne관계는 페치 조인해도 페이징에 영향을 주지 않으므로 페치조인으로 쿼리수를 줄여서 해결하고
     *  나머지는 Hibernate.default_batch_fetch_size로 최적화하자.
     *  size는 100~1000사이 권장(In 절 파라미터), 1000으로 설정하는 것이 성능상 가장 좋지만,
     *  결국 DB든 애플리케이션이든 순간 부하를 어디까지 견딜 수 있는지로 결정
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_1(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                     @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit); // 컬렉션 제외 다대일만 페치조인

        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        return result;
    }
}
