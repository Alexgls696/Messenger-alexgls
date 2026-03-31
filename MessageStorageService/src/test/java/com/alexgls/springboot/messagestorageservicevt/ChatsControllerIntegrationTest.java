package com.alexgls.springboot.messagestorageservicevt;


import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;



@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
public class ChatsControllerIntegrationTest {

    /*@Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthHttpClient authHttpClient;

    @MockitoBean
    private KafkaAdmin kafkaAdmin;

    @Test
    void createPrivateChat_WhenChatCreateRequest_ReturnsNewChatDto() throws Exception {
        //given
        int receiverId = 1;
        int senderId = 2;
        ChatDto expectedDto = new ChatDto();
        expectedDto.setChatId(1);
        expectedDto.setGroup(false);
        expectedDto.setType("PRIVATE");
        //when
        var createResponse = mockMvc.perform(post("/api/chats/private/{id}", receiverId)
                        .with((jwt().jwt(builder -> builder.claim("userId", senderId)))))
                .andReturn();
        String responseJson = createResponse.getResponse().getContentAsString();
        ChatDto responseDto = objectMapper.readValue(responseJson, ChatDto.class);

        //then
        assertEquals(expectedDto, responseDto);
    }

    @Test
    void createPrivateChat_WhenChatCreateRequestIfChatExists_ShouldReturnsExistedChat() throws Exception {
        //given
        int receiverId = 1;
        int senderId = 2;
        ChatDto expectedDto = new ChatDto();
        expectedDto.setChatId(1);
        expectedDto.setGroup(false);
        expectedDto.setType("PRIVATE");
        //when
        var createResponse = mockMvc.perform(post("/api/chats/private/{id}", receiverId)
                        .with((jwt().jwt(builder -> builder.claim("userId", senderId)))))
                .andReturn();
        String responseJson = createResponse.getResponse().getContentAsString();
        ChatDto responseDto = objectMapper.readValue(responseJson, ChatDto.class);

        var newCreateChatResponse = mockMvc.perform(post("/api/chats/private/{id}", senderId)
                        .with((jwt().jwt(builder -> builder.claim("userId", receiverId)))))
                .andReturn();

        String responseJson2 = newCreateChatResponse.getResponse().getContentAsString();
        ChatDto responseDto2 = objectMapper.readValue(responseJson2, ChatDto.class);

        //then
        assertEquals(expectedDto, responseDto);
        assertEquals(responseDto, responseDto2);
    }*/
}
