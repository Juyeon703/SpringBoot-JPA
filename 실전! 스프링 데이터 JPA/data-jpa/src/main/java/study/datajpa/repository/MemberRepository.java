package study.datajpa.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;

import java.util.List;
import java.util.Optional;

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
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    /**
     * 1.메소드 이름으로 쿼리 생성
     *  - 조회: find…By ,read…By ,query…By get…By,
     *  - COUNT: count…By 반환타입 long
     *  - EXISTS: exists…By 반환타입 boolean
     *  - 삭제: delete…By, remove…By 반환타입 long
     *  - DISTINCT: findDistinct, findMemberDistinctBy
     *  - LIMIT: findFirst3, findFirst, findTop, findTop3
     *
     * 2.메소드 이름으로 JPA NamedQuery 호출 **잘 안씀**
     *  - Member, MemberJPARepository 예시1
     *  - Member, MemberRepository 예시2
     *  - 장점 : 애플리케이션 실행 시점에 오타(문법) 오류 찾아낼 수 있음.
     *
     * 3.@Query 어노테이션을 사용해서 리파지토리 인터페이스에 쿼리 직접 정의 (메서드에 JPQL 작성)
     * - 파라미터가 증가하면 1번의 경우 지저분하기 때문에 3번 기능 자주 사용함.
     * - 장점 : 애플리케이션 실행 시점에 오타(문법) 오류 찾아낼 수 있음.
     */
    List<Member> findByUsernameAndAgeGreaterThan(String username, int age); // 이름이 같고 age 이상

    // NamedQuery
    @Query(name = "Member.findByUsername") // name이랑 '도메인.메서드' 이름 같으면 @Query 생략 가능
    List<Member> findByUsername(@Param("username") String username);

    // 메서드에 JPQL 작성
    @Query("select m from Member m where m.username = :username and m.age = :age")
    List<Member> findUser(@Param("username") String username, @Param("age") int age);

    // @Query 값 조회
    @Query("select m.username from Member m")
    List<String> findUsernameList();

    // @Query Dto 조회
    @Query("select new study.datajpa.dto.MemberDto(m.id, m.username, t.name) from Member m join m.team t")
    List<MemberDto> findMemberDto();

    /**
     * 파라미터 바인딩
     * 1. 위치 기반
     *      select m from Member m where m.username = ?0
     * 2. 이름 기반
     *      select m from Member m where m.username = :name
     */
    // 컬렉션 파라미터 바인딩
    @Query("select m from Member m where m.username in :names")
    List<Member> findByNames(@Param("names") List<String> names);

    /**
     * 반환 타입
     * - 컬렉션
     *   결과 없음: 빈 컬렉션 반환
     * - 단건 조회
     *   결과 없음: null 반환
     *   결과가 2건 이상: javax.persistence.NonUniqueResultException 예외 발생
     */
    List<Member> findListByUsername(String name); //컬렉션
    Member findMemberByUsername(String name); //단건
    Optional<Member> findOptionalByUsername(String name); //단건 Optional

    /**
     * 스프링 데이터 JPA 페이징과 정렬
     *
     * - 페이징과 정렬 파라미터
     *      - org.springframework.data.domain.Sort : 정렬 기능
     *      - org.springframework.data.domain.Pageable : 페이징 기능 (내부에 Sort 포함)
     *
     * - 특별한 반환 타입
     *      - org.springframework.data.domain.Page : 추가 count 쿼리 결과를 포함하는 페이징
     *      - org.springframework.data.domain.Slice : 추가 count 쿼리 없이 다음 페이지만 확인 가능
     *                                                (내부적으로 limit + 1조회)
     *      - List (자바 컬렉션): 추가 count 쿼리 없이 결과만 반환
     */
    Page<Member> findByAge(int age, Pageable pageable); //count 쿼리 사용

    /* count 쿼리를 다음과 같이 분리할 수 있음
    @Query(value = "select m from Member m left join m.team t",
        countQuery = "select count(m) from Member m")
    Page<Member> findByAge(int age, Pageable pageable); //count 쿼리 사용
    */
    // Slice<Member> findByAge(int age, Pageable pageable); //count 쿼리 사용 안함, 추가로 limit + 1을 조회(다음 페이지 여부)
    // List<Member> findByAge(int age, Pageable pageable); //count 쿼리 사용 안함
    // List<Member> findByAge(int age, Sort sort);

    /**
     * 스프링 데이터 JPA 벌크성 수정 쿼리
     */
    @Modifying(clearAutomatically = true) // executeUpdate 실행함, clearAutomatically = 영속성컨텍스트 초기화
    @Query("update Member m set m.age = m.age + 1 where m.age >= :age")
    int bulkAgePlus(@Param("age") int age);

    /**
     * @EntityGraph
     * -> 연관된 데이터 한번에 가져오기
     *      사실상 페치 조인(FETCH JOIN)의 간편 버전
     *      LEFT OUTER JOIN 사용
     *
     * + @NamedEntityGraph
     */
    @Query("select m from Member m left join fetch m.team")
    List<Member> findMemberFetchJoin();

    @Override
    @EntityGraph(attributePaths = {"team"})
    List<Member> findAll();

    @EntityGraph(attributePaths = {"team"})
    @Query("select m from Member m")
    List<Member> findMemberEntityGraph();

    // @NamedEntityGraph 사용
    @EntityGraph("Member.all")
    List<Member> findEntityGraphByUsername(String username);

    /**
     * 지연로딩 확인 방법
     * 1. Hibernate 기능으로 확인
     *    - Hibernate.isInitialized(member.getTeam())
     * 2. JPA 표준 방법으로 확인
     *    - PersistenceUnitUtil util = em.getEntityManagerFactory().getPersistenceUnitUtil();
     *      util.isLoaded(member.getTeam());
     */

    /**
     * JPA Hint : SQL 힌트가 아니라 JPA 구현체에게 제공하는 힌트
     * -> 성능최적화를 위함(변경감지를 위해 필요한 비교할 객체 2개를 변경감지 안할거라고 가정하고 1개만 관리하는것?)
     */
    @QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Member findReadOnlyByUsername(String username); // readOnly라서 변경감지가 안됨 -> update 안됨.

    // forCounting : 반환 타입으로 Page 인터페이스를 적용하면 추가로 호출하는 페이징을 위한 count 쿼리도 쿼리 힌트 적용(기본값 true )
    @QueryHints(value = { @QueryHint(name = "org.hibernate.readOnly", value = "true")}, forCounting = true)
    Page<Member> findByUsername(String name, Pageable pageable);

    /**
     * Lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Member> findLockByUsername(String name);
}
