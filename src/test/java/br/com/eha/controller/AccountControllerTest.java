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
    }
}
