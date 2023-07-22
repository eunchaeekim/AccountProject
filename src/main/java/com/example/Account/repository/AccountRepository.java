package com.example.Account.repository;

import com.example.Account.domain.Account;
import com.example.Account.domain.AccountStatus;
import com.example.Account.domain.AccountUser;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // Optional<Account>: 조회된 결과가 없을 수 있으므로, Optional을 사용하여 nullable한 값을 다룸
    // 즉, 결과가 없을 경우에도 null을 반환하지 않고, Optional.empty()를 반환

    //indFirstByOrderIdDesc(): 해당 메소드는 메소드 이름을 보고 쿼리를 자동으로 생성
    // findFirstBy는 가장 처음에 매칭되는 엔티티를 반환하고,
    // 정렬 기준은 OrderId로 하여 Desc 내림차순으로 정렬
    // 즉, 가장 최근에 생성된 Account 엔티티를 반환
    Optional<Account> findFirstByOrderByIdDesc();

    // 계좌가 10개(사용자당 최대 보유 가능 계좌 수)인 경우 실패 응답
    Integer countByAccountUser(AccountUser accountUser);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByAccountUser(AccountUser accountUser);


}
