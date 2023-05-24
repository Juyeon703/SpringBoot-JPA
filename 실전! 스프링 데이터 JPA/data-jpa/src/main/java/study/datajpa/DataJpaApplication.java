package study.datajpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

@EnableJpaAuditing//(modifyOnCreate = false) -> 생성 시 수정일 null로 들어감 //@EntityListeners(AuditingEntityListener.class)
@SpringBootApplication
public class DataJpaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataJpaApplication.class, args);
	}

	/**
	 * Auditing
	 * 엔티티를 생성, 변경할 때 변경한 사람과 시간을 추적하고 싶으면?
	 */
	// 등록자, 수정자 처리해주는 AuditorAware 스프링 빈 등록
	// 실무에서는 세션 정보나, 스프링 시큐리티 로그인 정보에서 ID를 받음
	@Bean
	public AuditorAware<String> auditorProvider() {
		return () -> Optional.of(UUID.randomUUID().toString());
	}

}
