package com.habitat.api.service;

import com.habitat.api.dto.user.BankAccountResponse;
import com.habitat.api.dto.user.UpsertBankAccountRequest;
import com.habitat.api.entity.BankAccount;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.BankAccountType;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.BankAccountRepository;
import com.habitat.api.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {

    @Mock BankAccountRepository accounts;
    @Mock PropertyRepository properties;
    @Mock PropertyService propertyService;
    @InjectMocks BankAccountService service;

    private static final UUID PROPERTY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    // ── getForProperty ───────────────────────────────────────────────

    @Test
    void getForProperty_returns_empty_when_no_row_exists() {
        Property p = property();
        when(properties.findById(PROPERTY_ID)).thenReturn(Optional.of(p));
        when(accounts.findByProperty_Id(PROPERTY_ID)).thenReturn(Optional.empty());

        Optional<BankAccountResponse> out = service.getForProperty(PROPERTY_ID);

        assertThat(out).isEmpty();
        verify(propertyService).requireCanEdit(p);
    }

    @Test
    void getForProperty_returns_row_when_one_exists() {
        Property p = property();
        BankAccount row = bankRow(p);
        when(properties.findById(PROPERTY_ID)).thenReturn(Optional.of(p));
        when(accounts.findByProperty_Id(PROPERTY_ID)).thenReturn(Optional.of(row));

        Optional<BankAccountResponse> out = service.getForProperty(PROPERTY_ID);

        assertThat(out).isPresent();
        assertThat(out.get().bankName()).isEqualTo("FNB");
        assertThat(out.get().accountHolder()).isEqualTo("Thandi Vilakazi");
    }

    @Test
    void getForProperty_throws_when_property_missing() {
        when(properties.findById(PROPERTY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForProperty(PROPERTY_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(accounts, never()).findByProperty_Id(any());
    }

    @Test
    void getForProperty_throws_when_caller_cannot_edit_property() {
        Property p = property();
        when(properties.findById(PROPERTY_ID)).thenReturn(Optional.of(p));
        doThrow(new ForbiddenException("nope")).when(propertyService).requireCanEdit(p);

        assertThatThrownBy(() -> service.getForProperty(PROPERTY_ID))
                .isInstanceOf(ForbiddenException.class);
        verify(accounts, never()).findByProperty_Id(any());
    }

    // ── upsertForProperty ────────────────────────────────────────────

    @Test
    void upsertForProperty_creates_new_row_when_none_exists() {
        Property p = property();
        when(properties.findById(PROPERTY_ID)).thenReturn(Optional.of(p));
        when(accounts.findByProperty_Id(PROPERTY_ID)).thenReturn(Optional.empty());
        when(accounts.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        BankAccountResponse out = service.upsertForProperty(PROPERTY_ID, request());

        ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);
        verify(accounts).save(captor.capture());
        BankAccount saved = captor.getValue();
        assertThat(saved.getProperty()).isSameAs(p);
        assertThat(saved.getBankName()).isEqualTo("FNB");
        assertThat(saved.getAccountHolder()).isEqualTo("Thandi Vilakazi");
        assertThat(saved.getAccountNumber()).isEqualTo("62000111222");
        assertThat(saved.getAccountType()).isEqualTo(BankAccountType.CHEQUE);
        assertThat(saved.getBranchCode()).isEqualTo("250655");
        assertThat(saved.getVatNumber()).isEqualTo("4123456789");
        assertThat(out.bankName()).isEqualTo("FNB");
    }

    @Test
    void upsertForProperty_overwrites_existing_row_in_place() {
        Property p = property();
        BankAccount existing = bankRow(p);
        when(properties.findById(PROPERTY_ID)).thenReturn(Optional.of(p));
        when(accounts.findByProperty_Id(PROPERTY_ID)).thenReturn(Optional.of(existing));
        when(accounts.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        UpsertBankAccountRequest req = new UpsertBankAccountRequest(
                "ABSA", "New Holder", "63111222333", BankAccountType.SAVINGS, "632005", null);

        service.upsertForProperty(PROPERTY_ID, req);

        assertThat(existing.getBankName()).isEqualTo("ABSA");
        assertThat(existing.getAccountHolder()).isEqualTo("New Holder");
        assertThat(existing.getAccountNumber()).isEqualTo("63111222333");
        assertThat(existing.getAccountType()).isEqualTo(BankAccountType.SAVINGS);
        assertThat(existing.getBranchCode()).isEqualTo("632005");
        assertThat(existing.getVatNumber()).isNull();
        verify(accounts).save(existing);
    }

    @Test
    void upsertForProperty_throws_when_property_missing() {
        when(properties.findById(PROPERTY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsertForProperty(PROPERTY_ID, request()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(accounts, never()).save(any());
    }

    @Test
    void upsertForProperty_throws_when_caller_cannot_edit_property() {
        Property p = property();
        when(properties.findById(PROPERTY_ID)).thenReturn(Optional.of(p));
        doThrow(new ForbiddenException("nope")).when(propertyService).requireCanEdit(p);

        assertThatThrownBy(() -> service.upsertForProperty(PROPERTY_ID, request()))
                .isInstanceOf(ForbiddenException.class);
        verify(accounts, never()).save(any());
    }

    // ── fixtures ─────────────────────────────────────────────────────

    private static UpsertBankAccountRequest request() {
        return new UpsertBankAccountRequest(
                "FNB", "Thandi Vilakazi", "62000111222",
                BankAccountType.CHEQUE, "250655", "4123456789");
    }

    private static Property property() {
        Property p = Property.builder().title("Vilakazi St").build();
        return withId(p, PROPERTY_ID);
    }

    private static BankAccount bankRow(Property p) {
        return BankAccount.builder()
                .property(p)
                .bankName("FNB")
                .accountHolder("Thandi Vilakazi")
                .accountNumber("62000111222")
                .accountType(BankAccountType.CHEQUE)
                .branchCode("250655")
                .vatNumber("4123456789")
                .build();
    }

    private static <T> T withId(T entity, UUID id) {
        try {
            Field f = BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }
}
