package com.moderntube.moderntubo_backend.service;

import com.moderntube.moderntubo_backend.exception.UploadException;
import com.moderntube.moderntubo_backend.model.CustomUserDetails;
import com.moderntube.moderntubo_backend.model.Video;
import com.moderntube.moderntubo_backend.model.payload.response.VideoUploadResponse;
import com.moderntube.moderntubo_backend.repository.UserRepository;
import com.moderntube.moderntubo_backend.repository.VideoRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.shared.CodecType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@AllArgsConstructor
public class VideoService {

    private static final String STORAGE_DIR = "./streams";

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    public VideoUploadResponse uploadVideo(MultipartFile file, CustomUserDetails currentUser) {
        if (file.isEmpty()) {
            throw new UploadException("파일이 없습니다.");
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

        Video video = buildVideo(filename, file, targetPath, currentUser);
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

    private Video buildVideo(String filename, MultipartFile file, Path videoPath, CustomUserDetails currentUser) {
        try {
            FFprobe ffprobe = new FFprobe("/opt/homebrew/bin/ffprobe");
            FFmpegProbeResult result = ffprobe.probe(videoPath.toString());

            FFmpegStream videoStream = result.getStreams().stream()
                    .filter(s -> s.codec_type == CodecType.VIDEO)
                    .findFirst()
                    .orElseThrow(() -> new UploadException("영상 스트림을 찾을수 없습니다."));

            Video video = new Video();
            video.setVideoName(filename);
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
}
