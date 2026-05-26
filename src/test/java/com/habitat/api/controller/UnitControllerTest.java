package com.habitat.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.property.UnitResponse;
import com.habitat.api.dto.property.UpdateUnitRequest;
import com.habitat.api.enums.PaymentFrequency;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.enums.UnitType;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.JwtService;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.TokenBlocklistService;
import com.habitat.api.service.UnitImageService;
import com.habitat.api.service.UnitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UnitController.class)
@AutoConfigureMockMvc(addFilters = false)
class UnitControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean UnitService units;
    @MockBean UnitImageService unitImages;
    @MockBean SecurityUtils security;
    @MockBean JwtService jwtService;
    @MockBean TokenBlocklistService blocklist;
    @MockBean JwtAuthenticationFilter jwtFilter;

    private static final UUID UNIT_ID = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");
    private static final UUID PROP_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void update_returns_200() throws Exception {
        UpdateUnitRequest req = new UpdateUnitRequest(
                null, "Renamed", null, null, null, null,
                new BigDecimal("19000"), null, null,
                3, null, null, null, null, null, null, null
        );
        UnitResponse sample = new UnitResponse(
                UNIT_ID, PROP_ID, null, "Renamed", null, UnitType.APARTMENT, UnitStatus.AVAILABLE,
                null, new BigDecimal("19000"), PaymentFrequency.MONTHLY, null,
                3, 1, null, null, false, false, false, null, List.of()
        );
        when(units.update(eq(UNIT_ID), any(UpdateUnitRequest.class))).thenReturn(sample);

        mvc.perform(patch(ApiRoutes.UNITS + "/" + UNIT_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.price").value(19000));
    }

    @Test
    void delete_returns_204() throws Exception {
        mvc.perform(delete(ApiRoutes.UNITS + "/" + UNIT_ID).with(csrf()))
                .andExpect(status().isNoContent());
        verify(units).delete(eq(UNIT_ID));
    }
}
