package com.yr.talkDice.controller;

import com.yr.talkDice.service.TalkDiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiceController {


    @GetMapping("/dice")
    public String getDice() {
        return TalkDiceService.getTalkDiceStings();
    }
}
