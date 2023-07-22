package com.example.Account.service;

import com.example.Account.domain.Account;
import com.example.Account.domain.AccountStatus;
import com.example.Account.domain.AccountUser;
import com.example.Account.dto.AccountDto;
import com.example.Account.exception.AccountException;
import com.example.Account.repository.AccountRepository;
import com.example.Account.repository.AccountUserRepository;
import com.example.Account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.example.Account.domain.AccountStatus.IN_USE;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    /*
     * 가장 최근에 생성된 계좌의 번호를 조회하여,
     * 그 번호에 1을 더하여 새로운 계좌 번호를 생성.  만약 조회된 계좌가 없을 경우 기본적으로 "1000000000"으로 설정.
     * 계좌를 저장하고, 그 정보를 넘긴다.
     */

    // 리턴타입으로 Account가 아닌 AccountDto를 사용하는 이유는,
    // Controller 응답에 있는 데이터가 Account에 있는 정보 중 몇 개만 필요할 수도 있고 더 많은 정보가 필요 할 수도 있음
    // 이와 같이 요구사항이 있을 때 entity를 바꿔서 줄 수는 없음 (entity는 DB table과 1:1 매칭 관계이기 때문)
    // 따라서 entity인 Account를 그대로 넘겨주기 보다는 controller와 service 간 통신하는 별도의 DTO를 만들어 사용 하는 것 추천
    // 또한 레이지 로딩 시 발생하는 트랜잭션 문제도 해결 할 수 있음
    // * Lazy loading : 데이터가 실제로 필요할 때까지 데이터를 로드하는 것을 지연시키는 것으로, 필요한 시점에서 데이터를 로드하여 메모리와 자원을 절약하는 기술
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        validateCreateAccount(accountUser);
        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> (Integer.parseInt(account.getAccountNumber())) + 1 + "")
                .orElse("1000000000");

        return AccountDto.fromEntity(
                accountRepository.save(Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(IN_USE)
                        .accountNumber(newAccountNumber)
                        .balance(initialBalance)
                        .registeredAt(LocalDateTime.now())
                        .build())
        );

    }

    private void validateCreateAccount(AccountUser accountUser) {
        if (accountRepository.countByAccountUser(accountUser)==10){
            throw new AccountException(ErrorCode.MAX_ACCOUNT_PER_USER_10);
        }
    }
    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(AccountStatus.UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        return AccountDto.fromEntity(account);
    }

    private void validateDeleteAccount(AccountUser accountUser, Account account) {

        // 사용자와 계좌의 소유자가 다른 경우
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }

        // 계좌가 이미 해지된 경우
        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }

        // 계좌에 잔액이 남아 있는 경우
        if (account.getBalance() > 0) {
            throw new AccountException(ErrorCode.BALANCE_NOT_EMPTY);
        }
    }
    @Transactional
    public Account getAccount(Long id) {

        if (id < 0) {
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id).get();
    }

}
