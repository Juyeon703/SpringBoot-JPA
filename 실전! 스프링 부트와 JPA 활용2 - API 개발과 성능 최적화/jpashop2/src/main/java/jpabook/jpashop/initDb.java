package jpabook.jpashop;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.*;
import jpabook.jpashop.domain.item.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component // 스프링의 컴포넌트 스캔의 대상이 됨
@RequiredArgsConstructor
public class initDb {

    private final InitService initService;

    @PostConstruct // 여기에 트랜잭션 먹이고 이런게 잘 안되서 별도의 빈 등록
    public void init() {
        initService.dbInit1();
        initService.dbInit2();
    }

    @Component
    @Transactional
    @RequiredArgsConstructor
    static class InitService {
        private final EntityManager em;

        public void dbInit1() {
            Member member = createMember("userA", "서울", "1", "11111");
            em.persist(member);

            Book book1 = createBook("JPA1 Book", 20000, 100);
            em.persist(book1);

            Book book2 = createBook("JPA2 Book", 15000, 100);
            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderitem(book1, 10000, 1);
            OrderItem orderItem2 = OrderItem.createOrderitem(book2, 20000, 2);

            Delivery delivery = createDelivery(member);
            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);
            em.persist(order);
        }

        private static Delivery createDelivery(Member member) {
            Delivery delivery = new Delivery();
            delivery.setAddress(member.getAddress());
            return delivery;
        }

        private static Book createBook(String name, int price, int stockQuantity) {
            Book book1 = new Book();
            book1.setName(name);
            book1.setPrice(price);
            book1.setStockQuantity(stockQuantity);
            return book1;
        }

        private static Member createMember(String name, String city, String street, String zipcode) {
            Member member = new Member();
            member.setName(name);
            member.setAddress(new Address(city, street, zipcode));
            return member;
        }

        public void dbInit2() {
            Member member = createMember("userB", "경기", "2", "123123");
            em.persist(member);

            Book book1 = createBook("Spring1 Book", 20000, 100);
            em.persist(book1);

            Book book2 = createBook("Spring2 Book", 40000, 100);
            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderitem(book1, 20000, 3);
            OrderItem orderItem2 = OrderItem.createOrderitem(book2, 40000, 4);

            Delivery delivery = createDelivery(member);
            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);
            em.persist(order);
        }
    }
}
