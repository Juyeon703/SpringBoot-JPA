package study.datajpa.controller;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;
import study.datajpa.repository.MemberRepository;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;

    @GetMapping("/members/{id}")
    public String findMember(@PathVariable("id") Long id) {
        Member member = memberRepository.findById(id).get();
        return member.getUsername();
    }

    /**
     * 도메인 클래스 컨버터
     * - HTTP 요청은 회원 id 를 받지만 도메인 클래스 컨버터가 중간에 동작해서 회원 엔티티 객체를 반환
     * - 도메인 클래스 컨버터도 리파지토리를 사용해서 엔티티를 찾음
     */
    @GetMapping("/members2/{id}")
    public String findMember(@PathVariable("id") Member member) {
        return member.getUsername();
    }

    /**
     * Web - 페이징과 정렬
     *
     * 1. 글로벌 설정(application.properties)
     *    spring.data.web.pageable.default-page-size=20 /# 기본 페이지 사이즈/
     *    spring.data.web.pageable.max-page-size=2000 /# 최대 페이지 사이즈/
     *
     * 2. 개별 설정
     *    @PageableDefault 어노테이션을 사용
     *
     *    + 접두사
     *    페이징 정보가 둘 이상이면 접두사로 구분
     *    @Qualifier 에 접두사명 추가 "{접두사명}_xxx”
     *      예제: /members?member_page=0&order_page=1
     *      @Qualifier("member") Pageable memberPageable,
     *      @Qualifier("order") Pageable orderPageable, ...
     *
     *    + 페이지 1부터 시작하기
     *      스프링 데이터는 Page를 0부터 시작한다.
     *      1. Pageable, Page를 파리미터와 응답 값으로 사용히지 않고, 직접 클래스를 만들어서 처리한다.
     *         그리고 직접 PageRequest(Pageable 구현체)를 생성해서 리포지토리에 넘긴다.
     *         물론 응답값도 Page 대신에 직접 만들어서 제공해야 한다.
     *      2. spring.data.web.pageable.one-indexed-parameters 를 true 로 설정한다. (application.properties)
     *         그런데 이 방법은 web에서 page 파라미터를 -1 처리 할 뿐이다.
     *         따라서 응답값인 Page에 모두 0 페이지 인덱스를 사용하는 한계가 있다.
     */
    @GetMapping("/members")
    public Page<Member> list(Pageable pageable) {
        Page<Member> page = memberRepository.findAll(pageable);
        return page;
    }

    @GetMapping("/members_page")
    public Page<MemberDto> list2(@PageableDefault(size = 12, sort = "username", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Member> page = memberRepository.findAll(pageable);

        // Dto로 변환
        // Page<MemberDto> map = page.map(m -> new MemberDto(m));
        Page<MemberDto> map = page.map(MemberDto::new);
        return map;
    }

    @PostConstruct
    public void init() {
        for (int i = 0; i < 100; i++) {
            memberRepository.save(new Member("user" + i, i));
        }
    }
}
