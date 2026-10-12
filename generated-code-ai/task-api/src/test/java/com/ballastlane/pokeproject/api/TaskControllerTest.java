package com.ballastlane.pokeproject.api;

import com.ballastlane.pokeproject.bl.TaskBl;
import com.ballastlane.pokeproject.config.SecurityConfig;
import com.ballastlane.pokeproject.dto.CreateTaskRequest;
import com.ballastlane.pokeproject.dto.TaskResponse;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.util.LogSanitizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, LogSanitizer.class})
class TaskControllerTest {

    private static final RequestPostProcessor ASH =
            jwt().jwt(j -> j.subject("ash")).authorities(new SimpleGrantedAuthority("ROLE_TRAINER"));
    private static final TaskResponse TASK = new TaskResponse(3L, "Catch Pikachu", null, "TODO",
            LocalDate.of(2026, 12, 31), 0, Instant.EPOCH, Instant.EPOCH);

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private TaskBl taskBl;

    @Test
    void createReturns201WithLocationAndUsesTheTokenAsOwner() throws Exception {
        when(taskBl.create(eq("ash"), eq(new CreateTaskRequest("Catch Pikachu", null, LocalDate.of(2026, 12, 31)))))
                .thenReturn(TASK);

        mvc.perform(post("/api/v1/tasks").with(ASH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Catch Pikachu\",\"dueDate\":\"2026-12-31\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/tasks/3"))
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.response.status").value("TODO"));
    }

    @Test
    void anOwnerFieldInTheBodyIsRejected() throws Exception {
        mvc.perform(post("/api/v1/tasks").with(ASH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"ownerId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1001"));
        verifyNoInteractions(taskBl);
    }

    @Test
    void missingTitleIs400() throws Exception {
        mvc.perform(post("/api/v1/tasks").with(ASH).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.response.title").exists());
    }

    @Test
    void pastDueDateIs400WithTheFieldMessage() throws Exception {
        when(taskBl.create(eq("ash"), any())).thenThrow(new BallastException(BallastErrorCode.INVALID_TASK,
                "Invalid task", Map.of("dueDate", "cannot be in the past")));

        mvc.perform(post("/api/v1/tasks").with(ASH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"dueDate\":\"2020-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.response.dueDate").value("cannot be in the past"));
    }

    @Test
    void withoutATokenIs401() throws Exception {
        mvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("4001"));
    }

    @Test
    void someoneElsesTaskIs404() throws Exception {
        when(taskBl.get("ash", 99L)).thenThrow(new BallastException(BallastErrorCode.TASK_NOT_FOUND));

        mvc.perform(get("/api/v1/tasks/99").with(ASH)).andExpect(status().isNotFound());
    }

    @Test
    void forbiddenTransitionIs409() throws Exception {
        when(taskBl.update(eq("ash"), eq(3L), any())).thenThrow(new BallastException(BallastErrorCode.INVALID_STATUS_TRANSITION));

        mvc.perform(put("/api/v1/tasks/3").with(ASH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"status\":\"DONE\",\"version\":0}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/v1/tasks/3").with(ASH)).andExpect(status().isNoContent());
    }

    @Test
    void deletingAMissingTaskIs404() throws Exception {
        doThrow(new BallastException(BallastErrorCode.TASK_NOT_FOUND)).when(taskBl).delete(eq("ash"), anyLong());

        mvc.perform(delete("/api/v1/tasks/99").with(ASH)).andExpect(status().isNotFound());
    }
}
