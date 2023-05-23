package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslTest {

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
     * 프로젝션과 결과 반환 - 기본
     * => 프로젝션? : select 대상 지정
     * <p>
     * 프로젝션 대상이 하나 -> 타입을 명확하게 지정할 수 있음
     * 프로젝션 대상이 둘 이상 -> 튜플이나 DTO로 조회
     */
    // 기본 조회
    @Test
    public void simpleProjection() throws Exception {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    // 튜블 조회
    @Test
    public void tupleProjection() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    /**
     * DTO 조회 - 순수 JPA
     * - new 명령어 사용해야함(패키지 적어줘야함).
     * - 생성자 방식만 지원함.
     */
    @Test
    public void findDtoByJPQL() throws Exception {
        List<MemberDto> result = em.createQuery(
                        "select new study.querydsl.dto.MemberDto(m.username, m.age) " +
                                "from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * DTO 조회 - Querydsl
     * - Querydsl 빈 생성
     * - 프로퍼티 접근 -> Projections.bean()
     * - 필드 직접 접근 -> Projections.fields()
     * - 생성자 사용 -> Projections.constructor()
     *
     * 프로퍼티나, 필드 접근 생성 방식에서 이름이 다를 때 해결 방안
     *  - ExpressionUtils.as(source,alias) : 필드나, 서브 쿼리에 별칭 적용
     *  - name.as("username") : 필드에 별칭 적용
     *  - 생성자는 타입을 보고 들어가기 때문에 상관없음.
     */
    // 프로퍼티 접근 - @Setter
    @Test
    public void findDtoBySetter() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 필드 직접 접근
    @Test
    public void findDtoByField() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 생성자 사용
    @Test
    public void findDtoByConstructor() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 별칭이 다를 때
    // - 엔티티랑 dto 변수명 다름 -> .as()
    // - 서브쿼리 -> ExprssionUtils.as(JPAExpressions.select~~, "별칭(alias)")
    @Test
    public void findUserDto() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> fetch = queryFactory
                .select(Projections.fields(UserDto.class,
                                member.username.as("name"),
                                ExpressionUtils.as(
                                        JPAExpressions
                                                .select(memberSub.age.max())
                                                .from(memberSub), "age")
                        )
                ).from(member)
                .fetch();
    }

    /**
     * @QueryProjection
     * -> 사용할 생성자 위에 붙여주면 dto를 Q파일로 만듬
     * - 컴파일러로 타입을 체크할 수 있으므로 가장 안전한 방법이다.
     * - 다만 DTO에 QueryDSL 어노테이션을 유지해야 하는 점과 DTO까지 Q 파일을 생성해야 하는 단점이 있음.
     */
    @Test
    public void findDtoByQueryProjection() throws Exception {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
    }

    /**
     * 동적 쿼리
     * 1. BooleanBuilder 사용
     * 2. Where 다중 파라미터 사용
     *      - where 조건에 null 값은 무시된다.
     *      - 메서드를 다른 쿼리에서도 재활용 할 수 있다.
     *      - 쿼리 자체의 가독성이 높아진다.
     */

    // BooleanBuilder
    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }
    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    // where 다중 파라미터
    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }
    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }
    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }
    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }
    // 조합 가능 - null 체크 주의
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * 수정, 삭제 벌크 연산
     * -> 영속성컨텍스트와 db 상태가 달라짐
     * - JPQL 배치와 마찬가지로, 영속성 컨텍스트에 있는 엔티티를 무시하고 실행되기 때문에
     *   배치 쿼리를 실행하고 나면 영속성 컨텍스트를 초기화 하는 것이 안전하다.
     */
    @Test
    public void bulkUpdate() throws Exception {
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        // 실행전
        // member1 = 10 -> DB member1  영속성 컨텍스트 member1
        // member2 = 20 -> DB member2  영속성 컨텍스트 member2
        // member3 = 30 -> DB member3  영속성 컨텍스트 member3
        // member4 = 40 -> DB member4  영속성 컨텍스트 member4

        // 실행 후
        // member1 = 10 -> DB 비회원    영속성 컨텍스트 member1
        // member2 = 20 -> DB 비회원    영속성 컨텍스트 member2
        // member3 = 30 -> DB member3  영속성 컨텍스트 member3
        // member4 = 40 -> DB member4  영속성 컨텍스트 member4

        // 다시 데이터를 조회해도 영속성컨텍스트에 데이터가 있기때문에
        // 영속성 컨텍스트에 있는 데이터를 가져옴. => 즉, 수정전 데이터가 조회되는 것임.
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        // 따라서 벌크 연산이 나간후
        em.flush();
        em.clear();
        // 영속성컨텍스트 초기화해주면 수정 후 데이터를 가져오게됨.
        List<Member> result2 = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member before : result) {
            System.out.println("초기화 전 = " + before);
        }
        for (Member after : result2) {
            System.out.println("초기화 후 = " + after);
        }
    }

    // 더하기: add(x)
    // 곱하기: multiply(x)
    @Test
    public void bulkAdd() throws Exception {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    public void bulkDelete() throws Exception {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    /**
     * SQL function 호출하기
     * -> SQL function은 JPA와 같이 Dialect에 등록된 내용만 호출할 수 있다.
     */
    @Test
    public void sqlFunction() throws Exception {
        String result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M")) // member라는 단어를 M으로 바꾸기
                .from(member)
                .fetchFirst();
    }

    @Test
    public void sqlFunction2() throws Exception {
        queryFactory
                .select(member.username)
                .from(member)
                /*
                .where(member.username.eq(
                        Expressions.stringTemplate(
                                "function('lower', {0})", member.username))) // 대문자 소문자로 바꾸기
                */
                .where(member.username.eq(member.username.lower())) // ansi 표준 함수들은 querydsl이 상당부분 내장
                .fetch();
    }

    
}

