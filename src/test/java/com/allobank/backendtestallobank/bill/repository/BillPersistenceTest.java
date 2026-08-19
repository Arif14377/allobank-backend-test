package com.allobank.backendtestallobank.bill.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.allobank.backendtestallobank.bill.entity.BillDebtorEntity;
import com.allobank.backendtestallobank.bill.entity.BillEntity;
import com.allobank.backendtestallobank.billgroup.entity.BillGroupEntity;
import com.allobank.backendtestallobank.billgroup.entity.BillGroupMemberEntity;
import com.allobank.backendtestallobank.billgroup.repository.BillGroupMemberRepository;
import com.allobank.backendtestallobank.billgroup.repository.BillGroupRepository;
import com.allobank.backendtestallobank.user.entity.UserEntity;
import com.allobank.backendtestallobank.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class BillPersistenceTest {

	private static final UUID CREATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BillGroupRepository billGroupRepository;

	@Autowired
	private BillGroupMemberRepository billGroupMemberRepository;

	@Autowired
	private BillRepository billRepository;

	@Autowired
	private BillDebtorRepository billDebtorRepository;

	@Test
	void persistsBillAndDebtorRows() {
		UserEntity creator = userRepository.save(new UserEntity(CREATOR_ID, "Arif Rahman", "arif@example.com"));
		UserEntity member = userRepository.save(new UserEntity(MEMBER_ID, "Budi Santoso", "budi@example.com"));
		BillGroupEntity group = billGroupRepository.save(BillGroupEntity.create("Trip Bandung", creator));
		billGroupMemberRepository.saveAll(List.of(
				BillGroupMemberEntity.create(group, creator),
				BillGroupMemberEntity.create(group, member)));

		BillEntity bill = billRepository.save(BillEntity.create(
				group,
				creator,
				new BigDecimal("300000.00"),
				"Lunch"));
		billDebtorRepository.saveAll(List.of(
				BillDebtorEntity.create(bill, creator, new BigDecimal("100000.00")),
				BillDebtorEntity.create(bill, member, new BigDecimal("200000.00"))));

		BillEntity savedBill = billRepository.findById(bill.getId()).orElseThrow();
		List<BillDebtorEntity> savedDebtors = billDebtorRepository.findByBill_Id(bill.getId());

		assertThat(savedBill.getGroup().getId()).isEqualTo(group.getId());
		assertThat(savedBill.getPayer().getId()).isEqualTo(CREATOR_ID);
		assertThat(savedBill.getAmount()).isEqualByComparingTo("300000.00");
		assertThat(savedBill.getDescription()).isEqualTo("Lunch");
		assertThat(savedBill.getCreatedAt()).isNotNull();
		assertThat(savedDebtors)
				.extracting(debtor -> debtor.getDebtor().getId())
				.containsExactlyInAnyOrder(CREATOR_ID, MEMBER_ID);
		assertThat(savedDebtors)
				.extracting(BillDebtorEntity::getAmount)
				.usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
				.containsExactlyInAnyOrder(new BigDecimal("100000.00"), new BigDecimal("200000.00"));
	}
}
