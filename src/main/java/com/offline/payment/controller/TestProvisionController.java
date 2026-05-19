package com.offline.payment.controller;

import com.offline.payment.model.Account;
import com.offline.payment.model.User;
import com.offline.payment.repository.AccountRepository;
import com.offline.payment.repository.UserRepository;
import com.offline.payment.security.ServerKeyHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestProvisionController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ServerKeyHolder serverKeyHolder;

    public TestProvisionController(UserRepository userRepository,
                                   AccountRepository accountRepository,
                                   ServerKeyHolder serverKeyHolder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.serverKeyHolder = serverKeyHolder;
    }

    /**
     * Simulates a user downloading the app on Wi-Fi and registering with the Bank.
     */
    @PostMapping("/mesh/provision")
    public Map<String, String> provisionDevice(@RequestBody Map<String, String> request) {
        String userVpa = request.get("vpa");
        String userPublicKey = request.get("publicKey");

        // 1. Save Alice's identity and Public Key to the DB
        User alice = new User();
        alice.setVpa(userVpa);
        alice.setPublicKeyBase64(userPublicKey);
        userRepository.save(alice);

        // 2. Give Alice and Bob some starting money using YOUR exact constructor
        Account aliceAccount = new Account(userVpa, "Alice", new BigDecimal("5000.00"));
        accountRepository.save(aliceAccount);

        Account bobAccount = new Account("bob@upi", "Bob", new BigDecimal("1000.00"));
        accountRepository.save(bobAccount);

        // 3. Hand the Bank's real Public Key back to Alice's phone
        return Map.of(
                "status", "Provisioned Successfully",
                "bankPublicKey", serverKeyHolder.getPublicKeyBase64()
        );
    }
}