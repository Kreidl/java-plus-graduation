package ru.practicum.controller;

import ru.practicum.comment.dto.CommentDto;
import ru.practicum.service.CommentService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentPublicController {
    private final CommentService commentService;

    @GetMapping("/{commentId}")
    public CommentDto getComments(@PathVariable Long commentId) {

        log.info("Get comment by id={} requested", commentId);
        return commentService.getById(commentId);
    }
}
