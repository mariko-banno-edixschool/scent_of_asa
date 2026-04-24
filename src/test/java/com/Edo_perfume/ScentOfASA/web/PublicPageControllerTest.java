package com.Edo_perfume.ScentOfASA.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.Edo_perfume.ScentOfASA.config.WebLocaleConfig;

@WebMvcTest(PublicPageController.class)
@Import(WebLocaleConfig.class)
class PublicPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homeRedirectsToEnglishStaticPage() throws Exception {
        mockMvc.perform(get("/").param("lang", "en"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/public/index.html"));
    }

    @Test
    void aboutRedirectsToJapaneseStaticPageWhenLangJa() throws Exception {
        mockMvc.perform(get("/about").param("lang", "ja"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/public/about-ja.html"));
    }

    @Test
    void bookingRedirectsToJapaneseStaticPageWhenLocaleChanges() throws Exception {
        mockMvc.perform(get("/booking").param("lang", "ja"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/public/booking-ja.html"));
    }

    @Test
    void confirmationRedirectsToJapaneseStaticPage() throws Exception {
        mockMvc.perform(get("/confirmation").param("lang", "ja"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/public/confirmation-ja.html"));
    }
}
