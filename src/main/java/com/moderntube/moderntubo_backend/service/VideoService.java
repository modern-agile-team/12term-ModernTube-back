package com.moderntube.moderntubo_backend.service;

import com.moderntube.moderntubo_backend.cache.ViewCountCache;
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
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
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
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class VideoService {

    private static final String STORAGE_DIR = "./streams";

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final String ffprobePath;
    private final String ffmpegPath;
    private final ViewCountCache viewCountCache;

    public VideoService(
            VideoRepository videoRepository, UserRepository userRepository,
            CommentRepository commentRepository, VideoLikeRepository videoLikeRepository,
            @Value("${app.ffmpeg.ffprobe-path}") String ffprobePath,
            @Value("${app.ffmpeg.ffmpeg-path}") String ffmpegPath,
            ViewCountCache viewCountCache) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.videoLikeRepository = videoLikeRepository;
        this.ffprobePath = ffprobePath;
        this.ffmpegPath = ffmpegPath;
        this.viewCountCache = viewCountCache;
    }

    public VideoUploadResponse uploadVideo(
            MultipartFile file, String title, MultipartFile thumbnailFile,
            Double thumbnailTimestamp, CustomUserDetails currentUser
    ) {
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

        Video video = buildVideo(filename, title, file, thumbnailFile, thumbnailTimestamp, targetPath, currentUser);
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

    private Video buildVideo(
            String filename, String title, MultipartFile file, MultipartFile thumbnailFile,
            Double thumbnailTimestamp, Path videoPath, CustomUserDetails currentUser
    ) {
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
            video.setDuration(result.getFormat().duration);

            String thumbnailPath = resolveThumbnail(thumbnailFile, thumbnailTimestamp, result.getFormat().duration, videoPath, filename);
            video.setThumbnailPath(thumbnailPath);

            return video;
        } catch (IOException err) {
            log.error("메타데이터 추출 실패: {}", filename, err);
            throw new UploadException("메타데이터 추출 중 오류가 발생했습니다.");
        }
    }

    private String resolveThumbnail(
            MultipartFile thumbnailFile, Double timestamp,
            double duraion, Path videoPath, String baseFilename
    ) throws IOException {
        String thumbnailFilename = baseFilename + "_thumb.jpg";
        Path thumbnailPath = Paths.get(STORAGE_DIR, thumbnailFilename);

        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            if (thumbnailFile.getContentType() == null ||
                    !thumbnailFile.getContentType().startsWith("image/")) {
                throw new UploadException("썸네일은 이미지 파일만 가능합니다.");
            }

            thumbnailFile.transferTo(thumbnailPath);
            return thumbnailPath.toString();
        }

        double ts = (timestamp != null) ? timestamp : 1.0;
        if (ts < 0 || ts > duraion) {
            throw new UploadException("썸네일 시점이 영상 길이를 벗어났습니다.");
        }

        FFmpeg fFmpeg = new FFmpeg(ffmpegPath);
        FFprobe fFprobe = new FFprobe(ffprobePath);
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(videoPath.toString())
                .done().overrideOutputFiles(true)
                .addOutput(thumbnailPath.toString())
                .setStartOffset((long) (ts * 1000), TimeUnit.MILLISECONDS)
                .addExtraArgs("-vframes", "1")
                .done();
        new FFmpegExecutor(fFmpeg, fFprobe).createJob(builder).run();

        return thumbnailPath.toString();
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

    public VideoDetailResponse getVideoDetail(Long videoId, CustomUserDetails currentUser, String clientIp) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));

        boolean isNewView = (currentUser != null)
                ? viewCountCache.isNewView(currentUser.getId(), videoId)
                : viewCountCache.isNewView(clientIp, videoId);

        if (isNewView) {
            video.setViewCount(video.getViewCount() + 1);
            videoRepository.save(video);
        }

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

    private VideoListResponse toListResponse(Video video) {
        String thumbnailUrl = video.getThumbnailPath() != null
                ? "/api/videos/" + video.getVideoId() + "/thumbnail"
                : null;

        return new VideoListResponse(
                video.getVideoId(),
                video.getVideoName(),
                thumbnailUrl,
                video.getUploader().getName(),
                video.getViewCount(),
                video.getCreatedAt()
        );
    }

    public Resource getThumbnailResource(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));

        if (video.getThumbnailPath() == null) {
            throw new ResourceNotFoundException("Thumbnail", "videoId", videoId);
        }

        return new FileSystemResource(video.getThumbnailPath());
    }

    public PagedResponse<VideoListResponse> getVideoList(int page, int size) {
        ValidatePageNumberAndSize.validatePageNumberAndSize(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        // 페이지 목록을 가져올때 isHidden이 false인것과 isActive가 true인것을 가져오라는뜻.
        Page<Video> videos = videoRepository.findByIsHiddenFalseAndIsActiveTrue(pageable);
        List<VideoListResponse> content = videos.map(this::toListResponse).getContent();

        return new PagedResponse<>(content, videos.getNumber(), videos.getSize(),
                videos.getTotalElements(), videos.getTotalPages(), videos.isLast());
    }
}
