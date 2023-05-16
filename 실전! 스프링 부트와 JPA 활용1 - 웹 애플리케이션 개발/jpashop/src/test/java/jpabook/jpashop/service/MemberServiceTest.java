package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class) // 스프링과 테스트 통합
@SpringBootTest // 없을 시, @Autowired 실패
@Transactional // 데이터 롤백
public class MemberServiceTest {

    @Autowired MemberRepository memberRepository;
    @Autowired MemberService memberService;

    // @Autowired EntityManager em;

    @Test
    // @Rollback(value = false)
    public void 회원가입() throws Exception {
        //given
        Member member = new Member();
        member.setName("kim");

        //when
        Long savedId = memberService.join(member);

        //then
        // em.flush();
        assertEquals(member, memberRepository.findOne(savedId));

        /**
         * @Transactional로 인해 커밋하기 전에 롤백되기 때문에 insert문이 나가지 않음
         * 만약 insert쿼리를 보고 싶다면 em.flush()를 호출해거나 @Rollback 해주면 됨
         */
    }

    @Test(expected = IllegalStateException.class)
    public void 중복_회원_예외() throws Exception {
        //given
        Member member1 = new Member();
        member1.setName("kim");

        Member member2 = new Member();
        member2.setName("kim");

        //when
        memberService.join(member1);
        memberService.join(member2); // 예외가 발생해야 한다!

        //== 이거 대신 @Test(expected = IllegalStateException.class) 달아주면 됨 ==//
        // try {
        //     memberService.join(member2); // 예외가 발생해야 한다!
        // } catch (IllegalStateException e) {
        //     return;
        // }

        //then
        fail("예외가 발생해야 한다."); // 실행되면 안됨
    }

}