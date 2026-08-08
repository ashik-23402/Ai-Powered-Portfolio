package com.ashik.askaboutme.config;

import com.ashik.askaboutme.controller.AskController;
import com.ashik.askaboutme.controller.FileUploadController;
import com.ashik.askaboutme.service.AskService;
import com.ashik.askaboutme.service.FileMetadataService;
import com.ashik.askaboutme.service.FileUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AskController.class, FileUploadController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AskService askService;

    @MockitoBean
    private FileUploadService fileUploadService;

    @MockitoBean
    private FileMetadataService fileMetadataService;

    @Test
    void askIsPublic() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"hello\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void fileUploadRequiresAuthWhenAnonymous() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/files/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fileUploadPassesAuthenticatedRequestsThrough() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/files/initiate")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "change-me"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)));
    }
}
