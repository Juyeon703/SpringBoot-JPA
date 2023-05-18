package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true) // 읽기용, 데이터변경 안됨
@RequiredArgsConstructor // final 붙은 것만 생성자 만들어줌
// @AllArgsConstructor // 모든 생성자 만들어줌
public class MemberService {

    /**
     * 생성자 주입 방식을 권장 -> 변경 불가능한 안전한 객체 생성 가능
     * 생성자가 하나면, @Autowired(필드 주입) 생략 가능
     * final 키워드를 추가하면 컴파일 시점에 memberRepository를 설정하지 않는 오류를 체크할 수 있음.
     */
    private final MemberRepository memberRepository;

    // public MemberService(MemberRepository memberRepository) {
    //     this.memberRepository = memberRepository;
    // }

    // 회원가입
    @Transactional // 데이터 변경, 기본 : readOnly = false
    public Long join(Member member) {

        validateDuplicateMember(member); // 중복 회원 검증
        memberRepository.save(member);
        return member.getId();
    }

    private void validateDuplicateMember(Member member) {

        /**
         * 실무에서는 검증 로직이 있더라도 멀티 쓰레드 환경을 고려하여
         * 회원명 컬럼을 유니크 제약조건으로 설정하는 것을 권장
         */

        List<Member> findMembers = memberRepository.findByName(member.getName());
        if (!findMembers.isEmpty()) {
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        }
    }

    // 회원 전체 조회
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    // 회원 단건 조회
    public Member findOne(Long memberId) {
        return memberRepository.findOne(memberId);
    }

    @Transactional
    public void update(Long id, String name) {
        Member member = memberRepository.findOne(id);
        member.setName(name);
    }
}
