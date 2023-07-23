package com.example.Account.service;

import com.example.Account.domain.Account;
import com.example.Account.domain.AccountUser;
import com.example.Account.dto.AccountDto;
import com.example.Account.exception.AccountException;
import com.example.Account.repository.AccountRepository;
import com.example.Account.repository.AccountUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import static com.example.Account.dto.AccountDto.fromEntity;
import static com.example.Account.type.AccountStatus.IN_USE;
import static com.example.Account.type.AccountStatus.UNREGISTERED;
import static com.example.Account.type.ErrorCode.*;

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
        AccountUser accountUser = getAccountUser(userId);

        validateCreateAccount(accountUser);

        String newAccountNumber = createAccountNums();

        return fromEntity(
                accountRepository.save(Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(IN_USE)
                        .accountNumber(newAccountNumber)
                        .balance(initialBalance)
                        .registeredAt(LocalDateTime.now())
                        .build())
        );
    }

    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = getAccountUser(userId);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }

    @Transactional
    public List<AccountDto> getAccountByUserId(Long userId) {
        AccountUser accountUser = getAccountUser(userId);

        List<Account> accounts = accountRepository
                .findByAccountUser(accountUser);

        return accounts.stream()
                .map(account -> AccountDto.fromEntity(account))
                .collect(Collectors.toList());
    }


    private AccountUser getAccountUser(Long userId) {
        return accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
    }

    private void validateCreateAccount(AccountUser accountUser) {
        if (accountRepository.countByAccountUser(accountUser) >= 10) {
            throw new AccountException(MAX_ACCOUNT_PER_USER_10);
        }
    }

    private String createAccountNums() {
        Random random = new Random();
        String accountNums = "";
        do {
            for (int i = 0; i < 10; i++) {
                accountNums += String.valueOf(random.nextInt(9));
            }

            System.out.println(accountNums);
        } while (accountRepository.existsAccountByAccountNumber(accountNums));

        return accountNums;
    }

    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }

        if (account.getAccountStatus() == UNREGISTERED) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }

        if (account.getBalance() > 0) {
            throw new AccountException(BALANCE_NOT_EMPTY);
        }
    }
}
