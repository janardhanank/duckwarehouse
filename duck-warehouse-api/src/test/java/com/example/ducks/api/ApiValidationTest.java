package com.example.ducks.api;

import com.example.ducks.config.JsonConfiguration;
import com.example.ducks.warehouse.*;
import com.example.ducks.store.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest({WarehouseController.class, StoreController.class})
@Import(JsonConfiguration.class)
class ApiValidationTest {
    @Autowired MockMvc mvc;
    @MockitoBean WarehouseService warehouse;
    @MockitoBean StoreService store;

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"color\":\"Blue\",\"size\":\"Large\",\"price\":10,\"quantity\":1}",
        "{\"color\":\"Red\",\"size\":\"Large\",\"price\":0,\"quantity\":1}",
        "{\"color\":\"Red\",\"size\":\"Large\",\"price\":10.001,\"quantity\":1}",
        "{\"color\":\"Red\",\"size\":\"Large\",\"price\":10,\"quantity\":0}",
        "{\"color\":\"Red\",\"size\":\"Large\",\"price\":10,\"quantity\":1.5}",
        "{\"color\":\"Red\",\"size\":\"Large\",\"price\":10,\"quantity\":\"1\"}",
        "{\"color\":\"Red\",\"size\":\"Large\",\"price\":10,\"quantity\":2147483648}",
        "{\"color\":\"Red\",\"size\":\"Large\",\"price\":10,\"quantity\":1,\"deleted\":true}",
        "{\"color\":\"Red\",\"size\":\"Large\",\"price\":10,\"quantity\":1,\"quantity\":2}"
    })
    void rejectsInvalidAddsBeforeCallingService(String json) throws Exception {
        mvc.perform(post("/api/ducks").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").exists());
        verifyNoInteractions(warehouse);
    }
    @Test void rejectsEditingColorOrSize() throws Exception {
        mvc.perform(put("/api/ducks/1").contentType(MediaType.APPLICATION_JSON)
            .content("{\"price\":10,\"quantity\":2,\"color\":\"Green\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(warehouse);
    }
    @Test void rejectsOrderPriceAndInvalidId() throws Exception {
        mvc.perform(post("/api/store/orders").contentType(MediaType.APPLICATION_JSON)
            .content("{\"color\":\"Red\",\"size\":\"Large\",\"quantity\":1,\"destinationCountry\":\"USA\",\"shippingMode\":\"Air\",\"price\":1}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/ducks/0")).andExpect(status().isBadRequest());
        verifyNoInteractions(store, warehouse);
    }
    @Test void permitsValidAddAndZeroStockEdit() throws Exception {
        mvc.perform(post("/api/ducks").contentType(MediaType.APPLICATION_JSON)
            .content("{\"color\":\"Red\",\"size\":\"Large\",\"price\":\"10.00\",\"quantity\":1}"))
            .andExpect(status().isOk());
        mvc.perform(put("/api/ducks/1").contentType(MediaType.APPLICATION_JSON)
            .content("{\"price\":10,\"quantity\":0}"))
            .andExpect(status().isOk());
        verify(warehouse).add(any(AddDuckRequest.class));
        verify(warehouse).edit(eq(1), any(EditDuckRequest.class));
    }
}
