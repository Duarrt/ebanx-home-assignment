package br.com.eha.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void resetState() throws Exception {
        mockMvc.perform(post("/reset"));
    }

    private ResultActions postEvent(String body) throws Exception {
        return mockMvc.perform(post("/event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void resetReturnsOk() throws Exception {
        mockMvc.perform(post("/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Nested
    class Balance {

        @Test
        void nonExistingAccountReturns404WithZero() throws Exception {
            mockMvc.perform(get("/balance").param("account_id", "1234"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("0"));
        }

        @Test
        void existingAccountReturnsBalance() throws Exception {
            postEvent("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":10}");

            mockMvc.perform(get("/balance").param("account_id", "100"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("10"));
        }
    }

    @Nested
    class Deposit {

        @Test
        void createsAccountWithInitialBalance() throws Exception {
            postEvent("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":10}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.destination.id").value("100"))
                    .andExpect(jsonPath("$.destination.balance").value(10));
        }

        @Test
        void addsToExistingAccount() throws Exception {
            postEvent("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":10}");

            postEvent("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":10}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.destination.id").value("100"))
                    .andExpect(jsonPath("$.destination.balance").value(20));
        }

        @Test
        void negativeAndZeroAmount() throws Exception {
            postEvent("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":10}");

            postEvent("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":-10}")
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("0"));

            postEvent("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":0}")
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("0"));

            postEvent("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":\"\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("0"));
        }
    }

    @Nested
    class Withdraw {

        @Test
        void fromNonExistingAccountReturns404WithZero() throws Exception {
            postEvent("{\"type\":\"withdraw\",\"origin\":\"200\",\"amount\":10}")
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("0"));
        }

        @Test
        void fromExistingAccountReturnsUpdatedBalance() throws Exception {
            postEvent("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":20}");

            postEvent("{\"type\":\"withdraw\",\"origin\":\"100\",\"amount\":5}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.origin.id").value("100"))
                    .andExpect(jsonPath("$.origin.balance").value(15));
        }

        @Test
        void withInsufficientFundsReturns422() throws Exception {
            postEvent("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":10}");

            postEvent("{\"type\":\"withdraw\",\"origin\":\"100\",\"amount\":50}")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().string("0"));
        }
    }

    @Nested
    class Transfer {

        @Test
        void betweenExistingAccounts() throws Exception {
            postEvent("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":15}");

            postEvent("{\"type\":\"transfer\",\"origin\":\"100\",\"destination\":\"300\",\"amount\":15}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.origin.id").value("100"))
                    .andExpect(jsonPath("$.origin.balance").value(0))
                    .andExpect(jsonPath("$.destination.id").value("300"))
                    .andExpect(jsonPath("$.destination.balance").value(15));
        }

        @Test
        void fromNonExistingOriginReturns404WithZero() throws Exception {
            postEvent("{\"type\":\"transfer\",\"origin\":\"200\",\"destination\":\"300\",\"amount\":15}")
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("0"));
        }

        @Test
        void withInsufficientFundsReturns422AndDoesNotMoveMoney() throws Exception {
            postEvent("{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":10}");

            postEvent("{\"type\":\"transfer\",\"origin\":\"100\",\"destination\":\"300\",\"amount\":50}")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().string("0"));

            // origin keeps its balance...
            mockMvc.perform(get("/balance").param("account_id", "100"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("10"));

            // ...and destination was never created
            mockMvc.perform(get("/balance").param("account_id", "300"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class EventTypeParsing {

        @Test
        void acceptsUppercaseType() throws Exception {
            postEvent("{\"type\":\"DEPOSIT\",\"destination\":\"100\",\"amount\":10}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.destination.balance").value(10));
        }

        @Test
        void acceptsMixedCaseType() throws Exception {
            postEvent("{\"type\":\"Deposit\",\"destination\":\"100\",\"amount\":10}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.destination.balance").value(10));
        }

        @Test
        void rejectsUnknownTypeWithBadRequest() throws Exception {
            postEvent("{\"type\":\"foo\",\"destination\":\"100\",\"amount\":10}")
                    .andExpect(status().isBadRequest());
        }
    }
}
