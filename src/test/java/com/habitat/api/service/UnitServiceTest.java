package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.property.UnitResponse;
import com.habitat.api.dto.property.UpdateUnitRequest;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.enums.PaymentFrequency;
import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.enums.PropertyType;
import com.habitat.api.enums.Role;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.enums.UnitType;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.UnitRepository;
import com.habitat.api.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

    @Mock UnitRepository units;
    @Mock PropertyService properties;
    @Mock SecurityUtils security;
    @InjectMocks UnitService service;

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID UNIT_ID  = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");

    @Test
    void update_applies_only_non_null_fields() {
        Unit u = unitWith(new BigDecimal("12000"), 1, 1, UnitStatus.AVAILABLE);
        when(units.findById(UNIT_ID)).thenReturn(Optional.of(u));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        UnitResponse out = service.update(UNIT_ID, new UpdateUnitRequest(
                null, "Renamed Unit", null, null, null, null,
                new BigDecimal("18000"), null, null,
                2, null, null, null,
                Boolean.TRUE, null, null, null
        ));

        assertThat(u.getTitle()).isEqualTo("Renamed Unit");
        assertThat(u.getPrice()).isEqualByComparingTo("18000");
        assertThat(u.getBedrooms()).isEqualTo(2);
        assertThat(u.getWaterIncluded()).isTrue();
        assertThat(out.bedrooms()).isEqualTo(2);
    }

    @Test
    void update_throws_when_property_not_owned() {
        Unit u = unitWith(new BigDecimal("12000"), 1, 1, UnitStatus.AVAILABLE);
        when(units.findById(UNIT_ID)).thenReturn(Optional.of(u));
        org.mockito.Mockito.doThrow(new ForbiddenException(ErrorMessages.FORBIDDEN))
                .when(properties).requireCanEdit(u.getProperty());

        assertThatThrownBy(() -> service.update(UNIT_ID, new UpdateUnitRequest(
                null, "x", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_throws_when_unit_missing() {
        when(units.findById(UNIT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(UNIT_ID, new UpdateUnitRequest(
                null, "x", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_soft_deletes_for_owner() {
        Unit u = unitWith(new BigDecimal("12000"), 1, 1, UnitStatus.AVAILABLE);
        when(units.findById(UNIT_ID)).thenReturn(Optional.of(u));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        service.delete(UNIT_ID);

        assertThat(u.getDeletedAt()).isNotNull();
    }

    @Test
    void delete_throws_when_not_owner() {
        Unit u = unitWith(new BigDecimal("12000"), 1, 1, UnitStatus.AVAILABLE);
        when(units.findById(UNIT_ID)).thenReturn(Optional.of(u));
        org.mockito.Mockito.doThrow(new ForbiddenException(ErrorMessages.FORBIDDEN))
                .when(properties).requireCanEdit(u.getProperty());

        assertThatThrownBy(() -> service.delete(UNIT_ID))
                .isInstanceOf(ForbiddenException.class);
        assertThat(u.getDeletedAt()).isNull();
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static Unit unitWith(BigDecimal price, int beds, int baths, UnitStatus status) {
        User owner = User.builder()
                .email("u@example.co.za")
                .firstName("U").surname("ser")
                .passwordHash("h")
                .roles(new HashSet<>(List.of(Role.USER)))
                .activeRole(Role.USER)
                .emailVerified(true)
                .build();
        setId(owner, OWNER_ID);

        Landlord ownerLandlord = Landlord.builder()
                .type(LandlordType.ONLINE).user(owner).build();
        Property p = Property.builder()
                .landlord(ownerLandlord)
                .manager(owner)
                .title("Property")
                .propertyType(PropertyType.HOUSE)
                .status(PropertyStatus.LISTED)
                .build();
        setId(p, UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));

        Unit u = Unit.builder()
                .property(p)
                .title("Unit")
                .unitType(UnitType.HOUSE)
                .status(status)
                .price(price)
                .paymentFrequency(PaymentFrequency.MONTHLY)
                .bedrooms(beds)
                .bathrooms(baths)
                .build();
        setId(u, UNIT_ID);
        return u;
    }

    private static void setId(BaseEntity entity, UUID id) {
        try {
            Field f = BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
