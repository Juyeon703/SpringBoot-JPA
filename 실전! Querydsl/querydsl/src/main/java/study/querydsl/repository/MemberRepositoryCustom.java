package study.querydsl.repository;

import study.querydsl.dto.MemberSearchCond;
import study.querydsl.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom {

    List<MemberTeamDto> search(MemberSearchCond condition);
}
