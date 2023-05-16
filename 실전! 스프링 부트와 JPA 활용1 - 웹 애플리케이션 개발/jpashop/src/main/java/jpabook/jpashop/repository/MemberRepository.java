package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jpabook.jpashop.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // 스프링 빈으로 등록, JPA 예외를 스프링 기반 예외로 변환
@RequiredArgsConstructor
public class MemberRepository {

    private final EntityManager em;

    // @PersistenceContext // EntityManager 주입
    // private EntityManager em;

    // @PersistenceUnit // EntityManagerFactory 주입
    // private EntityManagerFactory emf;

    public void save(Member member) {
        em.persist(member);
        /**
         * persist한다고 기본적으로 insert문이 나가지 않음
         * 커밋하고 flush()할 때 쿼리가 나감
         */
    }

    public Member findOne(Long id) {
        return em.find(Member.class, id);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findByName(String name) {
        return em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", name)
                .getResultList();
    }
}
