package com.habitat.api.service;

import com.habitat.api.entity.Application;
import com.habitat.api.entity.ApplicationDocument;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.enums.DocumentType;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.ApplicationDocumentRepository;
import com.habitat.api.repository.ApplicationRepository;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.storage.StorageService;
import com.habitat.api.storage.StoredResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock ApplicationRepository applications;
    @Mock ApplicationDocumentRepository appDocs;
    @Mock StorageService storage;
    @Mock SecurityUtils security;
    @InjectMocks FileService service;

    private static final UUID TENANT_ID  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MANAGER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_ID   = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID APP_ID     = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DOC_ID     = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void owner_tenant_gets_a_download_handle() {
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(applications.findById(APP_ID)).thenReturn(Optional.of(application()));
        when(appDocs.findById(DOC_ID)).thenReturn(Optional.of(document(APP_ID)));
        when(storage.open("documents/abc-id.pdf"))
                .thenReturn(new StoredResource(
                        new ByteArrayInputStream(new byte[]{1}), "application/pdf", 1L));

        FileService.DownloadHandle handle =
                service.openApplicationDocument(APP_ID, DOC_ID);

        assertThat(handle.fileName()).isEqualTo("id.pdf");
        assertThat(handle.resource().mimeType()).isEqualTo("application/pdf");
    }

    @Test
    void property_manager_can_open_a_tenant_document() {
        when(security.requireUserId()).thenReturn(MANAGER_ID);
        when(applications.findById(APP_ID)).thenReturn(Optional.of(application()));
        when(appDocs.findById(DOC_ID)).thenReturn(Optional.of(document(APP_ID)));
        when(storage.open("documents/abc-id.pdf"))
                .thenReturn(new StoredResource(
                        new ByteArrayInputStream(new byte[0]), "application/pdf", 0L));

        FileService.DownloadHandle handle =
                service.openApplicationDocument(APP_ID, DOC_ID);

        assertThat(handle).isNotNull();
    }

    @Test
    void unrelated_caller_is_forbidden() {
        when(security.requireUserId()).thenReturn(OTHER_ID);
        when(applications.findById(APP_ID)).thenReturn(Optional.of(application()));

        assertThatThrownBy(() -> service.openApplicationDocument(APP_ID, DOC_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void missing_application_throws_not_found() {
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(applications.findById(APP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.openApplicationDocument(APP_ID, DOC_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void document_belonging_to_another_application_throws_not_found() {
        UUID otherApp = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(applications.findById(APP_ID)).thenReturn(Optional.of(application()));
        when(appDocs.findById(DOC_ID)).thenReturn(Optional.of(document(otherApp)));

        assertThatThrownBy(() -> service.openApplicationDocument(APP_ID, DOC_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void missing_document_throws_not_found() {
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(applications.findById(APP_ID)).thenReturn(Optional.of(application()));
        when(appDocs.findById(DOC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.openApplicationDocument(APP_ID, DOC_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void manager_check_handles_property_without_a_manager() {
        // Backroom-era listings without a manager set must still resolve
        // the ownership check (tenant only).
        Application app = applicationNoManager();
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(applications.findById(APP_ID)).thenReturn(Optional.of(app));
        when(appDocs.findById(DOC_ID)).thenReturn(Optional.of(document(APP_ID)));
        when(storage.open("documents/abc-id.pdf"))
                .thenReturn(new StoredResource(
                        new ByteArrayInputStream(new byte[0]), "application/pdf", 0L));

        FileService.DownloadHandle handle =
                service.openApplicationDocument(APP_ID, DOC_ID);

        assertThat(handle).isNotNull();
    }

    private static Application application() {
        User tenant = withId(User.builder().firstName("T").surname("X")
                .email("t@example.co.za").build(), TENANT_ID);
        User manager = withId(User.builder().firstName("M").surname("Y")
                .email("m@example.co.za").build(), MANAGER_ID);
        Property property = withId(Property.builder().manager(manager).build(),
                UUID.randomUUID());
        Unit unit = withId(Unit.builder().property(property).build(), UUID.randomUUID());
        return withId(Application.builder().tenant(tenant).unit(unit).build(), APP_ID);
    }

    private static Application applicationNoManager() {
        User tenant = withId(User.builder().firstName("T").surname("X")
                .email("t@example.co.za").build(), TENANT_ID);
        Property property = withId(Property.builder().manager(null).build(),
                UUID.randomUUID());
        Unit unit = withId(Unit.builder().property(property).build(), UUID.randomUUID());
        return withId(Application.builder().tenant(tenant).unit(unit).build(), APP_ID);
    }

    private static ApplicationDocument document(UUID applicationId) {
        Application app = withId(Application.builder().build(), applicationId);
        return withId(ApplicationDocument.builder()
                .application(app)
                .docType(DocumentType.SA_ID)
                .fileUrl("documents/abc-id.pdf")
                .fileName("id.pdf")
                .build(), DOC_ID);
    }

    private static <T> T withId(T entity, UUID id) {
        try {
            Field f = entity.getClass().getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }
}
