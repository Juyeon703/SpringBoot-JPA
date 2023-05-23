package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    /**
     * JPQL
     */
    @Test
    public void startJPQL() throws Exception {
        Member findMember = em.createQuery(
                        "select m from Member m " +
                                "where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /**
     * Querydsl
     * <p>
     * <기본 Q-Type 활용>
     * => Q클래스 인스턴스를 사용하는 방법
     * <p>
     * 1. 별칭 직접 사용 = 셀프조인 할 경우 사용
     * QMember qMember = new QMember("m");
     * <p>
     * 2. 기본 인스턴스 사용 = 주로 사용
     * QMember qMember = QMember.member;
     * <p>
     * => import 해서 사용하는거 추천
     * import static study.querydsl.entity.QMember.*;
     */
    @Test
    public void startQuerydsl() throws Exception {
        // JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        // QMember m = new QMember("m"); // 별칭 m

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) // 파라미터 바인딩 자동
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() throws Exception {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        // .and() 대신 쉼표(,)로도 가능
        //      .where(
        //              member.username.eq("member1"),
        //              member.age.eq(10)
        //             )

        assertThat(findMember.getUsername()).isEqualTo("member1");

        /**
         * 검색 조건 쿼리
         */
        // member.username.eq("member1") // username = 'member1'
        // member.username.ne("member1") //username != 'member1'
        // member.username.eq("member1").not() // username != 'member1'

        // member.username.isNotNull() //이름이 is not null

        // member.age.in(10, 20) // age in (10,20)
        // member.age.notIn(10, 20) // age not in (10, 20)

        // member.age.between(10,30) //between 10, 30

        // member.age.goe(30) // age >= 30
        // member.age.gt(30) // age > 30
        // member.age.loe(30) // age <= 30
        // member.age.lt(30) // age < 30

        // member.username.like("member%") //like 검색
        // member.username.contains("member") // like ‘%member%’ 검색
        // member.username.startsWith("member") //like ‘member%’ 검색
    }

    /**
     * 결과 조회
     * - fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환
     * - fetchOne() : 단 건 조회
     * - 결과가 없으면 : null
     * - 결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
     * - fetchFirst() : limit(1).fetchOne()
     * - fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행
     * - fetchCount() : count 쿼리로 변경해서 count 수 조회
     */
    @Test
    public void resultFetch() throws Exception {
        /*
        // 리스트
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        // 단건 조회
        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        // 처음 한 건 조회
        Member findMember2 = queryFactory
                .selectFrom(member)
                .fetchFirst();
        */
    }

    /**
     * 정렬
     * - ASC() : 오름차순
     * - DESC() : 내림차순
     * - nullsLast() : null 데이터 마지막
     * - nullsFirst() : null 데이터 처음
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        // 나이 내림차순, 이름 올림차순, 회원 이름이 없으면 마지막에 출력
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    /**
     * 페이징
     */
    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터 시작(zero index)
                .limit(2) //최대 2건 조회
                .fetch();
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    /**
     * 집합함수
     * JPQL
     * select
     * COUNT(m), //회원수
     * SUM(m.age), //나이 합
     * AVG(m.age), //평균 나이
     * MAX(m.age), //최대 나이
     * MIN(m.age) //최소 나이
     * from Member m
     */
    @Test
    public void aggregation() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * Group by, Having
     * - .groupBy(item.price)
     * - .having(item.price.gt(1000))
     */
    @Test
    public void group() throws Exception {
        // 팀 이름과 팀 평균연령
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10 + 20) / 2
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30 + 40) / 2
    }

    /**
     * 기본 조인
     * => join(조인 대상, 별칭으로 사용할 Q타입)
     * <p>
     * - join() , innerJoin() : 내부 조인(inner join)
     * - leftJoin() : left 외부 조인(left outer join)
     * - rightJoin() : rigth 외부 조인(rigth outer join)
     */
    @Test
    public void join() throws Exception {
        // 팀 A에 소속된 모든 회원
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인(연관관계가 없는 필드로 조인) - cross join
     * -> 외부 조인 불가능
     */
    @Test
    public void theta_join() throws Exception {
        // 회원의 이름이 팀 이름과 같은 회원 조회
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 조인 - on절
     * <p>
     * 1. 조인 대상 필터링
     * on 절을 활용해 조인 대상을 필터링 할 때, 외부조인이 아니라 내부조인(inner join)을 사용하면, where 절에서 필터링 하는 것과 기능이 동일하다.
     * 따라서 on 절을 활용한 조인 대상 필터링을 사용할 때, 내부조인 이면 익숙한 where 절로 해결하고, 정말 외부조인이 필요한 경우에만 이 기능을 사용하자.
     * <p>
     * 2. 연관관계 없는 엔티티 외부 조인
     * 일반조인: from(member).leftJoin(member.team, team).on(xxx)
     * 연관관계 없는 on조인: from(member).leftJoin(team).on(xxx)
     */
    // 1. 조인 대상 필터링
    // 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
    // JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
    // SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='teamA'
    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        // tuple =[Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
        // tuple =[Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
        // tuple =[Member(id=5, username=member3, age=30), null]
        // tuple =[Member(id=6, username=member4, age=40), null]
    }

    // 2. 연관관계 없는 엔티티 외부 조인
    // 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
    // JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
    // SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
        // t=[Member(id=3, username=member1, age=10), null]
        // t=[Member(id=4, username=member2, age=20), null]
        // t=[Member(id=5, username=member3, age=30), null]
        // t=[Member(id=6, username=member4, age=40), null]
        // t=[Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
        // t=[Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
    }

    /**
     * fetch 조인
     * - 페치 조인은 SQL에서 제공하는 기능은 아니다.
     * - SQL조인을 활용해서 연관된 엔티티를 SQL 한번에 조회하는 기능이다.
     * - 주로 성능 최적화에 사용하는 방법이다.
     * <p>
     * => 조인 기능 뒤에 fetchJoin() 이라고 추가
     */
    // 페치 조인 미적용
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear(); // 영속성 컨텍스트 날리고 페치조인 확인하기

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded =
                emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // 이미 로딩된 엔티티인지 초기화가 안된 엔티티인지 알려줌.

        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    // 페치 조인 적용
    @Test
    public void fetchJoinUse() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded =
                emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 서브 쿼리
     * => JPAExpressions
     * <p>
     * where 절, select 절은 가능
     * from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다.
     * => 해결방안
     * 1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
     * 2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
     * 3. nativeSQL을 사용한다.
     */
    // 서브 쿼리 eq
    // 나이가 가장 많은 회원
    @Test
    public void subQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max()) // alias가 중복되면 안됨.
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    // 서브 쿼리 goe
    // 평균 나이 이상인 회원
    @Test
    public void subQueryGoe() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    // 서브쿼리 여러건 처리, in 사용
    @Test
    public void subQueryIn() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    // select 절에 subquery
    @Test
    public void selectSubQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> fetch = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ).from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("username = " + tuple.get(member.username));
            System.out.println("age = " +
                    tuple.get(JPAExpressions.select(memberSub.age.avg())
                            .from(memberSub)));
        }
    }

    // static import 활용
    // import static com.querydsl.jpa.JPAExpressions.select;
    @Test
    public void importSubQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");
        /*
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        */
    }

    /**
     * CASE문
     * -> select, 조건절(where), order by에서 사용 가능
     */
    // 단순한 케이스
    @Test
    public void basicCase() throws Exception {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
    }

    // 복잡한 케이스
    @Test
    public void complexCase() throws Exception {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
    }

    //order by에서 case문 사용하기 예제
    @Test
    public void orderByCase() throws Exception {
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = " + rank);
        }
        // username = member4 age = 40 rank = 3
        // username = member1 age = 10 rank = 2
        // username = member2 age = 20 rank = 2
        // username = member3 age = 30 rank = 1
    }

    /**
     * 상수 더하기
     * => Expressions
     */
    @Test
    public void constant() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        // tuple = [member1, A]
        // tuple = [member2, A]
        // tuple = [member3, A]
        // tuple = [member4, A]
    }

    /**
     * 문자 더하기
     * .concat()
     * 문자가 아닌 다른 타입들은 stringValue()로 문자로 변환할 수 있다.
     */
    @Test
    public void concat() throws Exception {
        // {username}_{age}
        String result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        System.out.println("result = " + result);
        // result = member1_10
    }

    // distinct
    @Test
    public void distinct() throws Exception {
        List<String> result = queryFactory
                .select(member.username).distinct()
                .from(member)
                .fetch();
    }
}
