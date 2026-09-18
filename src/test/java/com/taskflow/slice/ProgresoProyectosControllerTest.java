package com.taskflow.slice;

import com.taskflow.controller.ReportController;
import com.taskflow.dto.ProjectProgressResponse;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProgresoProyectosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getProgress_devuelve200ConCadaCampo() throws Exception {
        when(projectService.progresoPorProyecto()).thenReturn(List.of(
                new ProjectProgressResponse(1L, "Plataforma TaskFlow", 5, 1, 20.0),
                new ProjectProgressResponse(2L, "App Móvil", 3, 1, 33.3)
        ));

        mockMvc.perform(get("/reports/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(1))
                .andExpect(jsonPath("$[0].percentDone").value(20.0))
                .andExpect(jsonPath("$[1].totalTasks").value(3));
    }
}
