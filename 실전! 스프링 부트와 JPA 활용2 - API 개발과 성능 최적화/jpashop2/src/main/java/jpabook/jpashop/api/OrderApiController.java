package jpabook.jpashop.api;

import jpabook.jpashop.domain.*;
import jpabook.jpashop.repository.query.OrderFlatDto;
import jpabook.jpashop.repository.query.OrderItemQueryDto;
import jpabook.jpashop.repository.query.OrderQueryDto;
import jpabook.jpashop.repository.query.OrderQueryRepository;
import jpabook.jpashop.repository.OrderRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 권장 순서
 * 1. 엔티티 조회 방식으로 우선 접근
 *      1. 페치조인으로 쿼리 수를 최적화
 *      2. 컬렉션 최적화
 *          1. 페이징 필요 hibernate.default_batch_fetch_size , @BatchSize 로 최적화
 *          2. 페이징 필요X 페치 조인 사용
 * 2. 엔티티 조회 방식으로 해결이 안되면 DTO 조회 방식 사용
 * 3. DTO 조회 방식으로 해결이 안되면 NativeSQL or 스프링 JdbcTemplate
 *
 * 참고: 엔티티 조회 방식은 페치 조인이나, hibernate.default_batch_fetch_size , @BatchSize 같이 코드를 거의 수정하지 않고, 옵션만 약간 변경해서, 다양한 성능 최적화를 시도할 수 있다.
 * 반면에 DTO를 직접 조회하는 방식은 성능을 최적화 하거나 성능 최적화 방식을 변경할 때 많은 코드를 변경해야 한다.
 *
 * 참고: 개발자는 성능 최적화와 코드 복잡도 사이에서 줄타기를 해야 한다. 항상 그런 것은 아니지만, 보통 성능 최적화는 단순한 코드를 복잡한 코드로 몰고간다.
 * 엔티티 조회 방식은 JPA가 많은 부분을 최적화 해주기 때문에, 단순한 코드를 유지하면서, 성능을 최적화 할 수 있다.
 * 반면에 DTO 조회 방식은 SQL을 직접 다루는 것과 유사하기 때문에, 둘 사이에 줄타기를 해야 한다.
 *
 *
 * == DTO 조회 방식의 선택지 ==
 * DTO로 조회하는 방법도 각각 장단이 있다. V4, V5, V6에서 단순하게 쿼리가 1번 실행된다고 V6이 항상 좋은 방법인 것은 아니다.
 *
 * V4는 코드가 단순하다. 특정 주문 한건만 조회하면 이 방식을 사용해도 성능이 잘 나온다.
 * 예를 들어서 조회한 Order 데이터가 1건이면 OrderItem을 찾기 위한 쿼리도 1번만 실행하면 된다.
 *
 * V5는 코드가 복잡하다. 여러 주문을 한꺼번에 조회하는 경우에는 V4 대신에 이것을 최적화한 V5 방식을 사용해야 한다.
 * 예를 들어서 조회한 Order 데이터가 1000건인데, V4 방식을 그대로 사용하면, 쿼리가 총 1 + 1000번 실행된다.
 * 여기서 1은 Order 를 조회한 쿼리고, 1000은 조회된 Order의 row 수다. V5방식으로 최적화 하면 쿼리가 총 1 + 1번만 실행된다.
 * 상황에 따라 다르겠지만 운영 환경에서 100배이상의 성능 차이가 날 수 있다.
 *
 * V6는 완전히 다른 접근방식이다. 쿼리 한번으로 최적화 되어서 상당히 좋아보이지만, Order를 기준으로 페이징이 불가능하다.
 * 실무에서는 이정도 데이터면 수백이나, 수천건 단위로 페이징 처리가 꼭 필요하므로, 이 경우 선택하기 어려운 방법이다.
 * 그리고 데이터가 많으면 중복 전송이 증가해서 V5와 비교해서 성능차이도 미비하다.
 */
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;


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

    /**
     * V4. JPA에서 DTO로 바로 조회, 컬렉션 N 조회 (1 + N Query)
     * - 페이징 가능
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    /**
     * V5. JPA에서 DTO로 바로 조회, 컬렉션 1 조회 최적화 버전 (1 + 1 Query)
     * - 페이징 가능
     */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    /**
     * V6. JPA에서 DTO로 바로 조회, 플랫 데이터(1Query) (1 Query)
     * - 페이징 불가능...
     *
     * V6 -> 데이터 중복(OrderItem 기준 갯수로 출력됨), 쿼리 1번
     * V6.1 -> 데이터 중복 없음, 쿼리 1번
     */
    @GetMapping("/api/v6/orders")
    public List<OrderFlatDto> ordersV6() {
        return orderQueryRepository.findAllByDto_flat();
    }

    @GetMapping("/api/v6.1/orders")
    public List<OrderQueryDto> ordersV6_1() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();
        return flats.stream()
                .collect(Collectors.groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        Collectors.mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), Collectors.toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(),e.getKey().getAddress(), e.getValue()))
                .collect(Collectors.toList());
    }
}
