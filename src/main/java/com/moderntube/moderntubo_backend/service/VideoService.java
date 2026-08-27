package com.moderntube.moderntubo_backend.service;

import com.moderntube.moderntubo_backend.exception.ResourceNotFoundException;
import com.moderntube.moderntubo_backend.exception.UploadException;
import com.moderntube.moderntubo_backend.model.CustomUserDetails;
import com.moderntube.moderntubo_backend.model.Video;
import com.moderntube.moderntubo_backend.model.VideoLike;
import com.moderntube.moderntubo_backend.model.payload.response.PagedResponse;
import com.moderntube.moderntubo_backend.model.payload.response.VideoDetailResponse;
import com.moderntube.moderntubo_backend.model.payload.response.VideoListResponse;
import com.moderntube.moderntubo_backend.model.payload.response.VideoUploadResponse;
import com.moderntube.moderntubo_backend.repository.CommentRepository;
import com.moderntube.moderntubo_backend.repository.UserRepository;
import com.moderntube.moderntubo_backend.repository.VideoLikeRepository;
import com.moderntube.moderntubo_backend.repository.VideoRepository;
import com.moderntube.moderntubo_backend.util.ValidatePageNumberAndSize;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.shared.CodecType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Slf4j
public class VideoService {

    private static final String STORAGE_DIR = "./streams";

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final String ffprobePath;

    public VideoService(
            VideoRepository videoRepository, UserRepository userRepository,
            CommentRepository commentRepository, VideoLikeRepository videoLikeRepository,
            @Value("${app.ffmpeg.ffprobe-path}") String ffprobePath
    ) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.videoLikeRepository = videoLikeRepository;
        this.ffprobePath = ffprobePath;
    }

    public VideoUploadResponse uploadVideo(MultipartFile file, String title, CustomUserDetails currentUser) {
        if (file.isEmpty()) {
            throw new UploadException("파일이 없습니다.");
        }

        if (title == null || title.isBlank()) {
            throw new UploadException("제목을 입력해주세요.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new UploadException("동영상 파일만 업로드가 가능합니다.");
        }

        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path targetPath = Paths.get(STORAGE_DIR, filename);

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException error) {
            log.error("동영상 업로드 실패: {}", String.valueOf(error));
            throw new UploadException("업로드 중 오류가 발생했습니다.");
        }

        log.info("동영상 업로드 완료: {}", filename);

        Video video = buildVideo(filename, title, file, targetPath, currentUser);
        Video savedVideo = videoRepository.save(video);

        return new VideoUploadResponse(
                savedVideo.getVideoId(),
                savedVideo.getVideoName(),
                savedVideo.getDuration(),
                savedVideo.getVideoWidth(),
                savedVideo.getVideoHeight(),
                savedVideo.getCodec(),
                savedVideo.getBitrate()
        );
    }

    private Video buildVideo(String filename, String title, MultipartFile file, Path videoPath, CustomUserDetails currentUser) {
        try {
            FFprobe ffprobe = new FFprobe(ffprobePath);
            FFmpegProbeResult result = ffprobe.probe(videoPath.toString());

            FFmpegStream videoStream = result.getStreams().stream()
                    .filter(s -> s.codec_type == CodecType.VIDEO)
                    .findFirst()
                    .orElseThrow(() -> new UploadException("영상 스트림을 찾을수 없습니다."));

            Video video = new Video();
            video.setVideoName(title);
            video.setOriginalFilename(file.getOriginalFilename());
            video.setFilePath(videoPath.toString());
            video.setUploader(userRepository.getReferenceById(currentUser.getId()));
            video.setVideoSize(file.getSize());
            video.setVideoWidth(videoStream.width);
            video.setVideoHeight(videoStream.height);
            video.setDuration(result.getFormat().duration);
            video.setBitrate(result.getFormat().bit_rate);
            video.setCodec(videoStream.codec_name);
            video.setHidden(false);
            video.setActive(true);

            return video;
        } catch (IOException err) {
            log.error("메타데이터 추출 실패: {}", filename, err);
            throw new UploadException("메타데이터 추출 중 오류가 발생했습니다.");
        }
    }

    public ResourceRegion createRegion(Long id, HttpHeaders headers) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", id));

        Resource videoResource = new FileSystemResource(video.getFilePath());

        long contentLength;
        try {
            contentLength = videoResource.contentLength();
        } catch (IOException e) {
            throw new ResourceNotFoundException("Video", "id", id);
        }

        return headers.getRange().stream().findFirst()
                .map(range -> range.toResourceRegion(videoResource))
                .orElseGet(() -> new ResourceRegion(videoResource, 0, Math.min(1_000_000, contentLength)));
    }

    public VideoDetailResponse getVideoDetail(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));

        video.setViewCount(video.getViewCount() + 1);
        videoRepository.save(video);

        long likeCount = videoLikeRepository.countByVideo_VideoId(videoId);
        long commentCount = commentRepository.countByVideo_VideoId(videoId);

        VideoUploadResponse videoInfo = new VideoUploadResponse(
                video.getVideoId(),
                video.getVideoName(),
                video.getDuration(),
                video.getVideoWidth(),
                video.getVideoHeight(),
                video.getCodec(),
                video.getBitrate()
        );

        return new VideoDetailResponse(
                video.getVideoName(),
                video.getUploader().getName(),
                videoInfo,
                video.getViewCount(),
                likeCount,
                commentCount,
                false // myLike — 로그인 여부에 따라 나중에 채우면 됨
        );
    }

    /**
     * 좋아요 토글 (이미 눌렀으면 취소, 안 눌렀으면 등록)
     * @param videoId 좋아요를 누를 동영상 ID
     * @param currentUser 요청 사용자
     * @return 처리 후 좋아요 상태 (true = 눌림, false = 취소됨)
     */
    public boolean toggleLike(Long videoId, CustomUserDetails currentUser) {
        Long userId = currentUser.getId();

        if (videoLikeRepository.existsByVideo_VideoIdAndUser_Id(videoId, userId)) {
            videoLikeRepository.deleteByVideo_VideoIdAndUser_Id(videoId, userId);
            return false;
        }

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));
        VideoLike videoLike = new VideoLike(video, userRepository.getReferenceById(userId));
        videoLikeRepository.save(videoLike);
        return true;
    }

    private PagedResponse<VideoListResponse> getVideoList(int page, int size) {
        ValidatePageNumberAndSize.validatePageNumberAndSize(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createAt");
        Page<Video> videos = videoRepository.findByIsHiddenFalseAndIsActiveTrue(pageable);

        return new PagedResponse<>();
    }
}
