package com.Edo_perfume.ScentOfASA.web;

import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicPageController {

    @GetMapping("/")
    public String home(Locale locale) {
        return "redirect:" + localizedStaticPage("index", locale);
    }

    @GetMapping("/about")
    public String about(Locale locale) {
        return "redirect:" + localizedStaticPage("about", locale);
    }

    @GetMapping("/booking")
    public String booking(Locale locale) {
        return "redirect:" + localizedStaticPage("booking", locale);
    }

    @GetMapping("/confirmation")
    public String confirmation(Locale locale) {
        return "redirect:" + localizedStaticPage("confirmation", locale);
    }

    private String localizedStaticPage(String pageName, Locale locale) {
        boolean japanese = Locale.JAPANESE.getLanguage().equals(locale.getLanguage());
        return japanese ? "/public/" + pageName + "-ja.html" : "/public/" + pageName + ".html";
    }
}
