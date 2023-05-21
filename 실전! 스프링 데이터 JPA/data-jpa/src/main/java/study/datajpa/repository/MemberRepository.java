package study.datajpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.datajpa.entity.Member;

/**
 * System.out.println(memberRepository.getClass()) => class com.sun.proxy.$ProxyXXX
 * @Reposiotry 생략 가능 => 컴포넌트 스캔과 JPA예외를 스프링 예외로 변환하는 과정 모두 자동으로 처리
 *
 * 제네릭 타입
 * T : 엔티티
 * ID : 엔티티의 식별자 타입
 * S : 엔티티와 그 자식 타입
 *
 * 주요 메서드
 * save(S) : 새로운 엔티티는 저장하고 이미 있는 엔티티는 병합한다.
 * delete(T) : 엔티티 하나를 삭제한다. 내부에서 EntityManager.remove() 호출
 * findById(ID) : 엔티티 하나를 조회한다. 내부에서 EntityManager.find() 호출
 * getOne(ID) : 엔티티를 프록시로 조회한다. 내부에서 EntityManager.getReference() 호출
 * findAll(…) : 모든 엔티티를 조회한다. 정렬( Sort )이나 페이징( Pageable ) 조건을 파라미터로 제공할수 있다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

}
