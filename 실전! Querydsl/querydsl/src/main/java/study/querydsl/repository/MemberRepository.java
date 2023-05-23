package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom , QuerydslPredicateExecutor<Member> {
    // 순수 JPA 리포지토리 -> 스프링 데이터 JPA 리포지토리로 변경

    List<Member> findByUsername(String username);

    /**
     * === 스프링 데이터 JPA가 제공하는 Querydsl 추가 기능 ===
     *
     * 1. 인터페이스 지원 - QuerydslPredicateExecutor
     *
     *      Iterable<Member> result = memberRepository.findAll(QMember.member.age.between(10, 40)
     *                 .and(QMember.member.username.eq("member1")));
     *
     *      - Pageable, Sort 모두 지원
     *      - 조인X (묵시적 조인은 가능하지만 left join이 불가능하다.)
     *      - 클라이언트가 Querydsl에 의존해야 한다. 서비스 클래스가 Querydsl이라는 구현 기술에 의존해야 한다.
     *      - 복잡한 실무환경에서 사용하기에는 한계가 명확하다.
     *
     *
     * 2. Querydsl Web 지원 - 컨트롤러
     *
     *      @RequestMapping(value = "/", method = RequestMethod.GET)
     *      String index(Model model, @QuerydslPredicate(root = User.class) Predicate predicate,
     *                   Pageable pageable, @RequestParam MultiValueMap<String, String> parameters) {
     *          model.addAttribute("users", repository.findAll(predicate, pageable));
     *          return "index";
     *      }
     *
     *      - 단순한 조건만 가능
     *      - 조건을 커스텀하는 기능이 복잡하고 명시적이지 않음
     *      - 컨트롤러가 Querydsl에 의존
     *      - 복잡한 실무환경에서 사용하기에는 한계가 명확
     *
     *
     * 3. 리포지토리 지원 - QuerydslRepositorySupport 상속
     *      - 장점
     *          - getQuerydsl().applyPagination() 스프링 데이터가 제공하는 페이징을 Querydsl로 편리하게 변환 가능(단! Sort는 오류발생)
     *          - from() 으로 시작 가능(최근에는 QueryFactory를 사용해서 select() 로 시작하는 것이 더 명시적)
     *          - EntityManager 제공
     *      - 한계
     *          - Querydsl 3.x 버전을 대상으로 만듬
     *          - Querydsl 4.x에 나온 JPAQueryFactory로 시작할 수 없음
     *          - select로 시작할 수 없음 (from으로 시작해야함)
     *          - QueryFactory 를 제공하지 않음
     *          - 스프링 데이터 Sort 기능이 정상 동작하지 않음
     */
}
