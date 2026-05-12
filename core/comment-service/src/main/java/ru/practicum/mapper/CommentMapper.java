package ru.practicum.mapper;

import java.time.LocalDateTime;

import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.model.Comment;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CommentMapper {

    public CommentDto toCommentDto(Comment comment, Long authorId) {
        return new CommentDto(
                comment.getId(),
                comment.getText(),
                authorId,
                comment.getCreated(),
                comment.isEdited());
    }

    public Comment toEntity(NewCommentDto newCommentDto, Long authorId, Long eventId) {
        return new Comment(
                null, newCommentDto.text(), authorId, eventId, LocalDateTime.now(), false);
    }
}
