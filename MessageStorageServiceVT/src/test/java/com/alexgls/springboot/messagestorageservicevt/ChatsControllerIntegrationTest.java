package com.alexgls.springboot.messagestorageservicevt;

import com.alexgls.springboot.messagestorageservicevt.dto.ChatDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
public class ChatsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private KafkaAdmin kafkaAdmin;

    @Test
    void createPrivateChat() throws Exception {
        //given
        int receiverId = 1;
        int senderId = 2;
        ChatDto expectedDto = new ChatDto();
        expectedDto.setChatId(1);
        expectedDto.setGroup(false);
        expectedDto.setType("PRIVATE");
        //when
        var response = mockMvc.perform(post("/api/chats/private/{id}", receiverId)
                        .with((jwt().jwt(builder -> builder.claim("userId", senderId)))))
                .andReturn();
        String responseJson = response.getResponse().getContentAsString();
        ChatDto responseDto = objectMapper.readValue(responseJson, ChatDto.class);

        //then
        assertEquals(expectedDto, responseDto);

    }
}
