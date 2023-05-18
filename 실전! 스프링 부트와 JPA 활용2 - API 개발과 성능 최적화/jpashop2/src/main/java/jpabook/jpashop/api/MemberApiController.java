package jpabook.jpashop.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController // @Controller + @ResponseBody
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    /**
     * 회원 생성 - 요청 값으로 엔티티를 직접 받음.
     *
     * ==> 문제점 <==
     * 엔티티에 화면을 위한 로직이 들어간다.(@NotEmpty 등)
     * 엔티티가 변경되면 API 스펙이 변한다.
     *
     * ====> 결론 <=====
     * API 요청 스펙에 맞추어 별도의 DTO를 파라미터로 받는다.
     */
    @PostMapping("api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /**
     * 회원 생성 - 요청 값으로 별도의 DTO를 받는다.
     *
     * == 장점 ==
     * 엔티티와 프레젠테이션 계층을 위한 로직을 분리할 수 있다.
     * 엔티티와 API 스펙을 명확하게 분리할 수 있다.
     * 엔티티가 변해도 API 스펙이 변하지 않는다.
     */
    @PostMapping("api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /**
     * 회원 수정
     * => 변경 감지 이용해서 데이터 수정
     *
     * == PUT은 전체 업데이트를 할 때 사용하는 것이 맞다.
     *    부분 업데이트를 하려면 PATCH나 POST를 사용하자.
     */
    @PutMapping("api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id,
                                             @RequestBody @Valid UpdateMemberRequest request) {
        memberService.update(id, request.getName());
        Member findMember = memberService.findOne(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    /**
     * 회원 조회 - 응답 값으로 엔티티를 외부에 직접 노출.
     *
     * ==> 문제점 <==
     * 응답 스펙에 맞추기 위해 @JsonIgnore 등 로직이 추가된다.
     */
    @GetMapping("api/v1/members")
    public List<Member> membersV1() {
        return memberService.findMembers();
    }

    /**
     * 회원 조회 - 응답 값으로 별도의 DTO를 반환.
     *
     * == 추가 ==
     * Result 클래스로 컬렉션을 감싸서 향후 필요한 필드를 추가 할 수 있음.
     * collect => 배열
     * new Result(collect) => 객체? 겉을 감쌈
     */
    @GetMapping("api/v2/members")
    public Result membersV2() {
        List<Member> findMembers = memberService.findMembers();
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName()))
                .collect(Collectors.toList());

        return new Result(collect);
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String name;
    }

    @Data
    static class CreateMemberRequest {
        @NotEmpty
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }

    @Data
    static class UpdateMemberRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }
}
