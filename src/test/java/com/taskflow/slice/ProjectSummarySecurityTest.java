package com.taskflow.slice;

import com.taskflow.controller.ProjectController;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc
class ProjectSummarySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private com.taskflow.security.JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void getSummary_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/projects/2/summary"))
                .andExpect(status().isUnauthorized());
    }
}
