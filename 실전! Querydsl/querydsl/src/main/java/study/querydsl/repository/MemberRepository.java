package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    // 순수 JPA 리포지토리 -> 스프링 데이터 JPA 리포지토리로 변경

    List<Member> findByUsername(String username);
}
