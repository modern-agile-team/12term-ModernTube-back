package com.moderntube.moderntubo_backend.service;

import com.moderntube.moderntubo_backend.exception.ResourceNotFoundException;
import com.moderntube.moderntubo_backend.model.Comment;
import com.moderntube.moderntubo_backend.model.CustomUserDetails;
import com.moderntube.moderntubo_backend.model.Video;
import com.moderntube.moderntubo_backend.model.payload.response.CommentResponse;
import com.moderntube.moderntubo_backend.model.payload.response.PagedResponse;
import com.moderntube.moderntubo_backend.repository.CommentRepository;
import com.moderntube.moderntubo_backend.repository.UserRepository;
import com.moderntube.moderntubo_backend.repository.VideoRepository;
import com.moderntube.moderntubo_backend.util.ValidatePageNumberAndSize;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    /**
     * 특정 동영상의 댓글 목록을 페이징으로 조회
     * @param videoId 조회할 동영상 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 댓글 목록을 담은 PagedResponse
     */
    public PagedResponse<CommentResponse> getComments(Long videoId, int page, int size) {
        ValidatePageNumberAndSize.validatePageNumberAndSize(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Comment> comments = commentRepository.findByVideo_VideoId(videoId, pageable);

        List<CommentResponse> content = comments.map(this::toResponse).getContent();

        return new PagedResponse<>(content, comments.getNumber(), comments.getSize(),
                comments.getTotalElements(), comments.getTotalPages(), comments.isLast());
    }

    /**
     * 특정 동영상에 댓글 작성
     * @param videoId 댓글을 달 동영상 ID
     * @param currentUser 작성자
     * @param content 댓글 내용
     */
    public void addComment(Long videoId, CustomUserDetails currentUser, String content) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));

        Comment comment = new Comment();
        comment.setVideo(video);
        comment.setUser(userRepository.getReferenceById(currentUser.getId()));
        comment.setContent(content);

        commentRepository.save(comment);
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getId(),
                comment.getUser().getName(),
                comment.getCreatedAt()
        );
    }
}
