package com.allobank.backendtestallobank.service.bill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.allobank.backendtestallobank.repository.bill.BillDebtorRepository;
import com.allobank.backendtestallobank.repository.bill.BillRepository;
import com.allobank.backendtestallobank.dto.bill.BillDebtorRequest;
import com.allobank.backendtestallobank.dto.bill.CreateBillRequest;
import com.allobank.backendtestallobank.entity.billgroup.BillGroupEntity;
import com.allobank.backendtestallobank.entity.billgroup.BillGroupMemberEntity;
import com.allobank.backendtestallobank.entity.user.UserEntity;
import com.allobank.backendtestallobank.repository.billgroup.BillGroupMemberRepository;
import com.allobank.backendtestallobank.repository.billgroup.BillGroupRepository;
import com.allobank.backendtestallobank.repository.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:bill-service-transaction-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"app.security.jwt.secret=test-only-32-byte-minimum-secret"
})
class BillServiceTransactionTest {

	private static final UUID CREATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Autowired
	private BillService billService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BillGroupRepository billGroupRepository;

	@Autowired
	private BillGroupMemberRepository billGroupMemberRepository;

	@Autowired
	private BillRepository billRepository;

	@MockitoBean
	private BillDebtorRepository billDebtorRepository;

	@Test
	void createRollsBackBillWhenDebtorPersistenceFails() {
		UserEntity creator = userRepository.save(new UserEntity(CREATOR_ID, "Arif Rahman", "arif@example.com"));
		UserEntity member = userRepository.save(new UserEntity(MEMBER_ID, "Budi Santoso", "budi@example.com"));
		BillGroupEntity group = billGroupRepository.save(BillGroupEntity.create("Trip Bandung", creator));
		billGroupMemberRepository.saveAll(List.of(
				BillGroupMemberEntity.create(group, creator),
				BillGroupMemberEntity.create(group, member)));
		CreateBillRequest request = new CreateBillRequest(
				CREATOR_ID,
				new BigDecimal("300000.00"),
				"Lunch",
				List.of(
						new BillDebtorRequest(CREATOR_ID, new BigDecimal("100000.00")),
						new BillDebtorRequest(MEMBER_ID, new BigDecimal("200000.00"))));

		when(billDebtorRepository.saveAll(anyList())).thenThrow(new RuntimeException("debtor persistence failed"));

		assertThatThrownBy(() -> billService.create(CREATOR_ID, group.getId(), request))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("debtor persistence failed");

		assertThat(billRepository.count()).isZero();
	}
}
